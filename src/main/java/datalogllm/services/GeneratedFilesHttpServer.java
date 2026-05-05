package datalogllm.services;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import datalogllm.pipeline.PlantUmlPipeline;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GeneratedFilesHttpServer {

    private static final int PORT = 8080;
    private static final Path OUTPUT_ROOT = Path.of("target", "generated");
    private static final GeneratedFilesService GENERATED_FILES_SERVICE = new GeneratedFilesService(OUTPUT_ROOT);

    private GeneratedFilesHttpServer() {
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUTPUT_ROOT);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/generate", GeneratedFilesHttpServer::handleGenerate);
        server.createContext("/api/files", GeneratedFilesHttpServer::handleFilesList);
        server.createContext("/api/files/content", GeneratedFilesHttpServer::handleFileContent);
        server.createContext("/api/files/archive", GeneratedFilesHttpServer::handleFilesArchive);
        server.setExecutor(null);
        server.start();

        System.out.println("Generated files server running on http://localhost:" + PORT);
    }

    private static void handleGenerate(HttpExchange exchange) throws IOException {
        if (handlePreflight(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, new JSONObject().put("error", "Method not allowed").toString());
            return;
        }

        try (InputStream bodyStream = exchange.getRequestBody()) {
            String body = new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8);
            JSONObject request = new JSONObject(body);
            String plantUml = request.optString("plantUml", "").trim();
            if (plantUml.isEmpty()) {
                writeJson(exchange, 400, new JSONObject().put("error", "plantUml is required").toString());
                return;
            }

            PlantUmlPipeline.generateDatalogAndJson(plantUml, OUTPUT_ROOT);
            PlantUmlPipeline.generateJavaAndSqlFromUmlMetamodel(plantUml, OUTPUT_ROOT, "generated", "generated.liveness");

            JSONArray files = toJsonArray(GENERATED_FILES_SERVICE.listGeneratedFiles());
            writeJson(exchange, 200, new JSONObject().put("files", files).toString());
        } catch (Exception ex) {
            writeJson(exchange, 500, new JSONObject().put("error", ex.getMessage()).toString());
        }
    }

    private static void handleFilesList(HttpExchange exchange) throws IOException {
        if (handlePreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, new JSONObject().put("error", "Method not allowed").toString());
            return;
        }

        try {
            JSONArray response = toJsonArray(GENERATED_FILES_SERVICE.listGeneratedFiles());
            writeJson(exchange, 200, response.toString());
        } catch (Exception ex) {
            writeJson(exchange, 500, new JSONObject().put("error", ex.getMessage()).toString());
        }
    }

    private static void handleFileContent(HttpExchange exchange) throws IOException {
        if (handlePreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, new JSONObject().put("error", "Method not allowed").toString());
            return;
        }

        try {
            Map<String, String> query = parseQuery(exchange.getRequestURI());
            String path = query.getOrDefault("path", "");
            if (path.isBlank()) {
                writeJson(exchange, 400, new JSONObject().put("error", "path is required").toString());
                return;
            }
            String content = GENERATED_FILES_SERVICE.readGeneratedFile(path);
            writeText(exchange, 200, content, "text/plain; charset=utf-8");
        } catch (IllegalArgumentException ex) {
            writeJson(exchange, 400, new JSONObject().put("error", ex.getMessage()).toString());
        } catch (Exception ex) {
            writeJson(exchange, 500, new JSONObject().put("error", ex.getMessage()).toString());
        }
    }

    private static void handleFilesArchive(HttpExchange exchange) throws IOException {
        if (handlePreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, new JSONObject().put("error", "Method not allowed").toString());
            return;
        }

        try {
            byte[] zip = GENERATED_FILES_SERVICE.buildGeneratedFilesZip();
            Headers headers = exchange.getResponseHeaders();
            applyCors(headers);
            headers.set("Content-Type", "application/zip");
            headers.set("Content-Disposition", "attachment; filename=\"generated-files.zip\"");
            exchange.sendResponseHeaders(200, zip.length);
            exchange.getResponseBody().write(zip);
            exchange.close();
        } catch (Exception ex) {
            writeJson(exchange, 500, new JSONObject().put("error", ex.getMessage()).toString());
        }
    }

    private static JSONArray toJsonArray(List<GeneratedFilesService.GeneratedFileInfo> files) {
        JSONArray array = new JSONArray();
        for (GeneratedFilesService.GeneratedFileInfo file : files) {
            array.put(new JSONObject()
                    .put("name", file.name())
                    .put("path", file.path())
                    .put("directory", file.directory())
                    .put("size", file.size()));
        }
        return array;
    }

    private static boolean handlePreflight(HttpExchange exchange) throws IOException {
        if (!"OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) return false;
        Headers headers = exchange.getResponseHeaders();
        applyCors(headers);
        headers.set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
        return true;
    }

    private static void writeJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        writeText(exchange, statusCode, json, "application/json; charset=utf-8");
    }

    private static void writeText(HttpExchange exchange, int statusCode, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        applyCors(headers);
        headers.set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void applyCors(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> result = new HashMap<>();
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) return result;

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            String key = urlDecode(keyValue[0]);
            String value = keyValue.length > 1 ? urlDecode(keyValue[1]) : "";
            result.put(key, value);
        }
        return result;
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
