# Pranikov Healthcare - Production Deployment Guide

## Overview
This guide provides step-by-step instructions for deploying the Pranikov Healthcare Platform using Docker Compose with NGINX reverse proxy, load balancing, and security features.

---

## 1. Prerequisites

### System Requirements
- Docker (v20.10+)
- Docker Compose (v2.0+)
- 4GB RAM minimum
- 20GB disk space
- Ubuntu/Linux or Windows with WSL2

### Before Starting
- Update your domain name in `nginx.conf` (replace `pranikov.example.com`)
- Prepare SSL certificates (or generate self-signed)
- Configure database credentials securely

---

## 2. Environment Setup

### 2.1 Install Docker & Docker Compose

**On Ubuntu/Linux:**
```bash
# Install Docker
sudo apt-get update
sudo apt-get install -y docker.io docker-compose

# Start Docker daemon
sudo systemctl start docker
sudo systemctl enable docker

# Add current user to docker group
sudo usermod -aG docker $USER
```

**On Windows:**
- Download [Docker Desktop](https://www.docker.com/products/docker-desktop)
- Enable WSL2 backend during installation

### 2.2 Create Required Directories

```bash
# Create SSL directory
mkdir -p nginx/ssl

# Create logs directory
mkdir -p logs/nginx
mkdir -p logs/backend
```

---

## 3. SSL/TLS Certificate Setup

### Option A: Self-Signed Certificate (Development)

```bash
cd nginx/ssl

# Generate private key
openssl genrsa -out key.pem 2048

# Generate certificate
openssl req -new -x509 -key key.pem -out cert.pem -days 365 \
  -subj "/C=IN/ST=State/L=City/O=Pranikov/CN=pranikov.example.com"

# Verify certificates
ls -la
```

### Option B: Let's Encrypt Certificate (Production)

```bash
# Install Certbot
sudo apt-get install -y certbot python3-certbot-nginx

# Generate certificate
sudo certbot certonly --standalone \
  -d pranikov.example.com \
  -d www.pranikov.example.com

# Copy to nginx/ssl (requires sudo)
sudo cp /etc/letsencrypt/live/pranikov.example.com/fullchain.pem nginx/ssl/cert.pem
sudo cp /etc/letsencrypt/live/pranikov.example.com/privkey.pem nginx/ssl/key.pem
sudo chown $USER:$USER nginx/ssl/cert.pem nginx/ssl/key.pem
```

### Certificate Renewal (Let's Encrypt)
```bash
# Add to crontab for automatic renewal
# Run monthly to renew 30 days before expiration
0 0 1 * * sudo certbot renew --quiet && sudo cp /etc/letsencrypt/live/pranikov.example.com/fullchain.pem nginx/ssl/cert.pem
```

---

## 4. Configuration Updates

### 4.1 Update Domain in NGINX Config

```bash
# Edit nginx/nginx.conf
sed -i 's/pranikov.example.com/your-actual-domain.com/g' nginx/nginx.conf
```

### 4.2 Update Database Credentials

**Option 1: Update in docker-compose.yml**
```yaml
environment:
  - POSTGRES_PASSWORD=your_secure_password_here
  - SPRING_DATASOURCE_PASSWORD=your_secure_password_here
```

**Option 2: Use .env file (Recommended)**
```bash
# Create .env file
cat > .env << EOF
DB_USER=uphill_user
DB_PASSWORD=your_secure_password_here
DB_NAME=pranikov_uphill
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/pranikov_uphill
EOF

# Update docker-compose.yml to use variables
env_file: .env
```

### 4.3 Update Backend Configuration

Edit `demo/src/main/resources/application.properties`:
```properties
# Database (if changed)
spring.datasource.url=jdbc:postgresql://postgres:5432/pranikov_uphill
spring.datasource.username=uphill_user
spring.datasource.password=your_password

# Payment Settings
app.payment.upi-id=your-upi-id@bank
app.payment.upi-name=Pranikov Healthcare

# Security
jwt.secret=your_very_long_secure_random_key_here
jwt.expiration=86400000  # 24 hours in milliseconds

# Server
server.port=5000
```

---

## 5. Build & Deployment

### 5.1 Build Backend JAR

```bash
# Navigate to backend directory
cd demo

# Clean and build with Maven
./mvnw.cmd clean package -DskipTests

# Verify JAR was created
ls -la target/demo-0.0.1-SNAPSHOT.jar

# Copy to Docker working directory (if needed)
cd ..
```

### 5.2 Build Frontend

```bash
# Navigate to frontend directory
cd FRONTEND

# Install dependencies
npm install
# or
bun install

# Build production bundle
npm run build
# This creates 'dist' directory for production

cd ..
```

### 5.3 Start Services with Docker Compose

```bash
# Start all services in background
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f nginx
docker-compose logs -f backend-1
docker-compose logs -f postgres

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

---

## 6. Health Checks & Verification

### 6.1 Verify Services are Running

```bash
# List running containers
docker-compose ps

# Expected output:
# CONTAINER          STATUS
# pranikov-nginx     Up (healthy)
# pranikov-backend-1 Up (healthy)
# pranikov-backend-2 Up (healthy)
# pranikov-frontend  Up
# pranikov-postgres  Up (healthy)
```

### 6.2 Test NGINX Health

```bash
# Test health endpoint
curl http://localhost/health

# Response should be: healthy

# Check NGINX status (from within Docker network)
curl http://localhost/nginx_status
```

### 6.3 Test API Endpoints

```bash
# Health check backend
curl -k https://pranikov.example.com/api/health

# Login test
curl -X POST https://pranikov.example.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@pranikov.com","password":"admin123"}'

# Should return JWT token in response
```

### 6.4 Test Frontend Access

```bash
# Open in browser
https://pranikov.example.com

# Should load the React application
```

---

## 7. Load Balancing Configuration

### Current Setup
- **Backend Load Balancing:** 2 instances (backend-1:5000, backend-2:5001)
- **Load Balancing Algorithm:** Least connections
- **Health Check:** 30-second intervals, 3 retries

### Add More Backend Instances

1. **Update docker-compose.yml:**
```yaml
backend-3:
  container_name: pranikov-backend-3
  image: openjdk:17-slim
  command: java -jar demo-0.0.1-SNAPSHOT.jar
  environment:
    - SERVER_PORT=5002
    - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/pranikov_uphill
  ports:
    - "5002:5002"
  networks:
    - pranikov-network
  depends_on:
    - postgres
```

2. **Update nginx/nginx.conf upstream:**
```nginx
upstream backend {
    least_conn;
    server backend-1:5000 weight=1 max_fails=3 fail_timeout=30s;
    server backend-2:5001 weight=1 max_fails=3 fail_timeout=30s;
    server backend-3:5002 weight=1 max_fails=3 fail_timeout=30s;
    keepalive 32;
}
```

3. **Restart services:**
```bash
docker-compose up -d
```

---

## 8. Monitoring & Logging

### 8.1 View Logs

```bash
# NGINX access logs
docker-compose logs nginx | grep "GET\|POST\|DELETE\|PUT"

# Backend application logs
docker-compose logs backend-1

# Database logs
docker-compose logs postgres

# Real-time monitoring
docker-compose logs -f --tail=100
```

### 8.2 Monitor Container Resources

```bash
# Real-time resource usage
docker stats

# Specific container stats
docker stats pranikov-backend-1 pranikov-nginx
```

### 8.3 Check NGINX Status

```bash
# Access NGINX status page (from container network)
docker exec pranikov-nginx curl localhost/nginx_status

# Shows:
# - Active connections
# - Accepted connections
# - Handled connections
# - Requests served
```

---

## 9. Security Best Practices

### 9.1 Implemented Security Features

✅ **SSL/TLS Encryption**
- TLSv1.2 and TLSv1.3 only
- Strong cipher suites (HIGH:!aNULL:!MD5)

✅ **Security Headers**
- HSTS (HTTP Strict Transport Security)
- X-Frame-Options: SAMEORIGIN
- X-Content-Type-Options: nosniff
- Content-Security-Policy
- CSP prevents inline script injection

✅ **Rate Limiting**
- Login attempts: 5 requests/minute
- API requests: 20 requests/second
- Prevents brute force and DDoS

✅ **Access Control**
- Denies access to hidden files (/.*)
- Blocks access to environment files
- Restricts NGINX status to localhost

### 9.2 Additional Security Hardening

```bash
# Disable server tokens (hide NGINX version)
# Add to nginx.conf http block:
server_tokens off;

# Enable ModSecurity (optional WAF)
# Install ModSecurity module for NGINX

# Regular security updates
docker pull nginx:alpine
docker pull openjdk:17-slim
docker pull postgres:15-alpine
docker-compose up -d
```

---

## 10. Database Backup & Recovery

### 10.1 Backup PostgreSQL

```bash
# Backup database
docker-compose exec postgres pg_dump -U uphill_user pranikov_uphill > backup.sql

# Backup with timestamp
docker-compose exec postgres pg_dump -U uphill_user pranikov_uphill > backup_$(date +%Y%m%d_%H%M%S).sql
```

### 10.2 Restore Database

```bash
# Restore from backup
docker-compose exec -T postgres psql -U uphill_user pranikov_uphill < backup.sql

# Create backup volume (for automatic backups)
docker run -v pranikov-network_postgres_data:/data \
  -v $(pwd):/backup \
  postgres:15-alpine \
  sh -c 'pg_dump -U uphill_user pranikov_uphill > /backup/auto_backup.sql'
```

### 10.3 Automated Backup Schedule

```bash
# Create backup script
cat > backup.sh << 'EOF'
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="./backups"
mkdir -p $BACKUP_DIR
docker-compose exec -T postgres pg_dump -U uphill_user pranikov_uphill > $BACKUP_DIR/backup_$DATE.sql
echo "Backup created: backup_$DATE.sql"
EOF

chmod +x backup.sh

# Add to crontab (daily at 2 AM)
# 0 2 * * * /path/to/backup.sh
```

---

## 11. Troubleshooting

### Issue: NGINX Not Starting
```bash
# Check NGINX config syntax
docker-compose exec nginx nginx -t

# View NGINX logs
docker-compose logs nginx

# Rebuild NGINX container
docker-compose up -d --force-recreate nginx
```

### Issue: Backend Can't Connect to Database
```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Test connection
docker-compose exec postgres psql -U uphill_user -c "SELECT 1"

# View backend logs
docker-compose logs backend-1
```

### Issue: SSL Certificate Errors
```bash
# Verify certificate files exist
ls -la nginx/ssl/

# Check certificate validity
openssl x509 -in nginx/ssl/cert.pem -text -noout

# Check certificate matches key
openssl x509 -noout -modulus -in nginx/ssl/cert.pem | openssl md5
openssl rsa -noout -modulus -in nginx/ssl/key.pem | openssl md5
# Both should return same hash
```

### Issue: High Memory Usage
```bash
# Monitor container resources
docker stats

# Reduce JVM heap size in docker-compose.yml
JAVA_OPTS=-Xmx256m -Xms128m  # From 512m to 256m
```

---

## 12. Performance Tuning

### 12.1 NGINX Optimization

```nginx
# In nginx.conf http block:
worker_processes auto;              # Use all CPU cores
worker_connections 2048;           # Increase from default 1024
use epoll;                         # Linux event model
multi_accept on;                   # Accept multiple connections
```

### 12.2 Backend Optimization

```properties
# In application.properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

### 12.3 Frontend Optimization

```bash
# In FRONTEND/vite.config.ts
export default {
  build: {
    minify: 'terser',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor': ['react', 'react-dom'],
          'ui': ['@/components/ui']
        }
      }
    }
  }
}
```

---

## 13. Scaling & High Availability

### 13.1 Add More Backend Nodes

Already configured in docker-compose.yml. To add:
1. Update docker-compose.yml with new backend-N service
2. Update nginx.conf upstream block
3. Run `docker-compose up -d`

### 13.2 Database Replication (Advanced)

For production high availability, set up PostgreSQL replication:
```bash
# Use managed PostgreSQL (AWS RDS, Azure Database, etc.)
# Or configure streaming replication with standby servers
```

### 13.3 CDN for Static Assets

```nginx
# In NGINX config, redirect static assets to CDN:
location ~* ^/(js|css|images|fonts)/ {
    return 301 https://cdn.example.com/$request_uri;
}
```

---

## 14. Monitoring & Alerts

### 14.1 Using Prometheus + Grafana (Optional)

```bash
# Add to docker-compose.yml:
prometheus:
  image: prom/prometheus
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml
  ports:
    - "9090:9090"

grafana:
  image: grafana/grafana
  ports:
    - "3000:3000"
  environment:
    - GF_SECURITY_ADMIN_PASSWORD=admin
```

### 14.2 Email Alerts

```bash
# Configure alerting in your monitoring tool
# Send alerts for:
# - Service down (HTTP 5xx errors > 10%)
# - High response time (>2s)
# - Database connection pool exhausted
# - Disk space low
```

---

## 15. Rollback & Disaster Recovery

### 15.1 Rollback to Previous Version

```bash
# Pull specific image tag
docker pull openjdk:17-slim

# Create backup of current database
docker-compose exec postgres pg_dump -U uphill_user pranikov_uphill > backup_before_rollback.sql

# Stop services
docker-compose down

# Restore old JAR version
# Build from git previous version or restore from artifact repository

# Start services
docker-compose up -d
```

### 15.2 Disaster Recovery Plan

1. **Weekly full database backups** (automated)
2. **Monthly disaster recovery test**
3. **Document all credentials** (in secure vault)
4. **Maintain infrastructure-as-code** (git repository)
5. **Test restore procedures regularly**

---

## 16. Production Checklist

- [ ] Domain registered and DNS configured
- [ ] SSL certificate obtained and installed
- [ ] Database credentials changed from defaults
- [ ] JWT secret updated to random value
- [ ] Environment variables configured
- [ ] Backup strategy implemented
- [ ] Monitoring and logging configured
- [ ] Security headers verified
- [ ] Rate limiting tested
- [ ] Load balancing tested with multiple backends
- [ ] SSL certificate auto-renewal configured
- [ ] Disaster recovery plan documented
- [ ] Team trained on operations procedures
- [ ] Health checks verified for all services
- [ ] Firewall rules configured (only allow 80, 443)

---

## 17. Support & Maintenance

### Regular Maintenance Tasks

**Daily:**
- Monitor error logs for critical issues
- Check disk space usage

**Weekly:**
- Review access logs for suspicious patterns
- Verify all health checks passing
- Test backup restoration

**Monthly:**
- Update container images to latest patches
- Review and optimize slow queries
- Audit security settings

**Quarterly:**
- Full disaster recovery test
- Capacity planning review
- Security audit

---

## 18. Additional Resources

- NGINX Documentation: https://nginx.org/en/docs/
- Docker Documentation: https://docs.docker.com/
- Spring Boot Production Readiness: https://spring.io/guides/gs/spring-boot/
- PostgreSQL Documentation: https://www.postgresql.org/docs/
- Let's Encrypt: https://letsencrypt.org/

---

**Last Updated:** 2024
**Version:** 1.0
**Maintained By:** Pranikov Engineering Team
