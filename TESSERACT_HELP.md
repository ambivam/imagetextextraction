# Tesseract OCR Help & Execution Guide

This guide provides comprehensive instructions for checking, configuring, and executing Tesseract OCR with Maven for the PDF highlight extraction project.

## Quick Tesseract Check

### Using the Built-in Checker Script
```bash
# Run the Tesseract installation checker
.\check-tesseract.bat
```

This script will verify:
- ✅ Tesseract executable in PATH
- ✅ Tessdata directories and language files
- ✅ Environment variables
- ✅ Quick functionality test

## Maven-Based Tesseract Execution

### 1. Prerequisites Check with Maven

#### Verify Java and Maven Setup
```bash
# Check Java version (requires Java 11+)
java -version

# Check Maven version (requires Maven 3.6+)
mvn -version

# Verify project compilation
mvn clean compile
```

#### Check Tesseract Installation
```bash
# Method 1: Use the batch script
.\check-tesseract.bat

# Method 2: Direct command line check
tesseract --version
tesseract --list-langs
```

### 2. Maven Execution Commands

#### Primary OCR Execution
```bash
# Build project and execute OCR extractor
mvn clean compile dependency:copy-dependencies
mvn exec:java -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" -Dexec.args="\"PDF for Automation Testing.pdf\""
```

#### One-Line Execution
```bash
# Combined build and execute
mvn clean compile dependency:copy-dependencies exec:java -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" -Dexec.args="\"PDF for Automation Testing.pdf\""
```

#### With Custom JVM Arguments
```bash
# Execute with increased memory and native access
mvn exec:java \
  -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" \
  -Dexec.args="\"PDF for Automation Testing.pdf\"" \
  -Dexec.jvmArgs="-Xmx4g --enable-native-access=ALL-UNNAMED"
```

### 3. Maven Profile for OCR (Optional)

You can add this profile to your `pom.xml` for easier OCR execution:

```xml
<profiles>
    <profile>
        <id>ocr-extract</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.codehaus.mojo</groupId>
                    <artifactId>exec-maven-plugin</artifactId>
                    <version>3.1.0</version>
                    <configuration>
                        <mainClass>com.scotiapdf.OcrPdfHighlightExtractor</mainClass>
                        <options>
                            <option>-Xmx4g</option>
                            <option>--enable-native-access=ALL-UNNAMED</option>
                        </options>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

Then execute with:
```bash
mvn clean compile -Pocr-extract exec:java -Dexec.args="\"PDF for Automation Testing.pdf\""
```

## Tesseract Configuration Verification

### 1. Check Tesseract Installation Paths

The OCR extractor searches for Tesseract in these locations:
```
C:\Users\[username]\AppData\Local\Programs\Tesseract-OCR\tessdata
C:\Program Files\Tesseract-OCR\tessdata
C:\Program Files (x86)\Tesseract-OCR\tessdata
C:\Users\[username]\AppData\Local\Tesseract-OCR\tessdata
%TESSDATA_PREFIX%
.\tessdata
```

### 2. Manual Verification Commands
```bash
# Check if Tesseract is in PATH
where tesseract

# List available languages
tesseract --list-langs

# Check specific language file
dir "C:\Users\%USERNAME%\AppData\Local\Programs\Tesseract-OCR\tessdata\eng.traineddata"
```

### 3. Environment Variable Setup (if needed)
```bash
# Set TESSDATA_PREFIX environment variable
set TESSDATA_PREFIX=C:\Users\%USERNAME%\AppData\Local\Programs\Tesseract-OCR\tessdata

# Or add to system PATH
set PATH=%PATH%;C:\Users\%USERNAME%\AppData\Local\Programs\Tesseract-OCR
```

## Troubleshooting Maven + Tesseract Issues

### Issue 1: "Tesseract tessdata not found"

**Solution using Maven:**
```bash
# 1. Check Tesseract installation
.\check-tesseract.bat

# 2. If not installed, install Tesseract first
# Download from: https://github.com/UB-Mannheim/tesseract/wiki

# 3. Verify and re-run
mvn clean compile
mvn exec:java -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" -Dexec.args="\"PDF for Automation Testing.pdf\""
```

### Issue 2: "Invalid memory access" Error

**Solution:**
```bash
# Execute with native access enabled
mvn exec:java \
  -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" \
  -Dexec.args="\"PDF for Automation Testing.pdf\"" \
  -Dexec.jvmArgs="--enable-native-access=ALL-UNNAMED"
```

### Issue 3: Out of Memory Errors

**Solution:**
```bash
# Increase heap size
mvn exec:java \
  -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" \
  -Dexec.args="\"PDF for Automation Testing.pdf\"" \
  -Dexec.jvmArgs="-Xmx8g"
```

### Issue 4: Maven Compilation Errors

**Solution:**
```bash
# Force clean rebuild
mvn clean
mvn compile -U

# Check for dependency issues
mvn dependency:tree
mvn dependency:resolve
```

## Alternative Execution Methods

### 1. Fallback to Simple Extractor (No OCR)
```bash
# If Tesseract issues persist, use simple extractor
mvn exec:java -Dexec.mainClass="com.scotiapdf.SimplePdfHighlightExtractor" -Dexec.args="\"PDF for Automation Testing.pdf\""
```

### 2. Direct Java Execution (Bypass Maven)
```bash
# Compile with Maven, execute with Java
mvn clean compile dependency:copy-dependencies
java -cp "target/classes;target/dependency/*" com.scotiapdf.OcrPdfHighlightExtractor "PDF for Automation Testing.pdf"
```

### 3. Batch Script Execution
```bash
# Use pre-configured batch scripts
extract-highlights-ocr.bat "PDF for Automation Testing.pdf"
extract-highlights-simple.bat "PDF for Automation Testing.pdf"
```

## Maven Execution Workflow

### Complete Step-by-Step Process

```bash
# Step 1: Verify prerequisites
java -version
mvn -version
.\check-tesseract.bat

# Step 2: Clean and build
mvn clean
mvn compile
mvn dependency:copy-dependencies

# Step 3: Execute OCR extraction
mvn exec:java -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" -Dexec.args="\"PDF for Automation Testing.pdf\""

# Step 4: Verify output
type ocr-highlights-extraction.json
```

### Debug Mode Execution
```bash
# Enable debug logging
mvn exec:java \
  -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" \
  -Dexec.args="\"PDF for Automation Testing.pdf\"" \
  -Dexec.jvmArgs="-Dorg.slf4j.simpleLogger.defaultLogLevel=debug"
```

## Performance Optimization

### For Large PDFs
```bash
# High memory + parallel GC
mvn exec:java \
  -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" \
  -Dexec.args="\"large-document.pdf\"" \
  -Dexec.jvmArgs="-Xmx8g -XX:+UseG1GC -XX:+UseStringDeduplication"
```

### For Better OCR Accuracy
The OCR extractor uses 300 DPI by default. You can modify the `DPI` constant in `OcrPdfHighlightExtractor.java`:
- **Higher DPI (400-600)**: Better accuracy, slower processing
- **Lower DPI (200-250)**: Faster processing, lower accuracy

## Expected Output

After successful Maven execution, you should see:
```
✓ Using Tesseract data path: C:\Users\[username]\AppData\Local\Programs\Tesseract-OCR\tessdata
✓ Found eng.traineddata at: [path]\eng.traineddata
✓ Tesseract initialized successfully
Processing page 1...
  Saved page image: page_1.png
    YELLOW: "Autorizo a Scotiabank Inverlat..."
Found 1 highlights on page 1
...
JSON OUTPUT SAVED TO: ocr-highlights-extraction.json
```

## Quick Reference Commands

| Task | Maven Command |
|------|---------------|
| **Check Setup** | `.\check-tesseract.bat` |
| **Build Project** | `mvn clean compile dependency:copy-dependencies` |
| **Run OCR Extractor** | `mvn exec:java -Dexec.mainClass="com.scotiapdf.OcrPdfHighlightExtractor" -Dexec.args="\"PDF for Automation Testing.pdf\""` |
| **Run Simple Extractor** | `mvn exec:java -Dexec.mainClass="com.scotiapdf.SimplePdfHighlightExtractor" -Dexec.args="\"PDF for Automation Testing.pdf\""` |
| **Debug Mode** | Add `-Dexec.jvmArgs="-Dorg.slf4j.simpleLogger.defaultLogLevel=debug"` |
| **High Memory** | Add `-Dexec.jvmArgs="-Xmx4g"` |

This guide provides comprehensive Maven-based execution instructions for Tesseract OCR integration with the PDF highlight extraction project.
