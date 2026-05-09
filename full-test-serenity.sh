#!/bin/bash

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=true

echo "=================================================="
echo "     SERENITY FINAL MICROSERVICES TEST"
echo "=================================================="

echo ""
echo "1. Checking containers..."

docker ps --format "table {{.Names}}\t{{.Status}}"

echo ""
echo "2. Checking Redis..."

REDIS=$(docker exec serenity-redis-1 redis-cli ping 2>/dev/null)

if [[ "$REDIS" == "PONG" ]]; then
    echo -e "${GREEN}[OK] Redis operational${NC}"
else
    echo -e "${RED}[FAIL] Redis failed${NC}"
    PASS=false
fi

echo ""
echo "3. Checking MySQL container..."

MYSQL_RUNNING=$(docker ps --format "{{.Names}}" | grep serenity-mysql-db-1)

if [[ ! -z "$MYSQL_RUNNING" ]]; then
    echo -e "${GREEN}[OK] MySQL container running${NC}"
else
    echo -e "${RED}[FAIL] MySQL container down${NC}"
    PASS=false
fi

echo ""
echo "4. Checking PostgreSQL container..."

POSTGRES_RUNNING=$(docker ps --format "{{.Names}}" | grep serenity-postgres-db-1)

if [[ ! -z "$POSTGRES_RUNNING" ]]; then
    echo -e "${GREEN}[OK] PostgreSQL container running${NC}"
else
    echo -e "${RED}[FAIL] PostgreSQL container down${NC}"
    PASS=false
fi

echo ""
echo "5. Testing User Service..."

USER_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/users)

if [[ "$USER_CODE" == "401" || "$USER_CODE" == "403" || "$USER_CODE" == "200" ]]; then
    echo -e "${GREEN}[OK] User Service reachable through Gateway${NC}"
else
    echo -e "${RED}[FAIL] User Service unreachable${NC}"
    PASS=false
fi

echo ""
echo "6. Testing Doctor Service..."

DOCTOR_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/doctors)

if [[ "$DOCTOR_CODE" == "401" || "$DOCTOR_CODE" == "403" || "$DOCTOR_CODE" == "200" ]]; then
    echo -e "${GREEN}[OK] Doctor Service reachable through Gateway${NC}"
else
    echo -e "${RED}[FAIL] Doctor Service unreachable${NC}"
    PASS=false
fi

echo ""
echo "7. Testing Authentication Login..."

LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{
  "email":"rayen@test.com",
  "password":"Password123!"
}')

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d':' -f2 | tr -d '"')

if [[ ! -z "$TOKEN" ]]; then
    echo -e "${GREEN}[OK] Authentication working${NC}"
else
    echo -e "${RED}[FAIL] Authentication failed${NC}"
    PASS=false
fi

echo ""
echo "8. Testing JWT protected route..."

ME_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
-H "Authorization: Bearer $TOKEN" \
http://localhost:8080/api/users/me)

if [[ "$ME_CODE" == "200" ]]; then
    echo -e "${GREEN}[OK] JWT authorization working${NC}"
else
    echo -e "${RED}[FAIL] JWT authorization failed${NC}"
    PASS=false
fi

echo ""
echo "9. Testing Docker DNS..."

DNS_USER=$(docker exec serenity-api-gateway-1 getent hosts user-service >/dev/null 2>&1; echo $?)
DNS_DOCTOR=$(docker exec serenity-api-gateway-1 getent hosts doctor-service >/dev/null 2>&1; echo $?)

if [[ "$DNS_USER" == "0" ]]; then
    echo -e "${GREEN}[OK] user-service DNS OK${NC}"
else
    echo -e "${RED}[FAIL] user-service DNS failed${NC}"
    PASS=false
fi

if [[ "$DNS_DOCTOR" == "0" ]]; then
    echo -e "${GREEN}[OK] doctor-service DNS OK${NC}"
else
    echo -e "${RED}[FAIL] doctor-service DNS failed${NC}"
    PASS=false
fi

echo ""
echo "10. Testing API Gateway routes..."

GW_USERS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/users)
GW_DOCTORS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/doctors)

if [[ "$GW_USERS" == "401" || "$GW_USERS" == "403" || "$GW_USERS" == "200" ]]; then
    echo -e "${GREEN}[OK] Gateway route /api/users working${NC}"
else
    echo -e "${RED}[FAIL] Gateway route /api/users failed${NC}"
    PASS=false
fi

if [[ "$GW_DOCTORS" == "401" || "$GW_DOCTORS" == "403" || "$GW_DOCTORS" == "200" ]]; then
    echo -e "${GREEN}[OK] Gateway route /api/doctors working${NC}"
else
    echo -e "${RED}[FAIL] Gateway route /api/doctors failed${NC}"
    PASS=false
fi

echo ""
echo "11. Testing inter-service communication..."

INTERNAL_USER=$(docker exec serenity-api-gateway-1 sh -c "curl -s -o /dev/null -w '%{http_code}' http://user-service:8081" 2>/dev/null)

if [[ "$INTERNAL_USER" == "401" || "$INTERNAL_USER" == "403" || "$INTERNAL_USER" == "200" ]]; then
    echo -e "${GREEN}[OK] Gateway -> User Service communication OK${NC}"
else
    echo -e "${RED}[FAIL] Gateway -> User Service failed${NC}"
    PASS=false
fi

INTERNAL_DOCTOR=$(docker exec serenity-api-gateway-1 sh -c "curl -s -o /dev/null -w '%{http_code}' http://doctor-service:8083/api/doctors" 2>/dev/null)

if [[ "$INTERNAL_DOCTOR" == "401" || "$INTERNAL_DOCTOR" == "403" || "$INTERNAL_DOCTOR" == "200" ]]; then
    echo -e "${GREEN}[OK] Gateway -> Doctor Service communication OK${NC}"
else
    echo -e "${RED}[FAIL] Gateway -> Doctor Service failed${NC}"
    PASS=false
fi

echo ""
echo "=================================================="
echo "                 FINAL STATUS"
echo "=================================================="

if [ "$PASS" = true ]; then
    echo -e "${GREEN}✔ ALL TESTS PASSED${NC}"
    echo -e "${GREEN}✔ API Gateway operational${NC}"
    echo -e "${GREEN}✔ User Service operational${NC}"
    echo -e "${GREEN}✔ Doctor Service operational${NC}"
    echo -e "${GREEN}✔ JWT authentication operational${NC}"
    echo -e "${GREEN}✔ Redis operational${NC}"
    echo -e "${GREEN}✔ MySQL operational${NC}"
    echo -e "${GREEN}✔ PostgreSQL operational${NC}"
    echo -e "${GREEN}✔ Docker networking operational${NC}"
    echo -e "${GREEN}✔ Inter-service communication operational${NC}"
    echo -e "${GREEN}✔ Full Serenity architecture operational${NC}"
else
    echo -e "${YELLOW}⚠ Some tests failed${NC}"
fi

echo ""
echo "=================================================="
echo "                 TEST FINISHED"
echo "=================================================="
