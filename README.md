# PDF Highlight Extractor

A Java application that extracts highlighted text from PDF documents, specifically targeting green, yellow, and purple highlights.

## Features

- **Multi-color highlight detection**: Identifies green, yellow, and purple highlights
- **Multiple extraction methods**: 
  - Coordinate-based text extraction (AdvancedPdfHighlightExtractor)
  - OCR-based extraction using Tesseract (OcrPdfHighlightExtractor) - **Recommended**
- **High accuracy OCR**: Uses Tesseract OCR engine for precise text extraction
- **JSON output**: Structured data format for easy integration
- **Batch processing**: Process entire PDF documents at once
- **Detailed reporting**: Console output with extraction statistics
- **Error handling**: Robust error handling and troubleshooting information
- **JSON output**: Provides structured output with highlights grouped by color
- **Coordinate tracking**: Records the position of highlights within the document
- **Page-aware processing**: Tracks which page each highlight appears on

## Requirements

- Java 11 or higher
- Maven 3.6 or higher
- Apache PDFBox library (automatically managed by Maven)
- **For OCR extraction**: Tesseract OCR engine (see [TESSERACT_SETUP.md](TESSERACT_SETUP.md))

## Installation

1. Ensure you have Java 11+ and Maven installed
2. Clone or download this project
3. Navigate to the project directory
4. Install dependencies:
   ```bash
   mvn clean install
   ```

## Usage

### Command Line Execution

#### Basic Extractor
```bash
# Using Maven exec plugin
mvn exec:java -Dexec.args="path/to/your/document.pdf"

# Or compile and run directly
mvn compile
java -cp target/classes:target/dependency/* com.scotiapdf.PdfHighlightExtractor "path/to/your/document.pdf"
```

#### Enhanced Extractor
```bash
# Compile dependencies first
mvn clean compile dependency:copy-dependencies

# Run the enhanced analyzer
java -cp "target/classes;target/dependency/*" com.scotiapdf.EnhancedPdfHighlightExtractor "path/to/your/document.pdf"

# Or use the convenient batch script (Windows)
extract-highlights.bat "path/to/your/document.pdf"
```

#### OCR-Based Extractor (Recommended)
```bash
# Ensure Tesseract OCR is installed (see TESSERACT_SETUP.md)
# Compile dependencies first
mvn clean compile dependency:copy-dependencies

# Run the OCR-based extractor
java -cp "target/classes;target/dependency/*" com.scotiapdf.OcrPdfHighlightExtractor "path/to/your/document.pdf"

# Or use the convenient batch script (Windows)
extract-highlights-ocr.bat "path/to/your/document.pdf"
```

### Example with your PDF

```bash
# OCR-based extraction (recommended)
extract-highlights-ocr.bat "PDF for Automation Testing.pdf"

# Or coordinate-based extraction
extract-highlights.bat "PDF for Automation Testing.pdf"
```

## Output Format

The application provides two types of output:

### 1. JSON Format
```json
{
  "totalHighlights": 15,
  "extractionDate": "Mon Dec 01 09:42:00 IST 2025",
  "highlightsByColor": {
    "green": [
      {
        "text": "Important green highlighted text",
        "page": 1,
        "coordinates": "PDRectangle{lowerLeftX=100.0, lowerLeftY=200.0, upperRightX=300.0, upperRightY=220.0}"
      }
    ],
    "yellow": [
      {
        "text": "Key yellow highlighted information",
        "page": 2,
        "coordinates": "PDRectangle{lowerLeftX=150.0, lowerLeftY=400.0, upperRightX=350.0, upperRightY=420.0}"
      }
    ],
    "purple": [
      {
        "text": "Critical purple highlighted content",
        "page": 1,
        "coordinates": "PDRectangle{lowerLeftX=200.0, lowerLeftY=300.0, upperRightX=400.0, upperRightY=320.0}"
      }
    ]
  }
}
```

### 2. Summary Format
```
SUMMARY
==================================================
Total highlights found: 15
GREEN highlights: 8
YELLOW highlights: 5
PURPLE highlights: 2
```

## Technical Details

### Highlight Detection Methods

1. **Annotation-based**: Extracts highlights from PDF text markup annotations
2. **Formatting-based**: Analyzes text rendering properties for colored text

### Color Matching

The application uses approximate color matching with a tolerance threshold to account for slight variations in highlight colors:

- **Green**: RGB values close to (0, 255, 0)
- **Yellow**: RGB values close to (255, 255, 0)  
- **Purple**: RGB values close to (128, 0, 128)

### Dependencies

- **Apache PDFBox 2.0.29**: Core PDF processing library
- **Jackson 2.15.2**: JSON processing for structured output
- **SLF4J 1.7.36**: Logging framework

## Troubleshooting

### Common Issues

1. **No highlights found**: 
   - Verify the PDF contains actual highlight annotations
   - Check if highlights are embedded as annotations vs. just colored text
   - Ensure highlight colors match the target colors (green, yellow, purple)

2. **Memory issues with large PDFs**:
   - Increase JVM heap size: `-Xmx2g`
   - Process pages individually for very large documents

3. **Color detection issues**:
   - Adjust color tolerance threshold in the code
   - Check if highlights use non-standard color values

### Debug Mode

To enable detailed logging, add this JVM argument:
```bash
-Dorg.slf4j.simpleLogger.defaultLogLevel=debug
```

## Project Structure

```
src/
├── main/
│   └── java/
│       └── com/
│           └── scotiapdf/
│               ├── PdfHighlightExtractor.java         # Basic extractor
│               ├── EnhancedPdfHighlightExtractor.java # Coordinate-based extractor
│               ├── AdvancedPdfHighlightExtractor.java # Advanced coordinate-based extractor
│               ├── OcrPdfHighlightExtractor.java      # OCR-based extractor (recommended)
│               └── HighlightedText.java               # Data model for highlights
├── pom.xml                                            # Maven configuration
├── extract-highlights.bat                             # Windows batch script for coordinate-based extraction
├── extract-highlights-ocr.bat                         # Windows batch script for OCR-based extraction
├── TESSERACT_SETUP.md                                 # Tesseract OCR installation guide
└── README.md                                          # This file
```

## License

This project is provided as-is for educational and development purposes.
