#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Start compose stack, smoke-test write-side ingest APIs, commit changes on success.

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

$WriteSidePort = 8080
$IngestTests = @(
    @{ Path = "turbine"; Body = '{"turbineId":"T-001","rpm":12.5,"powerOutput":1500,"vibrationLevel":0.4}' },
    @{ Path = "grid";    Body = '{"region":"north-sea","demandMW":1200,"supplyMW":1150}' }
)

Write-Host ">> Free ports 8080-8081 (stop host listeners if any)"
8080, 8081 | ForEach-Object {
    Get-NetTCPConnection -LocalPort $_ -State Listen -ErrorAction SilentlyContinue |
        ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
}

Write-Host ">> Docker compose up (full stack)"
docker compose -f infra/docker-compose.yml up -d --build | Out-Host
if ($LASTEXITCODE -ne 0) { throw "docker compose up failed" }

Write-Host ">> Wait for write-side health"
$deadline = (Get-Date).AddSeconds(300)
do {
    try {
        $h = Invoke-RestMethod "http://localhost:$WriteSidePort/actuator/health" -TimeoutSec 3
        if ($h.status -eq "UP") { break }
    } catch { }
    if ((Get-Date) -ge $deadline) { throw "write-side not healthy within 300s" }
    Start-Sleep -Seconds 5
} while ($true)

Write-Host ">> Smoke tests (write-side ingest API)"
foreach ($test in $IngestTests) {
    $uri = "http://localhost:$WriteSidePort/api/ingest/$($test.Path)"
    $r = Invoke-RestMethod -Method POST -Uri $uri -ContentType "application/json" -Body $test.Body
    if (-not $r.eventId -or $r.status -ne "PENDING") {
        throw "ingest/$($test.Path) failed: $($r | ConvertTo-Json -Compress)"
    }
    Write-Host "   OK ingest/$($test.Path) eventId=$($r.eventId)"
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
