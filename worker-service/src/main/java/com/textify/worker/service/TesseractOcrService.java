package com.textify.worker.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Service
public class TesseractOcrService {
  private static final Logger logger = LoggerFactory.getLogger(TesseractOcrService.class);

    @Value("${tesseract.path}")
    private String tesseractPath;

    @Value("${tesseract.data-path}")
    private String tesseractDataPath;

    @Value("${tesseract.language}")
    private String tesseractLang;

    private ITesseract tesseract; // The tess4j instance

    // Initialize the Tesseract instance after Spring injects properties
    @PostConstruct
    public void init() {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath(tesseractDataPath);
        this.tesseract.setLanguage(tesseractLang);
        this.tesseract.setTessVariable("user_defined_dpi", "300"); // Example: Set DPI for better recognition
        // You might need to set the path to the executable if not in system PATH
        // If 'tesseract.path' is correctly configured and Tesseract is in system PATH, this might not be strictly needed.
        // However, explicitly setting it is safer.
        this.tesseract.setTessVariable("TESSDATA_PREFIX", tesseractDataPath); // Ensure Tesseract finds data files
        this.tesseract.setPageSegMode(1); // Auto page segmentation mode (e.g., PSM.AUTO)
        this.tesseract.setOcrEngineMode(1); // Tesseract LSTM Only (or 0 for legacy, 3 for both)
        logger.info("Tesseract initialized with path: {}, data-path: {}, language: {}",
                tesseractPath, tesseractDataPath, tesseractLang);

        // Verify if tesseract executable exists
        if (!new java.io.File(tesseractPath).exists()) {
             logger.warn("Tesseract executable not found at '{}'. Please ensure Tesseract OCR is installed and the path is correct.", tesseractPath);
             // Depending on criticality, you might throw an exception here or have a fallback
        }
    }


    /**
     * Extracts text from an image (e.g., JPG, PNG).
     *
     * @param imageBytes The byte array of the image file.
     * @return The extracted text.
     * @throws RuntimeException if OCR fails or image cannot be read.
     */
    public String extractTextFromImage(byte[] imageBytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(bis);
            if (image == null) {
                throw new IllegalArgumentException("Could not read image bytes into BufferedImage.");
            }
            logger.info("Performing OCR on image ({}x{})", image.getWidth(), image.getHeight());
            String result = tesseract.doOCR(image);
            logger.info("OCR completed for image. Text length: {}", result.length());
            return result;
        } catch (IOException e) {
            logger.error("Error reading image bytes for OCR: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read image for OCR: " + e.getMessage(), e);
        } catch (TesseractException e) {
            logger.error("Tesseract OCR failed for image: {}", e.getMessage(), e);
            throw new RuntimeException("OCR processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts text from a PDF document.
     * This method attempts to extract native text first using PDFBox.
     * If no text is found (e.g., scanned PDF), it falls back to OCRing each page as an image.
     *
     * @param pdfBytes The byte array of the PDF file.
     * @return The extracted text.
     * @throws RuntimeException if PDF processing or OCR fails.
     */
    public String extractTextFromPdf(byte[] pdfBytes) {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            // Strategy 1: Try to extract native text using PDFBox (faster and more accurate for text-based PDFs)
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String nativeText = pdfTextStripper.getText(document);

            if (nativeText != null && !nativeText.trim().isEmpty()) {
                logger.info("Native text extracted from PDF. Length: {}", nativeText.length());
                return nativeText;
            } else {
                logger.info("No native text found in PDF, falling back to OCR for each page.");
                // Strategy 2: If no native text, assume it's a scanned PDF and perform OCR page by page
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                StringBuilder ocrText = new StringBuilder();
                for (int page = 0; page < document.getNumberOfPages(); page++) {
                    logger.info("OCR'ing page {} of PDF...", page + 1);
                    // Render PDF page to a BufferedImage
                    // DPI (dots per inch) can significantly impact OCR quality. 300 DPI is a common good starting point.
                    BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300);
                    String pageText = tesseract.doOCR(image);
                    ocrText.append(pageText).append("\n"); // Add newline between pages
                }
                logger.info("OCR completed for PDF. Total text length: {}", ocrText.length());
                return ocrText.toString();
            }
        } catch (IOException e) {
            logger.error("Error loading or processing PDF bytes: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read or render PDF: " + e.getMessage(), e);
        } catch (TesseractException e) {
            logger.error("Tesseract OCR failed for PDF: {}", e.getMessage(), e);
            throw new RuntimeException("OCR processing failed for PDF: " + e.getMessage(), e);
        }
    }
}