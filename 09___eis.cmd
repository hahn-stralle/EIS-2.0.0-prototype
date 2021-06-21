@echo off
setlocal enabledelayedexpansion

set /a lap=0
for %%f in (%*) do (
	if !lap! equ 1 (
		if exist "%%f\" (
			cd %%f
			for /r %%r in (*) do (
				call %~dp0%1 return %%r
			)
			cd..
		) else (
			call %~dp0%1 return %%f
		)
	) else (
		set /a lap+=1
	)
)
endlocal
pause
exit