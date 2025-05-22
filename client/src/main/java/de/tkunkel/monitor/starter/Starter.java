package de.tkunkel.monitor.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext; // Import this
import de.tkunkel.monitor.monitors.MonitorExecutor;

@SpringBootApplication(scanBasePackages = "de.tkunkel.monitor")
public class Starter {
    private static final Logger LOGGER = LoggerFactory.getLogger(Starter.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Starter.class, args);

        LOGGER.info("Application Context started successfully!");

        MonitorExecutor monitorExecutor = context.getBean(MonitorExecutor.class);
        LOGGER.info("Successfully retrieved Starter bean from context: " + monitorExecutor.getClass().getName());

        monitorExecutor.execute();
    }

}
