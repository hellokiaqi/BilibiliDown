@echo off
setlocal
cd /d "%~dp0"

rem ---- 选择 Java 运行环境：随包精简JRE > runtime目录 > JAVA_HOME > 系统PATH ----
set "JAVA_EXE=java"
if exist "%~dp0minimal-bilibilidown-jre\bin\java.exe" set "JAVA_EXE=%~dp0minimal-bilibilidown-jre\bin\java.exe"
if exist "%~dp0runtime\bin\java.exe" set "JAVA_EXE=%~dp0runtime\bin\java.exe"
if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

set "TARGET_JAR=INeedBiliAV.jar"
if exist "%~dp0launch.jar" set "TARGET_JAR=launch.jar"
if not exist "%~dp0%TARGET_JAR%" (
    echo 未找到 %TARGET_JAR%，请确认压缩包已完整解压后再运行。
    pause
    exit /b 1
)

echo 正在启动 %TARGET_JAR% ... 关闭窗口即结束程序。
"%JAVA_EXE%" -Dfile.encoding=utf-8 -Dhttps.protocols=TLSv1.2 -jar "%TARGET_JAR%"
echo.
echo 程序已退出，退出码 %errorlevel%。日志见上方输出或 config 目录。
pause