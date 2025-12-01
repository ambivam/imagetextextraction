# Tesseract OCR Setup Guide

This guide helps you install and configure Tesseract OCR for the OCR-based PDF highlight extractor.

## Windows Installation

### Option 1: Download Pre-built Installer (Recommended)

1. **Download Tesseract for Windows:**
   - Go to: https://github.com/UB-Mannheim/tesseract/wiki
   - Download the latest Windows installer (e.g., `tesseract-ocr-w64-setup-5.3.3.20231005.exe`)

2. **Install Tesseract:**
   - Run the installer as Administrator
   - **Important:** During installation, make sure to select "Additional language data" if you need languages other than English
   - Default installation path: `C:\Program Files\Tesseract-OCR`

3. **Add to System PATH (Optional but Recommended):**
   - Open System Properties → Advanced → Environment Variables
   - Add `C:\Program Files\Tesseract-OCR` to your PATH variable
   - This allows you to run `tesseract` from command line

### Option 2: Using Package Manager

**Using Chocolatey:**
```bash
choco install tesseract
```

**Using Scoop:**
```bash
scoop install tesseract
```

## Verify Installation

Open Command Prompt and run:
```bash
tesseract --version
```

You should see output like:
```
tesseract 5.3.3
 leptonica-1.83.1
  libgif 5.2.1 : libjpeg 8d (libjpeg-turbo 2.1.4) : libpng 1.6.39 : libtiff 4.5.1 : zlib 1.2.13 : libwebp 1.3.2 : libopenjp2 2.5.0
 Found AVX2
 Found AVX
 Found FMA
 Found SSE4.1
 Found OpenMP 201511
 Found libarchive 3.6.2 zlib/1.2.13 liblzma/5.2.9 bz2/1.0.8 libzstd/1.5.2
 Found libcurl/8.4.0 (schannel) libssh2/1.11.0
```

## Configuration for Java Application

The OCR extractor automatically searches for Tesseract in these locations:
- `C:\Program Files\Tesseract-OCR\tessdata`
- `C:\Program Files (x86)\Tesseract-OCR\tessdata`
- `tessdata` (current directory)
- Environment variable `TESSDATA_PREFIX`

### Manual Configuration (if needed)

If automatic detection fails, you can:

1. **Set TESSDATA_PREFIX environment variable:**
   ```bash
   set TESSDATA_PREFIX=C:\Program Files\Tesseract-OCR\tessdata
   ```

2. **Or create a tessdata folder in your project directory** and copy the language files from the Tesseract installation.

## Language Data

The extractor uses English (`eng`) by default. Language data files are located in:
- `C:\Program Files\Tesseract-OCR\tessdata\`

Common language files:
- `eng.traineddata` - English (required)
- `spa.traineddata` - Spanish
- `fra.traineddata` - French

## Testing OCR

Test Tesseract with a sample image:
```bash
tesseract sample_image.png output_text.txt
```

## Troubleshooting

### Common Issues:

1. **"Tesseract not found" error:**
   - Verify installation path
   - Check PATH environment variable
   - Try running `tesseract --version` in command prompt

2. **"tessdata not found" error:**
   - Verify tessdata folder exists in installation directory
   - Check TESSDATA_PREFIX environment variable
   - Ensure `eng.traineddata` file exists

3. **Poor OCR accuracy:**
   - Increase DPI in the Java application (default is 300)
   - Ensure PDF quality is good
   - Check if text is clearly visible in the PDF

4. **Memory issues with large PDFs:**
   - Process pages individually
   - Reduce DPI if needed
   - Increase JVM heap size: `-Xmx2g`

## Performance Tips

1. **Higher DPI = Better accuracy but slower processing**
   - Default: 300 DPI
   - For small text: 400-600 DPI
   - For large text: 200-300 DPI

2. **Image preprocessing can improve results:**
   - The extractor automatically scales images 2x
   - Consider additional contrast/brightness adjustments

3. **Language-specific improvements:**
   - Use appropriate language data files
   - Set correct language in the Java code if needed

## Advanced Configuration

You can modify the OCR settings in `OcrPdfHighlightExtractor.java`:

```java
tesseract.setPageSegMode(1); // Page segmentation mode
tesseract.setOcrEngineMode(1); // OCR engine mode
tesseract.setLanguage("eng"); // Language
```

Page Segmentation Modes:
- 0: Orientation and script detection (OSD) only
- 1: Automatic page segmentation with OSD (default)
- 3: Fully automatic page segmentation, but no OSD
- 6: Uniform block of text
- 8: Single word
- 13: Raw line. Treat the image as a single text line

## Support

For Tesseract-specific issues, refer to:
- Official documentation: https://tesseract-ocr.github.io/
- GitHub repository: https://github.com/tesseract-ocr/tesseract
- Tess4J documentation: http://tess4j.sourceforge.net/
