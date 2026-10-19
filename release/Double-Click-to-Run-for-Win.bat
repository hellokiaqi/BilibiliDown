@echo off
setlocal
cd /d "%~dp0"

rem ---- 选择 Java 运行环境：随包精简JRE > runtime目录 > JAVA_HOME > 系统PATH ----
set "JAVA_EXE=javaw"
if exist "%~dp0minimal-bilibilidown-jre\bin\javaw.exe" set "JAVA_EXE=%~dp0minimal-bilibilidown-jre\bin\javaw.exe"
if exist "%~dp0runtime\bin\javaw.exe" set "JAVA_EXE=%~dp0runtime\bin\javaw.exe"
if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVA_EXE=%JAVA_HOME%\bin\javaw.exe"

rem ---- 选择启动方式：有 launch.jar 时走低内存启动器，否则直接启动主jar ----
set "TARGET_JAR=INeedBiliAV.jar"
if exist "%~dp0launch.jar" set "TARGET_JAR=launch.jar"
if not exist "%~dp0%TARGET_JAR%" (
    echo 未找到 %TARGET_JAR%，请确认压缩包已完整解压后再运行。
    pause
    exit /b 1
)

if /i "%JAVA_EXE%"=="javaw" (
    where javaw >nul 2>nul
    if errorlevel 1 (
        echo 未检测到可用的 Java 运行环境。
        echo 请安装 JRE 8 及以上版本，或把便携版 JRE 放到 minimal-bilibilidown-jre 目录下。
        pause
        exit /b 1
    )
)

rem -Dhttps.protocols=TLSv1.2 用于兼容老版本 JDK 的 TLS 问题 https://bugs.openjdk.org/browse/JDK-8206923
start "" "%JAVA_EXE%" -Dfile.encoding=utf-8 -Dbilibili.prop.log=false -Dhttps.protocols=TLSv1.2 -jar "%TARGET_JAR%"