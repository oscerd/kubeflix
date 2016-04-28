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

package io.fabric8.kubeflix.ribbon

import com.netflix.loadbalancer.Server
import io.fabric8.kubernetes.api.builder.Visitor
import io.fabric8.kubernetes.api.model.EndpointSubsetBuilder
import io.fabric8.kubernetes.api.model.EndpointsBuilder
import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.server.mock.KubernetesMockServer
import spock.lang.Specification

class RibbonDiscoveryTest extends Specification {

    private static KubernetesMockServer mockServer = new KubernetesMockServer()
    private static KubernetesClient mockClient

    def setupSpec() {
        mockServer.init()
        mockClient = mockServer.createClient()
        //Configure the kubernetes master url to point to the mock server
        System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, mockClient.getConfiguration().getMasterUrl())
    }

    def cleanupSpec() {
        mockServer.destroy();
    }

    def newEndpoint(name, namespace, ip, port, number) {
        return new EndpointsBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .addToLabels("hystrix.enabled", "true")
                .endMetadata()
                .addNewSubset()
                .endSubset().accept(new Visitor<EndpointSubsetBuilder>() {
            @Override
            void visit(EndpointSubsetBuilder b) {
                for (int i = 1; i <= number; i++) {
                    int p = port + i - 1;
                    b.addNewPort().withName("port-" + p).withPort(p).endPort()
                    b.addNewAddresse().withIp("ip-"+i).endAddresse()
                }
            }
        }).build()
    }

    def "should return all endpoints of the configured service"() {
        given:
            mockServer.expect().get()
                    .withPath("/api/v1/namespaces/current/endpoints/service1")
                    .andReturn(200, newEndpoint("service1", "current", "ip",  8080, 3))
                    .once()
        when:
            KubernetesClientConfig config = new KubernetesClientConfig()
            config.setClientName("service1")
            config.setProperty(KubernetesConfigKey.Namespace, "current")
            config.setProperty(KubernetesConfigKey.PortName, "port-8080")
            KubernetesServerList serverList = new KubernetesServerList(config, mockServer.createClient())
            List<Server> servers  = serverList.getUpdatedListOfServers()
        then:
            servers != null
            servers.size() == 3
            servers.find({ s -> s.host == "ip-1" && s.port == 8080 })
            servers.find({ s -> s.host == "ip-2" && s.port == 8080 })
            servers.find({ s -> s.host == "ip-3" && s.port == 8080 })
    }


    def "if endpoint is single port it should be always matched"() {
        given:
        mockServer.expect().get()
                .withPath("/api/v1/namespaces/current/endpoints/service1")
                .andReturn(200, newEndpoint("service1", "current", "ip",  8080, 1))
                .once()
        when:
            KubernetesClientConfig config = new KubernetesClientConfig()
            config.setClientName("service1")
            config.setProperty(KubernetesConfigKey.Namespace, "current")
            config.setProperty(KubernetesConfigKey.PortName, "port-8080")
            KubernetesServerList serverList = new KubernetesServerList(config, mockServer.createClient())
            List<Server> servers  = serverList.getUpdatedListOfServers()
        then:
            servers != null
            servers.size() == 1
            servers.find({ s -> s.host == "ip-1" && s.port == 8080 })
    }
}
