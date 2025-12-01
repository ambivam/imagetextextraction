@echo off
echo Tesseract OCR Installation Checker
echo ==================================

echo.
echo 1. Checking if Tesseract is installed...

where tesseract >nul 2>&1
if %errorlevel% == 0 (
    echo ✓ Tesseract executable found in PATH
    tesseract --version
) else (
    echo ❌ Tesseract executable NOT found in PATH
    echo    You may need to add Tesseract to your PATH or install it
)

echo.
echo 2. Checking for tessdata directories...

set "found_tessdata=0"

if exist "C:\Program Files\Tesseract-OCR\tessdata\eng.traineddata" (
    echo ✓ Found tessdata at: C:\Program Files\Tesseract-OCR\tessdata\
    set "found_tessdata=1"
)

if exist "C:\Program Files (x86)\Tesseract-OCR\tessdata\eng.traineddata" (
    echo ✓ Found tessdata at: C:\Program Files (x86)^)\Tesseract-OCR\tessdata\
    set "found_tessdata=1"
)

if exist "%TESSDATA_PREFIX%\eng.traineddata" (
    echo ✓ Found tessdata at: %TESSDATA_PREFIX%\
    set "found_tessdata=1"
)

if exist "tessdata\eng.traineddata" (
    echo ✓ Found tessdata at: .\tessdata\
    set "found_tessdata=1"
)

if %found_tessdata% == 0 (
    echo ❌ No tessdata directories found with eng.traineddata
    echo.
    echo INSTALLATION REQUIRED:
    echo 1. Download Tesseract from: https://github.com/UB-Mannheim/tesseract/wiki
    echo 2. Install it to C:\Program Files\Tesseract-OCR\
    echo 3. Make sure to include language data during installation
    echo 4. Add C:\Program Files\Tesseract-OCR to your PATH environment variable
    echo.
    echo Or use package managers:
    echo   choco install tesseract
    echo   scoop install tesseract
) else (
    echo ✓ Tesseract language data found!
)

echo.
echo 3. Environment Variables:
if defined TESSDATA_PREFIX (
    echo ✓ TESSDATA_PREFIX = %TESSDATA_PREFIX%
) else (
    echo ⚠ TESSDATA_PREFIX not set (this is optional if Tesseract is properly installed)
)

echo.
echo 4. Quick Test:
if %found_tessdata% == 1 (
    echo Creating a test image...
    echo This is a test > test_text.txt
    
    where tesseract >nul 2>&1
    if %errorlevel% == 0 (
        echo Running Tesseract test...
        tesseract --list-langs 2>nul
        if %errorlevel% == 0 (
            echo ✓ Tesseract is working correctly!
        ) else (
            echo ❌ Tesseract test failed
        )
    )
    
    if exist test_text.txt del test_text.txt
)

echo.
pause
