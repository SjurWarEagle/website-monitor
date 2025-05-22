package de.tkunkel.monitor.starter;

import de.tkunkel.monitor.monitors.TelegramMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext; // Import this
import de.tkunkel.monitor.monitors.MonitorExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "de.tkunkel.monitor")
public class Starter {
    private static final Logger LOGGER = LoggerFactory.getLogger(Starter.class);
    private static ConfigurableApplicationContext applicationContext;

    // This method will run every day at 5:50 AM
    @Scheduled(cron = "0 50 5 * * *")
    public void runMonitors() {
        MonitorExecutor monitorExecutor = applicationContext.getBean(MonitorExecutor.class);
        monitorExecutor.execute();
    }

    // This method will run every Saturday at 7:55 AM
    @Scheduled(cron = "0 55 7 * * 6")
    public void sendLivesign() {
        MonitorExecutor monitorExecutor = applicationContext.getBean(MonitorExecutor.class);
        TelegramMessageSender telegramMessageSender = applicationContext.getBean(TelegramMessageSender.class);
        StringBuilder msg = new StringBuilder("⚒ LiveSign\n");
        msg.append("Just as info, these Monitors are active:\n");
        monitorExecutor.getAllMonitorNames().forEach(monitor -> {
            msg.append("- ").append(monitor).append("\n");
        });
        msg.append("\n");
        telegramMessageSender.sendMessage(msg.toString());
    }

    public static void main(String[] args) {
        applicationContext = SpringApplication.run(Starter.class, args);

        LOGGER.info("Application Context started successfully!");

        MonitorExecutor monitorExecutor = applicationContext.getBean(MonitorExecutor.class);
        LOGGER.info("Successfully retrieved Starter bean from context: " + monitorExecutor.getClass().getName());

        new Starter().sendLivesign();
    }


}
