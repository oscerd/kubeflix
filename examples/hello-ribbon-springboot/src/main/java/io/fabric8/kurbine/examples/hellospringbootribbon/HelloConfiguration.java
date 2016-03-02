package io.fabric8.kurbine.examples.hellospringbootribbon;

import org.springframework.cloud.netflix.ribbon.RibbonClient;

@RibbonClient(name = "hello-hystrix", configuration = HelloRibbonConfiguration.class)
public class HelloConfiguration {
}
