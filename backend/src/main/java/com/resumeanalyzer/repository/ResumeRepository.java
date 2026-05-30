package com.resumeanalyzer.repository;

import com.resumeanalyzer.model.Resume;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resume Repository - MongoDB CRUD operations
 *
 * Extends MongoRepository which provides:
 * - save(), findById(), findAll(), delete(), count(), etc.
 *
 * Custom query methods use Spring Data naming conventions
 */
@Repository
public interface ResumeRepository extends MongoRepository<Resume, String> {

    // Find resumes by experience level (Fresher, Junior, Mid, Senior)
    List<Resume> findByExperienceLevel(String experienceLevel);

    // Find resumes by ATS score greater than threshold
    List<Resume> findByAtsScoreGreaterThanEqual(int minScore);

    // Find resumes by candidate name (case-insensitive)
    List<Resume> findByCandidateNameContainingIgnoreCase(String name);

    // Find resumes containing a specific skill
    @Query("{ 'skills': { $in: [?0] } }")
    List<Resume> findBySkillsContaining(String skill);

    // Find resumes uploaded after a date
    List<Resume> findByUploadedAtAfter(LocalDateTime date);

    // Count resumes by experience level
    long countByExperienceLevel(String experienceLevel);

    // Find top resumes by ATS score
    List<Resume> findTop10ByOrderByAtsScoreDesc();
}
