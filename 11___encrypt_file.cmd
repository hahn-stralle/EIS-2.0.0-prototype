@echo off
setlocal enabledelayedexpansion

if not %1==return (
	call %~dp009___eis.cmd %~nx0 %*
) else (
	rem 入力ファイルを暗号化プログラムに渡します。
	java -jar %~dp008___eis.jar -encrypt %~dp0 %2
	echo.
)
endlocal
exit /b