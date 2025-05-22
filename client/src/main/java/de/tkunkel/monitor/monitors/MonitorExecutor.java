package de.tkunkel.monitor.monitors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MonitorExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorExecutor.class);
    private final List<Monitor> monitors; // <-- Declaring a list of the interface

    // Spring automatically finds all beans implementing 'Monitor' and injects them here
    public MonitorExecutor(List<Monitor> monitors) {
        this.monitors = monitors;
        LOGGER.info("MonitorExecutor initialized with {} monitors.", monitors.size());
    }

    public void execute() {
        if (monitors == null || monitors.isEmpty()) {
            LOGGER.warn("No monitors configured to execute.");
            return;
        }
        monitors.forEach(monitor -> {
            LOGGER.info("Going to start monitor: " + monitor.getName());
            try {
                monitor.execute();
                LOGGER.info("Monitor {} finished successfully.", monitor.getName());
            } catch (Exception e) {
                LOGGER.error("Error executing monitor: {}", monitor.getName(), e);
            }
        });
        LOGGER.info("All monitors processed.");
    }
}
