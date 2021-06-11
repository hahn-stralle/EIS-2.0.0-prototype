@echo off
setlocal enabledelayedexpansion

java -jar %~dp05___eis.jar -keygenerate %~dp0

endlocal
echo.
pause
exit /b