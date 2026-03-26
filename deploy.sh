#!/bin/bash
# ============================================
# SMS Platform - One-click Deploy Script
# Usage: bash deploy.sh user@server_ip
# ============================================
set -e

# --- Config ---
REMOTE=$1
REMOTE_DIR="/opt/sms-platform"
PACK_NAME="sms-deploy.tar.gz"

if [ -z "$REMOTE" ]; then
  echo "Usage: bash deploy.sh user@server_ip"
  echo "Example: bash deploy.sh root@192.168.1.100"
  exit 1
fi

echo "========================================"
echo "  SMS Platform Deploy"
echo "  Target: $REMOTE"
echo "========================================"

# --- Step 1: Build backend JAR ---
echo ""
echo "[1/5] Building backend JAR..."
mvn clean package -DskipTests -q
echo "  -> sms-admin JAR built OK"

# --- Step 2: Pack deploy files ---
echo ""
echo "[2/5] Packing deploy files..."
tar czf $PACK_NAME \
  docker-compose.yml \
  .env.example \
  doc/01-schema.sql \
  doc/02-schema-risk.sql \
  doc/03-schema-auth.sql \
  doc/04-country_data.sql \
  doc/05-schema-sprint8.sql \
  doc/06-add-customer-fields.sql \
  doc/07-add-portal-account.sql \
  doc/08-fix-sensitive-word.sql \
  sms-admin/Dockerfile \
  sms-admin/target/sms-admin-2.0.0-SNAPSHOT.jar \
  sms-ui/Dockerfile \
  sms-ui/nginx.conf \
  sms-ui/package.json \
  sms-ui/package-lock.json \
  sms-ui/vite.config.js \
  sms-ui/index.html \
  sms-ui/public/ \
  sms-ui/src/

PACK_SIZE=$(du -h $PACK_NAME | cut -f1)
echo "  -> $PACK_NAME ($PACK_SIZE)"

# --- Step 3: Upload to server ---
echo ""
echo "[3/5] Uploading to $REMOTE:$REMOTE_DIR ..."
ssh $REMOTE "mkdir -p $REMOTE_DIR"
scp $PACK_NAME $REMOTE:$REMOTE_DIR/
echo "  -> Upload done"

# --- Step 4: Extract & configure on server ---
echo ""
echo "[4/5] Extracting on server..."
ssh $REMOTE "cd $REMOTE_DIR && tar xzf $PACK_NAME && rm -f $PACK_NAME"

# create .env if not exists
ssh $REMOTE "cd $REMOTE_DIR && if [ ! -f .env ]; then
  JWT_SECRET=\$(openssl rand -base64 48)
  DB_PWD=\$(openssl rand -base64 16)
  cat > .env << ENVEOF
DB_PASSWORD=\$DB_PWD
JWT_SECRET=\$JWT_SECRET
JWT_EXPIRE_HOURS=24
WEB_PORT=8880
ENVEOF
  echo '  -> .env created with random secrets'
else
  echo '  -> .env already exists, keeping current config'
fi"

# --- Step 5: Docker compose up ---
echo ""
echo "[5/5] Starting services on server..."
ssh $REMOTE "cd $REMOTE_DIR && docker compose down 2>/dev/null; docker compose up -d --build"

echo ""
echo "========================================"
echo "  Deploy complete!"
echo "  Access: http://${REMOTE#*@}:8880"
echo "  Login:  admin / admin123"
echo "========================================"
echo ""
echo "Useful commands (run on server):"
echo "  cd $REMOTE_DIR"
echo "  docker compose ps        # check status"
echo "  docker compose logs -f   # view logs"
echo "  docker compose down      # stop all"

# cleanup local
rm -f $PACK_NAME
