@echo off
chcp 65001 >nul
echo ==========================================
echo 1. 8080 포트 사용 중인 프로세스 확인 및 종료
echo ==========================================
set FOUND=0
for /f "tokens=5" %%a in ('netstat -a -n -o ^| findstr :8080 ^| findstr LISTENING') do (
    set FOUND=1
    echo [알림] 포트 8080을 점유 중인 프로세스(PID: %%a)가 살아있습니다. 강제 종료합니다.
    taskkill /F /PID %%a
)

if "%FOUND%"=="0" (
    echo [알림] 8080 포트를 점유 중인 프로세스가 없습니다. (이미 죽어있음)
)

echo.
echo ==========================================
echo 2. 서버 재기동 (gradlew bootRun)
echo ==========================================
call gradlew.bat bootRun
pause