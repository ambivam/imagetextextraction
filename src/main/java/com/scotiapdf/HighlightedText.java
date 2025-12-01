package com.scotiapdf;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 * Represents a piece of highlighted text extracted from a PDF document.
 */
public class HighlightedText {
    private String text;
    private String color;
    private int pageNumber;
    private PDRectangle coordinates;
    
    public HighlightedText(String text, String color, int pageNumber, PDRectangle coordinates) {
        this.text = text;
        this.color = color;
        this.pageNumber = pageNumber;
        this.coordinates = coordinates;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public int getPageNumber() {
        return pageNumber;
    }
    
    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
    
    public PDRectangle getCoordinates() {
        return coordinates;
    }
    
    public void setCoordinates(PDRectangle coordinates) {
        this.coordinates = coordinates;
    }
    
    @Override
    public String toString() {
        return String.format("HighlightedText{color='%s', page=%d, text='%s', coordinates=%s}", 
                           color, pageNumber, text.length() > 50 ? text.substring(0, 50) + "..." : text, coordinates);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        HighlightedText that = (HighlightedText) obj;
        return pageNumber == that.pageNumber &&
               text.equals(that.text) &&
               color.equals(that.color) &&
               coordinates.equals(that.coordinates);
    }
    
    @Override
    public int hashCode() {
        int result = text.hashCode();
        result = 31 * result + color.hashCode();
        result = 31 * result + pageNumber;
        result = 31 * result + coordinates.hashCode();
        return result;
    }
}
