$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '../backend/badmintonshop')
try {
  & .\mvnw.cmd '-DskipTests' package
  if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
  $jar = Get-ChildItem ./target/*.jar | Select-Object -First 1
  & java -jar $jar.FullName '--spring.profiles.active=prod'
} finally { Pop-Location }
