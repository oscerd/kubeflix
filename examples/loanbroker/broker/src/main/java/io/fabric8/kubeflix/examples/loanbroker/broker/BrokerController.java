/*
 * Copyright (C) 2015 Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.kubeflix.examples.loanbroker.broker;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@RestController
public class BrokerController {

    private static final String PROJECT_NAME = "project";
    private static final String LOADBALANCER_BANK = "loanbroker-bank";

    private final KubernetesClient client = new DefaultKubernetesClient();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @RequestMapping("/ready")
    public String ready() {
        return "true";
    }

    @Autowired
    private RestTemplate template;

    @Value("${loanbroker.async:false}")
    private Boolean async;

    @RequestMapping("/quote")
    public List<Quote> quote(@RequestParam("ssn") Long ssn, @RequestParam("amount") Double amount, @RequestParam("duration") Integer duration) throws InterruptedException, ExecutionException, TimeoutException {
        HystrixRequestContext context = HystrixRequestContext.initializeContext();
        try {
            if (async) {
                //Broadcast requests async
                List<CompletableFuture<Quote>> futures =
                        client.services().withLabel(PROJECT_NAME, LOADBALANCER_BANK).list().getItems().stream()
                                .map(s -> this.requestQuoteAsync(s, ssn, amount, duration))
                                .collect(Collectors.toList());

                //Collect the results
                CompletableFuture<List<Quote>> result = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
                        .thenApply(v -> futures.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList()));

                return result.get(15, TimeUnit.SECONDS);
            }

            return client.services().withLabel(PROJECT_NAME,LOADBALANCER_BANK).list().getItems().stream()
                    .map(s -> this.requestQuote(s, ssn, amount, duration))
                    .collect(Collectors.toList());
        } finally {
            context.shutdown();
        }
    }

    public CompletableFuture<Quote> requestQuoteAsync(Service service, final Long ssn, final Double amount, final Integer duration) {
        return CompletableFuture.supplyAsync(() -> {
            HystrixRequestContext context = null;
            try {
                context = HystrixRequestContext.initializeContext();
                return new RequestQuoteFromBankCommand(template, service, ssn, amount, duration).execute();
            } finally {
                if (context != null) {
                    context.shutdown();
                }
            }
        }, executorService);
    }

    public Quote requestQuote(Service service, final Long ssn, final Double amount, final Integer duration) {
        try {
            return new RequestQuoteFromBankCommand(template, service, ssn, amount, duration).run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

