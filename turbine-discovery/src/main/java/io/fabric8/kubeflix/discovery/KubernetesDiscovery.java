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
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KubernetesDiscovery implements InstanceDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesDiscovery.class);

    private static final String HYSTRIX_ENABLED = "hystrix.enabled";
    private static final String HYSTRIX_CLUSTER = "hystrix.cluster";

    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT = "default";
    private static final Map<String, String> DEFAULT_LABELS = new HashMap<String, String>();

    static {
        DEFAULT_LABELS.put(HYSTRIX_ENABLED, "true");
    }


    private final KubernetesClient client;
    private final Map<String, String> labels;

    public KubernetesDiscovery() {
        this(new DefaultKubernetesClient(), DEFAULT_LABELS);
    }

    public KubernetesDiscovery(KubernetesClient client, Map<String, String> labels) {
        this.client = client;
        this.labels = labels;
    }

    public Collection<Instance>  getInstanceList() throws Exception {
        List<Instance> result = new ArrayList<Instance>();
        for (Endpoints endpoint : client.endpoints().withLabels(labels).list().getItems()) {
            try {
                result.addAll(toInstances(endpoint));
            } catch (Throwable t) {
                LOGGER.error("Error processing endpoint", t);
            }
        }
        return result;
    }


    private static List<EndpointAddress> addresses(Endpoints e) {
        List<EndpointAddress> result = new ArrayList<EndpointAddress>();
        for (EndpointSubset endpointSubset : e.getSubsets()) {
            result.addAll(endpointSubset.getAddresses());
        }
        return result;
    }


    private static List<Instance> toInstances(Endpoints e) {
        List<Instance> result = new ArrayList<Instance>();
        for (EndpointSubset subset : e.getSubsets()) {
            String clusterName = e.getMetadata().getLabels().containsKey(HYSTRIX_CLUSTER) ?
                    e.getMetadata().getLabels().get(HYSTRIX_CLUSTER) :
                    DEFAULT;
            for (EndpointAddress address : subset.getAddresses()) {
                result.add(new Instance(address.getIp(), clusterName, true));
            }
        }
        return result;
    }
}
