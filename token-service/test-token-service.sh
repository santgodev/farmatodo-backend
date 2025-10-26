#!/bin/bash

# Token Service Testing Script (Bash)
# This script tests all endpoints of the Token Service API

BASE_URL="http://localhost:8082"
API_KEY="your-secret-api-key-change-in-production"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}========================================"
echo -e "Token Service API Testing Script"
echo -e "========================================${NC}"
echo ""

# Function to print test header
print_test_header() {
    echo ""
    echo -e "${YELLOW}>>> $1${NC}"
    echo -e "${NC}---"
}

# Test 1: Ping endpoint
print_test_header "Test 1: Ping Endpoint (GET /ping)"
curl -s -w "\nHTTP Status: %{http_code}\n" "$BASE_URL/ping"

# Test 2: Health check
print_test_header "Test 2: Health Check (GET /api/tokens/health)"
curl -s -w "\nHTTP Status: %{http_code}\n" "$BASE_URL/api/tokens/health" | jq . 2>/dev/null || echo ""

# Test 3: Service info
print_test_header "Test 3: Service Info (GET /api/tokens/info)"
curl -s -w "\nHTTP Status: %{http_code}\n" "$BASE_URL/api/tokens/info" | jq . 2>/dev/null || echo ""

# Test 4: Successful tokenization - Visa
print_test_header "Test 4: Tokenize Visa Card (Valid)"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/tokenize" \
  -H "Content-Type: application/json" \
  -H "Authorization: ApiKey $API_KEY" \
  -d '{
    "cardNumber": "4111111111111111",
    "cvv": "123",
    "expiry": "12/28",
    "cardholderName": "John Doe"
  }' | jq . 2>/dev/null || echo ""

# Test 5: Successful tokenization - Mastercard
print_test_header "Test 5: Tokenize Mastercard (Valid)"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/tokenize" \
  -H "Content-Type: application/json" \
  -H "Authorization: ApiKey $API_KEY" \
  -d '{
    "cardNumber": "5425233430109903",
    "cvv": "456",
    "expiry": "06/29",
    "cardholderName": "Jane Smith"
  }' | jq . 2>/dev/null || echo ""

# Test 6: Successful tokenization - Amex
print_test_header "Test 6: Tokenize Amex (Valid - 4 digit CVV)"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/tokenize" \
  -H "Content-Type: application/json" \
  -H "Authorization: ApiKey $API_KEY" \
  -d '{
    "cardNumber": "378282246310005",
    "cvv": "1234",
    "expiry": "03/27",
    "cardholderName": "Robert Johnson"
  }' | jq . 2>/dev/null || echo ""

# Test 7: Invalid card number (failed Luhn)
print_test_header "Test 7: Invalid Card Number - Failed Luhn (Expected: 400)"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/tokenize" \
  -H "Content-Type: application/json" \
  -H "Authorization: ApiKey $API_KEY" \
  -d '{
    "cardNumber": "1234567890123456",
    "cvv": "123",
    "expiry": "12/28",
    "cardholderName": "John Doe"
  }' | jq . 2>/dev/null || echo ""

# Test 8: Invalid CVV
print_test_header "Test 8: Invalid CVV - Too Short (Expected: 400)"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/tokenize" \
  -H "Content-Type: application/json" \
  -H "Authorization: ApiKey $API_KEY" \
  -d '{
    "cardNumber": "4111111111111111",
    "cvv": "12",
    "expiry": "12/28",
    "cardholderName": "John Doe"
  }' | jq . 2>/dev/null || echo ""

# Test 9: Invalid expiry
print_test_header "Test 9: Invalid Expiry - Wrong Month (Expected: 400)"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/tokenize" \
  -H "Content-Type: application/json" \
  -H "Authorization: ApiKey $API_KEY" \
  -d '{
    "cardNumber": "4111111111111111",
    "cvv": "123",
    "expiry": "13/28",
    "cardholderName": "John Doe"
  }' | jq . 2>/dev/null || echo ""

# Test 10: Invalid cardholder name
print_test_header "Test 10: Invalid Cardholder Name - Too Short (Expected: 400)"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/tokenize" \
  -H "Content-Type: application/json" \
  -H "Authorization: ApiKey $API_KEY" \
  -d '{
    "cardNumber": "4111111111111111",
    "cvv": "123",
    "expiry": "12/28",
    "cardholderName": "J"
  }' | jq . 2>/dev/null || echo ""

# Test 11: Missing API key
print_test_header "Test 11: Missing API Key (Expected: 401)"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/tokenize" \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4111111111111111",
    "cvv": "123",
    "expiry": "12/28",
    "cardholderName": "John Doe"
  }' | jq . 2>/dev/null || echo ""

# Test 12: Invalid API key
print_test_header "Test 12: Invalid API Key (Expected: 401)"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/tokenize" \
  -H "Content-Type: application/json" \
  -H "Authorization: ApiKey wrong-api-key" \
  -d '{
    "cardNumber": "4111111111111111",
    "cvv": "123",
    "expiry": "12/28",
    "cardholderName": "John Doe"
  }' | jq . 2>/dev/null || echo ""

# Test 13: Card with spaces
print_test_header "Test 13: Card Number with Spaces (Should be cleaned)"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST "$BASE_URL/tokenize" \
  -H "Content-Type: application/json" \
  -H "Authorization: ApiKey $API_KEY" \
  -d '{
    "cardNumber": "4532 0151 1283 0366",
    "cvv": "789",
    "expiry": "09/26",
    "cardholderName": "Maria Garcia"
  }' | jq . 2>/dev/null || echo ""

echo ""
echo -e "${CYAN}========================================"
echo -e "All tests completed!"
echo -e "========================================${NC}"
