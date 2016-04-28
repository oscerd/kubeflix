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
                "spring.cloud.kubernetes.client.namespace=testns",
                "spring.cloud.kubernetes.client.trustCerts=true",
                "spring.cloud.kubernetes.config.namespace=testns"
        ])
class TurbineServerTest extends Specification {

    private static KubernetesMockServer mockServer = new KubernetesMockServer()
    private static KubernetesClient mockClient

    @Autowired
    EmbeddedWebApplicationContext server;

    def setupSpec() {
        mockServer.init()
        mockClient = mockServer.createClient()

        mockServer.expect().get()
                .withPath("/api/v1/namespaces/current/endpoints/service1")
                .andReturn(200, newEndpoint("service1", "current", "ip1"))
                .always()

        mockServer.expect().get()
                .withPath("/api/v1/namespaces/current/endpoints/service2")
                .andReturn(200, newEndpoint("service2", "current", "ip2"))
                .always()

        mockServer.expect().get()
                .withPath("/api/v1/namespaces/current/endpoints/service3")
                .andReturn(200, newEndpoint("service3", "current", "ip3"))
                .always()

        //Configure the kubernetes master url to point to the mock server
        System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, mockClient.getConfiguration().getMasterUrl())
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

    def "discovery should include all configured clusters in all configured namespaces"() {
        given:
            int port = server.embeddedServletContainer.port
            RestTemplate restTemplate = new TestRestTemplate()
        when:
            String response = restTemplate.getForObject("http://localhost:$port/discovery", String.class)
        then:
            response != null
            response.contains("http://ip1:8080/hystrix.stream")
            response.contains("http://ip2:8080/hystrix.stream")
            response.contains("http://ip3:8080/hystrix.stream")
    }
}
