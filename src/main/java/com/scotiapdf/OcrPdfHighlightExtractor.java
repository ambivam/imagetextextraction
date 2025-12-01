package com.scotiapdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

public class OcrPdfHighlightExtractor {
    
    private static final float DPI = 300f; // High DPI for better OCR accuracy
    private ITesseract tesseract;
    
    public OcrPdfHighlightExtractor() {
        initializeTesseract();
    }
    
    private void initializeTesseract() {
        tesseract = new Tesseract();
        
        // Try to set Tesseract data path - adjust this path based on your Tesseract installation
        String[] possiblePaths = {
            "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Programs\\Tesseract-OCR\\tessdata",
            "C:\\Program Files\\Tesseract-OCR\\tessdata",
            "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata",
            "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Tesseract-OCR\\tessdata",
            System.getenv("TESSDATA_PREFIX"),
            System.getProperty("user.dir") + "\\tessdata", // Current directory
            "tessdata" // Relative path
        };
        
        String foundPath = null;
        for (String path : possiblePaths) {
            if (path != null && new File(path).exists()) {
                File engFile = new File(path, "eng.traineddata");
                if (engFile.exists()) {
                    tesseract.setDatapath(path);
                    foundPath = path;
                    System.out.println("✓ Using Tesseract data path: " + path);
                    System.out.println("✓ Found eng.traineddata at: " + engFile.getAbsolutePath());
                    break;
                }
            }
        }
        
        if (foundPath == null) {
            System.err.println("❌ ERROR: Could not find Tesseract tessdata directory!");
            System.err.println("Searched in the following locations:");
            for (String path : possiblePaths) {
                if (path != null) {
                    System.err.println("  - " + path);
                }
            }
            System.err.println("\nPlease install Tesseract OCR or set TESSDATA_PREFIX environment variable.");
            System.err.println("See TESSERACT_SETUP.md for installation instructions.");
            throw new RuntimeException("Tesseract tessdata not found. Please install Tesseract OCR.");
        }
        
        try {
            tesseract.setLanguage("eng"); // English language
            tesseract.setPageSegMode(6); // Uniform block of text (more reliable than OSD)
            tesseract.setOcrEngineMode(1); // Neural nets LSTM engine only
            
            // Test Tesseract with a simple operation
            System.out.println("✓ Tesseract initialized successfully");
            
        } catch (Exception e) {
            System.err.println("❌ ERROR: Failed to initialize Tesseract: " + e.getMessage());
            throw new RuntimeException("Tesseract initialization failed", e);
        }
    }
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java OcrPdfHighlightExtractor <pdf-file-path>");
            System.exit(1);
        }
        
        String pdfPath = args[0];
        OcrPdfHighlightExtractor extractor = new OcrPdfHighlightExtractor();
        
        try {
            extractor.extractHighlightsWithOcr(pdfPath);
        } catch (Exception e) {
            System.err.println("Error processing PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void extractHighlightsWithOcr(String pdfPath) throws IOException {
        List<HighlightedText> highlights = new ArrayList<>();
        
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            System.out.println("=".repeat(60));
            System.out.println("OCR-BASED PDF HIGHLIGHT EXTRACTION");
            System.out.println("=".repeat(60));
            System.out.println("File: " + pdfPath);
            System.out.println("Total pages: " + document.getNumberOfPages());
            System.out.println("DPI: " + DPI);
            System.out.println();
            
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            
            for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                PDPage page = document.getPage(pageNum);
                System.out.println("Processing page " + (pageNum + 1) + "...");
                
                // Convert PDF page to high-resolution image
                BufferedImage pageImage = pdfRenderer.renderImageWithDPI(pageNum, DPI, ImageType.RGB);
                
                // Save page image for debugging (optional)
                File pageImageFile = new File("page_" + (pageNum + 1) + ".png");
                ImageIO.write(pageImage, "PNG", pageImageFile);
                System.out.println("  Saved page image: " + pageImageFile.getName());
                
                // Extract highlights from this page
                List<HighlightedText> pageHighlights = extractHighlightsFromPageImage(
                    document, page, pageImage, pageNum + 1);
                highlights.addAll(pageHighlights);
                
                System.out.println("  Found " + pageHighlights.size() + " highlights on page " + (pageNum + 1));
                
                // Clean up page image file
                pageImageFile.delete();
            }
            
            // Output results
            outputResults(highlights);
        }
    }
    
    private List<HighlightedText> extractHighlightsFromPageImage(PDDocument document, PDPage page, 
                                                               BufferedImage pageImage, int pageNumber) throws IOException {
        List<HighlightedText> highlights = new ArrayList<>();
        
        // Get all annotations on the page
        List<PDAnnotation> annotations = page.getAnnotations();
        
        // Get page dimensions for coordinate conversion
        PDRectangle pageBox = page.getMediaBox();
        float pageWidth = pageBox.getWidth();
        float pageHeight = pageBox.getHeight();
        
        int imageWidth = pageImage.getWidth();
        int imageHeight = pageImage.getHeight();
        
        for (PDAnnotation annotation : annotations) {
            if (annotation instanceof PDAnnotationTextMarkup) {
                PDAnnotationTextMarkup markup = (PDAnnotationTextMarkup) annotation;
                
                // Check if this is a target color
                String colorName = getColorName(markup);
                if (colorName != null) {
                    
                    // Extract text from the highlighted region using OCR
                    String extractedText = extractTextFromHighlightRegion(
                        markup, pageImage, pageWidth, pageHeight, imageWidth, imageHeight);
                    
                    if (extractedText != null && !extractedText.trim().isEmpty()) {
                        highlights.add(new HighlightedText(
                            extractedText.trim(),
                            colorName,
                            pageNumber,
                            markup.getRectangle()
                        ));
                        
                        System.out.println("    " + colorName + ": \"" + 
                                         (extractedText.length() > 60 ? extractedText.substring(0, 60) + "..." : extractedText) + "\"");
                    }
                }
            }
        }
        
        return highlights;
    }
    
    private String extractTextFromHighlightRegion(PDAnnotationTextMarkup markup, BufferedImage pageImage,
                                                 float pageWidth, float pageHeight, 
                                                 int imageWidth, int imageHeight) {
        try {
            // Get highlight rectangle
            PDRectangle rect = markup.getRectangle();
            
            // Convert PDF coordinates to image coordinates
            // PDF coordinates: (0,0) at bottom-left, Y increases upward
            // Image coordinates: (0,0) at top-left, Y increases downward
            
            float scaleX = (float) imageWidth / pageWidth;
            float scaleY = (float) imageHeight / pageHeight;
            
            int x = Math.max(0, (int) (rect.getLowerLeftX() * scaleX));
            int y = Math.max(0, (int) ((pageHeight - rect.getUpperRightY()) * scaleY));
            int width = Math.min(imageWidth - x, (int) (rect.getWidth() * scaleX));
            int height = Math.min(imageHeight - y, (int) (rect.getHeight() * scaleY));
            
            // Add some padding to ensure we capture the text
            int padding = 5;
            x = Math.max(0, x - padding);
            y = Math.max(0, y - padding);
            width = Math.min(imageWidth - x, width + 2 * padding);
            height = Math.min(imageHeight - y, height + 2 * padding);
            
            // Extract the highlighted region from the image
            if (width > 0 && height > 0) {
                BufferedImage highlightRegion = pageImage.getSubimage(x, y, width, height);
                
                // Enhance the image for better OCR (optional)
                BufferedImage enhancedRegion = enhanceImageForOcr(highlightRegion);
                
                // Save region image for debugging (optional)
                String regionFileName = "highlight_region_" + System.currentTimeMillis() + ".png";
                File regionFile = new File(regionFileName);
                ImageIO.write(enhancedRegion, "PNG", regionFile);
                
                // Perform OCR on the highlighted region
                String extractedText = tesseract.doOCR(enhancedRegion);
                
                // Clean up region file
                regionFile.delete();
                
                return cleanOcrText(extractedText);
            }
            
        } catch (Exception e) {
            System.out.println("      OCR extraction error: " + e.getMessage());
        }
        
        return null;
    }
    
    private BufferedImage enhanceImageForOcr(BufferedImage original) {
        // Create a new image with enhanced contrast and brightness for better OCR
        BufferedImage enhanced = new BufferedImage(
            original.getWidth() * 2, // Scale up for better OCR
            original.getHeight() * 2, 
            BufferedImage.TYPE_INT_RGB
        );
        
        Graphics2D g2d = enhanced.createGraphics();
        
        // Scale up the image
        g2d.drawImage(original, 0, 0, enhanced.getWidth(), enhanced.getHeight(), null);
        
        g2d.dispose();
        
        return enhanced;
    }
    
    private String cleanOcrText(String ocrText) {
        if (ocrText == null) return null;
        
        // Clean up common OCR errors and formatting issues
        return ocrText
            .replaceAll("\\s+", " ") // Multiple spaces to single space
            .replaceAll("\\n+", " ") // Newlines to spaces
            .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "") // Remove control characters
            .trim();
    }
    
    private String getColorName(PDAnnotationTextMarkup markup) {
        try {
            if (markup.getColor() == null) return null;
            
            float[] colorComponents = markup.getColor().getComponents();
            if (colorComponents.length < 3) return null;
            
            Color color = new Color(colorComponents[0], colorComponents[1], colorComponents[2]);
            return identifyTargetColor(color);
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private String identifyTargetColor(Color color) {
        if (color == null) return null;
        
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        
        // Green-ish colors (including light green like RGB(197, 251, 114))
        if (g > 200 && g > r && g > b) {
            return "GREEN";
        }
        
        // Yellow-ish colors (including orange-yellow like RGB(255, 193, 0))
        if (r > 200 && g > 150 && b < 100) {
            return "YELLOW";
        }
        
        // Purple-ish colors (including RGB(150, 67, 252))
        if (b > 200 && r > 100 && g < 150) {
            return "PURPLE";
        }
        
        return null;
    }
    
    private void outputResults(List<HighlightedText> highlights) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("OCR EXTRACTION RESULTS");
        System.out.println("=".repeat(60));
        
        if (highlights.isEmpty()) {
            System.out.println("No highlights found matching the target colors (green, yellow, purple).");
            System.out.println("\nTroubleshooting tips:");
            System.out.println("1. Ensure Tesseract OCR is installed and accessible");
            System.out.println("2. Check if the PDF contains actual highlight annotations");
            System.out.println("3. Verify highlight colors match the target colors");
            return;
        }
        
        // Group by color
        Map<String, List<HighlightedText>> groupedHighlights = new HashMap<>();
        for (HighlightedText highlight : highlights) {
            groupedHighlights.computeIfAbsent(highlight.getColor(), k -> new ArrayList<>()).add(highlight);
        }
        
        // Output summary
        System.out.println("SUMMARY:");
        System.out.println("Total highlights found: " + highlights.size());
        for (Map.Entry<String, List<HighlightedText>> entry : groupedHighlights.entrySet()) {
            System.out.println(entry.getKey() + " highlights: " + entry.getValue().size());
        }
        
        // Output detailed results
        System.out.println("\nDETAILED RESULTS:");
        for (Map.Entry<String, List<HighlightedText>> entry : groupedHighlights.entrySet()) {
            System.out.println("\n" + entry.getKey() + " HIGHLIGHTS:");
            System.out.println("-".repeat(40));
            
            for (int i = 0; i < entry.getValue().size(); i++) {
                HighlightedText highlight = entry.getValue().get(i);
                System.out.println((i + 1) + ". Page " + highlight.getPageNumber());
                System.out.println("   Text: \"" + highlight.getText() + "\"");
                System.out.println("   Location: " + highlight.getCoordinates());
                System.out.println();
            }
        }
        
        // Save to JSON file
        saveToJsonFile(highlights, groupedHighlights);
    }
    
    private void saveToJsonFile(List<HighlightedText> highlights, Map<String, List<HighlightedText>> groupedHighlights) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            
            root.put("totalHighlights", highlights.size());
            root.put("extractionDate", new Date().toString());
            root.put("extractionMethod", "OCR-based text extraction using Tesseract");
            root.put("dpi", DPI);
            
            ObjectNode colorGroups = mapper.createObjectNode();
            for (Map.Entry<String, List<HighlightedText>> entry : groupedHighlights.entrySet()) {
                ArrayNode colorArray = mapper.createArrayNode();
                for (HighlightedText highlight : entry.getValue()) {
                    ObjectNode highlightNode = mapper.createObjectNode();
                    highlightNode.put("text", highlight.getText());
                    highlightNode.put("page", highlight.getPageNumber());
                    highlightNode.put("coordinates", highlight.getCoordinates().toString());
                    colorArray.add(highlightNode);
                }
                colorGroups.set(entry.getKey().toLowerCase(), colorArray);
            }
            
            root.set("highlightsByColor", colorGroups);
            
            // Save to file
            String outputFileName = "ocr-highlights-extraction.json";
            try (java.io.FileWriter fileWriter = new java.io.FileWriter(outputFileName)) {
                String jsonOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
                fileWriter.write(jsonOutput);
                System.out.println("JSON OUTPUT SAVED TO: " + outputFileName);
                System.out.println("File location: " + new File(outputFileName).getAbsolutePath());
            }
            
        } catch (Exception e) {
            System.out.println("Error saving JSON file: " + e.getMessage());
        }
    }
}
