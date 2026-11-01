#!/bin/bash
# SS Mart Price Label Generator — Quick Launcher
# Double-click this file or run: bash start.sh

APP_DIR="$(dirname "$0")"
cd "$APP_DIR"

echo "🚀 Starting SS Mart Price Label Generator..."
echo "📄 Open your browser at: http://localhost:8080"
echo "   (Press Ctrl+C to stop)"
echo ""

# Open browser after 4 seconds
(sleep 4 && open http://localhost:8080) &

mvn spring-boot:run
