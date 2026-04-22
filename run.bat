@echo off
echo ============================================
echo    Cafe Management System - Build Script
echo ============================================

:: Check Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java not found. Please install Java 11+
    pause
    exit /b 1
)

if not exist "lib" mkdir lib

:: Check SQLite JDBC jar
if not exist "lib\sqlite-jdbc.jar" (
    echo Downloading SQLite JDBC driver...
    powershell -Command "Invoke-WebRequest -Uri 'https://github.com/xerial/sqlite-jdbc/releases/download/3.45.1.0/sqlite-jdbc-3.45.1.0.jar' -OutFile 'lib\sqlite-jdbc.jar'"
    if %errorlevel% neq 0 (
        echo FAILED to download SQLite JDBC. Please download manually.
        pause
        exit /b 1
    )
)

:: ---- FIX: Download SLF4J (required by sqlite-jdbc 3.45+) ----
if not exist "lib\slf4j-api-2.0.9.jar" (
    echo Downloading SLF4J API...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar' -OutFile 'lib\slf4j-api-2.0.9.jar'"
    if %errorlevel% neq 0 (
        echo FAILED to download SLF4J API.
        pause
        exit /b 1
    )
)

if not exist "lib\slf4j-simple-2.0.9.jar" (
    echo Downloading SLF4J Simple...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar' -OutFile 'lib\slf4j-simple-2.0.9.jar'"
    if %errorlevel% neq 0 (
        echo FAILED to download SLF4J Simple.
        pause
        exit /b 1
    )
)
:: ----------------------------------------------------------

:: Compile
echo Compiling Java source files...
if not exist "out" mkdir out

javac -cp "lib\sqlite-jdbc.jar;lib\slf4j-api-2.0.9.jar;lib\slf4j-simple-2.0.9.jar" -d out -sourcepath src src\cafe\ui\LoginFrame.java src\cafe\ui\MainFrame.java src\cafe\ui\DashboardPanel.java src\cafe\ui\MenuPanel.java src\cafe\ui\OrderPanel.java src\cafe\ui\OrdersListPanel.java src\cafe\ui\UserManagementPanel.java src\cafe\dao\UserDAO.java src\cafe\dao\MenuDAO.java src\cafe\dao\OrderDAO.java src\cafe\utils\DatabaseManager.java src\cafe\utils\UIConstants.java src\cafe\models\User.java src\cafe\models\MenuItem.java src\cafe\models\Order.java

if %errorlevel% neq 0 (
    echo COMPILATION FAILED. Check errors above.
    pause
    exit /b 1
)

echo Compilation successful!
echo Starting application...
java -cp "out;lib\sqlite-jdbc.jar;lib\slf4j-api-2.0.9.jar;lib\slf4j-simple-2.0.9.jar" cafe.ui.LoginFrame

pause

