@echo off
echo Simple PDF Highlight Extractor (No OCR Required)
echo ================================================

if "%~1"=="" (
    echo Usage: extract-highlights-simple.bat "path-to-pdf-file"
    echo Example: extract-highlights-simple.bat "PDF for Automation Testing.pdf"
    echo.
    echo NOTE: This version does NOT require Tesseract OCR installation.
    echo It uses area-based text extraction which may be less accurate than OCR.
    pause
    exit /b 1
)

echo Processing: %~1
echo Method: Simple area-based text extraction (no OCR)
echo.

java -cp "target/classes;target/dependency/*" com.scotiapdf.SimplePdfHighlightExtractor "%~1"

echo.
echo Extraction completed!
echo Check the generated file: simple-highlights-extraction.json
pause
