#!/bin/bash
# QA Test Script for SMS Platform
# Server: http://43.139.178.124:8880

BASE="http://43.139.178.124:8880/v1"
NOPROXY="--noproxy *"

# Login
echo "=== Step 0: Login ==="
TOKEN=$(curl -s $NOPROXY -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "Token: ${TOKEN:0:20}..."
AUTH="Authorization: Bearer $TOKEN"

pass=0; fail=0
check() {
  local name="$1" resp="$2" expect="$3"
  if echo "$resp" | grep -q "$expect"; then
    echo "  [PASS] $name"
    ((pass++))
  else
    echo "  [FAIL] $name => $resp"
    ((fail++))
  fi
}

# === Customer CRUD ===
echo ""
echo "=== Test 1: Create Post-paid Customer ==="
R=$(curl -s $NOPROXY -X POST "$BASE/admin/customer" -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"customerCode":"POSTPAY01","customerName":"PostPay Customer B","companyName":"PostPay Corp","paymentType":2,"contactName":"Li Si","contactPhone":"13900139001","contactEmail":"lisi@test.com","allowedSmsAttributes":"1,3","address":"Shanghai","remark":"QA test postpaid"}')
check "Create postpaid customer" "$R" '"code":0'
POSTPAY_ID=$(echo "$R" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "  PostPay Customer ID: $POSTPAY_ID"

echo ""
echo "=== Test 2: Customer List ==="
R=$(curl -s $NOPROXY "$BASE/admin/customer/list?page=1&size=10" -H "$AUTH")
check "Customer list returns data" "$R" '"total"'

echo ""
echo "=== Test 3: Customer Detail (PREPAY01 id=2) ==="
R=$(curl -s $NOPROXY "$BASE/admin/customer/2/detail" -H "$AUTH")
check "Customer detail with account" "$R" '"account"'
check "Portal account auto-generated" "$R" '"portalAccount":"PREPAY01@portal"'
check "Portal password auto-generated" "$R" '"portalPassword":"Sms@PREPAY012024"'

echo ""
echo "=== Test 4: Customer Update ==="
R=$(curl -s $NOPROXY -X PUT "$BASE/admin/customer/2" -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"customerCode":"PREPAY01","customerName":"PrePay Customer A Updated","companyName":"Test Corp Ltd","paymentType":1,"contactName":"Zhang San","contactPhone":"13800138001","contactEmail":"zhangsan@test.com","allowedSmsAttributes":"1,2,3,4","address":"Beijing Chaoyang","remark":"Updated remark"}')
check "Update customer" "$R" '"code":0'

# === Account Management ===
echo ""
echo "=== Test 5: Account - Recharge (Prepaid) ==="
R=$(curl -s $NOPROXY -X POST "$BASE/admin/customer/2/account/recharge" -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"amount":1000,"remark":"QA initial recharge"}')
check "Recharge 1000" "$R" '"code":0'

echo ""
echo "=== Test 6: Account - Check Balance ==="
R=$(curl -s $NOPROXY "$BASE/admin/customer/2/account" -H "$AUTH")
check "Balance shows 1000" "$R" '"balance":1000'

echo ""
echo "=== Test 7: Account - Deduct ==="
R=$(curl -s $NOPROXY -X POST "$BASE/admin/customer/2/account/deduct" -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"amount":50,"remark":"QA test deduct"}')
check "Deduct 50" "$R" '"code":0'

echo ""
echo "=== Test 8: Account - Credit Limit (Postpaid) ==="
if [ -n "$POSTPAY_ID" ]; then
  R=$(curl -s $NOPROXY -X PUT "$BASE/admin/customer/$POSTPAY_ID/account/credit-limit?creditLimit=5000" -H "$AUTH")
  check "Set credit limit 5000" "$R" '"code":0'
fi

# === Country Opening & Price ===
echo ""
echo "=== Test 9: Open Country for Customer ==="
# Open CN (id=36) for PREPAY01
R=$(curl -s $NOPROXY -X POST "$BASE/admin/customer/2/country/enable?countryId=36&countryCode=CN" -H "$AUTH")
check "Open CN for customer" "$R" '"code":0'

# Open US (id=189)
R=$(curl -s $NOPROXY -X POST "$BASE/admin/customer/2/country/enable?countryId=189&countryCode=US" -H "$AUTH")
check "Open US for customer" "$R" '"code":0'

# Open TH (id=176)
R=$(curl -s $NOPROXY -X POST "$BASE/admin/customer/2/country/enable?countryId=176&countryCode=TH" -H "$AUTH")
check "Open TH for customer" "$R" '"code":0'

echo ""
echo "=== Test 10: Set Prices ==="
# CN OTP price
R=$(curl -s $NOPROXY -X POST "$BASE/admin/customer/2/price" -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"countryCode":"CN","smsAttribute":1,"price":0.035,"currency":"USD"}')
check "Set CN OTP price" "$R" '"code":0'

# CN Notification price
R=$(curl -s $NOPROXY -X POST "$BASE/admin/customer/2/price" -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"countryCode":"CN","smsAttribute":3,"price":0.028,"currency":"USD"}')
check "Set CN Notification price" "$R" '"code":0'

# US OTP price
R=$(curl -s $NOPROXY -X POST "$BASE/admin/customer/2/price" -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"countryCode":"US","smsAttribute":1,"price":0.012,"currency":"USD"}')
check "Set US OTP price" "$R" '"code":0'

# TH Marketing price
R=$(curl -s $NOPROXY -X POST "$BASE/admin/customer/2/price" -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"countryCode":"TH","smsAttribute":4,"price":0.045,"currency":"USD"}')
check "Set TH Marketing price" "$R" '"code":0'

echo ""
echo "=== Test 11: List Customer Countries ==="
R=$(curl -s $NOPROXY "$BASE/admin/customer/2/countries" -H "$AUTH")
check "List opened countries" "$R" '"code":0'

echo ""
echo "=== Test 12: List Customer Prices ==="
R=$(curl -s $NOPROXY "$BASE/admin/customer/2/prices" -H "$AUTH")
check "List prices" "$R" '"code":0'

# === API Credentials ===
echo ""
echo "=== Test 13: Create API Credential ==="
R=$(curl -s $NOPROXY -X POST "$BASE/admin/customer/2/credential" -H "$AUTH")
check "Create API credential" "$R" '"apiKey"'
CRED_ID=$(echo "$R" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "  Credential ID: $CRED_ID"

echo ""
echo "=== Test 14: Reset API Secret ==="
if [ -n "$CRED_ID" ]; then
  R=$(curl -s $NOPROXY -X PUT "$BASE/admin/customer/credential/$CRED_ID/reset-secret" -H "$AUTH")
  check "Reset API secret" "$R" '"code":0'
fi

echo ""
echo "=== Test 15: Toggle Credential (disable) ==="
if [ -n "$CRED_ID" ]; then
  R=$(curl -s $NOPROXY -X PUT "$BASE/admin/customer/credential/$CRED_ID/toggle?active=false" -H "$AUTH")
  check "Disable credential" "$R" '"code":0'
fi

echo ""
echo "=== Test 16: Toggle Credential (enable) ==="
if [ -n "$CRED_ID" ]; then
  R=$(curl -s $NOPROXY -X PUT "$BASE/admin/customer/credential/$CRED_ID/toggle?active=true" -H "$AUTH")
  check "Enable credential" "$R" '"code":0'
fi

# === SID Assignment ===
echo ""
echo "=== Test 17: Assign SID to Customer ==="
# Get SID list first
SIDS=$(curl -s $NOPROXY "$BASE/admin/sid/list?page=1&size=50" -H "$AUTH")
SID1=$(echo "$SIDS" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "  First SID ID: $SID1"
if [ -n "$SID1" ]; then
  R=$(curl -s $NOPROXY -X POST "$BASE/admin/customer/2/sid/assign?sidId=$SID1" -H "$AUTH")
  check "Assign SID to customer" "$R" '"code":0'
fi

echo ""
echo "=== Test 18: List Customer SIDs ==="
R=$(curl -s $NOPROXY "$BASE/admin/customer/2/sids" -H "$AUTH")
check "List customer SIDs" "$R" '"code":0'

# === Status Operations ===
echo ""
echo "=== Test 19: Freeze Customer ==="
R=$(curl -s $NOPROXY -X PUT "$BASE/admin/customer/2/status?status=2" -H "$AUTH")
check "Freeze customer (status=2)" "$R" '"code":0'

echo ""
echo "=== Test 20: Unfreeze Customer ==="
R=$(curl -s $NOPROXY -X PUT "$BASE/admin/customer/2/status?status=1" -H "$AUTH")
check "Unfreeze customer (status=1)" "$R" '"code":0'

# === Country Management ===
echo ""
echo "=== Test 21: Country List ==="
R=$(curl -s $NOPROXY "$BASE/admin/country/list?page=1&size=5" -H "$AUTH")
check "Country list returns data" "$R" '"total"'

echo ""
echo "=== Test 22: Vendor List ==="
R=$(curl -s $NOPROXY "$BASE/admin/vendor/list?page=1&size=10" -H "$AUTH")
check "Vendor list returns data" "$R" '"total"'

echo ""
echo "=== Test 23: Channel List ==="
R=$(curl -s $NOPROXY "$BASE/admin/channel/list?page=1&size=10" -H "$AUTH")
check "Channel list returns data" "$R" '"total"'

echo ""
echo "=== Test 24: SID List ==="
R=$(curl -s $NOPROXY "$BASE/admin/sid/list?page=1&size=10" -H "$AUTH")
check "SID list returns data" "$R" '"total"'

# === Dashboard & Stats ===
echo ""
echo "=== Test 25: Dashboard ==="
R=$(curl -s $NOPROXY "$BASE/admin/stats/dashboard" -H "$AUTH")
check "Dashboard returns data" "$R" '"code":0'

# === Message List ===
echo ""
echo "=== Test 26: Message List ==="
R=$(curl -s $NOPROXY "$BASE/admin/message/list?page=1&size=10" -H "$AUTH")
check "Message list" "$R" '"code":0'

# === Risk Management ===
echo ""
echo "=== Test 27: Blacklist ==="
R=$(curl -s $NOPROXY "$BASE/admin/risk/blacklist/list?page=1&size=10" -H "$AUTH")
check "Blacklist list" "$R" '"code":0'

echo ""
echo "=== Test 28: Sensitive Words ==="
R=$(curl -s $NOPROXY "$BASE/admin/risk/sensitive-word/list?page=1&size=10" -H "$AUTH")
check "Sensitive word list" "$R" '"code":0'

# === Duplicate prevention ===
echo ""
echo "=== Test 29: Duplicate Customer Code ==="
R=$(curl -s $NOPROXY -X POST "$BASE/admin/customer" -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"customerCode":"PREPAY01","customerName":"Duplicate","paymentType":1}')
check "Duplicate customer code rejected" "$R" '"code":'
echo "  Response: $R"

# === Disable Country ===
echo ""
echo "=== Test 30: Disable Country for Customer ==="
R=$(curl -s $NOPROXY -X DELETE "$BASE/admin/customer/2/country/disable?countryId=176" -H "$AUTH")
check "Disable TH for customer" "$R" '"code":0'

# === Delete Price ===
echo ""
echo "=== Test 31: List Prices to find ID ==="
PRICES=$(curl -s $NOPROXY "$BASE/admin/customer/2/prices" -H "$AUTH")
PRICE_ID=$(echo "$PRICES" | grep -o '"id":[0-9]*' | tail -1 | cut -d: -f2)
echo "  Last price ID: $PRICE_ID"
if [ -n "$PRICE_ID" ]; then
  R=$(curl -s $NOPROXY -X DELETE "$BASE/admin/customer/price/$PRICE_ID" -H "$AUTH")
  check "Delete price" "$R" '"code":0'
fi

# === Unassign SID ===
echo ""
echo "=== Test 32: Unassign SID ==="
if [ -n "$SID1" ]; then
  R=$(curl -s $NOPROXY -X DELETE "$BASE/admin/customer/2/sid/unassign?sidId=$SID1" -H "$AUTH")
  check "Unassign SID" "$R" '"code":0'
fi

# === Summary ===
echo ""
echo "========================================"
echo "  QA Test Summary"
echo "  PASS: $pass"
echo "  FAIL: $fail"
echo "  TOTAL: $((pass + fail))"
echo "========================================"
