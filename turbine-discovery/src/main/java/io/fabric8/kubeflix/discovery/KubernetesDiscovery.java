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

package io.fabric8.kubeflix.discovery;

import com.netflix.turbine.discovery.Instance;
import com.netflix.turbine.discovery.InstanceDiscovery;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KubernetesDiscovery implements InstanceDiscovery {

    private static final String HYSTRIX_ENABLED = "hystrix.enabled";
    private static final String HYSTRIX_CLUSTER = "hystrix.cluster";
    private static final String TRUE = "true";

    private final KubernetesClient client;
    private final Collection<String> namespaces;
    private final Collection<String> clusters;


    private final Function<String, List<Endpoints>> ENDPOINTS_OF_NAMESPACE = new Function<String, List<Endpoints>>() {
        @Override
        public List<Endpoints> apply(String s) {
            if (!clusters.isEmpty()) {
                return clusters.stream()
                        .map(c -> client.endpoints().inNamespace(s).withName(c).get())
                        .filter(n -> n != null)
                        .map(e -> new EndpointsBuilder(e).editMetadata().addToLabels(HYSTRIX_CLUSTER, e.getMetadata().getName()).and().build())
                        .collect(Collectors.toList());
            } else {
                return client.endpoints().inNamespace(s).withLabel(HYSTRIX_ENABLED, TRUE).list().getItems().stream()
                        .map(e -> new EndpointsBuilder(e).editMetadata().addToLabels(HYSTRIX_CLUSTER, e.getMetadata().getNamespace()).and().build())
                        .collect(Collectors.toList());
            }
        }
    };

    private final Function<Endpoints, List<Instance>> ENDPOINTS_TO_INSTANCES = new Function<Endpoints, List<Instance>>() {
        @Override
        public List<Instance> apply(Endpoints endpoints) {
            String clusterName = endpoints.getMetadata().getLabels().containsKey(HYSTRIX_CLUSTER) ?
                    endpoints.getMetadata().getLabels().get(HYSTRIX_CLUSTER) :
                    endpoints.getMetadata().getNamespace();

            return endpoints.getSubsets().stream()
                    .flatMap(e -> e.getAddresses().stream())
                    .map(a -> new Instance(a.getIp(), clusterName, true))
                    .collect(Collectors.toList());
        }
    };


    public KubernetesDiscovery(KubernetesClient client, Collection<String> namespaces, Collection<String> clusters) {
        this.client = client;
        this.namespaces = namespaces;
        this.clusters = clusters;
    }

    public Collection<Instance> getInstanceList() throws Exception {
        return namespaces.stream()
                .flatMap(ENDPOINTS_OF_NAMESPACE.andThen(e -> e.stream()))
                .flatMap(ENDPOINTS_TO_INSTANCES.andThen(i -> i.stream()))
                .collect(Collectors.toList());
    }
}
