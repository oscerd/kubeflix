package io.fabric8.kubeflix;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "turbine.aggregator")
public class AggregatorProperties {

    private List<String> clusterConfig;

    private List<String> namespaceConfig;

    public List<String> getClusterConfig() {
        return clusterConfig;
    }

    public void setClusterConfig(List<String> clusterConfig) {
        this.clusterConfig = clusterConfig;
    }

    public List<String> getNamespaceConfig() {
        return namespaceConfig;
    }

    public void setNamespaceConfig(List<String> namespaceConfig) {
        this.namespaceConfig = namespaceConfig;
    }
}
