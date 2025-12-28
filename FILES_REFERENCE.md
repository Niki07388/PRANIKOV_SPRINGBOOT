# 📋 Infrastructure Files Reference Guide

## File Structure Overview

```
PRANIKOV/
├── docker-compose.yml              ← Main orchestration file
├── nginx/
│   └── nginx.conf                  ← NGINX configuration
├── pranikov.service                ← Systemd service
├── install.sh                       ← Installation script
├── health-check.sh                 ← Health monitoring script
├── DEPLOYMENT_GUIDE.md             ← Complete setup guide
├── QUICK_START.md                  ← Quick reference (5 min)
├── NGINX_INFRASTRUCTURE_SUMMARY.md ← Architecture details
├── PRODUCTION_CHECKLIST.md         ← Launch verification
└── COMPLETE_SETUP_SUMMARY.md       ← This overview
```

---

## 📄 File Details & Purpose

### 1. **docker-compose.yml**
**Purpose:** Docker service orchestration  
**Size:** ~150 lines  
**Key Sections:**
- Services: nginx, backend-1, backend-2, frontend, postgres
- Volumes: postgres_data, nginx_logs
- Networks: pranikov-network

**When to Use:** Run `docker-compose up -d` to start all services  
**Configuration Needed:** Update credentials (POSTGRES_PASSWORD, etc.)  
**Important Settings:**
- Backend ports: 5000, 5001
- Frontend port: 8080 (internal), 4173 (Vite preview)
- Database port: 5432

**Common Commands:**
```bash
docker-compose up -d        # Start all services
docker-compose ps           # Check status
docker-compose logs -f      # View logs
docker-compose down         # Stop services
docker-compose down -v      # Stop and remove data
```

---

### 2. **nginx/nginx.conf**
**Purpose:** NGINX reverse proxy, load balancing, security configuration  
**Size:** ~260 lines  
**Key Features:**
- Load balancing with least_conn algorithm
- SSL/TLS termination (TLSv1.2, TLSv1.3)
- Rate limiting zones
- Security headers
- Gzip compression
- Health check endpoint

**When to Use:** Configure during deployment  
**Configuration Needed:**
- Update `server_name` with your domain
- Update SSL certificate paths (cert.pem, key.pem)
- Adjust rate limits if needed

**Key Sections:**
```nginx
# Upstream backends
upstream backend {
    least_conn;
    server backend-1:5000;
    server backend-2:5001;
}

# Rate limiting zones
limit_req_zone $binary_remote_addr zone=login_limit:10m rate=5r/m;
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=20r/s;

# SSL configuration
ssl_certificate /etc/nginx/ssl/cert.pem;
ssl_certificate_key /etc/nginx/ssl/key.pem;

# Security headers
add_header Strict-Transport-Security "max-age=31536000";
add_header Content-Security-Policy "default-src 'self'";
```

**Common Modifications:**
- Add more backends (add to upstream block)
- Change domain name (server_name directive)
- Adjust rate limits (limit_req_zone directives)
- Update SSL paths (ssl_certificate directives)

---

### 3. **DEPLOYMENT_GUIDE.md**
**Purpose:** Comprehensive deployment and operations guide  
**Length:** 500+ lines, 18 sections  
**Covers:**
1. Prerequisites
2. Docker installation
3. SSL setup (Let's Encrypt, self-signed)
4. Configuration updates
5. Build and deployment
6. Health checks
7. Load balancing
8. Monitoring and logging
9. Security best practices
10. Database backup/recovery
11. Troubleshooting
12. Performance tuning
13. Scaling
14. Prometheus/Grafana setup
15. Rollback procedures
16. Production checklist
17. Support and maintenance
18. Resources

**When to Use:** Reference for setup, troubleshooting, and operations  
**Key Sections to Review First:**
- Section 3: SSL Certificate Setup
- Section 4: Configuration Updates
- Section 5: Build & Deployment
- Section 6: Health Checks

**Perfect For:**
- First-time setup
- Troubleshooting issues
- Adding new backends
- Setting up monitoring
- Database operations

---

### 4. **QUICK_START.md**
**Purpose:** Quick reference for 5-minute deployment  
**Length:** ~200 lines  
**Contains:**
- Quick deployment steps
- Common commands
- SSL setup
- Troubleshooting tips
- API endpoints
- Security features

**When to Use:** Quick reference during deployment or operations  
**Ideal For:**
- Getting started quickly
- Experienced operators
- Quick troubleshooting
- API endpoint reference

**Quick Commands Reference:**
```bash
docker-compose logs -f nginx          # View NGINX logs
docker-compose logs -f backend-1      # View backend logs
docker-compose ps                     # Check status
docker stats                          # Resource usage
docker-compose down -v                # Full cleanup
```

---

### 5. **pranikov.service**
**Purpose:** Systemd service file for Linux/Ubuntu  
**Type:** System configuration file  
**Installed At:** `/etc/systemd/system/pranikov.service`

**Key Settings:**
```ini
Description=Pranikov Healthcare Platform - Docker Compose Services
Type=oneshot
WorkingDirectory=/opt/pranikov
ExecStart=/usr/local/bin/docker-compose up -d
ExecStop=/usr/local/bin/docker-compose down
```

**When to Use:** Production deployments on Linux/Ubuntu  
**Requires:** Root access for installation  
**Management Commands:**
```bash
systemctl start pranikov           # Start service
systemctl stop pranikov            # Stop service
systemctl restart pranikov         # Restart service
systemctl status pranikov          # Check status
systemctl enable pranikov          # Auto-start on boot
journalctl -fu pranikov            # View logs
```

**Installation:**
```bash
sudo cp pranikov.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable pranikov
```

---

### 6. **install.sh**
**Purpose:** Automated installation script  
**Type:** Bash script  
**For:** Ubuntu/Linux systems

**What It Does:**
1. Checks root privileges
2. Installs Docker (if needed)
3. Creates directory structure
4. Generates SSL certificates
5. Updates nginx.conf with domain
6. Installs systemd service

**When to Use:** First-time setup on Linux/Ubuntu  
**Usage:**
```bash
sudo ./install.sh your-domain.com
```

**Prerequisites:**
- Root or sudo access
- Ubuntu/Debian-based system
- Internet connection

**Output:**
- Creates `/opt/pranikov` directory
- Generates SSL certificates
- Installs systemd service
- Updates domain in nginx.conf

---

### 7. **health-check.sh**
**Purpose:** Comprehensive health monitoring script  
**Type:** Bash script  
**For:** Ongoing operations monitoring

**What It Checks:**
1. Docker container status
2. API endpoint responses
3. Database connectivity
4. Resource usage
5. Recent error logs
6. Service health

**When to Use:** Regular monitoring, after deployments, troubleshooting  
**Usage:**
```bash
./health-check.sh your-domain.com
./health-check.sh your-domain.com false  # HTTP (dev only)
```

**Output Shows:**
- ✓ Running containers
- ✓ API health status
- ✓ Database connectivity
- ✓ Resource usage
- ✓ Recent errors (if any)

**Automation:**
Add to crontab for periodic monitoring:
```bash
*/5 * * * * /path/to/health-check.sh your-domain.com > /var/log/pranikov-health.log 2>&1
```

---

### 8. **NGINX_INFRASTRUCTURE_SUMMARY.md**
**Purpose:** Architecture overview and implementation details  
**Length:** ~400 lines  
**Contains:**
- Files overview and purpose
- Architecture diagram
- Security implementation details
- Load balancing strategy
- Performance optimizations
- Deployment flow
- Next steps
- Verification checklist

**When to Use:** Understanding the overall architecture  
**Perfect For:**
- Understanding system design
- Explaining to team members
- Architecture decisions
- Performance tuning
- Scaling planning

**Key Diagrams:**
- System architecture
- Traffic flow
- Service dependencies
- Load balancing flow

---

### 9. **PRODUCTION_CHECKLIST.md**
**Purpose:** Complete production deployment verification  
**Length:** ~500 lines  
**Organized By Phase:**
1. Pre-Deployment (1 week before)
2. Deployment Day (Setup)
3. Security Hardening
4. Monitoring & Logging Setup
5. Backup & Disaster Recovery
6. Team & Documentation
7. Performance & Load Testing
8. Launch Readiness
9. Daily Operations
10. Incident Response
11. Sign-off section
12. Contact information

**When to Use:** Before going live in production  
**Important Sections:**
- Launch Readiness (final checks)
- Security Verification (security team sign-off)
- Verification Tests (API and database)
- Post-Launch monitoring

**Sign-Off Section:**
Provides space for team signatures confirming readiness

---

### 10. **COMPLETE_SETUP_SUMMARY.md**
**Purpose:** Overview of entire NGINX infrastructure setup  
**Length:** ~400 lines  
**Contains:**
- Quick deliverables summary
- Architecture overview
- Feature list
- Quick start instructions
- Security features
- Performance characteristics
- Scaling options
- Maintenance schedule
- Troubleshooting quick guide
- Pre-launch checklist
- Support resources

**When to Use:** Overview and getting started  
**Perfect For:**
- Executive summary
- Getting started quickly
- Feature overview
- Support resource reference

---

## 🎯 Which File To Use When?

### "How do I start services?"
→ Use **docker-compose.yml** with command: `docker-compose up -d`

### "I need to set up SSL certificates"
→ See **DEPLOYMENT_GUIDE.md** Section 3

### "What's wrong with my system?"
→ Run **health-check.sh** then check **DEPLOYMENT_GUIDE.md** troubleshooting

### "How do I add another backend server?"
→ See **DEPLOYMENT_GUIDE.md** Section 7 or **QUICK_START.md**

### "I need to change the domain"
→ Update **nginx/nginx.conf** (server_name directive)

### "How do I deploy to production?"
→ Use **PRODUCTION_CHECKLIST.md** step by step

### "I need quick commands"
→ See **QUICK_START.md** for common commands section

### "How is the system architected?"
→ Review **NGINX_INFRASTRUCTURE_SUMMARY.md**

### "Set up monitoring"
→ See **health-check.sh** and **DEPLOYMENT_GUIDE.md** Section 8

### "I'm deploying to Linux/Ubuntu"
→ Use **install.sh** script and **pranikov.service** file

---

## 📝 Configuration Reference

### Must Change Before Deployment

1. **Domain Name**
   - File: `nginx/nginx.conf`
   - Find: `server_name pranikov.example.com;`
   - Change to: `server_name your-actual-domain.com;`

2. **SSL Certificates**
   - Directory: `nginx/ssl/`
   - File: `cert.pem` and `key.pem`
   - Must provide before starting NGINX

3. **Database Password**
   - File: `docker-compose.yml`
   - Variable: `POSTGRES_PASSWORD`
   - Must change from default value

4. **JWT Secret**
   - File: `demo/src/main/resources/application.properties`
   - Variable: `jwt.secret`
   - Must set to long random string

---

## 🔐 Security Checklist

- [ ] SSL certificates installed
- [ ] Domain updated in nginx.conf
- [ ] Database passwords changed
- [ ] JWT secret configured
- [ ] Firewall rules set (allow 80, 443 only)
- [ ] Security headers verified
- [ ] Rate limiting tested
- [ ] Access logs configured

---

## 🆘 Troubleshooting Path

1. **Problem?** Run: `./health-check.sh your-domain.com`
2. **Service issue?** Check: `docker-compose ps`
3. **Error in logs?** View: `docker-compose logs -f [service-name]`
4. **Configuration issue?** Review: **DEPLOYMENT_GUIDE.md** troubleshooting
5. **Still stuck?** Check specific guide file in the "Covers" section above

---

## 📊 Quick File Stats

| File | Lines | Purpose | Frequency |
|------|-------|---------|-----------|
| docker-compose.yml | 150 | Service orchestration | Every deployment |
| nginx.conf | 260 | Reverse proxy config | Setup + changes |
| DEPLOYMENT_GUIDE.md | 500+ | Complete guide | Reference |
| QUICK_START.md | 200 | Quick reference | Frequent |
| PRODUCTION_CHECKLIST.md | 500+ | Launch verification | Pre-production |
| COMPLETE_SETUP_SUMMARY.md | 400 | Overview | Onboarding |
| NGINX_INFRASTRUCTURE_SUMMARY.md | 400 | Architecture | Reference |
| pranikov.service | 15 | Systemd service | Setup once |
| install.sh | 100 | Installation | Setup once |
| health-check.sh | 150 | Health monitor | Ongoing |

---

## 🎓 Recommended Reading Order

1. **First:** COMPLETE_SETUP_SUMMARY.md (10 min overview)
2. **Then:** QUICK_START.md (get up and running)
3. **Then:** DEPLOYMENT_GUIDE.md (detailed reference)
4. **Then:** NGINX_INFRASTRUCTURE_SUMMARY.md (understand architecture)
5. **Finally:** PRODUCTION_CHECKLIST.md (pre-launch verification)

---

## 📞 Quick Help

- **"Why won't NGINX start?"** → `docker-compose exec nginx nginx -t`
- **"How do I see logs?"** → `docker-compose logs -f [service]`
- **"How do I backup database?"** → See DEPLOYMENT_GUIDE.md Section 10
- **"How do I add a backend?"** → See DEPLOYMENT_GUIDE.md Section 7
- **"Is my system healthy?"** → Run `./health-check.sh your-domain`

---

**Version:** 1.0  
**Last Updated:** 2024  
**Status:** Complete and Ready for Production  
**Total Documentation:** 3,000+ lines across 10 files
