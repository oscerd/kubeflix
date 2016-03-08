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

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.generator.annotation.KubernetesModelProcessor;

@KubernetesModelProcessor
public class BankResourceCustomizer {

    private static final String BANK_ID = "bank.id";
    private static final String BANK_NAME = "bank.name";
    private static final String BANK_RATE = "bank.rate";

    private static final String BANK_NAME_ENV = "BANK_NAME";
    private static final String BANK_RATE_ENV = "BASE_RATE";

    public void on(ReplicationControllerBuilder builder) {
        String bankId = System.getProperty(BANK_ID);
        if (bankId != null && !bankId.isEmpty()) {
            builder.editMetadata().withName(bankId).endMetadata();
            builder.editSpec()
                    .addToSelector(BANK_ID, bankId)
                    .editTemplate()
                        .editMetadata()
                            .addToLabels(BANK_ID, bankId)
                        .endMetadata()
                    .endTemplate()
                    .endSpec();
        }
    }

    public void on(ServiceBuilder builder) {
        String bankId = System.getProperty(BANK_ID);
        if (bankId != null && !bankId.isEmpty()) {
            builder.editMetadata().withName(bankId).endMetadata();
            builder.editSpec()
                    .addToSelector(BANK_ID, bankId)
                    .endSpec();
        }
    }

    public void on(ContainerBuilder containerBuilder) {
        String bankName = System.getProperty(BANK_NAME);
        String bankRate = System.getProperty(BANK_RATE);

        if (bankName != null && !bankName.isEmpty()) {
            containerBuilder.addNewEnv().withName(BANK_NAME_ENV).withValue(bankName).endEnv();
        }

        if (bankRate != null && !bankRate.isEmpty()) {
            containerBuilder.addNewEnv().withName(BANK_RATE_ENV).withValue(bankRate).endEnv();
        }
    }
}
