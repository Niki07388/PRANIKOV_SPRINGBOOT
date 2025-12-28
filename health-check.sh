#!/bin/bash

# Pranikov Healthcare Platform - Health Check Script
# Monitors all services and reports status

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
DOMAIN="${1:-localhost}"
USE_SSL="${2:-true}"
PROTOCOL="https"
if [ "$USE_SSL" = "false" ]; then
    PROTOCOL="http"
fi

echo -e "${BLUE}=== Pranikov Healthcare Platform - Health Check ===${NC}"
echo -e "${BLUE}Domain: $DOMAIN${NC}"
echo -e "${BLUE}Protocol: $PROTOCOL${NC}"
echo

# Function to check URL
check_endpoint() {
    local name=$1
    local url=$2
    local expected_code=${3:-200}
    
    echo -n "Checking $name... "
    
    if [ "$PROTOCOL" = "https" ]; then
        response=$(curl -s -o /dev/null -w "%{http_code}" -k "$url" 2>/dev/null || echo "000")
    else
        response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
    fi
    
    if [ "$response" = "$expected_code" ]; then
        echo -e "${GREEN}✓ OK (HTTP $response)${NC}"
        return 0
    else
        echo -e "${RED}✗ FAILED (HTTP $response, expected $expected_code)${NC}"
        return 1
    fi
}

# Function to check Docker containers
check_container() {
    local container=$1
    
    echo -n "Checking Docker container: $container... "
    
    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        state=$(docker inspect -f '{{.State.Running}}' "$container")
        if [ "$state" = "true" ]; then
            health=$(docker inspect -f '{{.State.Health.Status}}' "$container" 2>/dev/null || echo "none")
            if [ "$health" = "healthy" ] || [ "$health" = "none" ]; then
                echo -e "${GREEN}✓ Running${NC}"
                return 0
            else
                echo -e "${YELLOW}⚠ $health${NC}"
                return 1
            fi
        else
            echo -e "${RED}✗ Not running${NC}"
            return 1
        fi
    else
        echo -e "${RED}✗ Not found${NC}"
        return 1
    fi
}

echo -e "${YELLOW}--- Docker Containers ---${NC}"
check_container "pranikov-nginx" || true
check_container "pranikov-backend-1" || true
check_container "pranikov-backend-2" || true
check_container "pranikov-frontend" || true
check_container "pranikov-postgres" || true

echo
echo -e "${YELLOW}--- API Endpoints ---${NC}"
check_endpoint "Health Check" "$PROTOCOL://$DOMAIN/health" "200" || true
check_endpoint "NGINX Status" "$PROTOCOL://$DOMAIN/nginx_status" "403" || true
check_endpoint "Frontend" "$PROTOCOL://$DOMAIN/" "200" || true

echo
echo -e "${YELLOW}--- Backend API ---${NC}"
check_endpoint "API Health" "$PROTOCOL://$DOMAIN/api/health" "200" || true
check_endpoint "Public Endpoints" "$PROTOCOL://$DOMAIN/api/pharmacy/products" "200" || true

echo
echo -e "${YELLOW}--- Docker Compose Status ---${NC}"
if command -v docker-compose &> /dev/null; then
    echo -e "${BLUE}Running containers:${NC}"
    docker-compose ps 2>/dev/null || echo "docker-compose not available in PATH"
fi

echo
echo -e "${YELLOW}--- Resource Usage ---${NC}"
echo "Docker memory usage:"
docker stats --no-stream --format "table {{.Container}}\t{{.MemUsage}}" 2>/dev/null | grep pranikov || echo "No containers running"

echo
echo -e "${YELLOW}--- Recent Logs ---${NC}"
echo "Recent NGINX errors (if any):"
docker logs --tail 5 pranikov-nginx 2>/dev/null | grep -i error || echo "No recent errors"

echo
echo -e "${YELLOW}--- Database Status ---${NC}"
if docker ps --format '{{.Names}}' | grep -q "pranikov-postgres"; then
    echo -n "Database connection... "
    if docker exec pranikov-postgres pg_isready -U uphill_user > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Accessible${NC}"
    else
        echo -e "${RED}✗ Not accessible${NC}"
    fi
fi

echo
echo -e "${BLUE}=== Health Check Complete ===${NC}"
echo
echo "Recommendations:"
echo "- If containers not running: docker-compose up -d"
echo "- If API failing: check backend logs with 'docker-compose logs backend-1'"
echo "- If SSL errors: verify certificates in nginx/ssl/"
echo "- For full diagnostics: check DEPLOYMENT_GUIDE.md"
echo
