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

public class EnhancedPdfHighlightExtractor {
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java EnhancedPdfHighlightExtractor <pdf-file-path>");
            System.exit(1);
        }
        
        String pdfPath = args[0];
        EnhancedPdfHighlightExtractor extractor = new EnhancedPdfHighlightExtractor();
        
        try {
            extractor.analyzeAndExtractHighlights(pdfPath);
        } catch (IOException e) {
            System.err.println("Error processing PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void analyzeAndExtractHighlights(String pdfPath) throws IOException {
        List<HighlightedText> highlights = new ArrayList<>();
        
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            System.out.println("=".repeat(60));
            System.out.println("PDF ANALYSIS REPORT");
            System.out.println("=".repeat(60));
            System.out.println("File: " + pdfPath);
            System.out.println("Total pages: " + document.getNumberOfPages());
            System.out.println();
            
            for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                PDPage page = document.getPage(pageNum);
                System.out.println("PAGE " + (pageNum + 1) + " ANALYSIS:");
                System.out.println("-".repeat(40));
                
                // Analyze annotations
                analyzeAnnotations(page, pageNum + 1, highlights);
                
                // Analyze text content and formatting
                analyzeTextContent(document, pageNum, highlights);
                
                System.out.println();
            }
            
            // Output final results
            outputDetailedResults(highlights);
            
        }
    }
    
    private void analyzeAnnotations(PDPage page, int pageNumber, List<HighlightedText> highlights) throws IOException {
        List<PDAnnotation> annotations = page.getAnnotations();
        System.out.println("Annotations found: " + annotations.size());
        
        int highlightCount = 0;
        for (PDAnnotation annotation : annotations) {
            System.out.println("  - Annotation type: " + annotation.getSubtype());
            
            if (annotation instanceof PDAnnotationTextMarkup) {
                PDAnnotationTextMarkup markup = (PDAnnotationTextMarkup) annotation;
                highlightCount++;
                
                // Analyze the annotation in detail
                analyzeMarkupAnnotation(markup, pageNumber, highlights);
            }
        }
        
        System.out.println("Highlight annotations: " + highlightCount);
    }
    
    private void analyzeMarkupAnnotation(PDAnnotationTextMarkup markup, int pageNumber, List<HighlightedText> highlights) {
        try {
            System.out.println("    Markup Details:");
            System.out.println("      - Subtype: " + markup.getSubtype());
            
            // Check for color information
            if (markup.getColor() != null) {
                float[] colorComponents = markup.getColor().getComponents();
                System.out.println("      - Color components: " + Arrays.toString(colorComponents));
                
                if (colorComponents.length >= 3) {
                    Color color = new Color(colorComponents[0], colorComponents[1], colorComponents[2]);
                    System.out.println("      - RGB Color: " + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue());
                    
                    String colorName = identifyTargetColor(color);
                    if (colorName != null) {
                        System.out.println("      - Matched target color: " + colorName);
                        
                        String text = extractTextFromAnnotation(markup);
                        highlights.add(new HighlightedText(text, colorName, pageNumber, markup.getRectangle()));
                    } else {
                        System.out.println("      - Color does not match target colors (green, yellow, purple)");
                    }
                }
            } else {
                System.out.println("      - No color information available");
            }
            
            // Check for contents
            String contents = markup.getContents();
            if (contents != null && !contents.trim().isEmpty()) {
                System.out.println("      - Contents: " + contents);
            }
            
            // Check for quad points
            float[] quadPoints = markup.getQuadPoints();
            if (quadPoints != null && quadPoints.length > 0) {
                System.out.println("      - Quad points available: " + quadPoints.length + " coordinates");
            }
            
            // Rectangle information
            PDRectangle rect = markup.getRectangle();
            System.out.println("      - Rectangle: " + rect);
            
        } catch (Exception e) {
            System.out.println("      - Error analyzing markup: " + e.getMessage());
        }
    }
    
    private String extractTextFromAnnotation(PDAnnotationTextMarkup markup) {
        try {
            // Try to get contents first
            String contents = markup.getContents();
            if (contents != null && !contents.trim().isEmpty()) {
                return contents.trim();
            }
            
            // If no contents, describe the location
            PDRectangle rect = markup.getRectangle();
            return String.format("Highlighted area at (%.1f, %.1f) size %.1fx%.1f", 
                                rect.getLowerLeftX(), rect.getLowerLeftY(), 
                                rect.getWidth(), rect.getHeight());
        } catch (Exception e) {
            return "Highlight found but text extraction failed: " + e.getMessage();
        }
    }
    
    private void analyzeTextContent(PDDocument document, int pageNum, List<HighlightedText> highlights) throws IOException {
        System.out.println("Text Analysis:");
        
        // Create a custom text stripper to analyze text properties
        DetailedTextStripper stripper = new DetailedTextStripper(pageNum + 1);
        stripper.setStartPage(pageNum + 1);
        stripper.setEndPage(pageNum + 1);
        
        String text = stripper.getText(document);
        
        System.out.println("  - Text characters processed: " + stripper.getCharacterCount());
        System.out.println("  - Unique fonts found: " + stripper.getUniqueFonts().size());
        System.out.println("  - Font sizes found: " + stripper.getFontSizes());
        
        // Add any potential highlights found through text analysis
        highlights.addAll(stripper.getPotentialHighlights());
    }
    
    private String identifyTargetColor(Color color) {
        if (color == null) return null;
        
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        
        // More flexible color matching based on the actual colors found in the PDF
        
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
        
        // Also check for exact matches with higher tolerance
        Map<String, Color> exactTargets = new HashMap<>();
        exactTargets.put("GREEN", Color.GREEN);
        exactTargets.put("YELLOW", Color.YELLOW);
        exactTargets.put("PURPLE", new Color(128, 0, 128));
        
        int tolerance = 80; // Increased tolerance
        
        for (Map.Entry<String, Color> entry : exactTargets.entrySet()) {
            Color target = entry.getValue();
            if (Math.abs(r - target.getRed()) <= tolerance &&
                Math.abs(g - target.getGreen()) <= tolerance &&
                Math.abs(b - target.getBlue()) <= tolerance) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    private void outputDetailedResults(List<HighlightedText> highlights) {
        System.out.println("=".repeat(60));
        System.out.println("EXTRACTION RESULTS");
        System.out.println("=".repeat(60));
        
        if (highlights.isEmpty()) {
            System.out.println("No highlights found matching the target colors (green, yellow, purple).");
            System.out.println();
            System.out.println("Possible reasons:");
            System.out.println("1. The PDF doesn't contain annotation-based highlights");
            System.out.println("2. Highlights are implemented as colored text rather than annotations");
            System.out.println("3. Highlight colors don't match the target colors exactly");
            System.out.println("4. Highlights are embedded in a different format");
            return;
        }
        
        // Group highlights by color
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
        System.out.println();
        
        // Output detailed results
        System.out.println("DETAILED RESULTS:");
        for (Map.Entry<String, List<HighlightedText>> entry : groupedHighlights.entrySet()) {
            System.out.println();
            System.out.println(entry.getKey() + " HIGHLIGHTS:");
            System.out.println("-".repeat(30));
            
            for (int i = 0; i < entry.getValue().size(); i++) {
                HighlightedText highlight = entry.getValue().get(i);
                System.out.println((i + 1) + ". Page " + highlight.getPageNumber());
                System.out.println("   Text: " + highlight.getText());
                System.out.println("   Location: " + highlight.getCoordinates());
                System.out.println();
            }
        }
        
        // Also output JSON format to file
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            
            root.put("totalHighlights", highlights.size());
            root.put("extractionDate", new Date().toString());
            
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
            
            // Generate JSON string
            String jsonOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            
            // Save to file
            String outputFileName = "highlights-extraction-results.json";
            try (java.io.FileWriter fileWriter = new java.io.FileWriter(outputFileName)) {
                fileWriter.write(jsonOutput);
                System.out.println("JSON OUTPUT SAVED TO FILE: " + outputFileName);
                System.out.println("File location: " + new File(outputFileName).getAbsolutePath());
            }
            
            // Also display in console (truncated)
            System.out.println();
            System.out.println("JSON PREVIEW (first 500 characters):");
            System.out.println("-".repeat(50));
            String preview = jsonOutput.length() > 500 ? jsonOutput.substring(0, 500) + "..." : jsonOutput;
            System.out.println(preview);
            
        } catch (Exception e) {
            System.out.println("Error generating JSON output: " + e.getMessage());
        }
    }
    
    // Enhanced text stripper for detailed analysis
    private static class DetailedTextStripper extends PDFTextStripper {
        private int characterCount = 0;
        private Set<String> uniqueFonts = new HashSet<>();
        private Set<Float> fontSizes = new HashSet<>();
        private List<HighlightedText> potentialHighlights = new ArrayList<>();
        private int pageNumber;
        
        public DetailedTextStripper(int pageNumber) throws IOException {
            super();
            this.pageNumber = pageNumber;
        }
        
        @Override
        protected void processTextPosition(TextPosition text) {
            super.processTextPosition(text);
            
            characterCount++;
            
            // Collect font information
            if (text.getFont() != null) {
                uniqueFonts.add(text.getFont().getName());
            }
            fontSizes.add(text.getFontSize());
            
            // Look for potential highlights based on text properties
            // This is a heuristic approach - you might need to adjust based on your PDF
            if (text.getFontSize() > 12 || 
                (text.getFont() != null && text.getFont().getName().toLowerCase().contains("bold"))) {
                
                // This could be highlighted text - add as potential highlight
                // In practice, you'd need more sophisticated detection
            }
        }
        
        public int getCharacterCount() { return characterCount; }
        public Set<String> getUniqueFonts() { return uniqueFonts; }
        public Set<Float> getFontSizes() { return fontSizes; }
        public List<HighlightedText> getPotentialHighlights() { return potentialHighlights; }
    }
}
