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

package io.fabric8.kubeflix.examples.loanbroker.bank;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class BankController {

    private static final int FALLBACK_SCORE = 500;

    @Value("${base.rate}")
    private float baseRate;

    @Value("${bank.name}")
    private String bankName;

    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping("/quote")
    @HystrixCommand(commandKey = "RequestScoreFromCreditBureau", fallbackMethod = "fallbackQuote", commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "5000")
    })
    public Quote quote(@RequestParam("ssn") Long ssn, @RequestParam("amount") Double amount, @RequestParam("duration") Integer duration) {
        Integer score = restTemplate.getForObject("http://credit-bureau/eval", Integer.class, ssn);
        Double rate = baseRate + (double) (duration / 12) / 10 + (double) (1000 - score) / 1000;
        return new Quote(bankName, rate, amount, duration);
    }

    public Quote fallbackQuote(@RequestParam("ssn") Long ssn, @RequestParam("amount") Double amount, @RequestParam("duration") Integer duration) {
        Double rate = baseRate + (double) (duration / 12) / 10 + (double) (1000 - FALLBACK_SCORE) / 1000;
        return new Quote(bankName, rate, amount, duration);
    }
}
