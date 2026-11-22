# Plagiarism Detection System

A complete web-based **Plagiarism Detection System** developed using **Java 17** and **MongoDB**. The system detects similarities between two or more text documents, identifies copied content down to sentence and phrase level, calculates mathematical similarity percentages, and visually highlights overlapping passages side-by-side.

---

## 🌟 Key Features

1. **User Authentication & Dashboard**
   - User registration and login with secure salted SHA-256 password hashing.
   - Comprehensive dashboard showing total scans, average similarity score, flagged documents, and database connection status.
   - Role-based demo accounts for instant testing (`admin`, `student`, `professor`).

2. **Text Input & Multi-Format Document Ingestion**
   - Manual text entry with real-time word count, character count, and reading metrics.
   - Drag-and-drop file upload supporting `.txt`, `.md`, `.json`, `.csv`, etc.
   - One-click sample test presets for quick demonstration (Copied AI Essay vs. Original Safe Paper).

3. **Plagiarism Detection Algorithms**
   - **Vector Space Cosine Similarity**: Tokenizes text, filters stop words, constructs Term Frequency (TF) vectors, and computes the cosine angle $\cos(\theta) = \frac{\vec{A} \cdot \vec{B}}{\|\vec{A}\| \|\vec{B}\|}$.
   - **N-Gram Analysis**: Extracts $N$-word sequences (bigrams and trigrams) to evaluate phrase-level syntactical structure and Jaccard containment $\frac{|N(A) \cap N(B)|}{|N(A)|}$.
   - **Harmonic Similarity Score**: Combines lexical distribution and structural phrasing into a unified $0\% - 100\%$ score.
   - **Risk Classification**:
     - 🟢 **Low Similarity**: $0\% - 20\%$ (Safe / Original)
     - 🟡 **Medium Similarity**: $20\% - 50\%$ (Moderate Overlap / Review Required)
     - 🔴 **High Similarity**: $> 50\%$ (Substantial Plagiarism / Copied)

4. **Synchronized Visual Diff & Highlighting**
   - Dual-column side-by-side view (Submitted Text vs. Matching Reference Document).
   - Highlighting categorized by match severity:
     - <mark class="match-exact" style="background:#fecdd3; color:#991b1b; padding:2px 4px;">Exact Match (&ge;80%)</mark>
     - <mark class="match-high" style="background:#fed7aa; color:#9a3412; padding:2px 4px;">High Match (60% - 79%)</mark>
     - <mark class="match-moderate" style="background:#fef9c3; color:#854d0e; padding:2px 4px;">Moderate Match (45% - 59%)</mark>

5. **MongoDB Integration**
   - Stores user accounts, uploaded documents in the reference corpus, and inspection reports.
   - Built-in automatic fallback to high-reliability memory storage if MongoDB is temporarily offline.
   - Pre-seeded with realistic computer science and academic reference documents for out-of-the-box comparison.

6. **Reporting & Scan History**
   - Complete scan history log with search and filtering by risk level.
   - Downloadable and printable **PDF Plagiarism Report** with breakdown metrics and highlighted passages.
   - Exportable **JSON Report** for programmatic integration.

---

## 🏗️ Project Architecture & Folder Structure

```
Plagiarism Detection System/
├── bin/                                  # Compiled Java bytecode (.class files)
├── lib/                                  # Standalone Java dependencies
│   ├── bson-5.0.1.jar                    # BSON document encoder/decoder
│   ├── gson-2.8.9.jar                    # JSON serialization engine
│   ├── jackson-annotations-2.17.1.jar    # Jackson metadata annotations
│   ├── jackson-core-2.17.1.jar           # Jackson streaming parser
│   ├── jackson-databind-2.17.1.jar       # Jackson object mapping
│   ├── mongodb-driver-core-5.0.1.jar     # Core MongoDB protocol implementation
│   ├── mongodb-driver-sync-5.0.1.jar     # Official MongoDB synchronous Java client
│   └── slf4j-api-2.0.13.jar              # Logging abstraction API
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── plagiarism/
│       │           ├── PlagiarismApplication.java    # Main application bootstrap
│       │           ├── PlagiarismEngineTest.java     # Algorithmic unit test suite
│       │           ├── algo/
│       │           │   ├── CosineSimilarity.java     # Term-Frequency Vector Cosine algorithm
│       │           │   ├── NGramAnalyzer.java        # N-Gram extraction & Jaccard containment
│       │           │   ├── PlagiarismEngine.java     # Core plagiarism scoring & matching
│       │           │   └── TextPreprocessor.java     # Tokenization, stop words & sentence split
│       │           ├── db/
│       │           │   ├── DataSeeder.java           # Initial test users & reference documents
│       │           │   └── MongoDBManager.java       # MongoDB connection & CRUD data access
│       │           ├── model/
│       │           │   ├── DocumentModel.java        # Corpus document entity
│       │           │   ├── MatchSegment.java         # Overlapping snippet model
│       │           │   ├── PlagiarismReport.java     # Analysis report & metrics model
│       │           │   └── User.java                 # User account entity
│       │           ├── server/
│       │           │   ├── HttpHandlerHelper.java    # HTTP exchange, JSON & query parser
│       │           │   ├── SessionManager.java       # User authentication session cache
│       │           │   └── WebServer.java            # Built-in REST API & static file server
│       │           └── util/
│       │               ├── JsonHelper.java           # JSON utility using Gson
│       │               └── PasswordHasher.java       # Salted SHA-256 password security
│       └── resources/
│           ├── application.properties                # Port & database configuration
│           └── static/                               # Modern Web Frontend
│               ├── css/
│               │   └── style.css                     # Responsive UI styling & highlight classes
│               ├── js/
│               │   ├── api.js                        # API client, tokens & toasts
│               │   ├── auth.js                       # Login & registration logic
│               │   ├── corpus.js                     # Reference corpus management
│               │   ├── dashboard.js                  # Analytics & recent activity
│               │   ├── history.js                    # Scan logs & filtering
│               │   ├── result.js                     # Circular gauge & synchronized diff
│               │   └── scan.js                       # Input ingestion & scan controller
│               ├── corpus.html                       # Reference Document Corpus UI
│               ├── dashboard.html                    # User Analytics Dashboard
│               ├── history.html                      # Scan History UI
│               ├── index.html                        # Modern Landing Page
│               ├── login.html                        # Sign In Page
│               ├── register.html                     # Registration Page
│               ├── result.html                       # Plagiarism Analysis Report UI
│               └── scan.html                         # Text & File Submission Studio
├── pom.xml                                           # Maven Project Configuration
├── run.bat                                           # One-click Windows Command Launcher
├── run.ps1                                           # PowerShell Launcher
├── start-background.bat                              # Start server silently in background
├── stop-server.bat                                   # Stop running background server
├── server-status.bat                                 # Probe server health & MongoDB status
├── start-mongo.bat                                   # Standalone local MongoDB runner
└── README.md                                         # Complete Project Documentation
```

---

## 🗄️ MongoDB Database Configuration

The application connects to MongoDB using the standard URI:
```properties
mongodb://localhost:27017/plagiarism_db
```

### Collections Schema:

1. **`users` Collection**
   ```json
   {
     "id": "USR-10A2B3C4",
     "username": "student",
     "email": "student@university.edu",
     "passwordHash": "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
     "fullName": "Alex Johnson",
     "role": "USER",
     "createdAt": "2026-11-22T19:00:00Z"
   }
   ```

2. **`documents` Collection (Reference Corpus)**
   ```json
   {
     "id": "doc_ai_intro",
     "userId": "usr_admin",
     "title": "Introduction to Artificial Intelligence and Machine Learning",
     "fileName": "ai_intro.txt",
     "content": "Artificial intelligence is a wide-ranging branch of computer science...",
     "wordCount": 114,
     "category": "Artificial Intelligence",
     "uploadedAt": "2026-11-22T19:00:00Z"
   }
   ```

3. **`scan_reports` Collection (Comparison Results)**
   ```json
   {
     "id": "REP-8A2F9C11",
     "userId": "usr_student",
     "documentTitle": "AI Research Essay",
     "originalContent": "...",
     "overallSimilarity": 84.7,
     "similarityLevel": "HIGH",
     "cosineSimilarity": 71.8,
     "ngramSimilarity": 95.2,
     "totalWords": 142,
     "matchedWords": 118,
     "topMatchedDocId": "doc_ai_intro",
     "topMatchedDocTitle": "Introduction to Artificial Intelligence and Machine Learning",
     "matchedSegments": [ ... ],
     "documentBreakdown": [ ... ],
     "checkedAt": "2026-11-22T19:15:00Z"
   }
   ```

---

## 🚀 Step-by-Step Execution Guide

### Prerequisites:
- **Java Development Kit (JDK) 17 or higher** installed.
- **MongoDB Community Server** (Optional but recommended; if MongoDB is not running, the application gracefully operates in memory cache mode).

---

### Method 1: One-Click Execution (Recommended for Windows)

Simply double-click:
```bash
run.bat
```
or run in PowerShell:
```powershell
.\run.ps1
```

**What the runner does automatically:**
1. Verifies `JAVA_HOME` and JDK 17.
2. Compiles all Java source files into `bin/`.
3. Starts the server on `http://localhost:8080`.
4. Automatically opens your default web browser to the application!

---

### Method 2: Manual Terminal Execution

#### 1. Compile Java Source Files:
```powershell
javac -encoding UTF-8 -cp "bin;lib/*" -d ".\bin" (Get-ChildItem -Path ".\src\main\java" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName)
```

#### 2. Start the Application:
```powershell
java -cp "bin;lib/*" com.plagiarism.PlagiarismApplication 8080
```

#### 3. Access in Browser:
Open [http://localhost:8080](http://localhost:8080).

---

### Method 3: Optional Local MongoDB Daemon

If you wish to run a dedicated local MongoDB instance:
```bash
start-mongo.bat
```
*(Or start the "MongoDB Server" service in Windows Services).*

---

## 👥 Default Demo Accounts

For immediate testing, use any of the pre-configured accounts:

| Role | Username | Password | Access Level |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` | Full corpus & report management |
| **Student** | `student` | `student123` | Document submission & history |
| **Professor** | `professor` | `prof123` | Document submission & grading |

*(Or click the one-click demo buttons on `login.html`).*

---

## 🧪 Testing the Algorithms

To run the standalone algorithmic verification suite:
```powershell
java -cp "bin;lib/*" com.plagiarism.PlagiarismEngineTest
```

**Output:**
```
>>> STARTING PLAGIARISM ALGORITHM VERIFICATION <<<
[TEST 1] Identical Text Cosine: 100.00% (Expected: 100%)
[TEST 2] Disjoint Text Cosine: 0.00% (Expected: 0%)
[TEST 3] Trigram Jaccard: 100.00%, Containment: 100.00%
[TEST 4] Reference Corpus size: 5 documents.
--------------------------------------------------
  Plagiarized Sample Results:
  Overall Similarity : 84.7%
  Similarity Level   : HIGH
  Cosine Similarity  : 71.8%
  N-Gram Similarity  : 95.2%
  Top Matched Doc    : Introduction to Artificial Intelligence and Machine Learning
  Matched Snippets   : 2
--------------------------------------------------
  Original Sample Results:
  Overall Similarity : 0.0%
  Similarity Level   : LOW
  Cosine Similarity  : 0.0%
  N-Gram Similarity  : 0.0%
--------------------------------------------------
[TEST 6] Direct Comparison Similarity: 84.7% (HIGH)
>>> ALL ALGORITHMIC TESTS PASSED PERFECTLY! <<<
```

---

## 🌐 REST API Endpoints Reference

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Authenticate user and issue session token |
| `GET` | `/api/auth/me` | Fetch currently logged in user profile |
| `POST` | `/api/auth/logout` | Terminate session |
| `GET` | `/api/corpus` | List all reference corpus documents |
| `POST` | `/api/corpus` | Add a new document to MongoDB corpus |
| `DELETE`| `/api/corpus?id={id}` | Delete a document from corpus |
| `POST` | `/api/plagiarism/check` | Analyze text or uploaded document for plagiarism |
| `GET` | `/api/history` | Retrieve user's past scan reports |
| `GET` | `/api/history?id={id}`| Retrieve a specific scan report by ID |
| `DELETE`| `/api/history?id={id}`| Delete a scan report |
| `GET` | `/api/stats` | Retrieve dashboard analytics & counts |
| `GET` | `/api/system/status` | Live JVM memory, uptime, threads, and MongoDB status |
| `GET` | `/api/report/export?id={id}&format={csv\|txt}` | Direct backend CSV or formatted TXT download |
| `GET` | `/api/report/printable?id={id}` | Render formatted printable / PDF-ready report |

---

## 📜 License
Educational and Academic Use • Developed for Plagiarism Detection System Project.
#   p l a g i a r i s m - d e t e c t i o n - s y s t e m  
 