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

package io.fabric8.kubeflix.turbine

import com.netflix.turbine.discovery.Instance
import io.fabric8.kubernetes.api.model.EndpointsBuilder
import io.fabric8.kubernetes.api.model.EndpointsListBuilder
import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.server.mock.KubernetesMockServer
import spock.lang.Specification


class TurbineDiscoveryTest extends Specification {

    private static KubernetesMockServer mockServer = new KubernetesMockServer()
    private static KubernetesClient mockClient

    def setupSpec() {
        mockServer.init()
        mockClient = mockServer.createClient()
        //Configure the kubernetes master url to point to the mock server
        System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, mockClient.getConfiguration().getMasterUrl())
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true")
        System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false")
        System.setProperty(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, "false")
    }

    def cleanupSpec() {
        mockServer.destroy();
    }

    def newEndpoint(name, namespace, ip) {
        return new EndpointsBuilder()
        .withNewMetadata()
            .withName(name)
            .withNamespace(namespace)
            .addToLabels("hystrix.enabled", "true")
        .endMetadata()
        .addNewSubset()
            .addNewAddresse()
                .withIp(ip)
            .endAddresse()
        .endSubset()
        .build()
    }

    def "should default to returning all hystrix.enabled endpoints in the current namespace"() {
        given:
            mockServer.expect().get()
                    .withPath("/api/v1/namespaces/current/endpoints?labelSelector=hystrix.enabled%3Dtrue")
                    .andReturn(200, new EndpointsListBuilder()
                        .addToItems(newEndpoint("service1", "current", "ip1"))
                    .build())
                    .once()
        when:
            TurbineDiscovery discovery = new TurbineDiscovery(mockServer.createClient(), ['default': ['current.*']])
            List<Instance> instances  = discovery.instanceList;
        then:
            instances != null
            instances.size() == 1
            instances.get(0).getHostname().equals("ip1")
            instances.get(0).getCluster().equals("default")
    }


    def "when one or more services are specified should return all endpoints with names equal to the service in the current namespace"() {
        given:
            mockServer.expect().get()
                .withPath("/api/v1/namespaces/current/endpoints/service1")
                .andReturn(200, newEndpoint("service1", "current", "ip1"))
                .once()

            mockServer.expect().get()
                .withPath("/api/v1/namespaces/current/endpoints/service2")
                .andReturn(200, newEndpoint("service2", "current", "ip2"))
                .once()

            mockServer.expect().get()
                .withPath("/api/v1/namespaces/current/endpoints/service3")
                .andReturn(200, newEndpoint("service3", "current", "ip3"))
                .once()

        when:
            TurbineDiscovery discovery = new TurbineDiscovery(mockServer.createClient(), ['default': ['current.service1','current.service2']])
            List<Instance> instances  = discovery.instanceList

        then:
            instances != null
            instances.size() == 2
            instances.find { i -> i.hostname == "ip1" }
            instances.find { i -> i.hostname == "ip2" }
            !instances.find { i -> i.hostname == "ip3" }
    }

    def "when one or more services and namespaces are specified should return all endpoints with names equal to the cluster in the specified namespace"() {
        given:
            mockServer.expect().get()
                .withPath("/api/v1/namespaces/ns1/endpoints/service1")
                .andReturn(200, newEndpoint("service1", "ns1", "ip1"))
                .once()

            mockServer.expect().get()
                .withPath("/api/v1/namespaces/ns2/endpoints/service2")
                .andReturn(200, newEndpoint("service2", "ns2", "ip2"))
                .once()

            mockServer.expect().get()
                .withPath("/api/v1/namespaces/current/endpoints/service2")
                .andReturn(200, newEndpoint("service2", "current", "ip2current"))
                .once()

            mockServer.expect().get()
                .withPath("/api/v1/namespaces/current/endpoints/service3")
                .andReturn(200, newEndpoint("service3", "current", "ip3"))
                .once()

        when:
            TurbineDiscovery discovery = new TurbineDiscovery(mockServer.createClient(), ['default': ['ns1.service1','ns2.service2']])
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
