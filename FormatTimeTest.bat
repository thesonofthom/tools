@echo off
java -cp %~dp0/dist/thesonofthom.jar com.thesonofthom.tools.examples.FormatTime test -out:formatTimeTest.txt %*
echo.
pause