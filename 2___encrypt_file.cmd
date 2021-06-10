@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

rem 出力ディレクトリパス追記。
set export_directory_path=%3encrypt\

if not %1==return (
	call %~dp04___bundle_file.cmd %~nx0 %*
) else (
	rem 入力ファイルを暗号化プログラムに渡します。
	java -jar %~dp05___eis.jar -encrypt %4 %5 %2 %export_directory_path% %~dp0
)
endlocal
exit /b