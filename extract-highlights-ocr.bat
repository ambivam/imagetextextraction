@echo off
echo OCR-Based PDF Highlight Extractor
echo ==================================

if "%~1"=="" (
    echo Usage: extract-highlights-ocr.bat "path-to-pdf-file"
    echo Example: extract-highlights-ocr.bat "PDF for Automation Testing.pdf"
    echo.
    echo NOTE: This requires Tesseract OCR to be installed on your system.
    echo Download from: https://github.com/UB-Mannheim/tesseract/wiki
    pause
    exit /b 1
)

echo Processing: %~1
echo Method: OCR-based text extraction using Tesseract
echo.

java -cp "target/classes;target/dependency/*" com.scotiapdf.OcrPdfHighlightExtractor "%~1"

echo.
echo Extraction completed!
echo Check the generated file: ocr-highlights-extraction.json
pause
