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

import io.fabric8.kubeflix.TurbineServerApplication
import io.fabric8.kubernetes.api.model.EndpointsBuilder
import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.server.mock.KubernetesMockServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.boot.test.TestRestTemplate
import org.springframework.boot.test.WebIntegrationTest
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

@EnableAutoConfiguration
@SpringApplicationConfiguration(TurbineServerApplication.class)
@WebIntegrationTest(
        [       "server.port:0",
                "spring.application.name=turbine-server",
                "spring.cloud.kubernetes.client.trustCerts=true",
                "spring.cloud.kubernetes.client.namespace=current",
                "turbine.aggregator.clusterConfig=mycluster",
                "turbine.aggregator.clusters.mycluster=ns1.service1,ns2.service2,ns3.service3"
        ])
class WithClusterAndNamespaceTest extends Specification {

    private static KubernetesMockServer mockServer = new KubernetesMockServer()
    private static KubernetesClient mockClient

    @Autowired
    EmbeddedWebApplicationContext server;

    def setupSpec() {
        mockServer.init()
        mockClient = mockServer.createClient()

        mockServer.expect().get()
                .withPath("/api/v1/namespaces/ns1/endpoints/service1")
                .andReturn(200, newEndpoint("service1", "ns1", "ip1"))
                .always()

        mockServer.expect().get()
                .withPath("/api/v1/namespaces/ns2/endpoints/service2")
                .andReturn(200, newEndpoint("service2", "ns2", "ip2"))
                .always()

        mockServer.expect().get()
                .withPath("/api/v1/namespaces/ns3/endpoints/service3")
                .andReturn(200, newEndpoint("service3", "ns3", "ip3"))
                .always()

        mockServer.expect().get()
                .withPath("/api/v1/ns3/current/endpoints/service4")
                .andReturn(200, newEndpoint("service4", "ns3", "ip4"))
                .always()

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
                    .addNewAddress()
                        .withIp(ip)
                    .endAddress()
                .endSubset()
                .build()
    }

    def "when explicitly specifying services and namespaces discovery should include all configured services in all configured namespaces"() {
        given:
            int port = server.embeddedServletContainer.port
            RestTemplate restTemplate = new TestRestTemplate()
        when:
            String response = restTemplate.getForObject("http://localhost:$port/discovery", String.class)
        then:
            response != null
            response.contains("http://ip1:8080/hystrix.stream mycluster:true")
            response.contains("http://ip2:8080/hystrix.stream mycluster:true")
            response.contains("http://ip3:8080/hystrix.stream mycluster:true")
            !response.contains("http://ip4:8080/hystrix.stream mycluster:true")
    }
}
