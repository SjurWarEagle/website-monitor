package de.tkunkel.monitor.monitors;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public abstract class Monitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Monitor.class);

    private final Gson gson = new Gson();

    public abstract String getName();

    public abstract String getConfigFileName();

    public abstract void execute();

    public void informAboutChange(String msg) {
        LOGGER.warn(msg);
    }

    public String readOldValue() {
        StringBuilder rc = new StringBuilder();
        try {
            if (!Files.exists(Path.of(getConfigFileName()))) {
                return rc.toString();
            }
            List<String> strings = Files.readAllLines(Path.of(getConfigFileName()));
            strings.forEach(s -> rc.append(s).append("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return rc.toString();
    }

    public void storeNewValue(String data) {
        try (FileWriter fw = new FileWriter(getConfigFileName())) {
            fw.write(data);
            fw.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
