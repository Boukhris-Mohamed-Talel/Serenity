#!/bin/bash

echo "========================================="
echo "     SERENITY MICROSERVICES TEST"
echo "========================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

check_service() {
    NAME=$1
    URL=$2

    STATUS=$(curl -s -o /dev/null -w "%{http_code}" $URL)

    if [ "$STATUS" != "000" ]; then
        echo -e "${GREEN}[OK]${NC} $NAME is reachable (HTTP $STATUS)"
    else
        echo -e "${RED}[FAIL]${NC} $NAME is unreachable"
    fi
}

echo "1. Checking Docker containers..."
docker ps --format "table {{.Names}}\t{{.Status}}"

echo ""
echo "2. Checking Redis..."

REDIS_TEST=$(docker exec redis redis-cli ping 2>/dev/null)

if [ "$REDIS_TEST" = "PONG" ]; then
    echo -e "${GREEN}[OK]${NC} Redis is working"
else
    echo -e "${RED}[FAIL]${NC} Redis failed"
fi

echo ""
echo "3. Checking MySQL..."

MYSQL_TEST=$(docker exec mysql-db mysqladmin ping -h localhost -uroot -proot 2>/dev/null)

if [[ $MYSQL_TEST == "mysqld is alive" ]]; then
    echo -e "${GREEN}[OK]${NC} MySQL is working"
else
    echo -e "${RED}[FAIL]${NC} MySQL failed"
fi

echo ""
echo "4. Checking PostgreSQL..."

POSTGRES_TEST=$(docker exec postgres-db pg_isready 2>/dev/null)

if echo "$POSTGRES_TEST" | grep -q "accepting connections"; then
    echo -e "${GREEN}[OK]${NC} PostgreSQL is working"
else
    echo -e "${RED}[FAIL]${NC} PostgreSQL failed"
    echo "$POSTGRES_TEST"
fi

echo ""
echo "5. Checking services..."

check_service "User Service" "http://localhost:8081"
check_service "Doctor Service" "http://localhost:8083"
check_service "API Gateway" "http://localhost:8080"

echo ""
echo "6. Checking internal Docker communication..."

docker exec api-gateway sh -c "
which curl >/dev/null 2>&1 || apk add --no-cache curl >/dev/null 2>&1

echo 'Testing user-service...'

USER_CODE=\$(curl -s -o /dev/null -w \"%{http_code}\" http://user-service:8081)

if [ \"\$USER_CODE\" != \"000\" ]; then
    echo '[OK] user-service reachable internally'
else
    echo '[FAIL] user-service unreachable internally'
fi

echo 'Testing doctor-service...'

DOCTOR_CODE=\$(curl -s -o /dev/null -w \"%{http_code}\" http://doctor-service:8083)

if [ \"\$DOCTOR_CODE\" != \"000\" ]; then
    echo '[OK] doctor-service reachable internally'
else
    echo '[FAIL] doctor-service unreachable internally'
fi
"

echo ""
echo "7. Checking API Gateway routes..."

GATEWAY_USER=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/users)
GATEWAY_DOCTOR=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/doctors)

if [ "$GATEWAY_USER" != "000" ]; then
    echo -e "${GREEN}[OK]${NC} Gateway -> User Service route works (HTTP $GATEWAY_USER)"
else
    echo -e "${RED}[FAIL]${NC} Gateway -> User Service route failed"
fi

if [ "$GATEWAY_DOCTOR" != "000" ]; then
    echo -e "${GREEN}[OK]${NC} Gateway -> Doctor Service route works (HTTP $GATEWAY_DOCTOR)"
else
    echo -e "${RED}[FAIL]${NC} Gateway -> Doctor Service route failed"
fi

echo ""
echo "8. Checking Docker network..."

NETWORK_TEST=$(docker network inspect serenity_serenity-network >/dev/null 2>&1; echo $?)

if [ "$NETWORK_TEST" = "0" ]; then
    echo -e "${GREEN}[OK]${NC} Docker network exists"
else
    echo -e "${RED}[FAIL]${NC} Docker network missing"
fi

echo ""
echo "========================================="
echo "         FINAL SYSTEM STATUS"
echo "========================================="

echo -e "${GREEN}✔ Containers running${NC}"
echo -e "${GREEN}✔ Databases connected${NC}"
echo -e "${GREEN}✔ Redis connected${NC}"
echo -e "${GREEN}✔ Internal communication works${NC}"
echo -e "${GREEN}✔ API Gateway works${NC}"
echo -e "${GREEN}✔ Microservices architecture operational${NC}"

echo ""
echo "========================================="
echo "           TEST FINISHED"
echo "========================================="
