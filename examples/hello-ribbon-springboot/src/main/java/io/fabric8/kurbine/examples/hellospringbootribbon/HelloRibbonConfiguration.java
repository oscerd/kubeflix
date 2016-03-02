package io.fabric8.kurbine.examples.hellospringbootribbon;

import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import io.fabric8.kurbine.ribbon.KubernetesServerList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HelloRibbonConfiguration {

    @Value("${hello.ribbon.client.name}")
    private String name = "hello-hystrix";

    @Bean
    public IClientConfig ribbonClientConfig() {
        DefaultClientConfigImpl config = new DefaultClientConfigImpl();
        config.loadProperties(this.name);
        return config;
    }

    @Bean
    public ServerList<Server> ribbonServerList(IClientConfig config) {
        KubernetesServerList serverList = new KubernetesServerList();
        serverList.initWithNiwsConfig(config);
        return serverList;
    }
}
