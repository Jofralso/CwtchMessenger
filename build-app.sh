#!/bin/bash
#
# Cwtch Messenger - Build Script
# Creates distributable applications for Linux, Windows, and macOS
#

set -e

APP_NAME="CwtchMessenger"
APP_VERSION="1.0.0"
MAIN_CLASS="app.TerminalMain"
MAIN_CLASS_GUI="app.Main"
JAR_NAME="cwtch-terminal.jar"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}"
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║           Cwtch Messenger - Build System                      ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}Error: Java 17+ required. Found Java $JAVA_VERSION${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java $JAVA_VERSION detected${NC}"

# Build function
build_jar() {
    echo -e "\n${CYAN}[1/4] Building JAR...${NC}"
    mvn clean package -DskipTests -q
    if [ -f "target/$JAR_NAME" ]; then
        echo -e "${GREEN}✓ JAR built: target/$JAR_NAME${NC}"
    else
        echo -e "${RED}✗ JAR build failed${NC}"
        exit 1
    fi
}

# Create launcher scripts
create_launchers() {
    echo -e "\n${CYAN}[2/4] Creating launcher scripts...${NC}"
    
    mkdir -p dist/bin
    
    # Linux/macOS launcher
    cat > dist/bin/cwtch << 'EOF'
#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
java -jar "$SCRIPT_DIR/../lib/cwtch-terminal.jar" "$@"
EOF
    chmod +x dist/bin/cwtch
    
    # Windows launcher (batch)
    cat > dist/bin/cwtch.bat << 'EOF'
@echo off
set SCRIPT_DIR=%~dp0
java -jar "%SCRIPT_DIR%..\lib\cwtch-terminal.jar" %*
EOF
    
    # Windows launcher (PowerShell)
    cat > dist/bin/cwtch.ps1 << 'EOF'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
java -jar "$ScriptDir\..\lib\cwtch-terminal.jar" $args
EOF
    
    echo -e "${GREEN}✓ Launcher scripts created${NC}"
}

# Create distribution structure
create_dist() {
    echo -e "\n${CYAN}[3/4] Creating distribution package...${NC}"
    
    mkdir -p dist/lib
    mkdir -p dist/docs
    
    # Copy JAR
    cp target/$JAR_NAME dist/lib/
    
    # Copy docs
    cp README.md dist/docs/ 2>/dev/null || true
    cp LICENSE dist/docs/ 2>/dev/null || true
    
    # Create version file
    echo "$APP_VERSION" > dist/VERSION
    
    # Create info file
    cat > dist/docs/INSTALL.txt << EOF
Cwtch Messenger v$APP_VERSION
==============================

INSTALLATION:
-------------
1. Ensure Java 17+ is installed
2. Extract this archive anywhere
3. Run the launcher:
   - Linux/macOS: ./bin/cwtch
   - Windows:     bin\cwtch.bat

COMMAND LINE OPTIONS:
---------------------
  --offline, -o    Start without Tor (for testing)
  --no-color, -n   Disable ANSI colors
  --help, -h       Show help

QUICK START:
------------
1. Run: ./bin/cwtch --offline
2. Type: /help
3. Type: /id  (shows your address)
4. Type: /quit

For Tor connectivity, install Tor and run without --offline flag.

REQUIREMENTS:
-------------
- Java 17 or higher
- Tor (optional, for real anonymous messaging)

EOF
    
    echo -e "${GREEN}✓ Distribution structure created${NC}"
}

# Package distribution
package_dist() {
    echo -e "\n${CYAN}[4/4] Packaging...${NC}"
    
    PLATFORM=$(uname -s | tr '[:upper:]' '[:lower:]')
    ARCH=$(uname -m)
    
    ARCHIVE_NAME="${APP_NAME}-${APP_VERSION}-${PLATFORM}-${ARCH}"
    
    # Create tar.gz for Linux/macOS
    if [ "$PLATFORM" != "windows" ]; then
        cd dist
        tar -czf "../${ARCHIVE_NAME}.tar.gz" .
        cd ..
        echo -e "${GREEN}✓ Created: ${ARCHIVE_NAME}.tar.gz${NC}"
    fi
    
    # Create zip (works everywhere)
    cd dist
    zip -rq "../${ARCHIVE_NAME}.zip" .
    cd ..
    echo -e "${GREEN}✓ Created: ${ARCHIVE_NAME}.zip${NC}"
}

# Native image with jpackage (if available)
build_native() {
    echo -e "\n${CYAN}Building native installer...${NC}"
    
    if ! command -v jpackage &> /dev/null; then
        echo -e "${YELLOW}⚠ jpackage not found (requires JDK 14+). Skipping native build.${NC}"
        return
    fi
    
    mkdir -p native-dist
    
    PLATFORM=$(uname -s | tr '[:upper:]' '[:lower:]')
    
    case $PLATFORM in
        linux)
            # Linux: Create .deb and .rpm if tools available
            jpackage \
                --input target \
                --name "$APP_NAME" \
                --main-jar "$JAR_NAME" \
                --main-class "$MAIN_CLASS" \
                --app-version "$APP_VERSION" \
                --dest native-dist \
                --type app-image \
                --java-options "-Xmx256m" \
                --icon assets/icon.png 2>/dev/null || \
            jpackage \
                --input target \
                --name "$APP_NAME" \
                --main-jar "$JAR_NAME" \
                --main-class "$MAIN_CLASS" \
                --app-version "$APP_VERSION" \
                --dest native-dist \
                --type app-image \
                --java-options "-Xmx256m"
            
            echo -e "${GREEN}✓ Native app image created in native-dist/${NC}"
            ;;
        darwin)
            # macOS: Create .app bundle
            jpackage \
                --input target \
                --name "$APP_NAME" \
                --main-jar "$JAR_NAME" \
                --main-class "$MAIN_CLASS" \
                --app-version "$APP_VERSION" \
                --dest native-dist \
                --type app-image \
                --java-options "-Xmx256m"
            
            echo -e "${GREEN}✓ macOS .app bundle created in native-dist/${NC}"
            ;;
        *)
            echo -e "${YELLOW}⚠ Native packaging not supported on this platform${NC}"
            ;;
    esac
}

# Main build process
main() {
    echo -e "${YELLOW}Building Cwtch Messenger v${APP_VERSION}...${NC}\n"
    
    build_jar
    create_launchers
    create_dist
    package_dist
    
    # Optional: Try native build
    if [ "$1" == "--native" ]; then
        build_native
    fi
    
    echo -e "\n${GREEN}════════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}                    BUILD COMPLETE!                              ${NC}"
    echo -e "${GREEN}════════════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "Distribution packages:"
    ls -lh *.tar.gz *.zip 2>/dev/null || true
    echo ""
    echo -e "Run with: ${CYAN}java -jar dist/lib/cwtch-terminal.jar --offline${NC}"
    echo -e "Or:       ${CYAN}./dist/bin/cwtch --offline${NC}"
}

main "$@"
