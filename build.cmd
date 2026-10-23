@echo off
setlocal

if defined VJ_JAVA_HOME (
	set "JAVA_HOME=%VJ_JAVA_HOME%"
) else if exist "%ProgramFiles%\Java\jdk-25" (
	set "JAVA_HOME=%ProgramFiles%\Java\jdk-25"
) else (
	for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-25*") do set "JAVA_HOME=%%D"
	for /d %%D in ("%ProgramFiles%\Java\jdk-2[6-9]*") do set "JAVA_HOME=%%D"
)

if not exist "%JAVA_HOME%\bin\javac.exe" (
	echo JAVA_HOME nao aponta para um JDK valido: "%JAVA_HOME%"
	echo Defina JAVA_HOME para um JDK 25+ ou use: set VJ_JAVA_HOME=C:\caminho\do\jdk-25
	exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Using JAVA_HOME=%JAVA_HOME%

call "%~dp0gradlew.bat" %* --console=plain --no-daemon > "%~dp0build-out.log" 2>&1
set "RESULT=%ERRORLEVEL%"

type "%~dp0build-out.log"
echo.
echo Exit code: %RESULT%

endlocal
exit /b %RESULT%
