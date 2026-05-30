package com.resumeanalyzer.service;

import com.resumeanalyzer.model.Resume;
import com.resumeanalyzer.repository.ResumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ResumeService
 *
 * Orchestrates the resume analysis workflow:
 * 1. Extract text from uploaded file
 * 2. Parse and analyze the text
 * 3. Save result to MongoDB
 * 4. Return analysis result
 */
@Service
public class ResumeService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeService.class);

    @Autowired
    private TextExtractionService textExtractionService;

    @Autowired
    private ResumeParserService resumeParserService;

    @Autowired
    private ResumeRepository resumeRepository;

    /**
     * Main method: Upload, analyze, and save a resume
     */
    public Resume analyzeResume(MultipartFile file) throws IOException {
        logger.info("=== Starting Resume Analysis for: {} ===", file.getOriginalFilename());

        // Step 1: Validate file
        validateFile(file);

        // Step 2: Extract text from PDF/DOCX
        String rawText = textExtractionService.extractText(file);
        String fileType = textExtractionService.getFileType(file.getOriginalFilename());

        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IllegalArgumentException("Could not extract text from the resume. Please ensure the file is not empty or image-only.");
        }

        // Step 3: Parse and analyze the extracted text
        Resume resume = resumeParserService.parseResume(
            rawText,
            file.getOriginalFilename(),
            fileType,
            file.getSize()
        );

        // Step 4: Save to MongoDB
        Resume savedResume = resumeRepository.save(resume);
        logger.info("Resume saved to MongoDB with ID: {}", savedResume.getId());

        return savedResume;
    }

    /**
     * Get all analyzed resumes from MongoDB
     */
    public List<Resume> getAllResumes() {
        return resumeRepository.findAll();
    }

    /**
     * Get a single resume by its MongoDB ID
     */
    public Optional<Resume> getResumeById(String id) {
        return resumeRepository.findById(id);
    }

    /**
     * Delete a resume by ID
     */
    public void deleteResume(String id) {
        if (!resumeRepository.existsById(id)) {
            throw new RuntimeException("Resume not found with ID: " + id);
        }
        resumeRepository.deleteById(id);
        logger.info("Resume deleted: {}", id);
    }

    /**
     * Search resumes by skill
     */
    public List<Resume> searchBySkill(String skill) {
        return resumeRepository.findBySkillsContaining(skill);
    }

    /**
     * Get dashboard statistics
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long total = resumeRepository.count();
        stats.put("totalResumes", total);
        stats.put("freshers", resumeRepository.countByExperienceLevel("Fresher"));
        stats.put("junior", resumeRepository.countByExperienceLevel("Junior"));
        stats.put("midLevel", resumeRepository.countByExperienceLevel("Mid-Level"));
        stats.put("senior", resumeRepository.countByExperienceLevel("Senior"));
        stats.put("expert", resumeRepository.countByExperienceLevel("Expert"));

        // Top 5 resumes by ATS score
        stats.put("topResumes", resumeRepository.findTop10ByOrderByAtsScoreDesc()
            .stream().limit(5).toList());

        return stats;
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a file");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String lowerName = fileName.toLowerCase();
        if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".docx")) {
            throw new IllegalArgumentException("Only PDF and DOCX files are supported");
        }

        // 10 MB limit
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must not exceed 10MB");
        }
    }
}
