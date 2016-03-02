package io.fabric8.kubeflix.ribbon;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractServerList;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KubernetesServerList extends AbstractServerList<Server> implements ServerList<Server> {

    private static final int FIRST = 0;

    private IClientConfig clientConfig;

    private String name;
    private String namespace;
    private String vipAddresses;
    private KubernetesClient client;

    public KubernetesServerList() {
    }

    public KubernetesServerList(IClientConfig clientConfig) {
        this.initWithNiwsConfig(clientConfig);
    }

    public void initWithNiwsConfig(IClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        this.client = new DefaultKubernetesClient();
        this.name = clientConfig.getClientName();
        //Use the specified namespace if provided or fallback to the "current"
        //this.namespace = Utils.isNotNullOrEmpty(clientConfig.getNameSpace()) ? clientConfig.getNameSpace() : client.getNamespace();
        this.namespace = client.getNamespace();
        this.vipAddresses = clientConfig.getPropertyAsString(CommonClientConfigKey.DeploymentContextBasedVipAddresses, null);
    }

    public List<Server> getInitialListOfServers() {
        return Collections.emptyList();
    }

    public List<Server> getUpdatedListOfServers() {
        Endpoints endpoints = client.endpoints().inNamespace(namespace).withName(name).get();
        List<Server> result = new ArrayList<Server>();
        if (endpoints != null) {
            for (EndpointSubset subset : endpoints.getSubsets()) {

                if (subset.getPorts().size() == 1) {
                    EndpointPort port = subset.getPorts().get(FIRST);
                    for (EndpointAddress address : subset.getAddresses()) {
                        result.add(new Server(address.getIp(), port.getPort()));
                    }
                } else {
                    for (EndpointPort port : subset.getPorts()) {
                        if (Utils.isNullOrEmpty(vipAddresses) || vipAddresses.endsWith(port.getName())) {
                            for (EndpointAddress address : subset.getAddresses()) {
                                result.add(new Server(address.getIp(), port.getPort()));
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}
