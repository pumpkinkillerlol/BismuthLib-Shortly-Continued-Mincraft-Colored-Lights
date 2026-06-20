$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

$projects = @("v1_20_0", "v1_20_1", "v1_20_2", "v1_20_3", "v1_20_4", "v1_20_5", "v1_20_6")

foreach ($p in $projects) {
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "  Building $p" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan

    & "$root\$p\gradlew.bat" -p "$root\$p" build --no-daemon
    if ($LASTEXITCODE -ne 0) {
        Write-Host "BUILD FAILED for $p" -ForegroundColor Red
        exit $LASTEXITCODE
    }

    $jar = Get-ChildItem -Path "$root\$p\build\libs" -Filter "*-0.1.0.jar" |
        Where-Object { $_.Name -notlike "*-sources.jar" } |
        Select-Object -First 1

    if ($jar) {
        Write-Host "Built: $($jar.FullName)" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "All builds succeeded." -ForegroundColor Green