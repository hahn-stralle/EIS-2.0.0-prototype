@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

java -jar %~dp05___eis.jar -keygenerate

endlocal
exit /b