#!/usr/bin/env python3
"""Deploy SMS platform to 119.29.128.12"""
import paramiko
import os, sys, tarfile, time

HOST = "119.29.128.12"
ROOT_PASS = "_}V%n=J[ks?V|!"
DB_ROOT_PASS = "BwhhNA3pw!mQ},"
DB_USER = "zhangu_sms"
DB_PASS = "JVEVwpY#EHS6M"
DB_NAME = "sms_platform"
REMOTE_DIR = "/opt/sms"
ADMIN_PORT = 5800   # sms-ui (admin frontend)
PORTAL_PORT = 5801  # sms-portal (customer portal)
BACKEND_PORT = 5802  # Spring Boot backend (internal)
JWT_SECRET = "BoRuiSmsPlatformJwtSecretKey2024MustBe32BytesLong!"

BASE = "D:/boruiworld-m800/sms-platform"

def ssh_connect(host, user, password):
    c = paramiko.SSHClient()
    c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    c.connect(host, username=user, password=password, timeout=30)
    return c

def run(client, cmd, timeout=120):
    print(f"  $ {cmd[:100]}")
    stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode(errors='replace').strip()
    err = stderr.read().decode(errors='replace').strip()
    rc = stdout.channel.recv_exit_status()
    if out: print(f"    {out[:500]}")
    if err and rc != 0: print(f"    ERR: {err[:300]}")
    return rc, out, err

def upload(sftp, local, remote):
    print(f"  upload {os.path.basename(local)} -> {remote}")
    sftp.put(local, remote)

def upload_dir(sftp, local_dir, remote_dir):
    """Upload a directory recursively"""
    run_cmd_ignore = lambda c, r: None
    for root, dirs, files in os.walk(local_dir):
        rel = os.path.relpath(root, local_dir).replace("\\", "/")
        rdir = remote_dir if rel == "." else f"{remote_dir}/{rel}"
        try: sftp.mkdir(rdir)
        except: pass
        for f in files:
            lf = os.path.join(root, f)
            rf = f"{rdir}/{f}"
            sftp.put(lf, rf)

print("=" * 60)
print("  SMS Platform Deploy to 119.29.128.12")
print("=" * 60)

# Connect
print("\n[1/8] Connecting to server...")
client = ssh_connect(HOST, "root", ROOT_PASS)
sftp = client.open_sftp()
print("  Connected OK")

# Install dependencies
print("\n[2/8] Installing Java 17 & Nginx...")
run(client, "which java || (apt-get update -qq && apt-get install -y -qq openjdk-17-jre-headless)", 300)
run(client, "which nginx || apt-get install -y -qq nginx", 120)
run(client, "java -version 2>&1 | head -1")

# Setup directories
print("\n[3/8] Setting up directories...")
run(client, f"mkdir -p {REMOTE_DIR}/backend {REMOTE_DIR}/ui {REMOTE_DIR}/portal {REMOTE_DIR}/logs")

# Setup MySQL database
print("\n[4/8] Setting up database...")
sql = f"""
CREATE DATABASE IF NOT EXISTS {DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '{DB_USER}'@'localhost' IDENTIFIED BY '{DB_PASS}';
GRANT ALL PRIVILEGES ON {DB_NAME}.* TO '{DB_USER}'@'localhost';
FLUSH PRIVILEGES;
"""
run(client, f'mysql -u root -p\'{DB_ROOT_PASS}\' -e "{sql.strip()}"', 30)

# Upload and run SQL migrations
print("  Uploading SQL migrations...")
doc_dir = f"{BASE}/doc"
sql_files = sorted([f for f in os.listdir(doc_dir) if f.endswith('.sql')])
for sf in sql_files:
    local_sql = os.path.join(doc_dir, sf)
    remote_sql = f"/tmp/{sf}"
    sftp.put(local_sql, remote_sql)
    rc, out, err = run(client, f'mysql -u root -p\'{DB_ROOT_PASS}\' {DB_NAME} < {remote_sql} 2>&1 | head -5')
    # Ignore errors (table already exists etc.)

# Upload backend JAR
print("\n[5/8] Uploading backend JAR...")
jar_path = f"{BASE}/sms-admin/target/sms-admin-2.0.0-SNAPSHOT.jar"
upload(sftp, jar_path, f"{REMOTE_DIR}/backend/sms-admin.jar")

# Create systemd service for backend
print("\n[6/8] Configuring backend service...")
service = f"""[Unit]
Description=SMS Admin Backend
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory={REMOTE_DIR}/backend
Environment="DB_HOST=localhost"
Environment="DB_PORT=3306"
Environment="DB_NAME={DB_NAME}"
Environment="DB_USER={DB_USER}"
Environment="DB_PASSWORD={DB_PASS}"
Environment="JWT_SECRET={JWT_SECRET}"
ExecStart=/usr/bin/java -jar {REMOTE_DIR}/backend/sms-admin.jar --spring.profiles.active=prod --server.port={BACKEND_PORT}
Restart=always
RestartSec=10
StandardOutput=append:{REMOTE_DIR}/logs/backend.log
StandardError=append:{REMOTE_DIR}/logs/backend.log

[Install]
WantedBy=multi-user.target
"""
# Write service file
stdin, stdout, stderr = client.exec_command(f"cat > /etc/systemd/system/sms-backend.service")
stdin.write(service)
stdin.channel.shutdown_write()
stdout.read()

run(client, "systemctl daemon-reload")
run(client, "systemctl enable sms-backend")
run(client, "systemctl restart sms-backend")

# Upload frontend dist
print("\n[7/8] Uploading frontend files...")
print("  Uploading admin UI (sms-ui)...")
upload_dir(sftp, f"{BASE}/sms-ui/dist", f"{REMOTE_DIR}/ui")
print("  Uploading customer portal (sms-portal)...")
upload_dir(sftp, f"{BASE}/sms-portal/dist", f"{REMOTE_DIR}/portal")

# Configure Nginx
print("\n[8/8] Configuring Nginx...")
nginx_conf = f"""server {{
    listen {ADMIN_PORT};
    server_name _;
    root {REMOTE_DIR}/ui;
    index index.html;

    location / {{
        try_files $uri $uri/ /index.html;
    }}

    location /v1/ {{
        proxy_pass http://127.0.0.1:{BACKEND_PORT};
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 60s;
    }}
}}

server {{
    listen {PORTAL_PORT};
    server_name _;
    root {REMOTE_DIR}/portal;
    index index.html;

    location / {{
        try_files $uri $uri/ /index.html;
    }}

    location /v1/ {{
        proxy_pass http://127.0.0.1:{BACKEND_PORT};
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 60s;
    }}
}}
"""
stdin, stdout, stderr = client.exec_command("cat > /etc/nginx/conf.d/sms.conf")
stdin.write(nginx_conf)
stdin.channel.shutdown_write()
stdout.read()

run(client, "nginx -t 2>&1")
run(client, "systemctl enable nginx && systemctl restart nginx")

# Open firewall ports
print("\n  Opening firewall ports...")
run(client, f"ufw allow {ADMIN_PORT}/tcp 2>/dev/null || iptables -I INPUT -p tcp --dport {ADMIN_PORT} -j ACCEPT 2>/dev/null; true")
run(client, f"ufw allow {PORTAL_PORT}/tcp 2>/dev/null || iptables -I INPUT -p tcp --dport {PORTAL_PORT} -j ACCEPT 2>/dev/null; true")

# Check status
print("\n[OK] Checking services...")
time.sleep(5)
run(client, "systemctl is-active sms-backend && echo 'Backend: running' || echo 'Backend: starting...'")
run(client, "systemctl is-active nginx && echo 'Nginx: running'")
run(client, f"ss -tlnp | grep -E '{ADMIN_PORT}|{PORTAL_PORT}|{BACKEND_PORT}'")

sftp.close()
client.close()

print("\n" + "=" * 60)
print(f"  Deploy complete!")
print(f"  Admin panel  : http://119.29.128.12:{ADMIN_PORT}")
print(f"  Customer portal: http://119.29.128.12:{PORTAL_PORT}")
print(f"  Backend API  : http://127.0.0.1:{BACKEND_PORT} (internal)")
print("=" * 60)
