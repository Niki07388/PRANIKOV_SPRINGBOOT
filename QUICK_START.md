# Pranikov Healthcare Platform - Quick Start Guide

## 🚀 Quick Deployment (5 minutes)

### Prerequisites
- Docker installed
- Docker Compose v2+
- SSL certificates ready (or use self-signed)

### Step 1: Prepare Files
```bash
# Ensure these directories exist
mkdir -p nginx/ssl
mkdir -p logs

# Copy SSL certificates
cp your-cert.pem nginx/ssl/cert.pem
cp your-key.pem nginx/ssl/key.pem
```

### Step 2: Configure Domain
```bash
# Update nginx/nginx.conf
sed -i 's/pranikov.example.com/your-domain.com/g' nginx/nginx.conf
```

### Step 3: Update Credentials
Edit `docker-compose.yml`:
```yaml
environment:
  - POSTGRES_PASSWORD=your_secure_password
  - SPRING_DATASOURCE_PASSWORD=your_secure_password
```

### Step 4: Build Backend
```bash
cd demo
./mvnw.cmd clean package -DskipTests
cd ..
```

### Step 5: Start Services
```bash
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

### Step 6: Verify Deployment
```bash
# Test health
curl http://localhost/health

# Test HTTPS
curl -k https://your-domain.com/health

# Test API
curl -k https://your-domain.com/api/health
```

## 📋 Common Commands

```bash
# View all logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f nginx
docker-compose logs -f backend-1

# Stop services
docker-compose down

# Stop and remove data
docker-compose down -v

# Rebuild services
docker-compose up -d --force-recreate

# Check container stats
docker stats
```

## 🔐 SSL Certificates

### Self-Signed (Development)
```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/key.pem \
  -out nginx/ssl/cert.pem \
  -subj "/C=IN/ST=State/L=City/O=Pranikov/CN=pranikov.example.com"
```

### Let's Encrypt (Production)
```bash
sudo certbot certonly --standalone -d your-domain.com
sudo cp /etc/letsencrypt/live/your-domain.com/fullchain.pem nginx/ssl/cert.pem
sudo cp /etc/letsencrypt/live/your-domain.com/privkey.pem nginx/ssl/key.pem
```

## ⚠️ Troubleshooting

**NGINX not starting?**
```bash
docker-compose exec nginx nginx -t
docker-compose logs nginx
```

**Backend won't start?**
```bash
docker-compose logs backend-1
# Check: Java installed, JAR file exists, ports not in use
```

**Database connection error?**
```bash
docker-compose exec postgres psql -U uphill_user -c "SELECT 1"
# Check: Database running, credentials correct, network connectivity
```

**Can't access frontend?**
```bash
# Check if frontend is running
docker-compose ps frontend

# View frontend logs
docker-compose logs frontend
```

## 🔄 Load Balancing

Currently configured with 2 backend nodes for load balancing:
- backend-1: port 5000
- backend-2: port 5001

Add more nodes by:
1. Adding backend-3 service in docker-compose.yml
2. Adding to upstream block in nginx.conf
3. Run: `docker-compose up -d`

## 📊 Monitoring

### View Container Status
```bash
docker-compose ps
```

### View Resource Usage
```bash
docker stats pranikov-nginx pranikov-backend-1 pranikov-postgres
```

### NGINX Status
```bash
# From container network
docker exec pranikov-nginx curl localhost/nginx_status
```

## 💾 Backup & Restore

### Backup Database
```bash
docker-compose exec postgres pg_dump -U uphill_user pranikov_uphill > backup.sql
```

### Restore Database
```bash
docker-compose exec -T postgres psql -U uphill_user pranikov_uphill < backup.sql
```

## 🔑 Default Credentials

Update these in production:
- **Admin Email:** admin@pranikov.com
- **Admin Password:** Change in database
- **Database User:** uphill_user
- **Database Name:** pranikov_uphill

## 📱 API Endpoints

**Available at:** https://your-domain.com/api/

Key endpoints:
- `POST /auth/login` - User login
- `POST /auth/register` - User registration
- `GET /pharmacy/products` - Get products
- `POST /pharmacy/orders` - Create order
- `GET /orders` - Get user orders
- `GET /admin/orders` - Get all orders (admin only)

## 🛡️ Security Features

✅ SSL/TLS encryption (TLSv1.2 & 1.3)
✅ Rate limiting (5 req/min for login, 20 req/s for API)
✅ Security headers (HSTS, CSP, X-Frame-Options, etc.)
✅ CORS protection
✅ SQL injection prevention (prepared statements)
✅ XSS protection (Content-Security-Policy)

## 📖 Full Documentation

See [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) for:
- Detailed configuration
- SSL setup (Let's Encrypt)
- Database backup strategies
- Performance tuning
- Scaling & high availability
- Monitoring & alerts
- Troubleshooting guide

## 🆘 Support

For issues, check:
1. `docker-compose logs` - Service logs
2. `docker-compose ps` - Container status
3. [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) - Troubleshooting section
4. NGINX error logs: `logs/nginx/error.log`

---

**Platform:** Pranikov Healthcare
**Version:** 1.0
**Last Updated:** 2024
