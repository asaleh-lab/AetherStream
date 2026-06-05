#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Start data source containers, smoke-test ingest APIs, commit changes on success.

.EXAMPLE
  .\scripts\smoke-ingest.ps1

.EXAMPLE
  .\scripts\smoke-ingest.ps1 -SkipCommit
#>
param(
    [switch]$SkipCommit,
    [string]$CommitMessage = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $RepoRoot

$DataSources = @(
    @{ Name = "datasource-weather"; Port = 8081; Path = "weather"; Body = '{"region":"north-sea","windSpeedMs":8.5,"temperatureC":12.3}' },
    @{ Name = "datasource-turbine"; Port = 8082; Path = "turbine"; Body = '{"turbineId":"T-001","rpm":12.5,"powerOutput":1500,"vibrationLevel":0.4}' },
    @{ Name = "datasource-grid";    Port = 8083; Path = "grid";    Body = '{"region":"north-sea","demandMW":1200,"supplyMW":1150}' }
)

Write-Host ">> Free ports 8081-8083 (stop host listeners if any)"
8081, 8082, 8083 | ForEach-Object {
    Get-NetTCPConnection -LocalPort $_ -State Listen -ErrorAction SilentlyContinue |
        ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
}

Write-Host ">> Docker compose up (infra + data sources)"
docker compose -f infra/docker-compose.yml up -d --build | Out-Host
if ($LASTEXITCODE -ne 0) { throw "docker compose up failed" }

Write-Host ">> Wait for health"
$deadline = (Get-Date).AddSeconds(240)
do {
    $allUp = $true
    foreach ($ds in $DataSources) {
        try {
            $h = Invoke-RestMethod "http://localhost:$($ds.Port)/actuator/health" -TimeoutSec 3
            if ($h.status -ne "UP") { $allUp = $false; break }
        } catch { $allUp = $false; break }
    }
    if ($allUp) { break }
    if ((Get-Date) -ge $deadline) { throw "Data sources not healthy within 240s" }
    Start-Sleep -Seconds 5
} while ($true)

Write-Host ">> Smoke tests"
foreach ($ds in $DataSources) {
    $uri = "http://localhost:$($ds.Port)/api/ingest/$($ds.Path)"
    $r = Invoke-RestMethod -Method POST -Uri $uri -ContentType "application/json" -Body $ds.Body
    if (-not $r.eventId -or $r.status -ne "PENDING") {
        throw "$($ds.Name) ingest failed: $($r | ConvertTo-Json -Compress)"
    }
    Write-Host "   OK $($ds.Name) eventId=$($r.eventId)"
}

if (-not $SkipCommit) {
    if (git status --porcelain) {
        if (-not $CommitMessage) { $CommitMessage = "chore(ingest): verify ingest smoke tests" }
        git add -A
        git commit -m $CommitMessage
        if ($LASTEXITCODE -ne 0) { throw "git commit failed" }
        Write-Host ">> Committed: $(git log -1 --oneline)"
    } else {
        Write-Host ">> No changes to commit"
    }
}

Write-Host ">> Done"
