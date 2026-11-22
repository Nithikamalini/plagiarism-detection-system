package com.plagiarism.server;

import com.plagiarism.algo.PlagiarismEngine;
import com.plagiarism.db.MongoDBManager;
import com.plagiarism.model.DocumentModel;
import com.plagiarism.model.PlagiarismReport;
import com.plagiarism.model.User;
import com.plagiarism.util.JsonHelper;
import com.plagiarism.util.PasswordHasher;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Built-in High-Performance HTTP Server.
 * Serves modern SPA static assets and provides RESTful API endpoints for:
 * - Authentication (Register, Login, Session)
 * - Reference Corpus Management
 * - Plagiarism Analysis (N-Gram & Cosine Similarity)
 * - Scan History & Analytics
 * - Printable Report Generation
 */
public class WebServer {

    private final int port;
    private HttpServer server;
    private final MongoDBManager db;

    public WebServer(int port) {
        this.port = port;
        this.db = MongoDBManager.getInstance();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(16));

        // Register API handlers
        server.createContext("/api/auth/", new AuthHandler());
        server.createContext("/api/corpus", new CorpusHandler());
        server.createContext("/api/plagiarism/check", new PlagiarismCheckHandler());
        server.createContext("/api/history", new HistoryHandler());
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/api/system/status", new SystemStatusHandler());
        server.createContext("/api/report/export", new ReportExportHandler());
        server.createContext("/api/report/printable", new PrintableReportHandler());

        // Static files handler
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println("==================================================================");
        System.out.println("  Plagiarism Detection System is RUNNING!");
        System.out.println("  Web UI Address: http://localhost:" + port);
        System.out.println("  MongoDB Status: " + (db.isConnectedToMongo() ? "Connected (localhost:27017)" : "Hybrid Memory Cache Mode"));
        System.out.println("==================================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // =========================================================================
    // AUTH HANDLER
    // =========================================================================
    private class AuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                HttpHandlerHelper.sendJsonResponse(exchange, 204, "");
                return;
            }

            Map<String, Object> resp = new HashMap<>();

            try {
                if (path.endsWith("/login") && "POST".equalsIgnoreCase(method)) {
                    String body = HttpHandlerHelper.readBody(exchange);
                    @SuppressWarnings("unchecked")
                    Map<String, String> req = JsonHelper.fromJson(body, Map.class);
                    String username = req.get("username");
                    String password = req.get("password");

                    User user = db.findUserByUsername(username);
                    if (user != null && PasswordHasher.verify(password, user.getPasswordHash())) {
                        String token = SessionManager.createSession(user);
                        resp.put("success", true);
                        resp.put("token", token);
                        resp.put("user", Map.of(
                                "id", user.getId(),
                                "username", user.getUsername(),
                                "fullName", user.getFullName(),
                                "email", user.getEmail(),
                                "role", user.getRole()
                        ));
                        HttpHandlerHelper.sendJsonResponse(exchange, 200, JsonHelper.toJson(resp));
                    } else {
                        resp.put("success", false);
                        resp.put("message", "Invalid username or password.");
                        HttpHandlerHelper.sendJsonResponse(exchange, 401, JsonHelper.toJson(resp));
                    }
                } else if (path.endsWith("/register") && "POST".equalsIgnoreCase(method)) {
                    String body = HttpHandlerHelper.readBody(exchange);
                    @SuppressWarnings("unchecked")
                    Map<String, String> req = JsonHelper.fromJson(body, Map.class);
                    String username = req.get("username");
                    String password = req.get("password");
                    String email = req.get("email");
                    String fullName = req.get("fullName");

                    if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                        resp.put("success", false);
                        resp.put("message", "Username and password are required.");
                        HttpHandlerHelper.sendJsonResponse(exchange, 400, JsonHelper.toJson(resp));
                        return;
                    }

                    if (db.findUserByUsername(username) != null) {
                        resp.put("success", false);
                        resp.put("message", "Username is already taken.");
                        HttpHandlerHelper.sendJsonResponse(exchange, 409, JsonHelper.toJson(resp));
                        return;
                    }

                    User newUser = new User(
                            "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                            username.trim(),
                            email != null ? email.trim() : "",
                            PasswordHasher.hash(password),
                            fullName != null && !fullName.trim().isEmpty() ? fullName.trim() : username.trim()
                    );
                    db.saveUser(newUser);

                    String token = SessionManager.createSession(newUser);
                    resp.put("success", true);
                    resp.put("token", token);
                    resp.put("user", Map.of(
                            "id", newUser.getId(),
                            "username", newUser.getUsername(),
                            "fullName", newUser.getFullName(),
                            "email", newUser.getEmail(),
                            "role", newUser.getRole()
                    ));
                    HttpHandlerHelper.sendJsonResponse(exchange, 201, JsonHelper.toJson(resp));
                } else if (path.endsWith("/me") && "GET".equalsIgnoreCase(method)) {
                    String token = HttpHandlerHelper.getAuthToken(exchange);
                    User user = SessionManager.getUser(token);
                    if (user != null) {
                        resp.put("success", true);
                        resp.put("user", Map.of(
                                "id", user.getId(),
                                "username", user.getUsername(),
                                "fullName", user.getFullName(),
                                "email", user.getEmail(),
                                "role", user.getRole()
                        ));
                        HttpHandlerHelper.sendJsonResponse(exchange, 200, JsonHelper.toJson(resp));
                    } else {
                        resp.put("success", false);
                        resp.put("message", "Unauthorized. Please log in.");
                        HttpHandlerHelper.sendJsonResponse(exchange, 401, JsonHelper.toJson(resp));
                    }
                } else if (path.endsWith("/logout") && "POST".equalsIgnoreCase(method)) {
                    String token = HttpHandlerHelper.getAuthToken(exchange);
                    SessionManager.removeSession(token);
                    resp.put("success", true);
                    HttpHandlerHelper.sendJsonResponse(exchange, 200, JsonHelper.toJson(resp));
                } else {
                    HttpHandlerHelper.sendJsonResponse(exchange, 404, "{\"error\": \"Endpoint not found\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                resp.put("success", false);
                resp.put("message", "Server error: " + e.getMessage());
                HttpHandlerHelper.sendJsonResponse(exchange, 500, JsonHelper.toJson(resp));
            }
        }
    }

    // =========================================================================
    // CORPUS HANDLER
    // =========================================================================
    private class CorpusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                HttpHandlerHelper.sendJsonResponse(exchange, 204, "");
                return;
            }

            try {
                if ("GET".equalsIgnoreCase(method)) {
                    List<DocumentModel> docs = db.getAllDocuments();
                    HttpHandlerHelper.sendJsonResponse(exchange, 200, JsonHelper.toJson(docs));
                } else if ("POST".equalsIgnoreCase(method)) {
                    String body = HttpHandlerHelper.readBody(exchange);
                    @SuppressWarnings("unchecked")
                    Map<String, String> req = JsonHelper.fromJson(body, Map.class);
                    String title = req.get("title");
                    String content = req.get("content");
                    String category = req.get("category");
                    String fileName = req.getOrDefault("fileName", "manual_input.txt");

                    if (title == null || content == null || content.trim().isEmpty()) {
                        HttpHandlerHelper.sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Title and content are required.\"}");
                        return;
                    }

                    String token = HttpHandlerHelper.getAuthToken(exchange);
                    User user = SessionManager.getUser(token);
                    String userId = user != null ? user.getId() : "GUEST";

                    DocumentModel doc = new DocumentModel(
                            "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                            userId,
                            title.trim(),
                            fileName,
                            content.trim(),
                            category != null ? category.trim() : "General"
                    );
                    db.saveDocument(doc);

                    Map<String, Object> resp = new HashMap<>();
                    resp.put("success", true);
                    resp.put("document", doc);
                    HttpHandlerHelper.sendJsonResponse(exchange, 201, JsonHelper.toJson(resp));
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    Map<String, String> q = HttpHandlerHelper.parseQueryParams(exchange.getRequestURI().getQuery());
                    String id = q.get("id");
                    if (id != null) {
                        db.deleteDocument(id);
                        HttpHandlerHelper.sendJsonResponse(exchange, 200, "{\"success\":true}");
                    } else {
                        HttpHandlerHelper.sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Missing document id\"}");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                HttpHandlerHelper.sendJsonResponse(exchange, 500, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // =========================================================================
    // PLAGIARISM CHECK HANDLER
    // =========================================================================
    private class PlagiarismCheckHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpHandlerHelper.sendJsonResponse(exchange, 204, "");
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpHandlerHelper.sendJsonResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
                return;
            }

            try {
                String body = HttpHandlerHelper.readBody(exchange);
                @SuppressWarnings("unchecked")
                Map<String, Object> req = JsonHelper.fromJson(body, Map.class);

                String mode = (String) req.getOrDefault("mode", "CORPUS"); // CORPUS or DIRECT
                String title = (String) req.getOrDefault("title", "Submitted Document");
                String content = (String) req.get("content");
                Boolean saveToCorpus = (Boolean) req.getOrDefault("saveToCorpus", false);

                if (content == null || content.trim().isEmpty()) {
                    HttpHandlerHelper.sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Input text content cannot be empty.\"}");
                    return;
                }

                String token = HttpHandlerHelper.getAuthToken(exchange);
                User user = SessionManager.getUser(token);
                String userId = user != null ? user.getId() : "GUEST";

                PlagiarismReport report;

                if ("DIRECT".equalsIgnoreCase(mode)) {
                    // Direct 2-document comparison
                    String doc2Title = (String) req.getOrDefault("doc2Title", "Reference Document");
                    String doc2Content = (String) req.get("doc2Content");
                    if (doc2Content == null || doc2Content.trim().isEmpty()) {
                        HttpHandlerHelper.sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Comparison document content is required for direct check.\"}");
                        return;
                    }
                    report = PlagiarismEngine.analyzeDirect(title, content, doc2Title, doc2Content);
                } else {
                    // Check against MongoDB corpus
                    List<DocumentModel> corpus = db.getAllDocuments();
                    report = PlagiarismEngine.analyzeAgainstCorpus(title, content, corpus);
                }

                report.setUserId(userId);
                // Save scan report to MongoDB
                db.saveReport(report);

                // If user opted to store document in corpus
                if (Boolean.TRUE.equals(saveToCorpus)) {
                    DocumentModel newCorpusDoc = new DocumentModel(
                            "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                            userId,
                            title,
                            "upload.txt",
                            content,
                            "User Upload"
                    );
                    db.saveDocument(newCorpusDoc);
                }

                Map<String, Object> resp = new HashMap<>();
                resp.put("success", true);
                resp.put("report", report);
                HttpHandlerHelper.sendJsonResponse(exchange, 200, JsonHelper.toJson(resp));
            } catch (Exception e) {
                e.printStackTrace();
                HttpHandlerHelper.sendJsonResponse(exchange, 500, "{\"success\":false,\"message\":\"Analysis failed: " + e.getMessage() + "\"}");
            }
        }
    }

    // =========================================================================
    // HISTORY HANDLER
    // =========================================================================
    private class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ("OPTIONS".equalsIgnoreCase(method)) {
                HttpHandlerHelper.sendJsonResponse(exchange, 204, "");
                return;
            }

            try {
                String token = HttpHandlerHelper.getAuthToken(exchange);
                User user = SessionManager.getUser(token);
                String userId = user != null ? user.getId() : "GUEST";

                Map<String, String> q = HttpHandlerHelper.parseQueryParams(exchange.getRequestURI().getQuery());

                if ("GET".equalsIgnoreCase(method)) {
                    String reportId = q.get("id");
                    if (reportId != null) {
                        PlagiarismReport report = db.getReportById(reportId);
                        if (report != null) {
                            HttpHandlerHelper.sendJsonResponse(exchange, 200, JsonHelper.toJson(report));
                        } else {
                            HttpHandlerHelper.sendJsonResponse(exchange, 404, "{\"success\":false,\"message\":\"Report not found\"}");
                        }
                    } else {
                        // Return list of past reports
                        List<PlagiarismReport> reports = db.getReportsByUser(userId);
                        HttpHandlerHelper.sendJsonResponse(exchange, 200, JsonHelper.toJson(reports));
                    }
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    String reportId = q.get("id");
                    if (reportId != null) {
                        db.deleteReport(reportId);
                        HttpHandlerHelper.sendJsonResponse(exchange, 200, "{\"success\":true}");
                    } else {
                        HttpHandlerHelper.sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Missing report id\"}");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                HttpHandlerHelper.sendJsonResponse(exchange, 500, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // =========================================================================
    // STATS HANDLER
    // =========================================================================
    private class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpHandlerHelper.sendJsonResponse(exchange, 204, "");
                return;
            }

            String token = HttpHandlerHelper.getAuthToken(exchange);
            User user = SessionManager.getUser(token);
            String userId = user != null ? user.getId() : "GUEST";

            List<PlagiarismReport> reports = db.getReportsByUser(userId);
            List<DocumentModel> docs = db.getAllDocuments();

            int totalScans = reports.size();
            double sumSim = 0.0;
            int lowCount = 0;
            int medCount = 0;
            int highCount = 0;

            for (PlagiarismReport r : reports) {
                sumSim += r.getOverallSimilarity();
                if ("LOW".equalsIgnoreCase(r.getSimilarityLevel())) lowCount++;
                else if ("MEDIUM".equalsIgnoreCase(r.getSimilarityLevel())) medCount++;
                else if ("HIGH".equalsIgnoreCase(r.getSimilarityLevel())) highCount++;
            }

            double avgSimilarity = totalScans > 0 ? Math.round((sumSim / totalScans) * 10.0) / 10.0 : 0.0;

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalScans", totalScans);
            stats.put("avgSimilarity", avgSimilarity);
            stats.put("corpusDocuments", docs.size());
            stats.put("lowRiskCount", lowCount);
            stats.put("mediumRiskCount", medCount);
            stats.put("highRiskCount", highCount);
            stats.put("isMongoConnected", db.isConnectedToMongo());

            HttpHandlerHelper.sendJsonResponse(exchange, 200, JsonHelper.toJson(stats));
        }
    }

    // =========================================================================
    // PRINTABLE REPORT GENERATOR
    // =========================================================================
    private class PrintableReportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> q = HttpHandlerHelper.parseQueryParams(exchange.getRequestURI().getQuery());
            String reportId = q.get("id");
            if (reportId == null) {
                HttpHandlerHelper.sendHtmlResponse(exchange, 400, "<h1>Missing Report ID</h1>");
                return;
            }

            PlagiarismReport r = db.getReportById(reportId);
            if (r == null) {
                HttpHandlerHelper.sendHtmlResponse(exchange, 404, "<h1>Report Not Found</h1>");
                return;
            }

            String levelColor = "#10b981"; // green
            if ("MEDIUM".equalsIgnoreCase(r.getSimilarityLevel())) levelColor = "#f59e0b"; // yellow
            if ("HIGH".equalsIgnoreCase(r.getSimilarityLevel())) levelColor = "#ef4444"; // red

            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
            sb.append("<title>Plagiarism Report - ").append(r.getId()).append("</title>");
            sb.append("<style>");
            sb.append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 40px; color: #1e293b; background: #fff; }");
            sb.append(".header { border-bottom: 2px solid #e2e8f0; padding-bottom: 20px; display: flex; justify-content: space-between; align-items: center; }");
            sb.append(".badge { display: inline-block; padding: 6px 14px; border-radius: 9999px; color: #fff; font-weight: bold; background: ").append(levelColor).append("; }");
            sb.append(".metrics-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin: 25px 0; }");
            sb.append(".metric-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 15px; text-align: center; }");
            sb.append(".metric-val { font-size: 24px; font-weight: 700; color: #0f172a; margin-top: 5px; }");
            sb.append(".content-box { border: 1px solid #cbd5e1; border-radius: 8px; padding: 20px; margin-top: 15px; line-height: 1.6; background: #fdfdfd; }");
            sb.append(".plag-highlight { background-color: #fef08a; padding: 2px 4px; border-radius: 3px; border-bottom: 2px solid #ca8a04; }");
            sb.append(".match-exact { background-color: #fecdd3; border-bottom-color: #e11d48; }");
            sb.append(".match-high { background-color: #fed7aa; border-bottom-color: #ea580c; }");
            sb.append(".btn-print { background: #4f46e5; color: #fff; border: none; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-size: 14px; margin-bottom: 20px; }");
            sb.append("@media print { .btn-print { display: none; } body { margin: 0; } }");
            sb.append("</style></head><body>");

            sb.append("<button class='btn-print' onclick='window.print()'>Print / Save as PDF</button>");
            sb.append("<div class='header'>");
            sb.append("<div><h1>Plagiarism Inspection Report</h1><p style='color:#64748b;'>Document: <strong>").append(r.getDocumentTitle()).append("</strong> | Report ID: ").append(r.getId()).append("</p></div>");
            sb.append("<div><span class='badge'>").append(r.getSimilarityLevel()).append(" SIMILARITY (").append(r.getOverallSimilarity()).append("%)</span></div>");
            sb.append("</div>");

            sb.append("<div class='metrics-grid'>");
            sb.append("<div class='metric-card'><div>Cosine Similarity</div><div class='metric-val'>").append(r.getCosineSimilarity()).append("%</div></div>");
            sb.append("<div class='metric-card'><div>N-Gram Analysis</div><div class='metric-val'>").append(r.getNgramSimilarity()).append("%</div></div>");
            sb.append("<div class='metric-card'><div>Total Words</div><div class='metric-val'>").append(r.getTotalWords()).append("</div></div>");
            sb.append("<div class='metric-card'><div>Matched Words</div><div class='metric-val'>").append(r.getMatchedWords()).append("</div></div>");
            sb.append("</div>");

            if (r.getTopMatchedDocTitle() != null) {
                sb.append("<p><strong>Primary Matched Reference Document:</strong> ").append(r.getTopMatchedDocTitle()).append("</p>");
            }

            sb.append("<h3>Highlighted Document Content</h3>");
            sb.append("<div class='content-box'>").append(r.getHighlightedSourceHtml()).append("</div>");

            if (r.getHighlightedMatchedHtml() != null && !r.getHighlightedMatchedHtml().isEmpty()) {
                sb.append("<h3>Top Matched Reference Document Content</h3>");
                sb.append("<div class='content-box'>").append(r.getHighlightedMatchedHtml()).append("</div>");
            }

            sb.append("<p style='margin-top:30px; font-size:12px; color:#94a3b8;'>Generated by Plagiarism Detection System using N-Gram Analysis & Cosine Similarity with MongoDB. Timestamp: ").append(r.getCheckedAt()).append("</p>");
            sb.append("</body></html>");

            HttpHandlerHelper.sendHtmlResponse(exchange, 200, sb.toString());
        }
    }

    // =========================================================================
    // SYSTEM / SERVER STATUS HANDLER
    // =========================================================================
    private class SystemStatusHandler implements HttpHandler {
        private final long startTime = System.currentTimeMillis();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpHandlerHelper.sendJsonResponse(exchange, 204, "");
                return;
            }

            Runtime rt = Runtime.getRuntime();
            long totalMem = rt.totalMemory() / (1024 * 1024);
            long freeMem = rt.freeMemory() / (1024 * 1024);
            long usedMem = totalMem - freeMem;
            long maxMem = rt.maxMemory() / (1024 * 1024);

            long uptimeSec = (System.currentTimeMillis() - startTime) / 1000;
            long hours = uptimeSec / 3600;
            long mins = (uptimeSec % 3600) / 60;
            long secs = uptimeSec % 60;
            String uptimeStr = String.format("%02dh %02dm %02ds", hours, mins, secs);

            Map<String, Object> status = new HashMap<>();
            status.put("status", "ONLINE");
            status.put("port", port);
            status.put("uptime", uptimeStr);
            status.put("uptimeSeconds", uptimeSec);
            status.put("usedMemoryMB", usedMem);
            status.put("totalMemoryMB", totalMem);
            status.put("maxMemoryMB", maxMem);
            status.put("activeThreads", Thread.activeCount());
            status.put("javaVersion", System.getProperty("java.version"));
            status.put("mongoConnected", db.isConnectedToMongo());
            status.put("databaseName", "plagiarism_db");
            status.put("corpusCount", db.getAllDocuments().size());
            status.put("totalScans", db.getReportsByUser("ALL").size());

            HttpHandlerHelper.sendJsonResponse(exchange, 200, JsonHelper.toJson(status));
        }
    }

    // =========================================================================
    // REPORT EXPORT HANDLER (CSV & TXT)
    // =========================================================================
    private class ReportExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> q = HttpHandlerHelper.parseQueryParams(exchange.getRequestURI().getQuery());
            String reportId = q.get("id");
            String format = q.getOrDefault("format", "csv").toLowerCase();

            if (reportId == null) {
                HttpHandlerHelper.sendJsonResponse(exchange, 400, "{\"error\":\"Missing report id\"}");
                return;
            }

            PlagiarismReport r = db.getReportById(reportId);
            if (r == null) {
                HttpHandlerHelper.sendJsonResponse(exchange, 404, "{\"error\":\"Report not found\"}");
                return;
            }

            if ("csv".equals(format)) {
                StringBuilder csv = new StringBuilder();
                csv.append("Report ID,Document Title,Overall Similarity %,Level,Cosine %,N-Gram %,Total Words,Matched Words,Top Matched Doc,Checked At\n");
                csv.append(String.format("\"%s\",\"%s\",%.1f,\"%s\",%.1f,%.1f,%d,%d,\"%s\",\"%s\"\n\n",
                        r.getId(),
                        (r.getDocumentTitle() != null ? r.getDocumentTitle() : "Untitled").replace("\"", "\"\""),
                        r.getOverallSimilarity(),
                        r.getSimilarityLevel(),
                        r.getCosineSimilarity(),
                        r.getNgramSimilarity(),
                        r.getTotalWords(),
                        r.getMatchedWords(),
                        (r.getTopMatchedDocTitle() != null ? r.getTopMatchedDocTitle() : "None").replace("\"", "\"\""),
                        r.getCheckedAt()));

                csv.append("Matched Snippet #,Match Type,Confidence %,Submitted Passage,Matched Reference Doc,Matched In Passage\n");
                int idx = 1;
                for (com.plagiarism.model.MatchSegment seg : r.getMatchedSegments()) {
                    csv.append(String.format("%d,\"%s\",%.1f,\"%s\",\"%s\",\"%s\"\n",
                            idx++,
                            seg.getMatchType(),
                            seg.getSimilarityScore(),
                            seg.getSourceSnippet().replace("\"", "\"\"").replace("\n", " "),
                            (seg.getReferenceDocTitle() != null ? seg.getReferenceDocTitle() : "").replace("\"", "\"\""),
                            seg.getMatchedSnippet().replace("\"", "\"\"").replace("\n", " ")));
                }

                byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"PlagiarismReport-" + r.getId() + ".csv\"");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.flush();
                os.close();
            } else {
                // Formatted Text Summary
                StringBuilder txt = new StringBuilder();
                txt.append("=================================================================\n");
                txt.append("               PLAGIARISM INSPECTION REPORT                      \n");
                txt.append("=================================================================\n\n");
                txt.append("Report ID:            ").append(r.getId()).append("\n");
                txt.append("Document Title:       ").append(r.getDocumentTitle()).append("\n");
                txt.append("Checked At:           ").append(r.getCheckedAt()).append("\n");
                txt.append("Overall Plagiarism:   ").append(r.getOverallSimilarity()).append("% (").append(r.getSimilarityLevel()).append(" RISK)\n");
                txt.append("Cosine Similarity:    ").append(r.getCosineSimilarity()).append("%\n");
                txt.append("N-Gram Containment:   ").append(r.getNgramSimilarity()).append("%\n");
                txt.append("Total Words:          ").append(r.getTotalWords()).append("\n");
                txt.append("Matched Words:        ").append(r.getMatchedWords()).append("\n");
                txt.append("Top Matched Doc:      ").append(r.getTopMatchedDocTitle() != null ? r.getTopMatchedDocTitle() : "None").append("\n\n");
                txt.append("-----------------------------------------------------------------\n");
                txt.append("IDENTIFIED MATCHING PASSAGES:\n");
                txt.append("-----------------------------------------------------------------\n");
                int i = 1;
                for (com.plagiarism.model.MatchSegment seg : r.getMatchedSegments()) {
                    txt.append(String.format("[%d] %s MATCH (Confidence: %.1f%%)\n", i++, seg.getMatchType(), seg.getSimilarityScore()));
                    txt.append("    Submitted Text:  \"").append(seg.getSourceSnippet()).append("\"\n");
                    txt.append("    Matched in:      ").append(seg.getReferenceDocTitle()).append("\n");
                    txt.append("    Reference Text:  \"").append(seg.getMatchedSnippet()).append("\"\n\n");
                }

                byte[] bytes = txt.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"PlagiarismReport-" + r.getId() + ".txt\"");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.flush();
                os.close();
            }
        }
    }

    // =========================================================================
    // STATIC FILE HANDLER
    // =========================================================================
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uriPath = exchange.getRequestURI().getPath();
            if (uriPath.equals("/") || uriPath.isEmpty()) {
                uriPath = "/index.html";
            }

            // Resolve file from static folder
            Path filePath = Paths.get("src", "main", "resources", "static", uriPath.replace("/", File.separator));

            // Also fallback to current directory if running standalone
            if (!Files.exists(filePath)) {
                filePath = Paths.get("static", uriPath.replace("/", File.separator));
            }

            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String contentType = getContentType(uriPath);
                byte[] bytes = Files.readAllBytes(filePath);

                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.flush();
                os.close();
            } else {
                // If not found, check if index.html can serve as SPA fallback
                Path indexHtml = Paths.get("src", "main", "resources", "static", "index.html");
                if (Files.exists(indexHtml)) {
                    byte[] bytes = Files.readAllBytes(indexHtml);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                    os.flush();
                    os.close();
                } else {
                    String notFound = "<h1>404 Not Found</h1><p>Resource " + uriPath + " not found.</p>";
                    exchange.sendResponseHeaders(404, notFound.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(notFound.getBytes());
                    os.flush();
                    os.close();
                }
            }
        }

        private String getContentType(String path) {
            String lower = path.toLowerCase();
            if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
            if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".ico")) return "image/x-icon";
            return "text/plain; charset=UTF-8";
        }
    }
}
