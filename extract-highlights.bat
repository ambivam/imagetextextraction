@echo off
echo PDF Highlight Extractor
echo =======================

if "%~1"=="" (
    echo Usage: extract-highlights.bat "path-to-pdf-file"
    echo Example: extract-highlights.bat "PDF for Automation Testing.pdf"
    pause
    exit /b 1
)

echo Processing: %~1
echo.

java -cp "target/classes;target/dependency/*" com.scotiapdf.AdvancedPdfHighlightExtractor "%~1"

echo.
echo Extraction completed!
echo Check the generated file: advanced-highlights-extraction.json
pause
