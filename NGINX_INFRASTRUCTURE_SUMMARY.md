# NGINX Infrastructure Setup - Complete Summary

## 📦 Files Created/Updated

### 1. **docker-compose.yml** (CREATED)
**Purpose:** Orchestrate all services (backend, frontend, database, NGINX)

**Services:**
- `backend-1`: Spring Boot on port 5000
- `backend-2`: Spring Boot on port 5001 (load balancing)
- `frontend`: React/Vite on port 8080 (internal), 4173 (preview)
- `postgres`: PostgreSQL database on port 5432
- `nginx`: NGINX reverse proxy on ports 80 & 443

**Features:**
- Automatic service startup and restart
- Health checks for all services
- Shared Docker network (pranikov-network)
- Volume persistence for database

### 2. **nginx/nginx.conf** (UPDATED)
**Purpose:** NGINX configuration for reverse proxy, load balancing, and security

**Key Configurations:**

**HTTP Block:**
- Worker processes: auto (uses all CPU cores)
- Worker connections: 2048
- Gzip compression enabled
- Rate limiting zones:
  - login_limit: 5 requests/minute
  - api_limit: 20 requests/second

**Upstream Blocks:**
```nginx
upstream backend {
    least_conn;  # Uses least connections algorithm
    server backend-1:5000 weight=1 max_fails=3 fail_timeout=30s;
    server backend-2:5001 weight=1 max_fails=3 fail_timeout=30s;
}
```

**Server Blocks:**
1. **HTTP (port 80):** Redirects to HTTPS, allows Let's Encrypt ACME challenges
2. **HTTPS (port 443):** Main server with SSL/TLS, security headers, and proxying

**Location Blocks:**
- `/api/` → backend (with rate limiting, 20 req/s)
- `/api/auth/(login|register)` → backend (rate limited to 5 req/min)
- `/api/ws/` → backend (WebSocket support)
- `/` → frontend (with SPA routing fallback)
- `/nginx_status` → NGINX stats (admin only, localhost)
- `/health` → health check endpoint

**Security Headers:**
- HSTS (HTTP Strict Transport Security): 1 year
- Content-Security-Policy: Default src 'self'
- X-Frame-Options: SAMEORIGIN
- X-Content-Type-Options: nosniff
- X-XSS-Protection: 1; mode=block

**SSL/TLS:**
- Protocols: TLSv1.2, TLSv1.3
- Certificate: `/etc/nginx/ssl/cert.pem`
- Private Key: `/etc/nginx/ssl/key.pem`
- Session cache: 10MB, timeout 10 minutes

### 3. **DEPLOYMENT_GUIDE.md** (CREATED)
**Purpose:** Comprehensive production deployment guide (18 sections)

**Covers:**
1. Prerequisites and system requirements
2. Docker & Docker Compose installation
3. SSL/TLS certificate setup (self-signed and Let's Encrypt)
4. Configuration updates (domain, credentials, backend)
5. Build & deployment procedures
6. Health checks and verification
7. Load balancing configuration
8. Monitoring and logging
9. Security best practices
10. Database backup & recovery
11. Troubleshooting guide
12. Performance tuning
13. Scaling & high availability
14. Monitoring with Prometheus/Grafana
15. Rollback & disaster recovery
16. Production checklist
17. Maintenance tasks
18. Additional resources

### 4. **QUICK_START.md** (CREATED)
**Purpose:** Quick reference for fast deployment (5 minutes)

**Quick Steps:**
1. Prepare files and directories
2. Configure domain in nginx.conf
3. Update credentials in docker-compose.yml
4. Build backend with Maven
5. Start services with docker-compose
6. Verify deployment

**Quick Commands:** Common docker-compose operations, SSL setup, troubleshooting

### 5. **pranikov.service** (CREATED)
**Purpose:** Systemd service file for Linux/Ubuntu

**Features:**
- Auto-start on system boot
- Requires Docker service
- Uses docker-compose for orchestration
- Logs to journal (journalctl)
- Starts: `systemctl start pranikov`
- Stop: `systemctl stop pranikov`
- Status: `systemctl status pranikov`

### 6. **install.sh** (CREATED)
**Purpose:** Automated installation script for production

**Steps:**
1. Checks root privileges
2. Installs Docker if needed
3. Creates directory structure
4. Generates SSL certificates (self-signed)
5. Updates NGINX configuration with domain
6. Installs systemd service

**Usage:** `sudo ./install.sh pranikov.example.com`

### 7. **health-check.sh** (CREATED)
**Purpose:** Comprehensive health monitoring script

**Checks:**
- Docker container status
- API endpoint responses
- Database connectivity
- Resource usage
- Recent error logs
- Container uptime

**Usage:** `./health-check.sh your-domain.com [true|false]`

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        Client Browser                        │
│                  (Internet / External User)                  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ HTTPS (443)
                     │
┌────────────────────▼────────────────────────────────────────┐
│                   NGINX Reverse Proxy                        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ • SSL/TLS Termination (TLSv1.2, TLSv1.3)             │   │
│  │ • Security Headers (HSTS, CSP, X-Frame-Options)      │   │
│  │ • Rate Limiting (Login: 5/min, API: 20/sec)          │   │
│  │ • Gzip Compression                                    │   │
│  │ • Static Asset Caching (1 year)                       │   │
│  │ • SPA Routing (404 → /index.html)                     │   │
│  │ • Health Checks (/health endpoint)                    │   │
│  └──────────────────────────────────────────────────────┘   │
└────┬──────────────────────────┬──────────────────────┬────┘
     │ Port 443                 │                      │
     │                          │                      │
  /api/*                   /api/ws/*              /*
     │                          │                      │
┌────▼────────────────┐  ┌──────▼──────┐   ┌──────────▼────┐
│ Load Balancer       │  │  WebSocket  │   │   Frontend    │
│ (Round-robin /     │  │   Proxy     │   │   React/Vite  │
│  Least Conn)        │  │             │   │   Port: 8080  │
└────┬───────────────┘  └──────┬──────┘   └───────────────┘
     │                         │
  ┌──┴───────┐           ┌─────▼────────┐
  │           │           │              │
┌─▼──────┐ ┌─▼──────┐  ┌─▼──────┐
│Backend │ │Backend │  │WebSocket│
│Spring  │ │Spring  │  │Handler  │
│Port:   │ │Port:   │  │(future) │
│5000    │ │5001    │  │         │
└──┬──────┘ └──┬──────┘  └─────────┘
   │           │
   └───┬───┬───┘
       │   │
    ┌──▼───▼─────┐
    │ PostgreSQL  │
    │ Database    │
    │ Port: 5432  │
    └─────────────┘
```

---

## 🔐 Security Implementation

### 1. **Transport Security**
- SSL/TLS v1.2 & v1.3
- Strong cipher suites
- HSTS (Strict-Transport-Security)
- Forward secrecy enabled

### 2. **Application Security**
- Security headers (CSP, X-Frame-Options, X-XSS-Protection)
- Rate limiting on sensitive endpoints
- Denies hidden files (/.*)
- Blocks access to environment files

### 3. **Network Security**
- Firewall rules (allow only 80, 443)
- Docker network isolation
- Health check monitoring
- Automatic restart on failure

### 4. **Data Protection**
- Database connection pooling
- Prepared statements (SQL injection prevention)
- Encrypted JWT tokens
- Secure password hashing

---

## 📊 Load Balancing Strategy

**Algorithm:** Least connections (least_conn)
- Distributes requests to server with fewest active connections
- Better than round-robin for varying request sizes

**Health Checks:**
- Interval: 30 seconds
- Max failures: 3
- Fail timeout: 30 seconds (temporary removal from pool)
- Auto-recovery: Automatic when backend recovers

**Scaling:**
- Add new backend service in docker-compose.yml
- Add to upstream block in nginx.conf
- Run: `docker-compose up -d`
- No downtime required

---

## 📈 Performance Optimizations

### NGINX Optimizations
- **Worker Processes:** Auto (matches CPU cores)
- **Keepalive Connections:** 32 concurrent connections per upstream
- **Connection Timeout:** 65 seconds
- **Gzip Compression:** 6 level (CPU/bandwidth trade-off)

### Backend Optimizations
- **JVM Heap:** 512MB (configurable)
- **Database Connection Pool:** 20 max, 5 min idle
- **Batch Size:** 20 for inserts/updates

### Frontend Optimizations
- **Build Minification:** Terser enabled
- **Code Splitting:** Vendor + UI chunking
- **Asset Caching:** 1 year for versioned files

---

## 🔄 Deployment Flow

### Development
```
1. Edit files locally
2. Run: docker-compose up -d
3. Test on http://localhost
```

### Production
```
1. Obtain SSL certificates (Let's Encrypt)
2. Update domain in nginx.conf
3. Update credentials in .env file
4. Build backend: ./mvnw clean package
5. Run: docker-compose up -d
6. Verify: ./health-check.sh your-domain.com
7. Point DNS to server IP
8. Test: https://your-domain.com
```

---

## 🚀 Next Steps

1. **SSL Certificates**
   - Self-signed for development: See DEPLOYMENT_GUIDE.md Section 3
   - Let's Encrypt for production: Follow certbot instructions

2. **Domain Configuration**
   - Update nginx.conf: `server_name your-domain.com;`
   - Point DNS A record to server IP

3. **Database Backup**
   - Create automated backup script
   - Test restoration procedures
   - Store backups securely

4. **Monitoring**
   - Set up log aggregation (ELK stack)
   - Configure alerts (Prometheus + Alertmanager)
   - Monitor resource usage (Grafana)

5. **Auto-scaling**
   - Use Kubernetes for advanced scaling (future)
   - Or manually add backend nodes as needed

---

## 📚 Documentation Files

| File | Purpose | Status |
|------|---------|--------|
| `docker-compose.yml` | Service orchestration | ✅ Created |
| `nginx/nginx.conf` | Reverse proxy & security | ✅ Updated |
| `DEPLOYMENT_GUIDE.md` | Comprehensive guide | ✅ Created |
| `QUICK_START.md` | 5-minute setup guide | ✅ Created |
| `pranikov.service` | Systemd service | ✅ Created |
| `install.sh` | Installation script | ✅ Created |
| `health-check.sh` | Monitoring script | ✅ Created |

---

## ✅ Verification Checklist

Before going live:

- [ ] SSL certificates installed in `nginx/ssl/`
- [ ] Domain updated in `nginx/nginx.conf`
- [ ] Database credentials updated in `.env` file
- [ ] Backend built: `./mvnw clean package`
- [ ] Services running: `docker-compose ps`
- [ ] Health checks passing: `./health-check.sh`
- [ ] Frontend accessible: https://your-domain.com
- [ ] API responding: https://your-domain.com/api/health
- [ ] Database connected: Test with API call
- [ ] Rate limiting working: Rapid login attempts rejected
- [ ] Load balancing working: Both backends receiving traffic
- [ ] Logs accessible: `docker-compose logs`
- [ ] Backup configured: Database backup script ready
- [ ] Monitoring enabled: Health checks running

---

## 🔧 Maintenance Commands

```bash
# View all logs
docker-compose logs -f

# Restart services
docker-compose restart

# Stop services
docker-compose down

# Update services (pull latest images)
docker-compose pull && docker-compose up -d

# Database backup
docker-compose exec postgres pg_dump -U uphill_user pranikov_uphill > backup.sql

# Database restore
docker-compose exec -T postgres psql -U uphill_user pranikov_uphill < backup.sql

# Check NGINX status
docker exec pranikov-nginx nginx -s status

# View NGINX access logs
docker-compose exec nginx tail -f /var/log/nginx/access.log

# Scale backend (add more instances)
# Edit docker-compose.yml, then: docker-compose up -d
```

---

**Version:** 1.0
**Last Updated:** 2024
**Status:** ✅ Complete and Ready for Deployment
