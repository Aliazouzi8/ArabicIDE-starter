#!/usr/bin/env bash
# ينزّل Monaco الرسمي بإصدار ثابت وينسخ ملفاته إلى assets/editor/vs
# يحتاج npm (Node.js). شغّله من جذر المشروع: bash scripts/fetch-monaco.sh
set -euo pipefail

MONACO_VERSION="0.52.2"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"

echo "تنزيل monaco-editor@${MONACO_VERSION} ..."
npm pack "monaco-editor@${MONACO_VERSION}" --pack-destination "$TMP" >/dev/null
tar -xzf "$TMP/monaco-editor-${MONACO_VERSION}.tgz" -C "$TMP"

rm -rf "$ROOT/app/src/main/assets/editor/vs"
mkdir -p "$ROOT/app/src/main/assets/editor"
cp -r "$TMP/package/min/vs" "$ROOT/app/src/main/assets/editor/vs"
rm -rf "$TMP"

echo "تم: Monaco ${MONACO_VERSION} داخل app/src/main/assets/editor/vs"
