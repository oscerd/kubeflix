Kubeflix: Kubernetes integration with Turbine
--------------------------------------------

This project provides simple integration with Netlix Turbine.

Specifically it provides:

* Kubernetes Instance Discovery
* A Kubernetes ready Docker image for Turbine
* Kubernetes configuration for the Hystrix dashboard
* A simple hystrix example to use as a demo


Instructions
------------

To build to the project:

    mvn clean install   

        
### Running a Hystrix example    
To start the a hystrix example

     cd examples/hello-hystrix
     mvn clean package docker:build fabric8:apply
     
This example is a simple hello world servlet, that uses a hystrix command to obtain the hello world message.
You can access the servlet at: http://hello-hystrix.vagrant.f8/hello (or wherever the hello-hystrix service is bound).
The metrics stream for this example shoule be available at at: http://hello-hystrix.vagrant.f8/hello (or wherever the hello-hystrix service is bound).

You can scale up the controller hello-hystrix controller to create more pods running the example.
     
### Starting the discovery server
   
    cd server
    mvn clean package docker:build fabric8:apply

The discovery server will discover all endpoints providing a hystrix stream (contain the label: **hystrix.enabled**=true)
You can access the aggregated stream at http://kubeflix-server.vagrant.f8/turbine.stream (or wherever the kubeflix-server service is bound).


### Starting the dashboard
     
     cd dashboard
     kubectl create -f controller.yml
     kubectl create -f service.yml

Open your browser at: http://hystrix-dashboard.vagrant.f8:8080  and enter the url of the aggragated stream  http://kubeflix-server.vagrant.f8/turbine.stream
Et voila!