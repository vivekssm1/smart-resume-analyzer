package com.resumeanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Smart Resume Analyzer - Main Application Entry Point
 *
 * This application analyzes resumes (PDF/DOCX) and extracts:
 * - Personal Information
 * - Skills
 * - Education
 * - Work Experience
 * - ATS Score
 */
@SpringBootApplication
public class SmartResumeAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartResumeAnalyzerApplication.class, args);
        System.out.println("\n✅ Smart Resume Analyzer is running!");
        System.out.println("📄 API available at: http://localhost:8080/api");
        System.out.println("🌐 Frontend available at: http://localhost:8080\n");
    }
}
