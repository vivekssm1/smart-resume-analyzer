# 🧠 Smart Resume Analyzer

An AI-powered resume analysis tool built with **Java Spring Boot** backend, **MongoDB** database, and a clean modern frontend.

Upload a PDF or DOCX resume and instantly get:
- ✅ Extracted contact info (name, email, phone, LinkedIn, GitHub)
- 🛠️ Skills & programming languages detection
- 🎓 Education history
- 💼 Work experience
- 📊 ATS (Applicant Tracking System) compatibility score
- 💡 Personalized improvement suggestions

---

## 📁 Project Structure

```
smart-resume-analyzer/
│
├── backend/                          ← Java Spring Boot application
│   ├── pom.xml                       ← Maven dependencies
│   └── src/main/
│       ├── java/com/resumeanalyzer/
│       │   ├── SmartResumeAnalyzerApplication.java   ← Entry point
│       │   ├── controller/
│       │   │   └── ResumeController.java             ← REST API endpoints
│       │   ├── model/
│       │   │   └── Resume.java                       ← MongoDB document model
│       │   ├── repository/
│       │   │   └── ResumeRepository.java             ← MongoDB queries
│       │   ├── service/
│       │   │   ├── ResumeService.java                ← Business logic
│       │   │   ├── ResumeParserService.java          ← Core analysis engine
│       │   │   └── TextExtractionService.java        ← PDF/DOCX text extraction
│       │   └── config/
│       │       └── WebConfig.java                    ← CORS + static files
│       └── resources/
│           ├── application.properties                ← App configuration
│           └── static/                               ← Frontend files (served by Spring)
│               ├── index.html
│               ├── css/style.css
│               └── js/app.js
│
├── frontend/                         ← Frontend source files
│   ├── index.html
│   ├── css/style.css
│   └── js/app.js
│
├── render.yaml                       ← Render.com deployment config
└── README.md                         ← This file
```

---

## 🛠️ Technology Stack

| Layer      | Technology                      |
|------------|----------------------------------|
| Backend    | Java 17, Spring Boot 3.2         |
| Database   | MongoDB (via Spring Data MongoDB) |
| PDF Parse  | Apache PDFBox 3.0                |
| DOCX Parse | Apache POI 5.2                   |
| Build Tool | Maven                             |
| Frontend   | HTML5, CSS3, Vanilla JavaScript   |
| Deployment | Render.com                        |

---

## 🚀 Getting Started — Local Development

### Prerequisites
- Java 17 or higher
- Maven 3.8+
- MongoDB (local or MongoDB Atlas)

### Step 1 — Clone / Extract the project
```bash
cd smart-resume-analyzer
```

### Step 2 — Configure MongoDB

**Option A — Local MongoDB:**
```properties
# backend/src/main/resources/application.properties
spring.data.mongodb.uri=mongodb://localhost:27017/resumeanalyzer
```

**Option B — MongoDB Atlas (Cloud):**
1. Go to https://cloud.mongodb.com
2. Create a free cluster
3. Get connection string: `mongodb+srv://user:pass@cluster.mongodb.net/resumeanalyzer`
4. Set it in `application.properties`:
```properties
spring.data.mongodb.uri=mongodb+srv://YOUR_USER:YOUR_PASS@cluster.mongodb.net/resumeanalyzer
```

### Step 3 — Build and Run
```bash
cd backend
mvn clean package -DskipTests
java -jar target/smart-resume-analyzer-1.0.0.jar
```

### Step 4 — Open in Browser
```
http://localhost:8080
```

---

## 🌐 Deploying to Render.com

### Step 1 — Push to GitHub
```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/YOUR_USERNAME/smart-resume-analyzer.git
git push -u origin main
```

### Step 2 — Create MongoDB Atlas Database
1. Go to https://cloud.mongodb.com → Create free cluster
2. Create database user (username + password)
3. Allow network access from anywhere: `0.0.0.0/0`
4. Copy your connection string:
   `mongodb+srv://user:password@cluster.mongodb.net/resumeanalyzer`

### Step 3 — Deploy on Render
1. Go to https://render.com → Sign up / Log in
2. Click **"New +"** → **"Web Service"**
3. Connect your GitHub repository
4. Configure:
   - **Name:** `smart-resume-analyzer`
   - **Runtime:** `Java`
   - **Build Command:** `cd backend && mvn clean package -DskipTests`
   - **Start Command:** `java -jar backend/target/smart-resume-analyzer-1.0.0.jar`
5. Add **Environment Variable:**
   - Key: `MONGODB_URI`
   - Value: *(your MongoDB Atlas connection string)*
6. Click **"Create Web Service"**
7. Wait ~5 minutes for build and deploy ✅

Your app will be live at: `https://smart-resume-analyzer.onrender.com`

---

## 📡 API Endpoints

| Method | Endpoint                     | Description                        |
|--------|------------------------------|------------------------------------|
| POST   | `/api/resumes/analyze`       | Upload & analyze resume (PDF/DOCX) |
| GET    | `/api/resumes`               | Get all analyzed resumes           |
| GET    | `/api/resumes/{id}`          | Get resume by ID                   |
| DELETE | `/api/resumes/{id}`          | Delete a resume                    |
| GET    | `/api/resumes/search?skill=` | Search resumes by skill            |
| GET    | `/api/dashboard/stats`       | Get statistics                     |
| GET    | `/api/health`                | Health check                       |

### Example: Analyze a resume
```bash
curl -X POST http://localhost:8080/api/resumes/analyze \
  -F "file=@resume.pdf"
```

### Example Response
```json
{
  "success": true,
  "message": "Resume analyzed successfully!",
  "data": {
    "id": "65f1a2b3c4d5e6f7g8h9i0j1",
    "candidateName": "John Smith",
    "email": "john.smith@email.com",
    "phone": "+91 98765 43210",
    "skills": ["Java", "Spring Boot", "MongoDB", "React", "Docker"],
    "experienceLevel": "Mid-Level",
    "totalExperienceYears": 3,
    "atsScore": 78,
    "suggestions": [
      "🔗 Add your LinkedIn profile URL",
      "💻 Add your GitHub profile"
    ]
  }
}
```

---

## 🧩 How ATS Score is Calculated

| Category          | Max Points |
|-------------------|-----------|
| Email present     | 8         |
| Phone present     | 7         |
| LinkedIn URL      | 5         |
| Skills count      | 25        |
| Education section | 15        |
| Work experience   | 20        |
| Content length    | 10        |
| Section count     | 10        |
| **Total**         | **100**   |

---

## 📝 Notes

- Resume text extraction works best with **text-based PDFs** (not scanned images)
- For image-only scanned PDFs, you would need to add OCR (Tesseract) — not included in this version
- The analysis is rule-based + regex pattern matching — no external AI API needed
- All data is stored in MongoDB and persists across sessions

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch: `git checkout -b feature/new-feature`
3. Commit your changes: `git commit -m 'Add new feature'`
4. Push to the branch: `git push origin feature/new-feature`
5. Open a Pull Request

---

## 📄 License

MIT License — free to use and modify for personal and commercial projects.
