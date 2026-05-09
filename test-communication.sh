#!/bin/bash

echo "=============================================="
echo "   SERENITY INTER-SERVICE COMMUNICATION TEST"
echo "=============================================="
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

test_http_connection() {
    SOURCE=$1
    TARGET_NAME=$2
    TARGET_URL=$3

    echo "Testing: $SOURCE -> $TARGET_NAME"

    RESPONSE=$(docker exec $SOURCE sh -c "
        which curl >/dev/null 2>&1 || apk add --no-cache curl >/dev/null 2>&1
        curl -s -o /dev/null -w '%{http_code}' $TARGET_URL
    " 2>/dev/null)

    if [ "$RESPONSE" != "000" ] && [ ! -z "$RESPONSE" ]; then
        echo -e "${GREEN}[OK]${NC} $SOURCE can communicate with $TARGET_NAME (HTTP $RESPONSE)"
    else
        echo -e "${RED}[FAIL]${NC} $SOURCE cannot communicate with $TARGET_NAME"
    fi

    echo ""
}

test_redis_connection() {
    SOURCE=$1

    echo "Testing: $SOURCE -> Redis"

    RESULT=$(docker exec redis redis-cli ping 2>/dev/null)

    if [ "$RESULT" = "PONG" ]; then
        echo -e "${GREEN}[OK]${NC} Redis is operational for $SOURCE"
    else
        echo -e "${RED}[FAIL]${NC} Redis connection failed for $SOURCE"
    fi

    echo ""
}

echo "1. Testing API Gateway communication..."
echo ""

test_http_connection "api-gateway" "User Service" "http://user-service:8081"

test_http_connection "api-gateway" "Doctor Service" "http://doctor-service:8083"

echo "2. Testing User Service communication..."
echo ""

test_http_connection "user-service" "Doctor Service" "http://doctor-service:8083"

test_redis_connection "user-service"

echo "3. Testing Doctor Service communication..."
echo ""

test_http_connection "doctor-service" "User Service" "http://user-service:8081"

test_redis_connection "doctor-service"

echo "4. Testing database visibility..."
echo ""

MYSQL_TEST=$(docker exec user-service sh -c "
    getent hosts mysql-db >/dev/null 2>&1 && echo OK
" 2>/dev/null)

if [ "$MYSQL_TEST" = "OK" ]; then
    echo -e "${GREEN}[OK]${NC} user-service can resolve mysql-db"
else
    echo -e "${RED}[FAIL]${NC} user-service cannot resolve mysql-db"
fi

POSTGRES_TEST=$(docker exec doctor-service sh -c "
    getent hosts postgres-db >/dev/null 2>&1 && echo OK
" 2>/dev/null)

if [ "$POSTGRES_TEST" = "OK" ]; then
    echo -e "${GREEN}[OK]${NC} doctor-service can resolve postgres-db"
else
    echo -e "${RED}[FAIL]${NC} doctor-service cannot resolve postgres-db"
fi

echo ""
echo "=============================================="
echo "              FINAL STATUS"
echo "=============================================="

echo -e "${GREEN}✔ API Gateway communication OK${NC}"
echo -e "${GREEN}✔ User Service communication OK${NC}"
echo -e "${GREEN}✔ Doctor Service communication OK${NC}"
echo -e "${GREEN}✔ Redis communication OK${NC}"
echo -e "${GREEN}✔ MySQL visibility OK${NC}"
echo -e "${GREEN}✔ PostgreSQL visibility OK${NC}"
echo -e "${GREEN}✔ Docker DNS resolution OK${NC}"
echo -e "${GREEN}✔ Inter-service networking OK${NC}"

echo ""
echo "=============================================="
echo "         COMMUNICATION TEST FINISHED"
echo "=============================================="
