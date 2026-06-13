<#
.SYNOPSIS
    debug APK をビルドして USB 接続中の実機にインストールし、アプリを起動する。

.DESCRIPTION
    gradlew installDebug でビルド〜署名〜インストールまでを一括実行する。
    adb の PATH 設定は不要(AGP が SDK 同梱の adb を利用)。
    インストール後にアプリを自動起動し、-Log 指定時はアプリのログのみを追従表示する。

.PARAMETER Log
    インストール・起動後に logcat をアプリの PID にフィルタして追従表示する。

.EXAMPLE
    .\deploy.ps1
    .\deploy.ps1 -Log
#>
param([switch]$Log)

$ErrorActionPreference = "Stop"
$appId = "com.fuku856.povomanager"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path $adb)) {
    Write-Host "adb が見つかりません: $adb" -ForegroundColor Red
    Write-Host "Android SDK Platform-Tools をインストールしてください。" -ForegroundColor Yellow
    exit 1
}

# 接続デバイス確認(末尾が "device" の行のみカウント。offline/unauthorized は除外)
# @() で配列化しないと 0 台時に $null となり .Count 判定が効かない(PS 5.1)
$devices = @(& $adb devices | Select-String "device$")
if ($devices.Count -eq 0) {
    Write-Host "実機が見つかりません。" -ForegroundColor Yellow
    Write-Host "  1. 端末の「開発者向けオプション」→「USB デバッグ」を ON" -ForegroundColor Yellow
    Write-Host "  2. PC に USB 接続" -ForegroundColor Yellow
    Write-Host "  3. 端末側の認証ダイアログを許可" -ForegroundColor Yellow
    exit 1
}
if ($devices.Count -gt 1) {
    Write-Host "複数デバイスが接続されています:" -ForegroundColor Yellow
    & $adb devices
    Write-Host "対象を 1 台に絞るには、対象シリアルを `$env:ANDROID_SERIAL に設定してから再実行してください。" -ForegroundColor Yellow
}

# ビルド + インストール
Write-Host "==> gradlew installDebug 実行中..." -ForegroundColor Cyan
.\gradlew.bat installDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "ビルド/インストールに失敗しました (exit $LASTEXITCODE)" -ForegroundColor Red
    exit $LASTEXITCODE
}

# アプリ起動
Write-Host "==> アプリを起動..." -ForegroundColor Cyan
& $adb shell am start -n "$appId/.MainActivity" | Out-Null

# 任意でログ追従
if ($Log) {
    $appPid = (& $adb shell pidof -s $appId).Trim()
    if ([string]::IsNullOrEmpty($appPid)) {
        Write-Host "アプリの PID を取得できませんでした。logcat をスキップします。" -ForegroundColor Yellow
        exit 0
    }
    Write-Host "==> logcat 追従 (PID=$appPid)。Ctrl+C で終了。" -ForegroundColor Cyan
    & $adb logcat --pid $appPid
}
