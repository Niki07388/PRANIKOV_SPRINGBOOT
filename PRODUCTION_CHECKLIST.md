# Production Deployment Checklist

## ✅ Pre-Deployment (1 week before)

### Infrastructure Planning
- [ ] Define target deployment environment (cloud provider, VPS, on-premises)
- [ ] Select appropriate server size (CPU, RAM, storage)
- [ ] Plan for high availability and disaster recovery
- [ ] Document network architecture and firewall rules
- [ ] Reserve static IP address for server

### Domain & DNS
- [ ] Register domain name
- [ ] Configure DNS A/AAAA records pointing to server IP
- [ ] Test DNS resolution (nslookup, dig)
- [ ] Set up domain renewal auto-payment

### SSL/TLS Certificates
- [ ] Decide certificate provider (Let's Encrypt, commercial CA)
- [ ] Obtain SSL certificates
- [ ] Store certificates securely
- [ ] Note certificate expiration date
- [ ] Set up auto-renewal (if applicable)
- [ ] Test certificate installation

### Security Planning
- [ ] Define access control policies
- [ ] Plan firewall rules (allow only 80, 443)
- [ ] Create backup and disaster recovery plan
- [ ] Document emergency procedures
- [ ] Plan security monitoring and alerting

---

## 🔧 Deployment Day (Setup)

### Infrastructure Setup
- [ ] Launch server/VM with Ubuntu 22.04 LTS (or equivalent)
- [ ] Update system packages: `apt-get update && apt-get upgrade`
- [ ] Install Docker: `apt-get install -y docker.io docker-compose`
- [ ] Start Docker service: `systemctl start docker`
- [ ] Enable Docker on boot: `systemctl enable docker`
- [ ] Add user to docker group: `usermod -aG docker $USER`

### Application Deployment
- [ ] Clone/copy application files to `/opt/pranikov`
- [ ] Set correct permissions: `chmod -R 755 /opt/pranikov`
- [ ] Create SSL directory: `mkdir -p /opt/pranikov/nginx/ssl`
- [ ] Copy SSL certificates to `nginx/ssl/`
- [ ] Update `nginx.conf` with actual domain name
- [ ] Update `docker-compose.yml` with production credentials
- [ ] Update `.env` file with secrets

### Configuration
- [ ] Configure JWT secret in `application.properties`
- [ ] Set database credentials in `docker-compose.yml`
- [ ] Update backend server port (5000)
- [ ] Update frontend configuration
- [ ] Review and update all environment variables
- [ ] Verify NGINX configuration syntax: `nginx -t`

### Database
- [ ] Create PostgreSQL database: `createdb pranikov_uphill`
- [ ] Create database user: `createuser uphill_user`
- [ ] Grant privileges to user
- [ ] Run database migrations (if any)
- [ ] Create initial admin user
- [ ] Test database connection from backend

### Service Startup
- [ ] Build backend JAR: `./mvnw clean package -DskipTests`
- [ ] Start services: `docker-compose up -d`
- [ ] Wait for services to initialize (2-3 minutes)
- [ ] Check service status: `docker-compose ps`
- [ ] Verify all services are healthy

### Verification
- [ ] [ ] **Test Health Endpoints**
  - `curl http://localhost/health`
  - `curl -k https://your-domain.com/health`
  - `curl -k https://your-domain.com/api/health`

- [ ] **Test Frontend Access**
  - Open https://your-domain.com in browser
  - Check that page loads without errors
  - Test responsive design (mobile view)

- [ ] **Test API Endpoints**
  ```bash
  # Get products (public)
  curl https://your-domain.com/api/pharmacy/products
  
  # Login test
  curl -X POST https://your-domain.com/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@pranikov.com","password":"admin123"}'
  ```

- [ ] **Test Database Connectivity**
  ```bash
  docker-compose exec postgres psql -U uphill_user -d pranikov_uphill -c "SELECT COUNT(*) FROM users;"
  ```

- [ ] **Test Load Balancing**
  - Monitor logs from both backends
  - Verify requests distributed to both servers

---

## 🔐 Security Hardening (Post-Deployment)

### Firewall Configuration
- [ ] Allow only ports 80 and 443 from internet
- [ ] Allow SSH (22) only from admin IPs
- [ ] Deny all other inbound traffic
- [ ] Test firewall rules

### SSL/TLS Verification
- [ ] Verify certificate validity: `openssl x509 -in cert.pem -text -noout`
- [ ] Check certificate chain completeness
- [ ] Test SSL with SSL Labs: https://www.ssllabs.com/ssltest/
- [ ] Verify HSTS header: `curl -i -k https://your-domain.com | grep Strict`
- [ ] Test with different TLS versions (1.2, 1.3)

### Application Security
- [ ] Verify security headers present
- [ ] Test CORS configuration
- [ ] Verify rate limiting works
- [ ] Test SQL injection prevention
- [ ] Test XSS protection (CSP headers)
- [ ] Verify file upload restrictions

### Database Security
- [ ] Change default database passwords
- [ ] Create strong JWT secret (random 32+ character string)
- [ ] Remove test/demo user accounts
- [ ] Enable PostgreSQL authentication logging
- [ ] Verify encrypted connections

### User Management
- [ ] Create strong admin account
- [ ] Change default credentials completely
- [ ] Remove demo accounts from database
- [ ] Set up user roles and permissions
- [ ] Document user management procedures

---

## 📊 Monitoring & Logging Setup

### Log Collection
- [ ] Configure log rotation for NGINX logs
- [ ] Configure log rotation for backend logs
- [ ] Enable PostgreSQL query logging (slow queries)
- [ ] Set up log aggregation (optional, for large deployments)
- [ ] Archive old logs to storage

### Health Monitoring
- [ ] Run health check script: `./health-check.sh your-domain.com`
- [ ] Set up automated health checks (cron job)
- [ ] Configure alerts for service failures
- [ ] Test alert notifications
- [ ] Document alert response procedures

### Performance Monitoring
- [ ] Monitor server resource usage (CPU, RAM, disk)
- [ ] Monitor network bandwidth
- [ ] Monitor response times
- [ ] Set performance thresholds and alerts
- [ ] Create dashboard for monitoring

### Security Monitoring
- [ ] Monitor access logs for suspicious patterns
- [ ] Set up alerts for failed login attempts
- [ ] Monitor for unusual API usage
- [ ] Review security headers in responses
- [ ] Set up DDoS detection alerts

---

## 💾 Backup & Disaster Recovery

### Database Backups
- [ ] Create initial full backup
- [ ] Test backup restoration
- [ ] Schedule daily automated backups
- [ ] Store backups in secure location (cloud storage)
- [ ] Create backup retention policy (30+ days)
- [ ] Document backup and restore procedures
- [ ] Test restore monthly

### Configuration Backups
- [ ] Backup nginx.conf
- [ ] Backup docker-compose.yml
- [ ] Backup application.properties
- [ ] Backup .env file (encrypted, secure location)
- [ ] Store in version control (private repository)

### Application Backups
- [ ] Backup JAR files to artifact repository
- [ ] Keep previous versions for quick rollback
- [ ] Document deployment history
- [ ] Create rollback procedure
- [ ] Test rollback process

### Disaster Recovery
- [ ] Document disaster recovery procedures
- [ ] Create runbook for service restoration
- [ ] Assign disaster recovery responsibilities
- [ ] Test recovery procedures monthly
- [ ] Maintain updated contact information

---

## 👥 Team & Documentation

### Documentation
- [ ] User guide for administrators
- [ ] API documentation
- [ ] Deployment procedures
- [ ] Troubleshooting guide
- [ ] Incident response procedures
- [ ] Emergency contact information

### Training
- [ ] Train ops team on service management
- [ ] Train team on monitoring and alerting
- [ ] Train team on backup and recovery
- [ ] Train team on security procedures
- [ ] Document and maintain knowledge base

### Handoff
- [ ] Create operational runbooks
- [ ] Document all customizations made
- [ ] Provide access credentials securely
- [ ] Schedule handoff meetings
- [ ] Get sign-off on system readiness

---

## 🧪 Performance & Load Testing

### Baseline Performance
- [ ] Record baseline response times
- [ ] Test with typical user load
- [ ] Test with peak load (2x expected traffic)
- [ ] Measure database query performance
- [ ] Monitor resource utilization under load

### Stress Testing
- [ ] Test load balancing under stress
- [ ] Verify graceful degradation
- [ ] Test rate limiting effectiveness
- [ ] Verify error handling and recovery
- [ ] Document performance limits

### Optimization
- [ ] Identify and fix performance bottlenecks
- [ ] Optimize database queries (if needed)
- [ ] Tune JVM parameters
- [ ] Optimize NGINX configuration
- [ ] Review and optimize frontend bundle

---

## ✨ Launch Readiness

### Final Checks (48 hours before launch)
- [ ] All tests passing
- [ ] Documentation complete and reviewed
- [ ] Team trained and ready
- [ ] Backup and disaster recovery verified
- [ ] Monitoring and alerting configured
- [ ] Security hardening complete
- [ ] Performance baseline established
- [ ] Load testing successful

### Launch Checklist
- [ ] Notify stakeholders of launch time
- [ ] Have team members on standby
- [ ] Monitor all systems continuously
- [ ] Have rollback plan ready
- [ ] Prepare incident response team
- [ ] Document launch date and time
- [ ] Collect metrics during launch

### Post-Launch (First 24 hours)
- [ ] Monitor error logs closely
- [ ] Track performance metrics
- [ ] Verify all features working
- [ ] Check user feedback
- [ ] Monitor database performance
- [ ] Verify backups running
- [ ] Document any issues
- [ ] Brief team on lessons learned

---

## 📋 Daily Operations (Ongoing)

### Daily Checks
- [ ] Verify all services running (docker-compose ps)
- [ ] Review error logs for critical issues
- [ ] Monitor system resource usage
- [ ] Check database size and growth
- [ ] Verify backups completed successfully
- [ ] Check for security alerts

### Weekly Tasks
- [ ] Review access logs for patterns
- [ ] Check certificate expiration (30+ days warning)
- [ ] Performance review and optimization
- [ ] Update security patches (if any)
- [ ] Review and archive old logs
- [ ] Test backup restoration

### Monthly Tasks
- [ ] Full security audit
- [ ] Capacity planning review
- [ ] Database maintenance
- [ ] Disaster recovery test
- [ ] User access review
- [ ] Performance analysis
- [ ] Cost optimization review

### Quarterly Tasks
- [ ] Major version updates (if any)
- [ ] Security penetration testing
- [ ] Architecture review
- [ ] Scaling assessment
- [ ] Documentation update
- [ ] Team training refresher

---

## 🚨 Incident Response

### Critical Issue Procedure
1. [ ] Alert on-call engineer immediately
2. [ ] Gather information about the issue
3. [ ] Check monitoring dashboards
4. [ ] Review recent logs and changes
5. [ ] Attempt mitigation steps
6. [ ] Escalate if needed
7. [ ] Keep stakeholders informed
8. [ ] Document issue and resolution
9. [ ] Post-incident review

### Common Issues & Responses
| Issue | First Steps | Escalation |
|-------|-------------|-----------|
| High error rate | Check logs, restart services | Rollback to previous version |
| Database down | Check connection, restart | Restore from backup |
| High latency | Monitor resources, check queries | Add more backend nodes |
| SSL certificate error | Check expiration, renew if needed | Obtain new certificate |
| Memory leak | Restart container, check logs | Contact development team |

---

## ✅ Sign-Off

- [ ] **Infrastructure Team:** _________________ Date: _______
- [ ] **Security Team:** _________________ Date: _______
- [ ] **Operations Team:** _________________ Date: _______
- [ ] **Project Manager:** _________________ Date: _______
- [ ] **Client/Stakeholder:** _________________ Date: _______

---

## 📞 Contacts

### Key Contacts
- **On-Call Engineer:** _________________ Phone: _________________
- **Database Admin:** _________________ Phone: _________________
- **Security Officer:** _________________ Phone: _________________
- **DevOps Lead:** _________________ Phone: _________________
- **Escalation Manager:** _________________ Phone: _________________

### External Contacts
- **Hosting Provider Support:** _________________
- **Certificate Provider:** _________________
- **Domain Registrar:** _________________

---

**Version:** 1.0
**Last Updated:** 2024
**Status:** Ready for Production
