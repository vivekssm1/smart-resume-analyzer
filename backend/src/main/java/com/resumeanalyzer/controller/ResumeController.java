package com.resumeanalyzer.controller;

import com.resumeanalyzer.model.Resume;
import com.resumeanalyzer.service.ResumeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ResumeController
 *
 * REST API Controller that exposes the following endpoints:
 *
 *  POST   /api/resumes/analyze        - Upload and analyze a resume
 *  GET    /api/resumes                - Get all resumes
 *  GET    /api/resumes/{id}           - Get a specific resume
 *  DELETE /api/resumes/{id}           - Delete a resume
 *  GET    /api/resumes/search?skill=  - Search resumes by skill
 *  GET    /api/dashboard/stats        - Get dashboard statistics
 *  GET    /api/health                 - Health check
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow all origins for development & Render deployment
public class ResumeController {

    private static final Logger logger = LoggerFactory.getLogger(ResumeController.class);

    @Autowired
    private ResumeService resumeService;

    // ─── Health Check ─────────────────────────────────────────────────

    /**
     * GET /api/health
     * Simple health check endpoint for Render deployment monitoring
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Smart Resume Analyzer is running!");
        return ResponseEntity.ok(response);
    }

    // ─── Analyze Resume ───────────────────────────────────────────────

    /**
     * POST /api/resumes/analyze
     * Upload a PDF or DOCX resume file and get analysis results
     *
     * Request: multipart/form-data with field "file"
     * Response: JSON with full resume analysis
     */
    @PostMapping(value = "/resumes/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzeResume(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("📥 Received resume upload: {} ({} bytes)",
                file.getOriginalFilename(), file.getSize());

            Resume analyzedResume = resumeService.analyzeResume(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Resume analyzed successfully!");
            response.put("data", analyzedResume);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error analyzing resume: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to analyze resume: " + e.getMessage());
        }
    }

    // ─── Get All Resumes ──────────────────────────────────────────────

    /**
     * GET /api/resumes
     * Returns all analyzed resumes stored in MongoDB
     */
    @GetMapping("/resumes")
    public ResponseEntity<?> getAllResumes() {
        try {
            List<Resume> resumes = resumeService.getAllResumes();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", resumes.size());
            response.put("data", resumes);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching resumes: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // ─── Get Resume By ID ─────────────────────────────────────────────

    /**
     * GET /api/resumes/{id}
     * Returns a single resume by MongoDB document ID
     */
    @GetMapping("/resumes/{id}")
    public ResponseEntity<?> getResumeById(@PathVariable String id) {
        try {
            Optional<Resume> resume = resumeService.getResumeById(id);
            if (resume.isPresent()) {
                return ResponseEntity.ok(Map.of("success", true, "data", resume.get()));
            } else {
                return buildErrorResponse(HttpStatus.NOT_FOUND, "Resume not found with ID: " + id);
            }
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // ─── Delete Resume ────────────────────────────────────────────────

    /**
     * DELETE /api/resumes/{id}
     * Deletes a resume from MongoDB by ID
     */
    @DeleteMapping("/resumes/{id}")
    public ResponseEntity<?> deleteResume(@PathVariable String id) {
        try {
            resumeService.deleteResume(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Resume deleted successfully"
            ));
        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // ─── Search by Skill ──────────────────────────────────────────────

    /**
     * GET /api/resumes/search?skill=Java
     * Search resumes that contain a specific skill
     */
    @GetMapping("/resumes/search")
    public ResponseEntity<?> searchBySkill(@RequestParam String skill) {
        try {
            List<Resume> results = resumeService.searchBySkill(skill);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "skill", skill,
                "count", results.size(),
                "data", results
            ));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // ─── Dashboard Stats ──────────────────────────────────────────────

    /**
     * GET /api/dashboard/stats
     * Returns aggregate statistics about all analyzed resumes
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats() {
        try {
            Map<String, Object> stats = resumeService.getDashboardStats();
            return ResponseEntity.ok(Map.of("success", true, "data", stats));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("status", status.value());
        return ResponseEntity.status(status).body(error);
    }
}
