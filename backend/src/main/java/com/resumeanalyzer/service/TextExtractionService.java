package com.resumeanalyzer.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * TextExtractionService
 *
 * Handles text extraction from uploaded resume files.
 * Supports:
 *   - PDF files (.pdf) using Apache PDFBox
 *   - Word documents (.docx) using Apache POI
 */
@Service
public class TextExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(TextExtractionService.class);

    /**
     * Extract text from an uploaded file (PDF or DOCX)
     *
     * @param file - the uploaded MultipartFile
     * @return extracted plain text
     */
    public String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();

        if (fileName == null) {
            throw new IllegalArgumentException("File name is null");
        }

        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".pdf")) {
            logger.info("Extracting text from PDF: {}", fileName);
            return extractFromPdf(file);
        } else if (lowerName.endsWith(".docx")) {
            logger.info("Extracting text from DOCX: {}", fileName);
            return extractFromDocx(file);
        } else {
            throw new IllegalArgumentException("Unsupported file type. Please upload a PDF or DOCX file.");
        }
    }

    /**
     * Extract text from PDF using Apache PDFBox
     */
    private String extractFromPdf(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // Maintain reading order

            String text = stripper.getText(document);
            logger.debug("PDF extraction successful. Characters: {}", text.length());
            return text;
        }
    }

    /**
     * Extract text from DOCX using Apache POI
     */
    private String extractFromDocx(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

            String text = extractor.getText();
            logger.debug("DOCX extraction successful. Characters: {}", text.length());
            return text;
        }
    }

    /**
     * Get file type from filename
     */
    public String getFileType(String fileName) {
        if (fileName == null) return "unknown";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".docx")) return "docx";
        return "unknown";
    }
}
