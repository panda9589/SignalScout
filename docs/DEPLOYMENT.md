# Deployment Guide

## Local Development

### Prerequisites

- Docker & Docker Compose
- Java 21
- Node.js 18+
- Git

### 1. Clone the Repository

```bash
cd c:\Users\tyb_l\SignalScout
git init
git config user.name "Your Name"
git config user.email "your.email@example.com"
```

### 2. Copy Environment Variables

```bash
copy .env.example .env
```

Edit `.env` and set your actual values:
- `OPENAI_API_KEY` - Your OpenAI API key
- `SPRING_DATASOURCE_PASSWORD` - Choose a secure password for Postgres
- `JWT_SECRET` - Generate a long random string

### 3. Start Docker Compose

```bash
docker-compose -f infra/docker-compose/docker-compose.yml up -d
```

Verify services are running:
```bash
docker ps
```

Wait for Postgres to be healthy (check logs):
```bash
docker logs signalscout-postgres
```

### 4. Set Up Backend

```bash
cd backend

# Create gradle wrapper (if needed)
gradle wrapper --gradle-version 8.3

# Build the backend
./gradlew build

# Run the backend
./gradlew bootRun
```

Backend should start on `http://localhost:8080/api`

Check health:
```bash
curl http://localhost:8080/api/health
```

### 5. Set Up Frontend

```bash
cd ../frontend

# Install dependencies
npm install

# Run development server
npm run dev
```

Frontend should start on `http://localhost:3000`

### 6. Test the Full Stack

Navigate to `http://localhost:3000` and you should see the SignalScout dashboard.

---

## Production Deployment

### Option 1: Render or Railway

1. Push code to GitHub
2. Connect repository to Render/Railway
3. Set environment variables in dashboard
4. Deploy automatically on push

### Option 2: AWS App Runner

```bash
# Create ECR repositories
aws ecr create-repository --repository-name signalscout-backend
aws ecr create-repository --repository-name signalscout-frontend

# Build and push backend
cd backend
./gradlew bootBuildImage --imageName=signalscout-backend:latest
docker tag signalscout-backend:latest <AWS_ACCOUNT>.dkr.ecr.<REGION>.amazonaws.com/signalscout-backend:latest
docker push <AWS_ACCOUNT>.dkr.ecr.<REGION>.amazonaws.com/signalscout-backend:latest

# Build and push frontend
cd ../frontend
npm run build
docker build -t signalscout-frontend:latest .
docker tag signalscout-frontend:latest <AWS_ACCOUNT>.dkr.ecr.<REGION>.amazonaws.com/signalscout-frontend:latest
docker push <AWS_ACCOUNT>.dkr.ecr.<REGION>.amazonaws.com/signalscout-frontend:latest

# Create App Runner services via AWS Console or CLI
```

### Option 3: Docker Compose on VPS

```bash
# On your VPS
ssh user@your-vps

# Clone repo
git clone https://github.com/your-username/signalscout.git
cd signalscout

# Copy production .env
cp .env.prod .env

# Start services
docker-compose -f infra/docker-compose/docker-compose.yml up -d

# Set up reverse proxy (nginx)
# See nginx.conf example below
```

### Nginx Reverse Proxy Example

```nginx
upstream backend {
  server localhost:8080;
}

upstream frontend {
  server localhost:3000;
}

server {
  listen 443 ssl http2;
  server_name signalscout.yourdomain.com;

  ssl_certificate /path/to/cert.pem;
  ssl_certificate_key /path/to/key.pem;

  location /api {
    proxy_pass http://backend;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }

  location / {
    proxy_pass http://frontend;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
}

server {
  listen 80;
  server_name signalscout.yourdomain.com;
  return 301 https://$server_name$request_uri;
}
```

### Database Backup

```bash
# Backup Postgres
docker exec signalscout-postgres pg_dump -U scout signalscout > backup_$(date +%Y%m%d).sql

# Restore from backup
docker exec -i signalscout-postgres psql -U scout signalscout < backup_20260604.sql
```

### Monitoring

```bash
# Check backend logs
docker logs signalscout-backend -f

# Check database logs
docker logs signalscout-postgres -f

# Check Redis logs
docker logs signalscout-redis -f
```

### Health Checks

```bash
# Backend health
curl https://signalscout.yourdomain.com/api/health

# Database connectivity
docker exec signalscout-postgres pg_isready -U scout -d signalscout

# Redis connectivity
docker exec signalscout-redis redis-cli ping
```

---

## Scaling Considerations

### Phase 1 (MVP)
- Single backend container
- Single Postgres database
- Redis optional
- Simple reverse proxy (nginx or cloud loadbalancer)

### Phase 2+
- Horizontal scaling: multiple backend containers behind load balancer
- Database read replicas for reporting
- Redis for job queue and caching
- CDN for frontend static assets
- Separate worker service for long-running jobs

---

**Last Updated**: 2026-06-04
