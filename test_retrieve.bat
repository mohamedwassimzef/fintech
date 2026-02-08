@echo off
setlocal enabledelayedexpansion

cd /d C:\Users\moham\IdeaProjects\untitled

echo.
echo ========================================
echo RETRIEVE OPERATION TEST - ALL ENTITIES
echo ========================================
echo.

C:\Users\moham\.jdks\ms-17.0.18\bin\java.exe -cp "target\classes;lib\mysql-connector-j-9.6.0.jar" -Dfile.encoding=UTF-8 tn.esprit.tests.RetrieveOperationTest

pause

