#!/bin/bash
echo "============================================"
echo "   Cafe Management System - Build Script"
echo "============================================"

# Check Java compiler
if ! command -v javac &> /dev/null; then
    echo "ERROR: javac not found. Please install JDK 11+"
    echo "Download from: https://adoptium.net"
    exit 1
fi

# Check SQLite JDBC jar
if [ ! -f "lib/sqlite-jdbc.jar" ]; then
    echo ""
    echo "ERROR: lib/sqlite-jdbc.jar not found!"
    echo "Please download it from:"
    echo "  https://github.com/xerial/sqlite-jdbc/releases/download/3.45.1.0/sqlite-jdbc-3.45.1.0.jar"
    echo "And save it as: lib/sqlite-jdbc.jar"
    echo ""
    # Try auto-download with curl or wget
    if command -v curl &> /dev/null; then
        echo "Trying auto-download with curl..."
        curl -L -o lib/sqlite-jdbc.jar "https://github.com/xerial/sqlite-jdbc/releases/download/3.45.1.0/sqlite-jdbc-3.45.1.0.jar"
    elif command -v wget &> /dev/null; then
        echo "Trying auto-download with wget..."
        wget -O lib/sqlite-jdbc.jar "https://github.com/xerial/sqlite-jdbc/releases/download/3.45.1.0/sqlite-jdbc-3.45.1.0.jar"
    else
        echo "Auto-download failed. Please download manually."
        exit 1
    fi
fi

# Create output directory
mkdir -p out

echo "Compiling Java source files..."

javac -cp "lib/sqlite-jdbc.jar:lib/slf4j-api-2.0.9.jar:lib/slf4j-simple-2.0.9.jar" \
      -d out \
      -sourcepath src \
      src/cafe/models/User.java \
      src/cafe/models/MenuItem.java \
      src/cafe/models/Order.java \
      src/cafe/utils/DatabaseManager.java \
      src/cafe/utils/UIConstants.java \
      src/cafe/dao/UserDAO.java \
      src/cafe/dao/MenuDAO.java \
      src/cafe/dao/OrderDAO.java \
      src/cafe/ui/DashboardPanel.java \
      src/cafe/ui/MenuPanel.java \
      src/cafe/ui/OrderPanel.java \
      src/cafe/ui/OrdersListPanel.java \
      src/cafe/ui/UserManagementPanel.java \
      src/cafe/ui/MainFrame.java \
      src/cafe/ui/LoginFrame.java

if [ $? -ne 0 ]; then
    echo "COMPILATION FAILED. Check errors above."
    exit 1
fi

echo "Compilation successful!"
echo "Starting Cafe Management System..."
echo ""

java -cp "out:lib/sqlite-jdbc.jar:lib/slf4j-api-2.0.9.jar:lib/slf4j-simple-2.0.9.jar" cafe.ui.LoginFrame
