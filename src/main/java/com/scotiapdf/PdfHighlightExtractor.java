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

public class PdfHighlightExtractor {
    
    private static final Map<String, Color> TARGET_COLORS = new HashMap<>();
    
    static {
        TARGET_COLORS.put("GREEN", Color.GREEN);
        TARGET_COLORS.put("YELLOW", Color.YELLOW);
        TARGET_COLORS.put("PURPLE", new Color(128, 0, 128)); // Purple color
    }
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java PdfHighlightExtractor <pdf-file-path>");
            System.exit(1);
        }
        
        String pdfPath = args[0];
        PdfHighlightExtractor extractor = new PdfHighlightExtractor();
        
        try {
            List<HighlightedText> highlights = extractor.extractHighlights(pdfPath);
            extractor.outputResults(highlights);
        } catch (IOException e) {
            System.err.println("Error processing PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public List<HighlightedText> extractHighlights(String pdfPath) throws IOException {
        List<HighlightedText> highlights = new ArrayList<>();
        
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            System.out.println("Processing PDF: " + pdfPath);
            System.out.println("Total pages: " + document.getNumberOfPages());
            
            for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                PDPage page = document.getPage(pageNum);
                System.out.println("\nProcessing page " + (pageNum + 1));
                
                // Extract highlights from annotations
                List<HighlightedText> pageHighlights = extractAnnotationHighlights(page, pageNum + 1);
                highlights.addAll(pageHighlights);
                
                // Also try to extract text and look for formatting-based highlights
                List<HighlightedText> formattingHighlights = extractFormattingHighlights(document, pageNum);
                highlights.addAll(formattingHighlights);
            }
        }
        
        return highlights;
    }
    
    private List<HighlightedText> extractAnnotationHighlights(PDPage page, int pageNumber) throws IOException {
        List<HighlightedText> highlights = new ArrayList<>();
        
        List<PDAnnotation> annotations = page.getAnnotations();
        System.out.println("Found " + annotations.size() + " annotations on page " + pageNumber);
        
        for (PDAnnotation annotation : annotations) {
            if (annotation instanceof PDAnnotationTextMarkup) {
                PDAnnotationTextMarkup markup = (PDAnnotationTextMarkup) annotation;
                
                // Get the color of the highlight
                Color color = getAnnotationColor(markup);
                String colorName = getColorName(color);
                
                if (colorName != null) {
                    // Get the highlighted text
                    String highlightedText = extractTextFromMarkup(page, markup);
                    
                    if (highlightedText != null && !highlightedText.trim().isEmpty()) {
                        highlights.add(new HighlightedText(
                            highlightedText.trim(),
                            colorName,
                            pageNumber,
                            markup.getRectangle()
                        ));
                        
                        System.out.println("Found " + colorName + " highlight: " + 
                                         highlightedText.substring(0, Math.min(50, highlightedText.length())) + "...");
                    }
                }
            }
        }
        
        return highlights;
    }
    
    private List<HighlightedText> extractFormattingHighlights(PDDocument document, int pageNum) throws IOException {
        List<HighlightedText> highlights = new ArrayList<>();
        
        // Custom text stripper to analyze text formatting
        CustomTextStripper stripper = new CustomTextStripper();
        stripper.setStartPage(pageNum + 1);
        stripper.setEndPage(pageNum + 1);
        
        String text = stripper.getText(document);
        
        // The custom stripper will populate highlights based on text formatting
        highlights.addAll(stripper.getHighlights());
        
        return highlights;
    }
    
    private Color getAnnotationColor(PDAnnotationTextMarkup markup) {
        try {
            float[] colorComponents = markup.getColor().getComponents();
            if (colorComponents.length >= 3) {
                return new Color(colorComponents[0], colorComponents[1], colorComponents[2]);
            }
        } catch (Exception e) {
            System.out.println("Could not extract color from annotation: " + e.getMessage());
        }
        return null;
    }
    
    private String getColorName(Color color) {
        if (color == null) return null;
        
        // Check for approximate color matches
        for (Map.Entry<String, Color> entry : TARGET_COLORS.entrySet()) {
            if (isColorSimilar(color, entry.getValue())) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    private boolean isColorSimilar(Color c1, Color c2) {
        if (c1 == null || c2 == null) return false;
        
        int threshold = 50; // Tolerance for color matching
        return Math.abs(c1.getRed() - c2.getRed()) < threshold &&
               Math.abs(c1.getGreen() - c2.getGreen()) < threshold &&
               Math.abs(c1.getBlue() - c2.getBlue()) < threshold;
    }
    
    private String extractTextFromMarkup(PDPage page, PDAnnotationTextMarkup markup) {
        try {
            // First try to get the contents directly from the annotation
            String contents = markup.getContents();
            if (contents != null && !contents.trim().isEmpty()) {
                return contents.trim();
            }
            
            // Get the quad points which define the highlighted area
            float[] quadPoints = markup.getQuadPoints();
            if (quadPoints == null || quadPoints.length == 0) {
                return "Highlight found but no text content available";
            }
            
            // For now, return information about the highlight area
            PDRectangle rect = markup.getRectangle();
            return String.format("Highlighted area at coordinates: x=%.1f, y=%.1f, width=%.1f, height=%.1f", 
                                rect.getLowerLeftX(), rect.getLowerLeftY(), 
                                rect.getWidth(), rect.getHeight());
            
        } catch (Exception e) {
            System.out.println("Error extracting text from markup: " + e.getMessage());
            return "Highlight detected but text extraction failed";
        }
    }
    
    
    private void outputResults(List<HighlightedText> highlights) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            
            root.put("totalHighlights", highlights.size());
            root.put("extractionDate", new Date().toString());
            
            // Group by color
            Map<String, List<HighlightedText>> groupedByColor = new HashMap<>();
            for (HighlightedText highlight : highlights) {
                groupedByColor.computeIfAbsent(highlight.getColor(), k -> new ArrayList<>()).add(highlight);
            }
            
            ObjectNode colorGroups = mapper.createObjectNode();
            for (Map.Entry<String, List<HighlightedText>> entry : groupedByColor.entrySet()) {
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
            
            // Pretty print JSON
            String jsonOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            System.out.println("\n" + "=".repeat(50));
            System.out.println("EXTRACTION RESULTS");
            System.out.println("=".repeat(50));
            System.out.println(jsonOutput);
            
            // Also print summary
            System.out.println("\n" + "=".repeat(50));
            System.out.println("SUMMARY");
            System.out.println("=".repeat(50));
            System.out.println("Total highlights found: " + highlights.size());
            for (Map.Entry<String, List<HighlightedText>> entry : groupedByColor.entrySet()) {
                System.out.println(entry.getKey() + " highlights: " + entry.getValue().size());
            }
            
        } catch (Exception e) {
            System.err.println("Error generating output: " + e.getMessage());
            
            // Fallback to simple text output
            System.out.println("\nHighlighted Text Extraction Results:");
            System.out.println("===================================");
            
            for (HighlightedText highlight : highlights) {
                System.out.println("\nColor: " + highlight.getColor());
                System.out.println("Page: " + highlight.getPageNumber());
                System.out.println("Text: " + highlight.getText());
                System.out.println("Coordinates: " + highlight.getCoordinates());
                System.out.println("-".repeat(30));
            }
        }
    }
    
    // Custom text stripper class for analyzing text formatting
    private static class CustomTextStripper extends PDFTextStripper {
        private List<HighlightedText> highlights = new ArrayList<>();
        
        public CustomTextStripper() throws IOException {
            super();
        }
        
        @Override
        protected void processTextPosition(TextPosition text) {
            super.processTextPosition(text);
            
            // Note: TextPosition doesn't directly provide color information
            // This is a placeholder for future implementation of text-based highlight detection
            // In practice, you would need to access the PDF's graphics state or use other methods
            
            // For now, we don't add any highlights here since annotation-based detection
            // is more reliable for actual highlight markup
        }
        
        public List<HighlightedText> getHighlights() {
            return highlights;
        }
    }
}
