package io.fabric8.kubeflix.examples.hellohystrix;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

import java.net.InetAddress;

public class HelloCommand extends HystrixCommand<String> {


    protected HelloCommand() {
        super(HystrixCommandGroupKey.Factory.asKey("HelloGroup"));
    }

    @Override
    protected String run() throws Exception {
        return "hello from " + InetAddress.getLocalHost().getHostName();
    }
}
