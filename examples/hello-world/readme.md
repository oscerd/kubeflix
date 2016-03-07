Kubeflix Examples: Hello World
------------------------------

This is a Hello World example that consists of the following parts:

- hello-hystrix: A simple hello world example with hystrix (no remote service).
- hello-ribbon: A simple loadbalancer for hello-hystrix
- hello-ribbon-springboot: A springboot powered ribbon loadbalancer based on @HystrixCommand.

Instructions
------------

To build to the project:

    mvn clean install   
        
### Running the Hello Hystrix example
To start the a hystrix example

     cd examples/hello-hystrix
     mvn clean package docker:build fabric8:apply
     
This example is a simple hello world servlet, that uses a hystrix command to obtain the hello world message.

The hello message is always __"Hello from ${HOSTNAME}"__.

You can access the servlet at: http://hello-hystrix.vagrant.f8/hello (or wherever the hello-hystrix service is bound).
The metrics stream for this example should be available at at: http://hello-hystrix.vagrant.f8/hello (or wherever the hello-hystrix service is bound).

You can scale up the controller hello-hystrix controller to create more pods running the example.
     
### Using Ribbon to call the Hello Hystrix example

The hello-hystrix app is dummy. It just uses a hystrix command to return a string. No remote services are called (so far).
To make things more interesting we are going to use ribbon to create rest template that will call the hystrix app we created in the previous step.
The rest template is packaged inside a servelet, so we have a servlet that uses ribbon to loadbalance invocations to an other servlet (the hello hystrix).

     cd examples/hello-ribbon
     mvn clean package docker:build fabric8:apply
     
You can access the servlet at: http://hello-ribbon.vagrant.f8/hello (or wherever the hello-ribbon service is bound).
The metrics stream for this example should be available at at: http://hello-ribbon.vagrant.f8/hello (or wherever the hello-ribbon service is bound).

To make things more interesting let's scale up the hello-hystrix app:

    kubectl scale --replicas=5 replicationController hello-hystrix
    
This will ask kubernetes to keep 5 replicas of hello-hystrix running each time. If you keep an calling the hello-ribbon servlet you'll see that each invocation gets its response from a different instance of hello-hystrix (the response contains the hostname, remember?).
    
### Using Ribbon with Spring Cloud to call the Hello Hystrix example    

A variation of the previous example that uses spring boot is also provided. 

You can build it and run it:
     
     cd examples/hello-ribbon-spring-boot
     mvn clean package docker:build fabric8:apply

You can access the servlet at: http://hello-ribbon-spring-boot.vagrant.f8/hello (or wherever the hello-ribbon-spring-boot service is bound).
The metrics stream for this example should be available at at: http://hello-ribbon-spring-boot.vagrant.f8/hello (or wherever the hello-ribbon service is bound).

### Using the Turbine Server and Hystrix Dashboard

It's not a mandatory step but it does help a lot if you bring up the **Turbine Server** and **Hystrix Dashboard** and have a glimpse at the dashboard.

 ![Hello World Dashboard](images/dashboard.png "Hello World Dashboard")

 