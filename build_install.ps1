$env:JAVA_HOME = "C:\Temp\Antigrvity\audio\.jdk17\jdk-17.0.11+9"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
Write-Host "JAVA_HOME set to: $env:JAVA_HOME"
& "C:\Temp\Antigrvity\audio\gradlew.bat" installDebug
