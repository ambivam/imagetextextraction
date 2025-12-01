# PDF Highlight Extraction - Execution Guide

This guide provides step-by-step instructions for executing the PDF highlight extraction tools, with a focus on the OCR-based (Tesseract) implementation.

## Prerequisites

1. **Java 11 or higher** installed and configured
2. **Maven 3.6 or higher** installed and in PATH
3. **Tesseract OCR** installed (for OCR-based extraction)

## Quick Start

### 1. Build the Project

```bash
# Clean and compile the project with dependencies
mvn clean compile dependency:copy-dependencies
```

### 2. Execute OCR-Based Extraction (Recommended)

#### Using Maven Exec Plugin
```bash
# Execute using Maven with the OCR extractor
mvn exec:java -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" -Dexec.args="\"PDF for Automation Testing.pdf\""
```

#### Using Java Command Directly
```bash
# Execute directly with Java classpath
java -cp "target/classes;target/dependency/*" com.scotiapdf.OcrPdfHighlightExtractor "PDF for Automation Testing.pdf"
```

#### Using Batch Script (Windows)
```bash
# Use the convenient batch script
extract-highlights-ocr.bat "PDF for Automation Testing.pdf"
```

## Alternative Execution Methods

### Simple Area-Based Extraction (No OCR Required)

#### Using Maven
```bash
mvn exec:java -Dexec.mainClass="com.scotiapdf.SimplePdfHighlightExtractor" -Dexec.args="\"PDF for Automation Testing.pdf\""
```

#### Using Java Command
```bash
java -cp "target/classes;target/dependency/*" com.scotiapdf.SimplePdfHighlightExtractor "PDF for Automation Testing.pdf"
```

#### Using Batch Script
```bash
extract-highlights-simple.bat "PDF for Automation Testing.pdf"
```

### Advanced Coordinate-Based Extraction

#### Using Maven
```bash
mvn exec:java -Dexec.mainClass="com.scotiapdf.AdvancedPdfHighlightExtractor" -Dexec.args="\"PDF for Automation Testing.pdf\""
```

#### Using Java Command
```bash
java -cp "target/classes;target/dependency/*" com.scotiapdf.AdvancedPdfHighlightExtractor "PDF for Automation Testing.pdf"
```

## Detailed Maven Execution Steps

### Step 1: Verify Prerequisites
```bash
# Check Java version
java -version

# Check Maven version
mvn -version

# Check Tesseract installation (for OCR)
tesseract --version
```

### Step 2: Clean and Build
```bash
# Clean any previous builds
mvn clean

# Compile source code
mvn compile

# Copy all dependencies to target/dependency
mvn dependency:copy-dependencies
```

### Step 3: Execute with Maven

#### OCR-Based Extraction (Best Results)
```bash
# Full Maven command for OCR extraction
mvn exec:java \
  -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" \
  -Dexec.args="\"PDF for Automation Testing.pdf\""
```

#### With Custom JVM Arguments (if needed)
```bash
# With increased memory and native access for JNA
mvn exec:java \
  -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" \
  -Dexec.args="\"PDF for Automation Testing.pdf\"" \
  -Dexec.jvmArgs="-Xmx2g --enable-native-access=ALL-UNNAMED"
```

## Troubleshooting

### Common Issues and Solutions

#### 1. Tesseract Not Found Error
```
Error: Could not find Tesseract tessdata directory!
```

**Solution:**
```bash
# Check Tesseract installation
.\check-tesseract.bat

# Or manually verify
tesseract --list-langs
```

#### 2. Memory Issues with Large PDFs
```bash
# Increase JVM heap size
mvn exec:java \
  -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" \
  -Dexec.args="\"large-document.pdf\"" \
  -Dexec.jvmArgs="-Xmx4g"
```

#### 3. JNA Native Access Warnings
```bash
# Suppress JNA warnings (Java 17+)
mvn exec:java \
  -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" \
  -Dexec.args="\"PDF for Automation Testing.pdf\"" \
  -Dexec.jvmArgs="--enable-native-access=ALL-UNNAMED"
```

#### 4. Compilation Errors
```bash
# Force recompilation
mvn clean compile

# Skip tests if they fail
mvn clean compile -DskipTests
```

## Output Files

After successful execution, the following files will be generated:

- **`ocr-highlights-extraction.json`** - OCR-based results
- **`simple-highlights-extraction.json`** - Simple extraction results
- **`advanced-highlights-extraction.json`** - Coordinate-based results

## Performance Optimization

### For Better OCR Accuracy
```bash
# The OCR extractor uses 300 DPI by default
# Higher DPI = better accuracy but slower processing
# Lower DPI = faster processing but lower accuracy
```

### For Large Documents
```bash
# Process with increased memory
mvn exec:java \
  -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" \
  -Dexec.args="\"large-document.pdf\"" \
  -Dexec.jvmArgs="-Xmx8g -XX:+UseG1GC"
```

## Maven Configuration

The project is configured with the following execution plugin in `pom.xml`:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.1.0</version>
    <configuration>
        <mainClass>com.scotiapdf.OcrPdfHighlightExtractor</mainClass>
    </configuration>
</plugin>
```

## Batch Scripts Summary

| Script | Purpose | OCR Required |
|--------|---------|--------------|
| `extract-highlights-ocr.bat` | OCR-based extraction (best results) | Yes |
| `extract-highlights-simple.bat` | Simple area-based extraction | No |
| `extract-highlights.bat` | Advanced coordinate-based | No |
| `check-tesseract.bat` | Verify Tesseract installation | N/A |

## Example Complete Workflow

```bash
# 1. Build the project
mvn clean compile dependency:copy-dependencies

# 2. Verify Tesseract (optional, for OCR)
.\check-tesseract.bat

# 3. Execute OCR extraction
mvn exec:java -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" -Dexec.args="\"PDF for Automation Testing.pdf\""

# 4. Check results
type ocr-highlights-extraction.json
```

## Advanced Usage

### Custom PDF File Path
```bash
# For files in different directories
mvn exec:java \
  -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" \
  -Dexec.args="\"C:\Documents\MyPDF.pdf\""
```

### Debug Mode
```bash
# Enable debug logging
mvn exec:java \
  -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" \
  -Dexec.args="\"PDF for Automation Testing.pdf\"" \
  -Dexec.jvmArgs="-Dorg.slf4j.simpleLogger.defaultLogLevel=debug"
```

This guide provides comprehensive instructions for executing the PDF highlight extraction tools using Maven, with special focus on the OCR-based implementation using Tesseract.
