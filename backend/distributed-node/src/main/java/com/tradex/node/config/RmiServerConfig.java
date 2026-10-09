package com.tradex.node.config;

import com.tradex.common.rmi.RemoteNodeService;
import com.tradex.node.service.NodeRmiServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

@Configuration
public class RmiServerConfig {

    private static final Logger log = LoggerFactory.getLogger(RmiServerConfig.class);

    @Value("${rmi.port:1099}")
    private int rmiPort;

    @Value("${rmi.host:localhost}")
    private String rmiHost;

    private final NodeRmiServiceImpl nodeRmiService;
    private Registry registry;

    public RmiServerConfig(NodeRmiServiceImpl nodeRmiService) {
        this.nodeRmiService = nodeRmiService;
    }

    @PostConstruct
    public void startRmiRegistry() {
        try {
            // Advertise correct reachable host for remote stubs in Docker network
            System.setProperty("java.rmi.server.hostname", rmiHost);

            try {
                registry = LocateRegistry.createRegistry(rmiPort);
                log.info("Created new Java RMI registry on port {}", rmiPort);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(rmiPort);
                log.info("Found existing Java RMI registry on port {}", rmiPort);
            }

            registry.rebind("RemoteNodeService", nodeRmiService);
            log.info("Successfully bound RemoteNodeService to RMI registry at {}:{}", rmiHost, rmiPort);
        } catch (Exception e) {
            log.error("Failed to initialize Java RMI registry: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void stopRmiRegistry() {
        try {
            if (registry != null) {
                registry.unbind("RemoteNodeService");
            }
        } catch (Exception ignored) {}
    }
}
