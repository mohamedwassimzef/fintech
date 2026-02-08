@echo off
echo Running CREATE, RETRIEVE, UPDATE, and DELETE tests...

cd /d C:\Users\moham\IdeaProjects\untitled

echo.
echo ========================================
echo Running CREATE OPERATION TEST
echo ========================================
C:\Users\moham\.jdks\ms-17.0.18\bin\java.exe -cp "target/classes;lib/mysql-connector-j-9.6.0.jar" -Dfile.encoding=UTF-8 tn.esprit.tests.CreateOperationTest

timeout /t 2 /nobreak

echo.
echo ========================================
echo Running RETRIEVE OPERATION TEST
echo ========================================
C:\Users\moham\.jdks\ms-17.0.18\bin\java.exe -cp "target/classes;lib/mysql-connector-j-9.6.0.jar" -Dfile.encoding=UTF-8 tn.esprit.tests.RetrieveOperationTest

timeout /t 2 /nobreak

echo.
echo ========================================
echo Running UPDATE OPERATION TEST
echo ========================================
C:\Users\moham\.jdks\ms-17.0.18\bin\java.exe -cp "target/classes;lib/mysql-connector-j-9.6.0.jar" -Dfile.encoding=UTF-8 tn.esprit.tests.UpdateOperationTest

timeout /t 2 /nobreak

echo.
echo ========================================
echo Running DELETE OPERATION TEST
echo ========================================
C:\Users\moham\.jdks\ms-17.0.18\bin\java.exe -cp "target/classes;lib/mysql-connector-j-9.6.0.jar" -Dfile.encoding=UTF-8 tn.esprit.tests.DeleteOperationTest

echo.
echo All tests completed!
pause


