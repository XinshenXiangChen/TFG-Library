package datalogllm.services;

import java.nio.file.Path;

/**
 * Placeholder service entry point for upcoming Spring backend logic.
 * Add your service methods here as you build the API layer.
 */
public class Services {

    public String health() {
        return "OK";
    }

    public GeneratedFilesService generatedFilesService(Path generatedRoot) {
        return new GeneratedFilesService(generatedRoot);
    }
}
