@echo off
cd /d "%~dp0"

start "Frontend" cmd /k "cd /d frontend && npm run dev"
start "API Gateway" cmd /k ".\gradlew.bat :services:api-gateway:bootRun"
start "User Service" cmd /k ".\gradlew.bat :services:user-service:bootRun"
start "Document Service" cmd /k ".\gradlew.bat :services:document-service:bootRun"
start "File Service" cmd /k ".\gradlew.bat :services:file-service:bootRun"
start "AI Processing" cmd /k ".\gradlew.bat :services:ai-processing-service:bootRun"
