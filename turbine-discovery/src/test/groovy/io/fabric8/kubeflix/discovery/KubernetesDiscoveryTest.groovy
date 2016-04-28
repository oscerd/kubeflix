/*
 * Copyright (C) 2016 Red Hat, Inc
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
 *
 */

package io.fabric8.kubeflix.discovery

import com.netflix.turbine.discovery.Instance
import io.fabric8.kubernetes.api.model.ConfigMapBuilder
import io.fabric8.kubernetes.api.model.EndpointsBuilder
import io.fabric8.kubernetes.api.model.EndpointsListBuilder
import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.server.mock.KubernetesMockServer
import spock.lang.Specification


class KubernetesDiscoveryTest extends Specification {

    private static KubernetesMockServer mockServer = new KubernetesMockServer();
    private static KubernetesClient mockClient;

    def setupSpec() {
        mockServer.init();
        mockClient = mockServer.createClient();

        //Setup configmap data
        Map<String, String> data = new HashMap<>();
        data.put("spring.kubernetes.test.value", "value1")
        mockServer.expect().get().withPath("/api/v1/namespaces/testns/configmaps/testapp").andReturn(200, new ConfigMapBuilder()
                .withData(data)
                .build()).always()

        //Configure the kubernetes master url to point to the mock server
        System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, mockClient.getConfiguration().getMasterUrl());

    }

    def cleanupSpec() {
        mockServer.destroy();
    }

    def "should default to returning all hystrix.enabled endpoints in the current namespace"() {
        given:
            mockServer.expect().get()
                    .withPath("/api/v1/namespaces/current/endpoints?labelSelector=hystrix.enabled%3Dtrue")
                    .andReturn(200, new EndpointsListBuilder()
                        .addNewItem()
                            .withNewMetadata()
                                .withName("service1")
                                .withNamespace("current")
                                .addToLabels("hystrix.enabled","true")
                            .endMetadata()
                            .addNewSubset()
                                .addNewAddresse()
                                    .withIp("ip1")
                                .endAddresse()
                            .endSubset()
                        .endItem()
                    .build()).once()
        when:
            KubernetesDiscovery discovery = new KubernetesDiscovery(mockServer.createClient(), Arrays.asList("current"), Collections.emptyList())
            List<Instance> instances  = discovery.instanceList;
        then:
            instances != null
            instances.size() == 1
            instances.get(0).getHostname().equals("ip1")
    }


    def "when one or more clusters are specified should return all endpoints with names equal to the cluster in the current namespace"() {
        given:
        mockServer.expect().get()
                .withPath("/api/v1/namespaces/current/endpoints/service1")
                .andReturn(200, new EndpointsBuilder()
                        .withNewMetadata()
                            .withName("service1")
                            .withNamespace("current")
                            .addToLabels("hystrix.enabled","true")
                        .endMetadata()
                        .addNewSubset()
                            .addNewAddresse()
                                .withIp("ip1")
                            .endAddresse()
                        .endSubset()
                .build()).once()

        mockServer.expect().get()
                .withPath("/api/v1/namespaces/current/endpoints/service2")
                .andReturn(200, new EndpointsBuilder()
                        .withNewMetadata()
                            .withName("service2")
                            .withNamespace("current")
                            .addToLabels("hystrix.enabled","true")
                        .endMetadata()
                        .addNewSubset()
                            .addNewAddresse()
                                .withIp("ip2")
                            .endAddresse()
                        .endSubset()
                .build()).once()

        mockServer.expect().get()
                .withPath("/api/v1/namespaces/current/endpoints/service3")
                .andReturn(200, new EndpointsBuilder()
                        .withNewMetadata()
                            .withName("service3")
                            .withNamespace("current")
                            .addToLabels("hystrix.enabled","true")
                        .endMetadata()
                        .addNewSubset()
                            .addNewAddresse()
                                .withIp("ip3")
                            .endAddresse()
                        .endSubset()
                .build()).once()

        when:
        KubernetesDiscovery discovery = new KubernetesDiscovery(mockServer.createClient(), Arrays.asList("current"), Arrays.asList("service1", "service2"))
        List<Instance> instances  = discovery.instanceList;
        then:
        instances != null
        instances.size() == 2
        instances.find { i -> i.hostname == "ip1" }
        instances.find { i -> i.hostname == "ip2" }
        !instances.find { i -> i.hostname == "ip3" }
    }

    def "when one or more clusters and namespaces are specified should return all endpoints with names equal to the cluster in the specified namespace"() {
        given:
        mockServer.expect().get()
                .withPath("/api/v1/namespaces/ns1/endpoints/service1")
                .andReturn(200, new EndpointsBuilder()
                        .withNewMetadata()
                            .withName("service1")
                            .withNamespace("ns1")
                            .addToLabels("hystrix.enabled","true")
                        .endMetadata()
                        .addNewSubset()
                            .addNewAddresse()
                                .withIp("ip1")
                            .endAddresse()
                        .endSubset()
                .build()).once()

        mockServer.expect().get()
                .withPath("/api/v1/namespaces/ns2/endpoints/service2")
                .andReturn(200, new EndpointsBuilder()
                        .withNewMetadata()
                            .withName("service2")
                            .withNamespace("ns2")
                            .addToLabels("hystrix.enabled","true")
                        .endMetadata()
                        .addNewSubset()
                            .addNewAddresse()
                                .withIp("ip2")
                            .endAddresse()
                        .endSubset()
                .build()).once()

        mockServer.expect().get()
                .withPath("/api/v1/namespaces/current/endpoints/service2")
                .andReturn(200, new EndpointsBuilder()
                        .withNewMetadata()
                            .withName("service2")
                            .withNamespace("current")
                            .addToLabels("hystrix.enabled","true")
                        .endMetadata()
                        .addNewSubset()
                            .addNewAddresse()
                                .withIp("ip2current")
                            .endAddresse()
                        .endSubset()
                .build()).once()

        mockServer.expect().get()
                .withPath("/api/v1/namespaces/current/endpoints/service3")
                .andReturn(200, new EndpointsBuilder()
                        .withNewMetadata()
                            .withName("service3")
                            .withNamespace("current")
                            .addToLabels("hystrix.enabled","true")
                        .endMetadata()
                        .addNewSubset()
                            .addNewAddresse()
                                .withIp("ip3")
                            .endAddresse()
                        .endSubset()
                .build()).once()

        when:
        KubernetesDiscovery discovery = new KubernetesDiscovery(mockServer.createClient(), Arrays.asList("ns1", "ns2"), Arrays.asList("service1", "service2"))
        List<Instance> instances  = discovery.instanceList;
        then:
        instances != null
        instances.size() == 2
        instances.find { i -> i.hostname == "ip1" }
        instances.find { i -> i.hostname == "ip2" }
        !instances.find { i -> i.hostname == "ip3" }
        !instances.find { i -> i.hostname == "ip2current" }
    }

}
