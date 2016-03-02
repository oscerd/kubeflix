package io.fabric8.kubeflix.examples.hellospringbootribbon;

import org.springframework.cloud.netflix.ribbon.RibbonClient;

@RibbonClient(name = "hello-hystrix", configuration = HelloRibbonConfiguration.class)
public class HelloConfiguration {
}
