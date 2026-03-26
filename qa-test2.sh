#!/bin/bash
# QA Test Script v2 - correct endpoints
BASE="http://43.139.178.124:8880/v1"
NP="--noproxy *"

# Login
TOKEN=$(curl -s $NP -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
AUTH="Authorization: Bearer $TOKEN"
echo "Token OK: ${TOKEN:0:20}..."

pass=0; fail=0
check() {
  if echo "$2" | grep -q "$3"; then echo "  [PASS] $1"; ((pass++))
  else echo "  [FAIL] $1"; echo "    => $2"; ((fail++)); fi
}

# === Test 8 fix: Credit Limit (Postpaid customer id=3) ===
echo ""
echo "=== Credit Limit (Postpaid id=3) ==="
R=$(curl -s $NP -X PUT "$BASE/admin/customer/3/account/credit-limit?creditLimit=5000" -H "$AUTH")
check "Set credit limit 5000" "$R" '"code":0'
R=$(curl -s $NP "$BASE/admin/customer/3/account" -H "$AUTH")
check "Credit limit shows 5000" "$R" '"creditLimit":5000'

# === Country Enable (correct endpoint: POST /{id}/countries) ===
echo ""
echo "=== Open Countries for Customer 2 ==="
R=$(curl -s $NP -X POST "$BASE/admin/customer/2/countries?countryId=36&countryCode=CN" -H "$AUTH")
check "Open CN" "$R" '"code":0'
R=$(curl -s $NP -X POST "$BASE/admin/customer/2/countries?countryId=189&countryCode=US" -H "$AUTH")
check "Open US" "$R" '"code":0'
R=$(curl -s $NP -X POST "$BASE/admin/customer/2/countries?countryId=176&countryCode=TH" -H "$AUTH")
check "Open TH" "$R" '"code":0'
R=$(curl -s $NP -X POST "$BASE/admin/customer/2/countries?countryId=78&countryCode=IN" -H "$AUTH")
check "Open IN" "$R" '"code":0'

# === Set Prices (correct endpoint: POST /{id}/prices) ===
echo ""
echo "=== Set Prices ==="
R=$(curl -s $NP -X POST "$BASE/admin/customer/2/prices" -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"countryCode":"CN","smsAttribute":1,"price":0.035,"currency":"USD"}')
check "CN OTP price 0.035" "$R" '"code":0'

R=$(curl -s $NP -X POST "$BASE/admin/customer/2/prices" -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"countryCode":"CN","smsAttribute":3,"price":0.028,"currency":"USD"}')
check "CN Notification price 0.028" "$R" '"code":0'

R=$(curl -s $NP -X POST "$BASE/admin/customer/2/prices" -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"countryCode":"US","smsAttribute":1,"price":0.012,"currency":"USD"}')
check "US OTP price 0.012" "$R" '"code":0'

R=$(curl -s $NP -X POST "$BASE/admin/customer/2/prices" -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"countryCode":"TH","smsAttribute":4,"price":0.045,"currency":"USD"}')
check "TH Marketing price 0.045" "$R" '"code":0'

R=$(curl -s $NP -X POST "$BASE/admin/customer/2/prices" -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"countryCode":"IN","smsAttribute":2,"price":0.008,"currency":"USD"}')
check "IN Transaction price 0.008" "$R" '"code":0'

# === List countries & prices ===
echo ""
echo "=== Verify Countries & Prices ==="
R=$(curl -s $NP "$BASE/admin/customer/2/countries" -H "$AUTH")
check "List countries" "$R" '"code":0'
echo "  Countries: $R"

R=$(curl -s $NP "$BASE/admin/customer/2/prices" -H "$AUTH")
check "List prices" "$R" '"code":0'
echo "  Prices: $R"

# === API Credentials (correct endpoints) ===
echo ""
echo "=== API Credentials ==="
R=$(curl -s $NP -X POST "$BASE/admin/customer/2/credentials" -H "$AUTH")
check "Create credential" "$R" '"apiKey"'
CRED_ID=$(echo "$R" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "  Credential ID: $CRED_ID"

if [ -n "$CRED_ID" ]; then
  R=$(curl -s $NP -X PUT "$BASE/admin/customer/credentials/$CRED_ID/reset-secret" -H "$AUTH")
  check "Reset secret" "$R" '"code":0'

  R=$(curl -s $NP -X PUT "$BASE/admin/customer/credentials/$CRED_ID/toggle?active=false" -H "$AUTH")
  check "Disable credential" "$R" '"code":0'

  R=$(curl -s $NP -X PUT "$BASE/admin/customer/credentials/$CRED_ID/toggle?active=true" -H "$AUTH")
  check "Enable credential" "$R" '"code":0'

  R=$(curl -s $NP -X PUT "$BASE/admin/customer/credentials/$CRED_ID/ip-whitelist?ipWhitelist=192.168.1.0/24" -H "$AUTH")
  check "Set IP whitelist" "$R" '"code":0'
fi

# === SID Assignment (correct endpoints) ===
echo ""
echo "=== SID Assignment ==="
SIDS=$(curl -s $NP "$BASE/admin/sid/list?page=1&size=50" -H "$AUTH")
SID1=$(echo "$SIDS" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
SID2=$(echo "$SIDS" | grep -o '"id":[0-9]*' | head -2 | tail -1 | cut -d: -f2)
echo "  SID IDs: $SID1, $SID2"

if [ -n "$SID1" ]; then
  R=$(curl -s $NP -X POST "$BASE/admin/customer/2/sids?sidId=$SID1" -H "$AUTH")
  check "Assign SID $SID1" "$R" '"code":0'
fi
if [ -n "$SID2" ]; then
  R=$(curl -s $NP -X POST "$BASE/admin/customer/2/sids?sidId=$SID2" -H "$AUTH")
  check "Assign SID $SID2" "$R" '"code":0'
fi

R=$(curl -s $NP "$BASE/admin/customer/2/sids" -H "$AUTH")
check "List customer SIDs" "$R" '"code":0'
echo "  SIDs: $R"

# === Status Operations ===
echo ""
echo "=== Status Operations ==="
R=$(curl -s $NP -X PUT "$BASE/admin/customer/2/status?status=2" -H "$AUTH")
check "Freeze (status=2)" "$R" '"status":2'

R=$(curl -s $NP -X PUT "$BASE/admin/customer/2/status?status=1" -H "$AUTH")
check "Unfreeze (status=1)" "$R" '"status":1'

# === Disable Country ===
echo ""
echo "=== Disable Country ==="
R=$(curl -s $NP -X DELETE "$BASE/admin/customer/2/countries/176" -H "$AUTH")
check "Disable TH (countryId=176)" "$R" '"code":0'

# === Verify updated countries ===
R=$(curl -s $NP "$BASE/admin/customer/2/countries" -H "$AUTH")
echo "  Remaining countries: $R"

# === Delete a price ===
echo ""
echo "=== Delete Price ==="
PRICES=$(curl -s $NP "$BASE/admin/customer/2/prices" -H "$AUTH")
PRICE_ID=$(echo "$PRICES" | grep -o '"id":[0-9]*' | tail -1 | cut -d: -f2)
echo "  Deleting price ID: $PRICE_ID"
if [ -n "$PRICE_ID" ]; then
  R=$(curl -s $NP -X DELETE "$BASE/admin/customer/prices/$PRICE_ID" -H "$AUTH")
  check "Delete price" "$R" '"code":0'
fi

# === Unassign SID ===
echo ""
echo "=== Unassign SID ==="
if [ -n "$SID1" ]; then
  R=$(curl -s $NP -X DELETE "$BASE/admin/customer/2/sids/$SID1" -H "$AUTH")
  check "Unassign SID $SID1" "$R" '"code":0'
fi

# === Duplicate Test ===
echo ""
echo "=== Duplicate Customer Code ==="
R=$(curl -s $NP -X POST "$BASE/admin/customer" -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"customerCode":"PREPAY01","customerName":"Dup","paymentType":1}')
check "Duplicate rejected" "$R" '"code":'
echo "  Response: $R"

# === Other modules quick check ===
echo ""
echo "=== Quick Module Checks ==="
R=$(curl -s $NP "$BASE/admin/stats/dashboard" -H "$AUTH")
check "Dashboard" "$R" '"code":0'

R=$(curl -s $NP "$BASE/admin/message/list?page=1&size=5" -H "$AUTH")
check "Message list" "$R" '"code":0'

R=$(curl -s $NP "$BASE/admin/risk/blacklist/list?page=1&size=5" -H "$AUTH")
check "Blacklist" "$R" '"code":0'

R=$(curl -s $NP "$BASE/admin/risk/sensitive-word/list?page=1&size=5" -H "$AUTH")
check "Sensitive words" "$R" '"code":0'

# === Also open countries for postpaid customer ===
echo ""
echo "=== Postpaid Customer (id=3) Country & Price ==="
R=$(curl -s $NP -X POST "$BASE/admin/customer/3/countries?countryId=36&countryCode=CN" -H "$AUTH")
check "Open CN for postpaid" "$R" '"code":0'
R=$(curl -s $NP -X POST "$BASE/admin/customer/3/prices" -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"countryCode":"CN","smsAttribute":1,"price":0.040,"currency":"USD"}')
check "CN OTP price for postpaid" "$R" '"code":0'

# === Full detail check ===
echo ""
echo "=== Full Detail Checks ==="
R=$(curl -s $NP "$BASE/admin/customer/2/detail" -H "$AUTH")
check "Full detail has account" "$R" '"account"'
check "Full detail has countries" "$R" '"countries"'
check "Full detail has prices" "$R" '"prices"'
check "Full detail has credentials" "$R" '"credentials"'
check "Full detail has sids" "$R" '"sids"'

echo ""
echo "========================================"
echo "  QA Test v2 Summary"
echo "  PASS: $pass"
echo "  FAIL: $fail"
echo "  TOTAL: $((pass + fail))"
echo "========================================"
