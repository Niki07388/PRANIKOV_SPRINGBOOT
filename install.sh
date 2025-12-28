#!/bin/bash

# Pranikov Healthcare Platform - Installation Script
# This script sets up the production environment

set -e

echo "=== Pranikov Healthcare Platform - Production Setup ==="
echo

# Check if running as root
if [ "$EUID" -ne 0 ]; then 
  echo "ERROR: This script must be run as root (use sudo)"
  exit 1
fi

# Variables
INSTALL_DIR="/opt/pranikov"
SERVICE_NAME="pranikov"
DOMAIN="${1:-pranikov.example.com}"

echo "Installation Directory: $INSTALL_DIR"
echo "Service Name: $SERVICE_NAME"
echo "Domain: $DOMAIN"
echo

# Step 1: Install Docker
echo "[1/6] Installing Docker..."
if ! command -v docker &> /dev/null; then
    apt-get update
    apt-get install -y docker.io docker-compose
    systemctl start docker
    systemctl enable docker
else
    echo "✓ Docker already installed"
fi

# Step 2: Create installation directory
echo "[2/6] Setting up directories..."
mkdir -p $INSTALL_DIR/{nginx/ssl,logs/nginx,logs/backend,backups}
chmod 755 $INSTALL_DIR

# Step 3: Copy application files
echo "[3/6] Copying application files..."
# This would be automated by deployment system
echo "→ Copy project files to $INSTALL_DIR"

# Step 4: Generate SSL certificates
echo "[4/6] Setting up SSL certificates..."
if [ ! -f "$INSTALL_DIR/nginx/ssl/cert.pem" ]; then
    echo "Generating self-signed certificate..."
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout $INSTALL_DIR/nginx/ssl/key.pem \
        -out $INSTALL_DIR/nginx/ssl/cert.pem \
        -subj "/C=IN/ST=State/L=City/O=Pranikov/CN=$DOMAIN" 2>/dev/null
    chmod 600 $INSTALL_DIR/nginx/ssl/key.pem
    echo "✓ Self-signed certificate created"
    echo "⚠ For production, use Let's Encrypt certificates"
else
    echo "✓ Certificate files already exist"
fi

# Step 5: Update configuration
echo "[5/6] Updating configuration..."
if [ -f "$INSTALL_DIR/nginx/nginx.conf" ]; then
    sed -i "s/pranikov.example.com/$DOMAIN/g" $INSTALL_DIR/nginx/nginx.conf
    echo "✓ Domain updated in NGINX configuration"
fi

# Step 6: Install systemd service
echo "[6/6] Installing systemd service..."
cp $INSTALL_DIR/pranikov.service /etc/systemd/system/$SERVICE_NAME.service
sed -i "s|/opt/pranikov|$INSTALL_DIR|g" /etc/systemd/system/$SERVICE_NAME.service
systemctl daemon-reload
systemctl enable $SERVICE_NAME
echo "✓ Systemd service installed"

echo
echo "=== Installation Complete ==="
echo
echo "Next steps:"
echo "1. Edit $INSTALL_DIR/docker-compose.yml to update credentials"
echo "2. Edit $INSTALL_DIR/.env with database passwords"
echo "3. For Let's Encrypt: sudo certbot certonly --standalone -d $DOMAIN"
echo "4. Copy certs: sudo cp /etc/letsencrypt/live/$DOMAIN/fullchain.pem $INSTALL_DIR/nginx/ssl/cert.pem"
echo "5. Start service: sudo systemctl start $SERVICE_NAME"
echo "6. Check status: sudo systemctl status $SERVICE_NAME"
echo "7. View logs: sudo journalctl -fu $SERVICE_NAME"
echo

echo "To view NGINX logs:"
echo "  docker-compose -f $INSTALL_DIR/docker-compose.yml logs nginx"
echo

echo "To restart services:"
echo "  sudo systemctl restart $SERVICE_NAME"
echo
