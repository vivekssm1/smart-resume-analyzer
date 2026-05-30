package com.resumeanalyzer.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resume Model - Stores analyzed resume data in MongoDB
 *
 * Collection: resumes
 * Each document represents one uploaded and analyzed resume
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "resumes")
public class Resume {

    @Id
    private String id;

    // ─── File Info ───────────────────────────────────────────────
    private String fileName;          // Original file name
    private String fileType;          // pdf or docx
    private long fileSizeBytes;       // File size in bytes

    // ─── Extracted Personal Info ─────────────────────────────────
    private String candidateName;     // Full name
    private String email;             // Email address
    private String phone;             // Phone number
    private String location;          // City/Country

    // ─── LinkedIn / GitHub ───────────────────────────────────────
    private String linkedInUrl;
    private String githubUrl;

    // ─── Raw Text ────────────────────────────────────────────────
    private String rawText;           // Full extracted text from resume

    // ─── Analyzed Sections ───────────────────────────────────────
    private List<String> skills;              // Technical & soft skills
    private List<String> programmingLanguages; // Detected languages
    private List<Education> education;         // Education history
    private List<Experience> workExperience;   // Work history
    private List<String> certifications;       // Certifications

    // ─── ATS Score & Analysis ────────────────────────────────────
    private int atsScore;                     // 0-100 ATS compatibility score
    private String experienceLevel;           // Fresher / Junior / Mid / Senior
    private int totalExperienceYears;         // Estimated years of experience
    private List<String> suggestions;         // Improvement suggestions
    private List<String> missingKeywords;     // Keywords to add

    // ─── Word Count Stats ─────────────────────────────────────────
    private int wordCount;
    private int sectionCount;

    // ─── Metadata ────────────────────────────────────────────────
    @CreatedDate
    private LocalDateTime uploadedAt;

    // ─── Nested Classes ──────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Education {
        private String degree;       // B.Tech, M.Sc, etc.
        private String institution;  // University / College name
        private String field;        // Computer Science, etc.
        private String year;         // Graduation year
        private String grade;        // CGPA / Percentage
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Experience {
        private String jobTitle;     // Software Engineer, etc.
        private String company;      // Company name
        private String duration;     // Jan 2021 - Dec 2022
        private String description;  // Role description
    }
}
