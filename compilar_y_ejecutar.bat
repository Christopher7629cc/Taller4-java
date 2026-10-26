@echo off
REM Uso:  compilar_y_ejecutar.bat [semilla]
if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -d out src\modelo\*.java src\app\*.java
if errorlevel 1 exit /b 1
java -cp out app.Main %*
