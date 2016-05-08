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

package io.fabric8.kubeflix.turbine;

import com.netflix.turbine.discovery.Instance;
import com.netflix.turbine.discovery.InstanceDiscovery;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TurbineDiscovery implements InstanceDiscovery {

    private static final String HYSTRIX_ENABLED = "hystrix.enabled";
    private static final String HYSTRIX_CLUSTER = "hystrix.cluster";
    private static final String DEFAULT_CLUSTER = "default";
    private static final String TRUE = "true";
    private static final String DELIMITER = ".";
    private static final String WILDCARD = "*";
    private static final String SERVICE = "service";
    private static final String NAMESPACE = "name";
    private static final String NAME_REGEX = "[a-z]([-a-z0-9]*[a-z0-9])?";
    private static final String NAME_OR_WILDCARD_REGEX = "(("+NAME_REGEX+")|\\*)";
    private static final String FQN_REGEX = "(?<" + NAMESPACE + ">" + NAME_REGEX + ")\\.(?<" + SERVICE + ">" + NAME_OR_WILDCARD_REGEX + ")";
    private static final Pattern FQN_PATTERN = Pattern.compile(FQN_REGEX);


    private final KubernetesClient client;
    private final Map<String, List<String>> clusters;

    private final Function<String, String> TO_FULLY_QUALIFIED_SERVICE = new Function<String, String>() {
        @Override
        public String apply(String service) {
            if (service.contains(DELIMITER)) {
                return service;
            } else {
                return client.getNamespace() + DELIMITER + service;
            }
        }
    };

    private final Function<String, List<Endpoints>> TO_ENDPOINT = new Function<String, List<Endpoints>>() {
        @Override
        public List<Endpoints> apply(String qualifiedService) {
            List<Endpoints> result = new ArrayList<>();
            Matcher m = FQN_PATTERN.matcher(qualifiedService);
            if (m.matches()) {
                String namespace = m.group(NAMESPACE);
                String service = m.group(SERVICE);
                if (service.equals(WILDCARD)) {
                    result.addAll(client.endpoints().inNamespace(namespace).withLabel(HYSTRIX_ENABLED, TRUE).list().getItems());
                } else {
                    Endpoints endpoints = client.endpoints().inNamespace(namespace).withName(service).get();
                    if (endpoints != null) {
                        result.add(endpoints);
                    }
                }
            }
            return result;
        }
    };

    private final Function<Endpoints, List<Instance>> TO_INSTANCES = new Function<Endpoints, List<Instance>>() {
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

    public TurbineDiscovery(KubernetesClient client) {
        this(client = client, new HashMap<>());
    }

    public TurbineDiscovery(KubernetesClient client, Map<String, List<String>> clusters) {
        this.client = client;
        this.clusters = clusters;
    }

    public Collection<Instance> getInstanceList() throws Exception {
        if (clusters == null || clusters.isEmpty()) {
            return client.endpoints().withLabel(HYSTRIX_ENABLED, TRUE).list().getItems().stream()
                    .map(e -> new EndpointsBuilder(e).editMetadata().addToLabels(HYSTRIX_CLUSTER, DEFAULT_CLUSTER).endMetadata().build())
                    .flatMap(TO_INSTANCES.andThen(i -> i.stream()))
                    .collect(Collectors.toList());
        } else {
            return clusters.keySet().stream()
                    .flatMap(cluster -> clusters.get(cluster)
                            .stream()
                            .map(TO_FULLY_QUALIFIED_SERVICE)
                            .flatMap(TO_ENDPOINT.andThen(e -> e.stream()))
                            .filter(e -> e != null)
                            .map(e -> new EndpointsBuilder(e).editMetadata().addToLabels(HYSTRIX_CLUSTER, cluster).endMetadata().build())
                            .flatMap(TO_INSTANCES.andThen(i -> i.stream())))
                    .collect(Collectors.toList());
        }
    }
}
