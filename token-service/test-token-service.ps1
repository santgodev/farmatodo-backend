# Token Service Testing Script (PowerShell)
# This script tests all endpoints of the Token Service API

$baseUrl = "http://localhost:8082"
$apiKey = "your-secret-api-key-change-in-production"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Token Service API Testing Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Function to print test header
function Print-TestHeader {
    param($testName)
    Write-Host ""
    Write-Host ">>> $testName" -ForegroundColor Yellow
    Write-Host "---" -ForegroundColor Gray
}

# Function to print response
function Print-Response {
    param($response, $statusCode)
    Write-Host "Status: $statusCode" -ForegroundColor $(if ($statusCode -ge 200 -and $statusCode -lt 300) { "Green" } else { "Red" })
    Write-Host "Response: $response" -ForegroundColor White
}

# Test 1: Ping endpoint
Print-TestHeader "Test 1: Ping Endpoint (GET /ping)"
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/ping" -Method Get
    Print-Response $response 200
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Health check
Print-TestHeader "Test 2: Health Check (GET /api/tokens/health)"
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/tokens/health" -Method Get
    Print-Response ($response | ConvertTo-Json -Depth 10) 200
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Service info
Print-TestHeader "Test 3: Service Info (GET /api/tokens/info)"
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/tokens/info" -Method Get
    Print-Response ($response | ConvertTo-Json -Depth 10) 200
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Successful tokenization - Visa
Print-TestHeader "Test 4: Tokenize Visa Card (Valid)"
try {
    $body = @{
        cardNumber = "4111111111111111"
        cvv = "123"
        expiry = "12/28"
        cardholderName = "John Doe"
    } | ConvertTo-Json

    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "ApiKey $apiKey"
    }

    $response = Invoke-RestMethod -Uri "$baseUrl/tokenize" -Method Post -Body $body -Headers $headers
    Print-Response ($response | ConvertTo-Json -Depth 10) 200
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorResponse = $_.ErrorDetails.Message
    Print-Response $errorResponse $statusCode
}

# Test 5: Successful tokenization - Mastercard
Print-TestHeader "Test 5: Tokenize Mastercard (Valid)"
try {
    $body = @{
        cardNumber = "5425233430109903"
        cvv = "456"
        expiry = "06/29"
        cardholderName = "Jane Smith"
    } | ConvertTo-Json

    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "ApiKey $apiKey"
    }

    $response = Invoke-RestMethod -Uri "$baseUrl/tokenize" -Method Post -Body $body -Headers $headers
    Print-Response ($response | ConvertTo-Json -Depth 10) 200
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorResponse = $_.ErrorDetails.Message
    Print-Response $errorResponse $statusCode
}

# Test 6: Successful tokenization - Amex
Print-TestHeader "Test 6: Tokenize Amex (Valid - 4 digit CVV)"
try {
    $body = @{
        cardNumber = "378282246310005"
        cvv = "1234"
        expiry = "03/27"
        cardholderName = "Robert Johnson"
    } | ConvertTo-Json

    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "ApiKey $apiKey"
    }

    $response = Invoke-RestMethod -Uri "$baseUrl/tokenize" -Method Post -Body $body -Headers $headers
    Print-Response ($response | ConvertTo-Json -Depth 10) 200
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorResponse = $_.ErrorDetails.Message
    Print-Response $errorResponse $statusCode
}

# Test 7: Invalid card number (failed Luhn)
Print-TestHeader "Test 7: Invalid Card Number - Failed Luhn (Expected: 400)"
try {
    $body = @{
        cardNumber = "1234567890123456"
        cvv = "123"
        expiry = "12/28"
        cardholderName = "John Doe"
    } | ConvertTo-Json

    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "ApiKey $apiKey"
    }

    $response = Invoke-RestMethod -Uri "$baseUrl/tokenize" -Method Post -Body $body -Headers $headers
    Print-Response ($response | ConvertTo-Json -Depth 10) 200
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorResponse = $_.ErrorDetails.Message
    Print-Response $errorResponse $statusCode
}

# Test 8: Invalid CVV
Print-TestHeader "Test 8: Invalid CVV - Too Short (Expected: 400)"
try {
    $body = @{
        cardNumber = "4111111111111111"
        cvv = "12"
        expiry = "12/28"
        cardholderName = "John Doe"
    } | ConvertTo-Json

    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "ApiKey $apiKey"
    }

    $response = Invoke-RestMethod -Uri "$baseUrl/tokenize" -Method Post -Body $body -Headers $headers
    Print-Response ($response | ConvertTo-Json -Depth 10) 200
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorResponse = $_.ErrorDetails.Message
    Print-Response $errorResponse $statusCode
}

# Test 9: Invalid expiry
Print-TestHeader "Test 9: Invalid Expiry - Wrong Month (Expected: 400)"
try {
    $body = @{
        cardNumber = "4111111111111111"
        cvv = "123"
        expiry = "13/28"
        cardholderName = "John Doe"
    } | ConvertTo-Json

    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "ApiKey $apiKey"
    }

    $response = Invoke-RestMethod -Uri "$baseUrl/tokenize" -Method Post -Body $body -Headers $headers
    Print-Response ($response | ConvertTo-Json -Depth 10) 200
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorResponse = $_.ErrorDetails.Message
    Print-Response $errorResponse $statusCode
}

# Test 10: Invalid cardholder name
Print-TestHeader "Test 10: Invalid Cardholder Name - Too Short (Expected: 400)"
try {
    $body = @{
        cardNumber = "4111111111111111"
        cvv = "123"
        expiry = "12/28"
        cardholderName = "J"
    } | ConvertTo-Json

    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "ApiKey $apiKey"
    }

    $response = Invoke-RestMethod -Uri "$baseUrl/tokenize" -Method Post -Body $body -Headers $headers
    Print-Response ($response | ConvertTo-Json -Depth 10) 200
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorResponse = $_.ErrorDetails.Message
    Print-Response $errorResponse $statusCode
}

# Test 11: Missing API key
Print-TestHeader "Test 11: Missing API Key (Expected: 401)"
try {
    $body = @{
        cardNumber = "4111111111111111"
        cvv = "123"
        expiry = "12/28"
        cardholderName = "John Doe"
    } | ConvertTo-Json

    $headers = @{
        "Content-Type" = "application/json"
    }

    $response = Invoke-RestMethod -Uri "$baseUrl/tokenize" -Method Post -Body $body -Headers $headers
    Print-Response ($response | ConvertTo-Json -Depth 10) 200
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorResponse = $_.ErrorDetails.Message
    Print-Response $errorResponse $statusCode
}

# Test 12: Invalid API key
Print-TestHeader "Test 12: Invalid API Key (Expected: 401)"
try {
    $body = @{
        cardNumber = "4111111111111111"
        cvv = "123"
        expiry = "12/28"
        cardholderName = "John Doe"
    } | ConvertTo-Json

    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "ApiKey wrong-api-key"
    }

    $response = Invoke-RestMethod -Uri "$baseUrl/tokenize" -Method Post -Body $body -Headers $headers
    Print-Response ($response | ConvertTo-Json -Depth 10) 200
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorResponse = $_.ErrorDetails.Message
    Print-Response $errorResponse $statusCode
}

# Test 13: Card with spaces
Print-TestHeader "Test 13: Card Number with Spaces (Should be cleaned)"
try {
    $body = @{
        cardNumber = "4532 0151 1283 0366"
        cvv = "789"
        expiry = "09/26"
        cardholderName = "Maria Garcia"
    } | ConvertTo-Json

    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "ApiKey $apiKey"
    }

    $response = Invoke-RestMethod -Uri "$baseUrl/tokenize" -Method Post -Body $body -Headers $headers
    Print-Response ($response | ConvertTo-Json -Depth 10) 200
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorResponse = $_.ErrorDetails.Message
    Print-Response $errorResponse $statusCode
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "All tests completed!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
