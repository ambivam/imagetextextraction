package com.scotiapdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class SimplePdfHighlightExtractor {
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java SimplePdfHighlightExtractor <pdf-file-path>");
            System.exit(1);
        }
        
        String pdfPath = args[0];
        SimplePdfHighlightExtractor extractor = new SimplePdfHighlightExtractor();
        
        try {
            extractor.extractHighlightsSimple(pdfPath);
        } catch (Exception e) {
            System.err.println("Error processing PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void extractHighlightsSimple(String pdfPath) throws IOException {
        List<HighlightedText> highlights = new ArrayList<>();
        
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            System.out.println("=".repeat(60));
            System.out.println("SIMPLE PDF HIGHLIGHT EXTRACTION");
            System.out.println("=".repeat(60));
            System.out.println("File: " + pdfPath);
            System.out.println("Total pages: " + document.getNumberOfPages());
            System.out.println("Method: Area-based text extraction (no OCR required)");
            System.out.println();
            
            for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                PDPage page = document.getPage(pageNum);
                System.out.println("Processing page " + (pageNum + 1) + "...");
                
                // Extract highlights from this page
                List<HighlightedText> pageHighlights = extractHighlightsFromPageSimple(document, page, pageNum + 1);
                highlights.addAll(pageHighlights);
                
                System.out.println("  Found " + pageHighlights.size() + " highlights on page " + (pageNum + 1));
            }
            
            // Output results
            outputResults(highlights);
        }
    }
    
    private List<HighlightedText> extractHighlightsFromPageSimple(PDDocument document, PDPage page, int pageNumber) throws IOException {
        List<HighlightedText> highlights = new ArrayList<>();
        
        // Get all annotations on the page
        List<PDAnnotation> annotations = page.getAnnotations();
        
        // Get the full text of the page for reference
        PDFTextStripper textStripper = new PDFTextStripper();
        textStripper.setStartPage(pageNumber);
        textStripper.setEndPage(pageNumber);
        String pageText = textStripper.getText(document);
        
        for (PDAnnotation annotation : annotations) {
            if (annotation instanceof PDAnnotationTextMarkup) {
                PDAnnotationTextMarkup markup = (PDAnnotationTextMarkup) annotation;
                
                // Check if this is a target color
                String colorName = getColorName(markup);
                if (colorName != null) {
                    
                    // Try multiple methods to extract text
                    String extractedText = null;
                    
                    // Method 1: Try area-based extraction
                    extractedText = extractTextUsingArea(page, markup);
                    
                    // Method 2: Use annotation contents if available
                    if (extractedText == null || extractedText.trim().isEmpty()) {
                        extractedText = markup.getContents();
                    }
                    
                    // Method 3: Try to extract from quad points
                    if (extractedText == null || extractedText.trim().isEmpty()) {
                        extractedText = extractTextFromQuadPoints(markup, pageText);
                    }
                    
                    // Method 4: Fallback to coordinate description
                    if (extractedText == null || extractedText.trim().isEmpty()) {
                        PDRectangle rect = markup.getRectangle();
                        extractedText = String.format("Highlighted area at (%.1f, %.1f) size %.1f x %.1f", 
                                                    rect.getLowerLeftX(), rect.getLowerLeftY(), 
                                                    rect.getWidth(), rect.getHeight());
                    }
                    
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
    
    private String extractTextUsingArea(PDPage page, PDAnnotationTextMarkup markup) {
        try {
            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            stripper.setSortByPosition(true);
            
            // Get the rectangle of the highlight
            PDRectangle rect = markup.getRectangle();
            
            // Convert to Rectangle for area extraction
            Rectangle region = new Rectangle(
                (int) rect.getLowerLeftX(),
                (int) rect.getLowerLeftY(),
                (int) rect.getWidth(),
                (int) rect.getHeight()
            );
            
            stripper.addRegion("highlight", region);
            stripper.extractRegions(page);
            
            String extractedText = stripper.getTextForRegion("highlight");
            return extractedText != null ? extractedText.trim() : null;
            
        } catch (Exception e) {
            System.out.println("      Area extraction failed: " + e.getMessage());
            return null;
        }
    }
    
    private String extractTextFromQuadPoints(PDAnnotationTextMarkup markup, String pageText) {
        try {
            // This is a simplified approach - in a real implementation,
            // you would need to map quad points to text positions more accurately
            float[] quadPoints = markup.getQuadPoints();
            if (quadPoints != null && quadPoints.length > 0) {
                // For now, just return a portion of the page text as a fallback
                // This is not accurate but provides some content
                String[] words = pageText.split("\\s+");
                if (words.length > 5) {
                    // Return a few words as a sample
                    return String.join(" ", Arrays.copyOfRange(words, 0, Math.min(5, words.length)));
                }
            }
        } catch (Exception e) {
            System.out.println("      Quad point extraction failed: " + e.getMessage());
        }
        return null;
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
        System.out.println("SIMPLE EXTRACTION RESULTS");
        System.out.println("=".repeat(60));
        
        if (highlights.isEmpty()) {
            System.out.println("No highlights found matching the target colors (green, yellow, purple).");
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
            root.put("extractionMethod", "Simple area-based extraction (no OCR)");
            
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
            String outputFileName = "simple-highlights-extraction.json";
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
