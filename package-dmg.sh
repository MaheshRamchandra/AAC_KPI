#!/bin/bash
set -euo pipefail

APP_NAME="AAC-KPI"
APP_VERSION="1.0.0"
BUILD_DIR="$(pwd)/dmg-output"
MAIN_JAR="target/kpi-data-generator-0.1.0-SNAPSHOT.jar"
FX_LIB="./javafx-sdk-17.0.16/lib"
ICON="KPILOGO.icns"
MAIN_CLASS="com.aac.kpi.Main"
STUB_DIR="./universal-stub-resources"
STUB_FILE="$STUB_DIR/universalJavaApplicationStub"

if [[ ! -f "${MAIN_JAR}" ]]; then
  echo "Missing ${MAIN_JAR}; run ./mvnw clean package first." >&2
  exit 1
fi

if [[ ! -d "${FX_LIB}" ]]; then
  echo "JavaFX library path not found at ${FX_LIB}." >&2
  exit 1
fi

if [[ ! -f "${ICON}" ]]; then
  echo "Icon file not found: ${ICON}" >&2
  exit 1
fi

if [[ ! -d "${STUB_DIR}" || ! -f "${STUB_FILE}" ]]; then
  echo "Universal stub resources missing; expected ${STUB_FILE}" >&2
  exit 1
fi

rm -rf "${BUILD_DIR}"

/usr/bin/jpackage \
  --type dmg \
  --dest "${BUILD_DIR}" \
  --name "${APP_NAME}" \
  --app-version "${APP_VERSION}" \
  --input target \
  --main-jar kpi-data-generator-0.1.0-SNAPSHOT.jar \
  --main-class "${MAIN_CLASS}" \
  --module-path "${FX_LIB}" \
  --add-modules javafx.controls,javafx.fxml,javafx.graphics \
  --icon "${ICON}" \
  --java-options "-Dprism.order=es2,sw" \
  --resource-dir "${STUB_DIR}"

DMG_FILE="${BUILD_DIR}/${APP_NAME}-${APP_VERSION}.dmg"
if [[ -f "${DMG_FILE}" ]]; then
  echo "DMG successfully created at ${DMG_FILE}"
else
  echo "jpackage finished but DMG was not found." >&2
  exit 1
fi
