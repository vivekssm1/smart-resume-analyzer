package com.resumeanalyzer.service;

import com.resumeanalyzer.model.Resume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ResumeParserService
 *
 * Core analysis engine that parses raw resume text and extracts:
 * - Personal information (name, email, phone, location)
 * - Skills and programming languages
 * - Education history
 * - Work experience
 * - ATS score calculation
 * - Improvement suggestions
 */
@Service
public class ResumeParserService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeParserService.class);

    // ─── Skill Keywords Database ──────────────────────────────────────

    private static final List<String> PROGRAMMING_LANGUAGES = Arrays.asList(
        "Java", "Python", "JavaScript", "TypeScript", "C", "C++", "C#",
        "Ruby", "Go", "Golang", "Rust", "Kotlin", "Swift", "Scala",
        "PHP", "R", "MATLAB", "Perl", "Shell", "Bash", "Dart", "Lua"
    );

    private static final List<String> FRAMEWORKS = Arrays.asList(
        "Spring Boot", "Spring", "React", "Angular", "Vue", "Node.js",
        "Express", "Django", "Flask", "FastAPI", "Laravel", "Ruby on Rails",
        "ASP.NET", ".NET", "Hibernate", "JPA", "Maven", "Gradle",
        "Bootstrap", "Tailwind", "Next.js", "Nuxt.js", "Svelte"
    );

    private static final List<String> DATABASES = Arrays.asList(
        "MongoDB", "MySQL", "PostgreSQL", "Oracle", "SQL Server", "Redis",
        "Cassandra", "DynamoDB", "Firebase", "SQLite", "MariaDB",
        "Elasticsearch", "Neo4j", "CouchDB"
    );

    private static final List<String> CLOUD_DEVOPS = Arrays.asList(
        "AWS", "Azure", "GCP", "Google Cloud", "Docker", "Kubernetes",
        "Jenkins", "CI/CD", "GitHub Actions", "Terraform", "Ansible",
        "Linux", "Unix", "Git", "GitHub", "GitLab", "Bitbucket"
    );

    private static final List<String> SOFT_SKILLS = Arrays.asList(
        "Leadership", "Teamwork", "Communication", "Problem Solving",
        "Agile", "Scrum", "Project Management", "Analytical", "Creative"
    );

    // ─── Regex Patterns ───────────────────────────────────────────────

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    private static final Pattern PHONE_PATTERN =
        Pattern.compile("(\\+?\\d{1,3}[\\s\\-]?)?(\\(?\\d{3}\\)?[\\s\\-]?)\\d{3}[\\s\\-]?\\d{4}");

    private static final Pattern LINKEDIN_PATTERN =
        Pattern.compile("(https?://)?(www\\.)?linkedin\\.com/in/[\\w\\-]+/?");

    private static final Pattern GITHUB_PATTERN =
        Pattern.compile("(https?://)?(www\\.)?github\\.com/[\\w\\-]+/?");

    private static final Pattern YEAR_PATTERN =
        Pattern.compile("\\b(19|20)\\d{2}\\b");

    // ─── Main Parse Method ────────────────────────────────────────────

    /**
     * Parse raw resume text and populate Resume object
     */
    public Resume parseResume(String rawText, String fileName, String fileType, long fileSize) {
        logger.info("Starting resume parsing for: {}", fileName);

        Resume resume = new Resume();
        resume.setFileName(fileName);
        resume.setFileType(fileType);
        resume.setFileSizeBytes(fileSize);
        resume.setRawText(rawText);
        resume.setWordCount(countWords(rawText));

        // Extract all sections
        resume.setEmail(extractEmail(rawText));
        resume.setPhone(extractPhone(rawText));
        resume.setLinkedInUrl(extractLinkedIn(rawText));
        resume.setGithubUrl(extractGithub(rawText));
        resume.setCandidateName(extractName(rawText));
        resume.setLocation(extractLocation(rawText));
        resume.setSkills(extractSkills(rawText));
        resume.setProgrammingLanguages(extractProgrammingLanguages(rawText));
        resume.setEducation(extractEducation(rawText));
        resume.setWorkExperience(extractExperience(rawText));
        resume.setCertifications(extractCertifications(rawText));
        resume.setTotalExperienceYears(estimateExperienceYears(rawText, resume.getWorkExperience()));
        resume.setExperienceLevel(determineExperienceLevel(resume.getTotalExperienceYears()));

        // ATS Score calculation
        int atsScore = calculateAtsScore(resume);
        resume.setAtsScore(atsScore);
        resume.setSuggestions(generateSuggestions(resume));
        resume.setMissingKeywords(findMissingKeywords(rawText));
        resume.setSectionCount(countSections(rawText));

        logger.info("Resume parsed successfully. ATS Score: {}", atsScore);
        return resume;
    }

    // ─── Extraction Methods ───────────────────────────────────────────

    private String extractEmail(String text) {
        Matcher m = EMAIL_PATTERN.matcher(text);
        return m.find() ? m.group().trim() : null;
    }

    private String extractPhone(String text) {
        Matcher m = PHONE_PATTERN.matcher(text);
        return m.find() ? m.group().trim() : null;
    }

    private String extractLinkedIn(String text) {
        Matcher m = LINKEDIN_PATTERN.matcher(text);
        return m.find() ? m.group().trim() : null;
    }

    private String extractGithub(String text) {
        Matcher m = GITHUB_PATTERN.matcher(text);
        return m.find() ? m.group().trim() : null;
    }

    /**
     * Extract candidate name - looks at the first few lines of the resume
     * Most resumes have the name at the top
     */
    private String extractName(String text) {
        String[] lines = text.split("\\n");
        for (int i = 0; i < Math.min(5, lines.length); i++) {
            String line = lines[i].trim();
            // Skip empty lines, emails, phones, and short lines
            if (line.isEmpty() || line.contains("@") || line.length() < 3) continue;
            if (line.matches(".*\\d{5,}.*")) continue; // Skip phone lines
            if (line.length() > 60) continue;           // Skip long header lines

            // Check if it looks like a name (2-4 capitalized words)
            if (line.matches("[A-Z][a-z]+(\\s[A-Z][a-z.]+){1,3}")) {
                return line;
            }
            // Also match ALL CAPS names
            if (line.matches("[A-Z]+(\\s[A-Z.]+){1,3}") && line.length() < 40) {
                return capitalizeWords(line.toLowerCase());
            }
        }
        return "Unknown Candidate";
    }

    private String capitalizeWords(String str) {
        return Arrays.stream(str.split(" "))
            .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1))
            .collect(Collectors.joining(" "));
    }

    private String extractLocation(String text) {
        // Look for city/state patterns
        Pattern locationPattern = Pattern.compile(
            "(?i)(located in|location:|address:)?\\s*([A-Z][a-z]+(?:,\\s*[A-Z][a-z]+)?(?:,\\s*[A-Z]{2})?)"
        );

        String[] lines = text.split("\\n");
        for (int i = 0; i < Math.min(10, lines.length); i++) {
            String line = lines[i].trim();
            if (line.toLowerCase().contains("india") || line.matches(".*,\\s*[A-Z]{2}\\s*\\d{5}.*")) {
                // Extract city part
                String cleaned = line.replaceAll("(?i)(email|phone|mobile|linkedin|github).*", "").trim();
                if (!cleaned.isEmpty() && cleaned.length() < 50) return cleaned;
            }
        }

        // Fallback: look for "City, Country" or "City, State" patterns
        Pattern cityPattern = Pattern.compile("([A-Z][a-z]+),\\s*(India|USA|UK|Canada|Australia|[A-Z]{2})");
        Matcher m = cityPattern.matcher(text);
        if (m.find()) return m.group();

        return null;
    }

    /**
     * Extract all skills by scanning for known skill keywords
     */
    private List<String> extractSkills(String text) {
        Set<String> found = new LinkedHashSet<>();
        String upperText = text.toUpperCase();

        // Combine all skill categories
        List<String> allSkills = new ArrayList<>();
        allSkills.addAll(PROGRAMMING_LANGUAGES);
        allSkills.addAll(FRAMEWORKS);
        allSkills.addAll(DATABASES);
        allSkills.addAll(CLOUD_DEVOPS);
        allSkills.addAll(SOFT_SKILLS);

        for (String skill : allSkills) {
            if (upperText.contains(skill.toUpperCase())) {
                found.add(skill);
            }
        }

        return new ArrayList<>(found);
    }

    private List<String> extractProgrammingLanguages(String text) {
        List<String> found = new ArrayList<>();
        String upperText = text.toUpperCase();
        for (String lang : PROGRAMMING_LANGUAGES) {
            if (upperText.contains(lang.toUpperCase())) {
                found.add(lang);
            }
        }
        return found;
    }

    /**
     * Extract education entries from resume text
     */
    private List<Resume.Education> extractEducation(String text) {
        List<Resume.Education> educationList = new ArrayList<>();

        // Common degree patterns
        Pattern degreePattern = Pattern.compile(
            "(?i)(B\\.?Tech|B\\.?E\\b|M\\.?Tech|M\\.?E\\b|B\\.?Sc|M\\.?Sc|MBA|BCA|MCA|Ph\\.?D|Bachelor|Master|Diploma|B\\.?Com|M\\.?Com)" +
            "(?:[\\s\\S]{0,100}?)(\\b\\d{4}\\b)?",
            Pattern.CASE_INSENSITIVE
        );

        Matcher m = degreePattern.matcher(text);
        while (m.find()) {
            String degreeBlock = m.group().trim();
            String degree = m.group(1);
            String year = m.group(2);

            // Try to find institution on same or adjacent line
            String institution = extractInstitutionNear(text, m.start());

            Resume.Education edu = Resume.Education.builder()
                .degree(degree)
                .institution(institution)
                .year(year)
                .build();

            if (educationList.size() < 4) { // Limit to 4 entries
                educationList.add(edu);
            }
        }

        return educationList;
    }

    private String extractInstitutionNear(String text, int position) {
        // Look for University/College/Institute keywords near the degree mention
        Pattern instPattern = Pattern.compile(
            "(?i)([A-Z][\\w\\s]+(?:University|College|Institute|School|IIT|NIT|BITS)[\\w\\s]*)"
        );
        String nearby = text.substring(Math.max(0, position - 100),
                                        Math.min(text.length(), position + 300));
        Matcher m = instPattern.matcher(nearby);
        return m.find() ? m.group(1).trim() : "Institution not specified";
    }

    /**
     * Extract work experience entries
     */
    private List<Resume.Experience> extractExperience(String text) {
        List<Resume.Experience> experiences = new ArrayList<>();

        // Job title patterns
        Pattern jobPattern = Pattern.compile(
            "(?i)(Software Engineer|Developer|Analyst|Manager|Lead|Architect|" +
            "Intern|Consultant|Designer|DevOps|Data Scientist|ML Engineer|" +
            "Full Stack|Backend|Frontend|QA Engineer|SRE|CTO|CIO|VP|Director)" +
            "[\\s\\S]{0,200}?" +
            "(?:(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s*\\'?\\d{2,4}|\\d{4})",
            Pattern.CASE_INSENSITIVE
        );

        Matcher m = jobPattern.matcher(text);
        int count = 0;
        while (m.find() && count < 5) {
            String block = m.group().trim();
            String jobTitle = m.group(1);

            // Extract company name (usually a proper noun near job title)
            String company = extractCompanyNear(text, m.start());

            Resume.Experience exp = Resume.Experience.builder()
                .jobTitle(jobTitle)
                .company(company)
                .duration(extractDuration(block))
                .description(block.length() > 150 ? block.substring(0, 150) + "..." : block)
                .build();

            experiences.add(exp);
            count++;
        }

        return experiences;
    }

    private String extractCompanyNear(String text, int position) {
        // Look for "at CompanyName" or "@ CompanyName" patterns
        Pattern companyPattern = Pattern.compile("(?i)(?:at|@|,)\\s+([A-Z][A-Za-z\\s&.]+(?:Ltd|Inc|Corp|Pvt|Technologies|Solutions|Systems|Labs)?)",
            Pattern.CASE_INSENSITIVE);
        String nearby = text.substring(Math.max(0, position - 50),
                                        Math.min(text.length(), position + 200));
        Matcher m = companyPattern.matcher(nearby);
        return m.find() ? m.group(1).trim() : "Company not specified";
    }

    private String extractDuration(String block) {
        Pattern durationPattern = Pattern.compile(
            "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s*\\'?\\d{2,4}\\s*[-–]\\s*" +
            "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s*\\'?\\d{2,4}|" +
            "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s*\\'?\\d{2,4}\\s*[-–]\\s*Present|" +
            "\\d{4}\\s*[-–]\\s*\\d{4}|\\d{4}\\s*[-–]\\s*Present",
            Pattern.CASE_INSENSITIVE
        );
        Matcher m = durationPattern.matcher(block);
        return m.find() ? m.group().trim() : "Duration not specified";
    }

    private List<String> extractCertifications(String text) {
        List<String> certs = new ArrayList<>();
        String[] certKeywords = {
            "AWS Certified", "Google Cloud Certified", "Azure Certified",
            "Certified Java", "Oracle Certified", "PMP", "CISSP", "CCNA",
            "Scrum Master", "ITIL", "CompTIA", "Certified Developer"
        };

        for (String cert : certKeywords) {
            if (text.toLowerCase().contains(cert.toLowerCase())) {
                certs.add(cert);
            }
        }
        return certs;
    }

    // ─── ATS Score Calculation ────────────────────────────────────────

    /**
     * Calculate ATS (Applicant Tracking System) compatibility score (0-100)
     *
     * ATS systems parse resumes to match job requirements.
     * Higher score = better ATS compatibility
     */
    private int calculateAtsScore(Resume resume) {
        int score = 0;

        // Contact Info (20 points)
        if (resume.getEmail() != null) score += 8;
        if (resume.getPhone() != null) score += 7;
        if (resume.getLinkedInUrl() != null) score += 5;

        // Skills (25 points)
        int skillCount = resume.getSkills() != null ? resume.getSkills().size() : 0;
        score += Math.min(25, skillCount * 2);

        // Education (15 points)
        if (resume.getEducation() != null && !resume.getEducation().isEmpty()) score += 15;

        // Experience (20 points)
        if (resume.getWorkExperience() != null && !resume.getWorkExperience().isEmpty()) {
            score += Math.min(20, resume.getWorkExperience().size() * 7);
        }

        // Content length (10 points)
        if (resume.getWordCount() > 300) score += 5;
        if (resume.getWordCount() > 500) score += 5;

        // Sections (10 points)
        if (resume.getSectionCount() >= 3) score += 5;
        if (resume.getSectionCount() >= 5) score += 5;

        return Math.min(100, score);
    }

    // ─── Experience & Level Detection ─────────────────────────────────

    private int estimateExperienceYears(String text, List<Resume.Experience> experiences) {
        if (experiences == null || experiences.isEmpty()) {
            // Check for fresher keywords
            if (text.toLowerCase().contains("fresher") || text.toLowerCase().contains("no experience")) return 0;
            return 0;
        }

        Matcher yearMatcher = YEAR_PATTERN.matcher(text);
        List<Integer> years = new ArrayList<>();
        while (yearMatcher.find()) {
            years.add(Integer.parseInt(yearMatcher.group()));
        }

        if (years.size() >= 2) {
            Collections.sort(years);
            int min = years.get(0);
            int max = years.get(years.size() - 1);
            int diff = max - min;
            return Math.max(0, Math.min(diff, 30)); // Cap at 30 years
        }
        return experiences.size(); // Rough estimate
    }

    private String determineExperienceLevel(int years) {
        if (years == 0) return "Fresher";
        if (years <= 2) return "Junior";
        if (years <= 5) return "Mid-Level";
        if (years <= 10) return "Senior";
        return "Expert";
    }

    // ─── Improvement Suggestions ──────────────────────────────────────

    private List<String> generateSuggestions(Resume resume) {
        List<String> suggestions = new ArrayList<>();

        if (resume.getEmail() == null)
            suggestions.add("📧 Add your email address — it's essential for recruiters to contact you");

        if (resume.getPhone() == null)
            suggestions.add("📱 Add your phone number for faster recruiter contact");

        if (resume.getLinkedInUrl() == null)
            suggestions.add("🔗 Add your LinkedIn profile URL to improve credibility");

        if (resume.getGithubUrl() == null && resume.getProgrammingLanguages() != null && !resume.getProgrammingLanguages().isEmpty())
            suggestions.add("💻 Add your GitHub profile to showcase your code projects");

        if (resume.getSkills() == null || resume.getSkills().size() < 5)
            suggestions.add("🛠️ Add more skills — aim for 8-12 relevant technical skills");

        if (resume.getWorkExperience() == null || resume.getWorkExperience().isEmpty())
            suggestions.add("💼 Add work experience or internship details");

        if (resume.getEducation() == null || resume.getEducation().isEmpty())
            suggestions.add("🎓 Add your education details including degree and institution");

        if (resume.getWordCount() < 300)
            suggestions.add("📝 Your resume is short. Add more details about projects and achievements");

        if (resume.getCertifications() == null || resume.getCertifications().isEmpty())
            suggestions.add("🏆 Consider adding certifications to boost your credibility");

        if (resume.getAtsScore() < 60)
            suggestions.add("⚠️ Low ATS score — use more industry-standard keywords and section headings");

        if (suggestions.isEmpty())
            suggestions.add("✅ Great resume! Keep it updated with your latest projects and skills");

        return suggestions;
    }

    private List<String> findMissingKeywords(String text) {
        List<String> missing = new ArrayList<>();
        String upperText = text.toUpperCase();

        String[] important = {"EXPERIENCE", "EDUCATION", "SKILLS", "PROJECTS", "SUMMARY", "OBJECTIVE"};
        for (String kw : important) {
            if (!upperText.contains(kw)) {
                missing.add(kw.charAt(0) + kw.substring(1).toLowerCase() + " section");
            }
        }
        return missing;
    }

    // ─── Utility Methods ──────────────────────────────────────────────

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        return text.trim().split("\\s+").length;
    }

    private int countSections(String text) {
        int count = 0;
        String[] sectionKeywords = {
            "EXPERIENCE", "EDUCATION", "SKILLS", "PROJECTS",
            "CERTIFICATIONS", "SUMMARY", "OBJECTIVE", "ACHIEVEMENTS"
        };
        String upperText = text.toUpperCase();
        for (String kw : sectionKeywords) {
            if (upperText.contains(kw)) count++;
        }
        return count;
    }
}
