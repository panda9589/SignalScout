#!/bin/bash

# SignalScout Phase 1 MVP - Quick Start Script

set -e

echo "=========================================="
echo "SignalScout Phase 1 MVP - Quick Start"
echo "=========================================="
echo ""

# Check prerequisites
echo "Checking prerequisites..."

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 21+"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | grep 'version' | head -1)
echo "✓ Java: $JAVA_VERSION"

# Check Gradle
if ! command -v gradle &> /dev/null; then
    echo "❌ Gradle not found. Please install Gradle 8.0+"
    exit 1
fi
echo "✓ Gradle installed"

# Check PostgreSQL
if ! command -v psql &> /dev/null; then
    echo "❌ PostgreSQL not found. Please install PostgreSQL 14+"
    exit 1
fi
echo "✓ PostgreSQL installed"

# Check Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js not found. Please install Node.js 18+"
    exit 1
fi
NODE_VERSION=$(node --version)
echo "✓ Node.js: $NODE_VERSION"

echo ""
echo "=========================================="
echo "Step 1: Set up PostgreSQL Database"
echo "=========================================="

read -p "Create signalscout database? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    psql postgres -c "CREATE DATABASE signalscout;" 2>/dev/null || echo "Database may already exist"
    psql signalscout < infra/postgres-init/001_init_schema.sql
    echo "✓ Database initialized"
else
    echo "Skipping database setup"
fi

echo ""
echo "=========================================="
echo "Step 2: Configure Environment Variables"
echo "=========================================="

if [ ! -f backend/.env ]; then
    echo "Creating backend/.env file..."
    cat > backend/.env << 'EOF'
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/signalscout
export SPRING_DATASOURCE_USERNAME=scout
export SPRING_DATASOURCE_PASSWORD=scout_dev_password
export OPENAI_API_KEY=sk-... # Replace with your OpenAI API key
EOF
    echo "✓ Created backend/.env"
    echo "⚠️  Please update OPENAI_API_KEY in backend/.env"
else
    echo "✓ backend/.env already exists"
fi

if [ ! -f frontend/.env.local ]; then
    echo "Creating frontend/.env.local..."
    cat > frontend/.env.local << 'EOF'
NEXT_PUBLIC_API_URL=http://localhost:8080/api
EOF
    echo "✓ Created frontend/.env.local"
else
    echo "✓ frontend/.env.local already exists"
fi

echo ""
echo "=========================================="
echo "Step 3: Build Backend"
echo "=========================================="

cd backend
echo "Building backend..."
gradle clean build -x test > /dev/null 2>&1 || {
    echo "❌ Backend build failed"
    exit 1
}
echo "✓ Backend built successfully"
cd ..

echo ""
echo "=========================================="
echo "Step 4: Install Frontend Dependencies"
echo "=========================================="

cd frontend
echo "Installing frontend dependencies..."
npm install > /dev/null 2>&1 || {
    echo "❌ Frontend install failed"
    exit 1
}
echo "✓ Frontend dependencies installed"
cd ..

echo ""
echo "=========================================="
echo "✅ Setup Complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo ""
echo "1. Update OpenAI API key:"
echo "   Edit backend/.env and replace OPENAI_API_KEY with your actual key"
echo ""
echo "2. Start backend (in terminal 1):"
echo "   cd backend && source .env && gradle bootRun"
echo ""
echo "3. Start frontend (in terminal 2):"
echo "   cd frontend && npm run dev"
echo ""
echo "4. Open browser:"
echo "   Frontend: http://localhost:3000"
echo "   Backend: http://localhost:8080/api"
echo ""
echo "5. Initialize sample data:"
echo "   Go to http://localhost:3000/watchlist and click 'Initialize Sample Data'"
echo ""
