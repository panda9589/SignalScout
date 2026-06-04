# SignalScout Phase 1 MVP - Quick Start (Windows)

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "SignalScout Phase 1 MVP - Quick Start" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Check prerequisites
Write-Host "Checking prerequisites..." -ForegroundColor Yellow

# Check Java
try {
    $java = java -version 2>&1
    Write-Host "✓ Java installed" -ForegroundColor Green
} catch {
    Write-Host "❌ Java not found. Please install Java 21+" -ForegroundColor Red
    exit 1
}

# Check Gradle
try {
    $gradle = gradle --version 2>&1
    Write-Host "✓ Gradle installed" -ForegroundColor Green
} catch {
    Write-Host "❌ Gradle not found. Please install Gradle 8.0+" -ForegroundColor Red
    exit 1
}

# Check PostgreSQL
try {
    $psql = psql --version 2>&1
    Write-Host "✓ PostgreSQL installed" -ForegroundColor Green
} catch {
    Write-Host "❌ PostgreSQL not found. Please install PostgreSQL 14+" -ForegroundColor Red
    exit 1
}

# Check Node.js
try {
    $node = node --version
    Write-Host "✓ Node.js $node installed" -ForegroundColor Green
} catch {
    Write-Host "❌ Node.js not found. Please install Node.js 18+" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Step 1: Set up PostgreSQL Database" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

$createDb = Read-Host "Create signalscout database? (y/n)"
if ($createDb -eq 'y' -or $createDb -eq 'Y') {
    try {
        psql postgres -c "CREATE DATABASE signalscout;" 2>$null
    } catch {
        Write-Host "Database may already exist" -ForegroundColor Yellow
    }
    
    $schema = Get-Content infra/postgres-init/001_init_schema.sql
    psql signalscout -c $schema > $null 2>&1
    Write-Host "✓ Database initialized" -ForegroundColor Green
} else {
    Write-Host "Skipping database setup" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Step 2: Configure Environment Variables" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

$backendEnv = "backend\.env"
if (-not (Test-Path $backendEnv)) {
    Write-Host "Creating backend\.env file..."
    @'
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/signalscout"
$env:SPRING_DATASOURCE_USERNAME="scout"
$env:SPRING_DATASOURCE_PASSWORD="scout_dev_password"
$env:OPENAI_API_KEY="sk-..." # Replace with your OpenAI API key
'@ | Out-File $backendEnv -Encoding UTF8
    Write-Host "✓ Created backend\.env" -ForegroundColor Green
    Write-Host "⚠️  Please update OPENAI_API_KEY in backend\.env" -ForegroundColor Yellow
} else {
    Write-Host "✓ backend\.env already exists" -ForegroundColor Green
}

$frontendEnv = "frontend\.env.local"
if (-not (Test-Path $frontendEnv)) {
    Write-Host "Creating frontend\.env.local..."
    @'
NEXT_PUBLIC_API_URL=http://localhost:8080/api
'@ | Out-File $frontendEnv -Encoding UTF8
    Write-Host "✓ Created frontend\.env.local" -ForegroundColor Green
} else {
    Write-Host "✓ frontend\.env.local already exists" -ForegroundColor Green
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Step 3: Build Backend" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

Push-Location backend
Write-Host "Building backend..."
gradle clean build -x test > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Backend build failed" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Backend built successfully" -ForegroundColor Green
Pop-Location

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Step 4: Install Frontend Dependencies" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

Push-Location frontend
Write-Host "Installing frontend dependencies..."
npm install > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Frontend install failed" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Frontend dependencies installed" -ForegroundColor Green
Pop-Location

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "✅ Setup Complete!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Update OpenAI API key:"
Write-Host "   Edit backend\.env and replace OPENAI_API_KEY with your actual key" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. Start backend (in terminal 1):" -ForegroundColor Cyan
Write-Host "   cd backend && .\.env && gradle bootRun" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Start frontend (in terminal 2):" -ForegroundColor Cyan
Write-Host "   cd frontend && npm run dev" -ForegroundColor Gray
Write-Host ""
Write-Host "4. Open browser:" -ForegroundColor Cyan
Write-Host "   Frontend: http://localhost:3000" -ForegroundColor Gray
Write-Host "   Backend:  http://localhost:8080/api" -ForegroundColor Gray
Write-Host ""
Write-Host "5. Initialize sample data:" -ForegroundColor Cyan
Write-Host "   Go to http://localhost:3000/watchlist and click 'Initialize Sample Data'" -ForegroundColor Gray
Write-Host ""
