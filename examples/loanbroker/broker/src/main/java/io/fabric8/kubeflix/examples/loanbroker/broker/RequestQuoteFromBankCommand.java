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

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.utils.URLUtils;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class RequestQuoteFromBankCommand extends HystrixCommand<Quote> {

    private final ClientHttpRequestFactory httpRequestFactory = new SimpleClientHttpRequestFactory();
    private final RestTemplate template = new RestTemplate(httpRequestFactory);

    private final Service service;
    private final Long ssn;
    private final Double amount;
    private final Integer duration;

    protected RequestQuoteFromBankCommand(Service service, Long ssn, Double amount, Integer duration) {
        super(Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(service.getMetadata().getName()))
                .andCommandKey(HystrixCommandKey.Factory.asKey("RequestQuoteFromBank"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionTimeoutInMilliseconds(10000)));
        this.service = service;
        this.ssn = ssn;
        this.amount = amount;
        this.duration = duration;
    }

    @Override
    protected Quote run() throws Exception {
        String url = URLUtils.join(getServiceURL(service), "/quote?ssn=" + ssn + "&duration=" + duration+"&amount="+amount);
        return template.getForEntity(url, Quote.class).getBody();
    }

    private static String getServiceURL(Service service) {
        if (service != null) {
            ServiceSpec spec = service.getSpec();
            if (spec != null) {
                String portalIP = spec.getClusterIP();
                if (portalIP != null) {
                    Integer port = spec.getPorts().iterator().next().getPort();
                    if (port != null && port > 0) {
                        portalIP += ":" + port;
                    }
                    return "http://" + portalIP;
                }
            }
        }
        return null;
    }
}
