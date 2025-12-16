@echo off
echo ========================================
echo    CITIZEN CARD DATABASE VIEWER
echo ========================================
echo.

cd desktop

echo Chon cach xem database:
echo 1. Mo H2 Web Console (khuyen nghi)
echo 2. In du lieu ra console
echo 3. Xem thong ke database
echo 4. Thoat
echo.

set /p choice="Nhap lua chon (1-4): "

if "%choice%"=="1" (
    echo.
    echo Dang khoi dong H2 Web Console...
    java -cp "target\classes;target\dependency\*" citizencard.util.DatabaseViewer console
) else if "%choice%"=="2" (
    echo.
    echo Dang lay du lieu database...
    java -cp "target\classes;target\dependency\*" citizencard.util.DatabaseViewer print
    echo.
    pause
) else if "%choice%"=="3" (
    echo.
    echo Dang tao thong ke...
    java -cp "target\classes;target\dependency\*" citizencard.util.DatabaseViewer stats
    echo.
    pause
) else if "%choice%"=="4" (
    echo Tam biet!
    exit /b 0
) else (
    echo Lua chon khong hop le!
    pause
    goto :eof
)