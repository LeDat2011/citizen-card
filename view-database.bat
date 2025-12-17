@echo off
REM ========================================
REM   CITIZEN CARD DATABASE VIEWER
REM ========================================
REM Simple script to view database content
REM Close the desktop app before running!

cd desktop

echo.
echo ========================================
echo    DATABASE VIEWER
echo ========================================
echo.

REM Check if database exists
if not exist "data\citizen_card.mv.db" (
    echo [ERROR] Database not found!
    echo.
    echo Please run the desktop application first.
    echo Database will be created at: desktop\data\citizen_card.mv.db
    echo.
    pause
    cd ..
    exit /b 1
)

REM Check if H2 jar exists, if not download it
if not exist "target\dependency\h2-2.2.224.jar" (
    echo [INFO] H2 library not found. Downloading...
    echo This will take a moment...
    echo.
    call mvn dependency:copy-dependencies -q
    if errorlevel 1 (
        echo [ERROR] Failed to download H2 library!
        echo.
        echo Please run manually:
        echo   cd desktop
        echo   mvn dependency:copy-dependencies
        echo.
        pause
        cd ..
        exit /b 1
    )
    echo [SUCCESS] H2 library downloaded!
    echo.
)

echo [INFO] Database: data\citizen_card.mv.db
echo [WARN] Make sure the desktop app is CLOSED!
echo.
echo Choose viewing method:
echo   1. Quick View (Terminal)
echo   2. H2 Web Console (Browser)
echo   3. Exit
echo.

set /p choice="Enter choice (1-3): "

if "%choice%"=="1" goto :terminal_view
if "%choice%"=="2" goto :web_console
if "%choice%"=="3" goto :exit
goto :invalid

:terminal_view
echo.
echo ========================================
echo    TERMINAL VIEW
echo ========================================
echo.

echo [1/5] Registered Cards:
echo ----------------------------------------
java -cp "target\dependency\h2-2.2.224.jar" org.h2.tools.Shell -url "jdbc:h2:file:./data/citizen_card" -sql "SELECT ID, CARD_ID, CARD_STATUS, REGISTERED_AT FROM REGISTERED_CARDS ORDER BY REGISTERED_AT DESC;"

echo.
echo [2/5] Top-up Requests:
echo ----------------------------------------
java -cp "target\dependency\h2-2.2.224.jar" org.h2.tools.Shell -url "jdbc:h2:file:./data/citizen_card" -sql "SELECT ID, CARD_ID, AMOUNT, STATUS, CREATED_AT, APPROVED_AT FROM TOPUP_REQUESTS ORDER BY CREATED_AT DESC;"

echo.
echo [3/5] Invoices:
echo ----------------------------------------
java -cp "target\dependency\h2-2.2.224.jar" org.h2.tools.Shell -url "jdbc:h2:file:./data/citizen_card" -sql "SELECT ID, CARD_ID, AMOUNT, DESCRIPTION, STATUS, CREATED_AT FROM INVOICES ORDER BY CREATED_AT DESC;"

echo.
echo [4/5] Recent Transactions:
echo ----------------------------------------
java -cp "target\dependency\h2-2.2.224.jar" org.h2.tools.Shell -url "jdbc:h2:file:./data/citizen_card" -sql "SELECT ID, CARD_ID, OPERATION_TYPE, AMOUNT, SUCCESS, TIMESTAMP FROM TRANSACTION_LOGS ORDER BY TIMESTAMP DESC LIMIT 10;"

echo.
echo [5/5] Statistics:
echo ----------------------------------------
java -cp "target\dependency\h2-2.2.224.jar" org.h2.tools.Shell -url "jdbc:h2:file:./data/citizen_card" -sql "SELECT CARD_STATUS, COUNT(*) AS COUNT FROM REGISTERED_CARDS GROUP BY CARD_STATUS;"

echo.
echo ========================================
pause
goto :exit

:web_console
echo.
echo ========================================
echo    H2 WEB CONSOLE
echo ========================================
echo.
echo Starting H2 Web Console...
echo Browser will open at: http://localhost:8082
echo.
echo CONNECTION INFO:
echo   JDBC URL: jdbc:h2:file:./data/citizen_card
echo   User: (leave empty)
echo   Password: (leave empty)
echo.
echo Press Ctrl+C to stop the console
echo.

timeout /t 2 /nobreak >nul
start http://localhost:8082

java -cp "target\dependency\h2-2.2.224.jar" org.h2.tools.Server -web -webAllowOthers -webPort 8082

goto :exit

:invalid
echo.
echo [ERROR] Invalid choice!
pause
goto :exit

:exit
cd ..
exit /b 0
