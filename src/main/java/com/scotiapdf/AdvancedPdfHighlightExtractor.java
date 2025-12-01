package com.scotiapdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class AdvancedPdfHighlightExtractor {
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java AdvancedPdfHighlightExtractor <pdf-file-path>");
            System.exit(1);
        }
        
        String pdfPath = args[0];
        AdvancedPdfHighlightExtractor extractor = new AdvancedPdfHighlightExtractor();
        
        try {
            extractor.extractHighlightsWithText(pdfPath);
        } catch (IOException e) {
            System.err.println("Error processing PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void extractHighlightsWithText(String pdfPath) throws IOException {
        List<HighlightedText> highlights = new ArrayList<>();
        
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            System.out.println("=".repeat(60));
            System.out.println("ADVANCED PDF HIGHLIGHT EXTRACTION");
            System.out.println("=".repeat(60));
            System.out.println("File: " + pdfPath);
            System.out.println("Total pages: " + document.getNumberOfPages());
            System.out.println();
            
            for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                PDPage page = document.getPage(pageNum);
                System.out.println("Processing page " + (pageNum + 1) + "...");
                
                // Extract highlights using coordinate-based text extraction
                List<HighlightedText> pageHighlights = extractHighlightsFromPage(document, page, pageNum + 1);
                highlights.addAll(pageHighlights);
                
                System.out.println("Found " + pageHighlights.size() + " highlights on page " + (pageNum + 1));
            }
            
            // Output results
            outputResults(highlights);
            
        }
    }
    
    private List<HighlightedText> extractHighlightsFromPage(PDDocument document, PDPage page, int pageNumber) throws IOException {
        List<HighlightedText> highlights = new ArrayList<>();
        
        // Get all annotations on the page
        List<PDAnnotation> annotations = page.getAnnotations();
        
        // Create a text stripper for this page to get all text positions
        TextPositionExtractor textExtractor = new TextPositionExtractor();
        textExtractor.setStartPage(pageNumber);
        textExtractor.setEndPage(pageNumber);
        textExtractor.getText(document); // This populates the text positions
        
        for (PDAnnotation annotation : annotations) {
            if (annotation instanceof PDAnnotationTextMarkup) {
                PDAnnotationTextMarkup markup = (PDAnnotationTextMarkup) annotation;
                
                // Check if this is a target color
                String colorName = getColorName(markup);
                if (colorName != null) {
                    
                    // Extract actual text from the highlighted area
                    String extractedText = extractTextFromHighlightArea(markup, textExtractor.getTextPositions(), page);
                    
                    if (extractedText != null && !extractedText.trim().isEmpty()) {
                        highlights.add(new HighlightedText(
                            extractedText.trim(),
                            colorName,
                            pageNumber,
                            markup.getRectangle()
                        ));
                        
                        System.out.println("  " + colorName + ": " + 
                                         (extractedText.length() > 50 ? extractedText.substring(0, 50) + "..." : extractedText));
                    }
                }
            }
        }
        
        return highlights;
    }
    
    private String extractTextFromHighlightArea(PDAnnotationTextMarkup markup, List<TextPosition> allTextPositions, PDPage page) {
        try {
            // Get the quad points that define the highlighted area
            float[] quadPoints = markup.getQuadPoints();
            if (quadPoints == null || quadPoints.length == 0) {
                // Fallback to rectangle if no quad points
                PDRectangle rect = markup.getRectangle();
                return extractTextFromRectangle(rect, allTextPositions);
            }
            
            // Convert quad points to rectangles and extract text
            StringBuilder extractedText = new StringBuilder();
            
            // Process quad points in groups of 8 (4 points = 1 rectangle)
            for (int i = 0; i < quadPoints.length; i += 8) {
                if (i + 7 < quadPoints.length) {
                    // Create rectangle from quad points
                    float minX = Math.min(Math.min(quadPoints[i], quadPoints[i + 2]), 
                                         Math.min(quadPoints[i + 4], quadPoints[i + 6]));
                    float maxX = Math.max(Math.max(quadPoints[i], quadPoints[i + 2]), 
                                         Math.max(quadPoints[i + 4], quadPoints[i + 6]));
                    float minY = Math.min(Math.min(quadPoints[i + 1], quadPoints[i + 3]), 
                                         Math.min(quadPoints[i + 5], quadPoints[i + 7]));
                    float maxY = Math.max(Math.max(quadPoints[i + 1], quadPoints[i + 3]), 
                                         Math.max(quadPoints[i + 5], quadPoints[i + 7]));
                    
                    PDRectangle rect = new PDRectangle(minX, minY, maxX - minX, maxY - minY);
                    String rectText = extractTextFromRectangle(rect, allTextPositions);
                    
                    if (rectText != null && !rectText.trim().isEmpty()) {
                        if (extractedText.length() > 0) {
                            extractedText.append(" ");
                        }
                        extractedText.append(rectText.trim());
                    }
                }
            }
            
            return extractedText.toString();
            
        } catch (Exception e) {
            System.out.println("    Error extracting text: " + e.getMessage());
            return null;
        }
    }
    
    private String extractTextFromRectangle(PDRectangle rect, List<TextPosition> allTextPositions) {
        StringBuilder text = new StringBuilder();
        
        // Sort text positions by Y coordinate (top to bottom), then X coordinate (left to right)
        List<TextPosition> sortedPositions = new ArrayList<>(allTextPositions);
        sortedPositions.sort((t1, t2) -> {
            int yCompare = Float.compare(t2.getY(), t1.getY()); // Reverse Y (PDF coordinates)
            if (yCompare != 0) return yCompare;
            return Float.compare(t1.getX(), t2.getX());
        });
        
        float tolerance = 2.0f; // Tolerance for coordinate matching
        
        for (TextPosition textPos : sortedPositions) {
            // Check if this text position is within the rectangle bounds
            if (isTextPositionInRectangle(textPos, rect, tolerance)) {
                text.append(textPos.getUnicode());
            }
        }
        
        return text.toString();
    }
    
    private boolean isTextPositionInRectangle(TextPosition textPos, PDRectangle rect, float tolerance) {
        float textX = textPos.getX();
        float textY = textPos.getY();
        
        return textX >= (rect.getLowerLeftX() - tolerance) &&
               textX <= (rect.getUpperRightX() + tolerance) &&
               textY >= (rect.getLowerLeftY() - tolerance) &&
               textY <= (rect.getUpperRightY() + tolerance);
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
        System.out.println("EXTRACTION RESULTS");
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
            root.put("extractionMethod", "Advanced coordinate-based text extraction");
            
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
            String outputFileName = "advanced-highlights-extraction.json";
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
    
    // Custom text stripper to collect all text positions
    private static class TextPositionExtractor extends PDFTextStripper {
        private List<TextPosition> textPositions = new ArrayList<>();
        
        public TextPositionExtractor() throws IOException {
            super();
        }
        
        @Override
        protected void processTextPosition(TextPosition text) {
            textPositions.add(text);
            super.processTextPosition(text);
        }
        
        public List<TextPosition> getTextPositions() {
            return textPositions;
        }
        
        public void clearTextPositions() {
            textPositions.clear();
        }
    }
}
