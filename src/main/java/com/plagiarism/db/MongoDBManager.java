package com.plagiarism.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.plagiarism.model.DocumentModel;
import com.plagiarism.model.PlagiarismReport;
import com.plagiarism.model.User;
import com.plagiarism.util.JsonHelper;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB Data Access Layer.
 * Connects to MongoDB on mongodb://localhost:27017/plagiarism_db
 * Features in-memory fallback cache so application remains robustly operable even if MongoDB daemon is offline.
 */
public class MongoDBManager {

    private static MongoDBManager instance;

    private MongoClient mongoClient;
    private MongoDatabase database;
    private boolean connectedToMongo = false;

    // In-memory fallback stores
    private final Map<String, User> memoryUsers = new ConcurrentHashMap<>();
    private final Map<String, DocumentModel> memoryDocuments = new ConcurrentHashMap<>();
    private final Map<String, PlagiarismReport> memoryReports = new ConcurrentHashMap<>();

    private MongoDBManager() {
        initConnection();
        seedInitialData();
    }

    public static synchronized MongoDBManager getInstance() {
        if (instance == null) {
            instance = new MongoDBManager();
        }
        return instance;
    }

    private void initConnection() {
        try {
            String connectionString = System.getProperty("mongodb.uri", "mongodb://localhost:27017/?serverSelectionTimeoutMS=2000&connectTimeoutMS=2000");
            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase("plagiarism_db");

            // Test connection with a quick ping command
            database.runCommand(new Document("ping", 1));
            connectedToMongo = true;
            System.out.println("[MongoDB] Successfully connected to MongoDB database 'plagiarism_db' at localhost:27017");
        } catch (Exception e) {
            connectedToMongo = false;
            System.err.println("[MongoDB] Warning: Could not connect to local MongoDB daemon (" + e.getMessage() + ").");
            System.err.println("[MongoDB] Running in high-reliability hybrid memory mode. Data is preserved during session.");
        }
    }

    private void seedInitialData() {
        // Seed users
        for (User u : DataSeeder.getSeedUsers()) {
            if (findUserByUsername(u.getUsername()) == null) {
                saveUser(u);
            }
        }

        // Seed documents
        if (getAllDocuments().isEmpty()) {
            for (DocumentModel doc : DataSeeder.getSeedDocuments()) {
                saveDocument(doc);
            }
            System.out.println("[MongoDB] Initialized reference corpus with " + getAllDocuments().size() + " standard documents.");
        }
    }

    // =========================================================================
    // USER OPERATIONS
    // =========================================================================

    public User findUserByUsername(String username) {
        if (username == null) return null;

        if (connectedToMongo) {
            try {
                MongoCollection<Document> col = database.getCollection("users");
                Document doc = col.find(Filters.eq("username", username)).first();
                if (doc != null) {
                    return documentToUser(doc);
                }
            } catch (Exception e) {
                System.err.println("[MongoDB] Error finding user: " + e.getMessage());
            }
        }

        for (User u : memoryUsers.values()) {
            if (username.equalsIgnoreCase(u.getUsername())) {
                return u;
            }
        }
        return null;
    }

    public User findUserById(String id) {
        if (id == null) return null;

        if (connectedToMongo) {
            try {
                MongoCollection<Document> col = database.getCollection("users");
                Document doc = col.find(Filters.eq("id", id)).first();
                if (doc != null) {
                    return documentToUser(doc);
                }
            } catch (Exception e) {
                System.err.println("[MongoDB] Error finding user by ID: " + e.getMessage());
            }
        }
        return memoryUsers.get(id);
    }

    public void saveUser(User user) {
        if (user == null) return;
        if (user.getId() == null) {
            user.setId("USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        memoryUsers.put(user.getId(), user);

        if (connectedToMongo) {
            try {
                MongoCollection<Document> col = database.getCollection("users");
                Document bDoc = userToDocument(user);
                col.replaceOne(Filters.eq("id", user.getId()), bDoc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
            } catch (Exception e) {
                System.err.println("[MongoDB] Error saving user to MongoDB: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // DOCUMENT / CORPUS OPERATIONS
    // =========================================================================

    public List<DocumentModel> getAllDocuments() {
        if (connectedToMongo) {
            try {
                MongoCollection<Document> col = database.getCollection("documents");
                List<DocumentModel> list = new ArrayList<>();
                for (Document doc : col.find()) {
                    list.add(documentToModel(doc));
                }
                return list;
            } catch (Exception e) {
                System.err.println("[MongoDB] Error fetching documents: " + e.getMessage());
            }
        }
        return new ArrayList<>(memoryDocuments.values());
    }

    public DocumentModel getDocumentById(String id) {
        if (id == null) return null;

        if (connectedToMongo) {
            try {
                MongoCollection<Document> col = database.getCollection("documents");
                Document doc = col.find(Filters.eq("id", id)).first();
                if (doc != null) {
                    return documentToModel(doc);
                }
            } catch (Exception e) {
                System.err.println("[MongoDB] Error fetching document by ID: " + e.getMessage());
            }
        }
        return memoryDocuments.get(id);
    }

    public void saveDocument(DocumentModel doc) {
        if (doc == null) return;
        if (doc.getId() == null) {
            doc.setId("DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        memoryDocuments.put(doc.getId(), doc);

        if (connectedToMongo) {
            try {
                MongoCollection<Document> col = database.getCollection("documents");
                Document bDoc = modelToDocument(doc);
                col.replaceOne(Filters.eq("id", doc.getId()), bDoc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
            } catch (Exception e) {
                System.err.println("[MongoDB] Error saving document to MongoDB: " + e.getMessage());
            }
        }
    }

    public boolean deleteDocument(String id) {
        memoryDocuments.remove(id);

        if (connectedToMongo) {
            try {
                MongoCollection<Document> col = database.getCollection("documents");
                col.deleteOne(Filters.eq("id", id));
                return true;
            } catch (Exception e) {
                System.err.println("[MongoDB] Error deleting document: " + e.getMessage());
            }
        }
        return true;
    }

    // =========================================================================
    // PLAGIARISM REPORT / HISTORY OPERATIONS
    // =========================================================================

    public void saveReport(PlagiarismReport report) {
        if (report == null) return;
        if (report.getId() == null) {
            report.setId("REP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        memoryReports.put(report.getId(), report);

        if (connectedToMongo) {
            try {
                MongoCollection<Document> col = database.getCollection("scan_reports");
                String json = JsonHelper.toJson(report);
                Document bDoc = Document.parse(json);
                col.replaceOne(Filters.eq("id", report.getId()), bDoc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
            } catch (Exception e) {
                System.err.println("[MongoDB] Error saving report to MongoDB: " + e.getMessage());
            }
        }
    }

    public List<PlagiarismReport> getReportsByUser(String userId) {
        if (connectedToMongo) {
            try {
                MongoCollection<Document> col = database.getCollection("scan_reports");
                List<PlagiarismReport> list = new ArrayList<>();
                com.mongodb.client.FindIterable<Document> query = (userId != null && !userId.isEmpty() && !"ALL".equalsIgnoreCase(userId))
                        ? col.find(Filters.eq("userId", userId))
                        : col.find();

                for (Document doc : query.sort(Sorts.descending("checkedAt"))) {
                    list.add(JsonHelper.fromJson(doc.toJson(), PlagiarismReport.class));
                }
                return list;
            } catch (Exception e) {
                System.err.println("[MongoDB] Error fetching reports: " + e.getMessage());
            }
        }

        List<PlagiarismReport> userReports = new ArrayList<>();
        for (PlagiarismReport rep : memoryReports.values()) {
            if (userId == null || "ALL".equalsIgnoreCase(userId) || userId.equals(rep.getUserId())) {
                userReports.add(rep);
            }
        }
        userReports.sort((a, b) -> {
            if (a.getCheckedAt() == null || b.getCheckedAt() == null) return 0;
            return b.getCheckedAt().compareTo(a.getCheckedAt());
        });
        return userReports;
    }

    public PlagiarismReport getReportById(String reportId) {
        if (reportId == null) return null;

        if (connectedToMongo) {
            try {
                MongoCollection<Document> col = database.getCollection("scan_reports");
                Document doc = col.find(Filters.eq("id", reportId)).first();
                if (doc != null) {
                    return JsonHelper.fromJson(doc.toJson(), PlagiarismReport.class);
                }
            } catch (Exception e) {
                System.err.println("[MongoDB] Error fetching report by ID: " + e.getMessage());
            }
        }
        return memoryReports.get(reportId);
    }

    public boolean deleteReport(String reportId) {
        memoryReports.remove(reportId);

        if (connectedToMongo) {
            try {
                MongoCollection<Document> col = database.getCollection("scan_reports");
                col.deleteOne(Filters.eq("id", reportId));
                return true;
            } catch (Exception e) {
                System.err.println("[MongoDB] Error deleting report: " + e.getMessage());
            }
        }
        return true;
    }

    public boolean isConnectedToMongo() {
        return connectedToMongo;
    }

    // =========================================================================
    // CONVERTERS
    // =========================================================================

    private Document userToDocument(User user) {
        return new Document("id", user.getId())
                .append("username", user.getUsername())
                .append("email", user.getEmail())
                .append("passwordHash", user.getPasswordHash())
                .append("fullName", user.getFullName())
                .append("role", user.getRole())
                .append("createdAt", user.getCreatedAt());
    }

    private User documentToUser(Document doc) {
        User u = new User();
        u.setId(doc.getString("id"));
        u.setUsername(doc.getString("username"));
        u.setEmail(doc.getString("email"));
        u.setPasswordHash(doc.getString("passwordHash"));
        u.setFullName(doc.getString("fullName"));
        u.setRole(doc.getString("role"));
        u.setCreatedAt(doc.getDate("createdAt"));
        return u;
    }

    private Document modelToDocument(DocumentModel model) {
        return new Document("id", model.getId())
                .append("userId", model.getUserId())
                .append("title", model.getTitle())
                .append("fileName", model.getFileName())
                .append("content", model.getContent())
                .append("wordCount", model.getWordCount())
                .append("category", model.getCategory())
                .append("uploadedAt", model.getUploadedAt());
    }

    private DocumentModel documentToModel(Document doc) {
        DocumentModel m = new DocumentModel();
        m.setId(doc.getString("id"));
        m.setUserId(doc.getString("userId"));
        m.setTitle(doc.getString("title"));
        m.setFileName(doc.getString("fileName"));
        m.setContent(doc.getString("content"));
        Integer wc = doc.getInteger("wordCount");
        m.setWordCount(wc != null ? wc : 0);
        m.setCategory(doc.getString("category"));
        m.setUploadedAt(doc.getDate("uploadedAt"));
        return m;
    }
}
