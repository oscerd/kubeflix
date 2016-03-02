package io.fabric8.kubeflix;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.netflix.turbine.plugins.PluginsFactory;
import io.fabric8.kubeflix.discovery.KubernetesDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.turbine.init.TurbineInit;

public class StartTurbineServer implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(StartTurbineServer.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Initing Turbine server");
        PluginsFactory.setInstanceDiscovery(new KubernetesDiscovery());
        TurbineInit.init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Stopping Turbine server");
        TurbineInit.stop();
    }
}
