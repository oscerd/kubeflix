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

package io.fabric8.kubeflix;

import com.netflix.turbine.discovery.InstanceDiscovery;
import com.netflix.turbine.streaming.servlet.TurbineStreamServlet;
import io.fabric8.kubeflix.discovery.KubernetesDiscovery;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.configuration.MapConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
public class TurbineConfiguration {

    private static final String TURBINE_AGGREGATOR_CLUSTER_CONFIG_PROPERTY_NAME = "turbine.aggregator.clusterConfig";
    private static final String DEFAULT_TURBINE_URL_MAPPING = "/turbine.stream";
    private static final String DEFAULT_DISCOVERY_URL_MAPPING = "/discovery";
    private static final Set<String> BLANK = new HashSet<>(Arrays.asList(""));

    @Value("${turbine.aggregator.clusterConfig}")
    private Set<String> clusters;

    @Value("${turbine.aggregator.namespaceConfig}")
    private Set<String> namespaces;

    @Bean
    MapConfiguration turbineClusterConfig(KubernetesClient client) {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put(TURBINE_AGGREGATOR_CLUSTER_CONFIG_PROPERTY_NAME, client.getNamespace());
        return new MapConfiguration(configuration);
    }

    @Bean
    InstanceDiscovery instanceDiscovery(KubernetesClient client) {
        return new KubernetesDiscovery(client,
                !namespaces.isEmpty() && !BLANK.equals(namespaces) ? namespaces : Arrays.asList(client.getNamespace()),
                !BLANK.equals(clusters) ? clusters : Collections.emptySet());
    }

    @Bean
    TurbineLifecycle turbineContextListener(InstanceDiscovery instanceDiscovery) {
        return new TurbineLifecycle(instanceDiscovery);
    }

    @Bean
    public ServletRegistrationBean turbineServletRegistration() {
        ServletRegistrationBean registration = new ServletRegistrationBean(turbineStreamServlet());
        registration.setUrlMappings(Arrays.asList(DEFAULT_TURBINE_URL_MAPPING));
        return registration;
    }

    @Bean
    public ServletRegistrationBean discoveryServletRegistration(InstanceDiscovery instanceDiscovery) {
        ServletRegistrationBean registration = new ServletRegistrationBean(discoveryFeedbackServlet(instanceDiscovery));
        registration.setUrlMappings(Arrays.asList(DEFAULT_DISCOVERY_URL_MAPPING));
        return registration;
    }

    @Bean
    public TurbineStreamServlet turbineStreamServlet() {
        return new TurbineStreamServlet();
    }

    @Bean
    public DiscoveryFeedbackServlet discoveryFeedbackServlet(InstanceDiscovery instanceDiscovery) {
        return new DiscoveryFeedbackServlet(instanceDiscovery);
    }
}
