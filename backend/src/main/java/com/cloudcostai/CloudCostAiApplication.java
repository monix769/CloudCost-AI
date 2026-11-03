package com.cloudcostai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * CloudCost AI - Main Application Entry Point
 * 
 * Intelligent Cloud Cost Monitoring &amp; FinOps Analytics Platform.
 */
@SpringBootApplication
@org.springframework.data.web.config.EnableSpringDataWebSupport(
        pageSerializationMode = org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class CloudCostAiApplication {

    private static final Logger log = LoggerFactory.getLogger(CloudCostAiApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CloudCostAiApplication.class);
        Environment env = app.run(args).getEnvironment();
        logApplicationStartup(env);
    }

    private static void logApplicationStartup(Environment env) {
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "/");
        if (!contextPath.endsWith("/")) {
            contextPath += "/";
        }
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using 'localhost' as fallback");
        }

        log.info("""
                
                ----------------------------------------------------------
                \tApplication '{}' is running! Access URLs:
                \tLocal: \t\t{}://localhost:{}{}
                \tExternal: \t{}://{}:{}{}
                \tProfile(s): \t{}
                \tSwagger UI: \t{}://localhost:{}{}swagger-ui.html
                \tOpenAPI: \t{}://localhost:{}{}v3/api-docs
                ----------------------------------------------------------
                """,
                env.getProperty("spring.application.name"),
                protocol, serverPort, contextPath,
                protocol, hostAddress, serverPort, contextPath,
                env.getActiveProfiles().length == 0 ? env.getDefaultProfiles() : env.getActiveProfiles(),
                protocol, serverPort, contextPath,
                protocol, serverPort, contextPath
        );
    }
}
