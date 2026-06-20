#!/bin/bash
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"

PROJECTS=("v1_20_0" "v1_20_1" "v1_20_2" "v1_20_3" "v1_20_4" "v1_20_5" "v1_20_6")

for p in "${PROJECTS[@]}"; do
    echo ""
    echo "=========================================="
    echo "  Building $p"
    echo "=========================================="

    chmod +x "$ROOT/$p/gradlew"
    "$ROOT/$p/gradlew" -p "$ROOT/$p" build --no-daemon

    echo "Built: $p"
done

echo ""
echo "All builds succeeded."