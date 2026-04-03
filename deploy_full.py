#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Full deployment to 119.29.128.12 - handles Java 17 via China mirrors"""
import paramiko, time, os, sys

HOST = "119.29.128.12"
ROOT_PASS = "_}V%n=J[ks?V|!"
DB_ROOT_PASS = "BwhhNA3pw!mQ},"
DB_USER = "zhangu_sms"
DB_PASS = "JVEVwpY#EHS6M"
DB_NAME = "sms_platform"
MYSQL = "/usr/local/mysql-8.0.37/bin/mysql"
MYSQL_SOCK = "/tmp/mysql.sock"
DB_PORT = "23308"
REMOTE_DIR = "/opt/sms"
ADMIN_PORT = "5800"
PORTAL_PORT = "5801"
BACKEND_PORT = "5802"
JWT_SECRET = "BoRuiSmsPlatformJwtSecretKey2024MustBe32BytesLong!"
BASE = "D:/boruiworld-m800/sms-platform"

print("=" * 60)
print("  SMS Platform Deploy  ->  119.29.128.12")
print("=" * 60)

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, username="root", password=ROOT_PASS, timeout=30)
sftp = client.open_sftp()

def run(cmd, timeout=120):
    print("  $ " + cmd[:130])
    try:
        stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
        out = stdout.read().decode("utf-8", errors="replace").strip()
        err = stderr.read().decode("utf-8", errors="replace").strip()
        rc = stdout.channel.recv_exit_status()
    except Exception as e:
        print("    [TIMEOUT] " + str(e)[:80])
        return 1, "", str(e)
    result = out or err
    if result:
        print("    => " + result[:300])
    return rc, out, err

MYSQL_CMD = MYSQL + " -S " + MYSQL_SOCK + " -u root -p'" + DB_ROOT_PASS + "'"

# -------------------------------------------------------
# Step 1: Ensure MySQL is running
# -------------------------------------------------------
print("\n[1/7] Starting MySQL...")
rc, out, _ = run(MYSQL_CMD + " -e 'SELECT 1;' 2>&1")
if "ERROR" in (out or "ERROR"):
    run("/usr/local/mysql-8.0.37/support-files/mysql.server start 2>&1 | tail -3", 30)
    time.sleep(5)
run(MYSQL_CMD + " -e 'SELECT VERSION();'")

# -------------------------------------------------------
# Step 2: Setup database + migrations
# -------------------------------------------------------
print("\n[2/7] Setting up database...")
sqls = [
    "CREATE DATABASE IF NOT EXISTS " + DB_NAME + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
    "CREATE USER IF NOT EXISTS '" + DB_USER + "'@'localhost' IDENTIFIED BY '" + DB_PASS + "'",
    "CREATE USER IF NOT EXISTS '" + DB_USER + "'@'127.0.0.1' IDENTIFIED BY '" + DB_PASS + "'",
    "GRANT ALL PRIVILEGES ON " + DB_NAME + ".* TO '" + DB_USER + "'@'localhost'",
    "GRANT ALL PRIVILEGES ON " + DB_NAME + ".* TO '" + DB_USER + "'@'127.0.0.1'",
    "FLUSH PRIVILEGES",
]
for sql in sqls:
    run(MYSQL_CMD + " -e \"" + sql + ";\" 2>&1 | grep -v Warning")

print("  Uploading + running SQL migrations...")
doc_dir = BASE + "/doc"
sql_files = sorted([f for f in os.listdir(doc_dir) if f.endswith(".sql")])
for sf in sql_files:
    remote_sql = "/tmp/" + sf
    try:
        sftp.put(os.path.join(doc_dir, sf), remote_sql)
    except:
        pass
    run(MYSQL_CMD + " " + DB_NAME + " < " + remote_sql
        + " 2>&1 | grep -iE 'error' | grep -v '1060\\|1062\\|1050\\|1061\\|Duplicate' | head -2")

# -------------------------------------------------------
# Step 3: Install Java 17
# -------------------------------------------------------
print("\n[3/7] Installing Java 17...")
rc, java17, _ = run(
    "find /usr/local -maxdepth 4 -name 'java' -path '*/bin/java' 2>/dev/null"
    " | xargs -I{} sh -c '{} -version 2>&1 | grep -q 17 && echo {}' 2>/dev/null | head -1"
)

if not java17:
    print("  Trying China mirrors...")
    # Try Alibaba Dragonwell 17 (OSS CDN, good China access)
    dl_urls = [
        # Alibaba Dragonwell 17
        "https://dragonwell-jdk.oss-cn-hangzhou.aliyuncs.com/17/17.0.12.0.13+9/Alibaba_Dragonwell_Standard_17.0.12.0.13%2B9_x64_linux.tar.gz",
        # Tencent Kona 17
        "https://github.com/Tencent/TencentKona-17/releases/download/TencentKona-17.0.11/TencentKona17.0.11.b1_jdk_linux-x86_64_17.0.11.tar.gz",
        # Amazon Corretto 17
        "https://corretto.aws/downloads/latest/amazon-corretto-17-x64-linux-jdk.tar.gz",
    ]
    for url in dl_urls:
        print("  Trying: " + url[:70] + "...")
        run("rm -f /tmp/jdk17.tar.gz")
        run("wget -q --no-check-certificate --timeout=90 --tries=1 '"
            + url + "' -O /tmp/jdk17.tar.gz 2>&1 | tail -2", 120)
        rc2, size, _ = run("stat -c%s /tmp/jdk17.tar.gz 2>/dev/null || echo 0")
        try:
            sz = int(size.strip())
        except:
            sz = 0
        if sz > 50000000:
            print("  Downloaded: " + str(sz // 1024 // 1024) + " MB")
            run("tar -xzf /tmp/jdk17.tar.gz -C /usr/local/ 2>&1 | head -3", 60)
            rc3, java17, _ = run(
                "find /usr/local -maxdepth 4 -name 'java' -path '*/bin/java' 2>/dev/null"
                " | xargs -I{} sh -c '{} -version 2>&1 | grep -q 17 && echo {}' 2>/dev/null | head -1"
            )
            if java17:
                break
        else:
            print("  Download failed/incomplete (" + str(sz) + " bytes)")

if not java17:
    print("  All downloads failed - checking yum alternatives...")
    run("yum install -y java-11-openjdk-headless 2>&1 | tail -3", 180)
    rc, java17, _ = run("alternatives --list 2>/dev/null | grep 'java ' | awk '{print $3}' | head -1")

print("  Java path: " + (java17 or "NOT FOUND"))

# -------------------------------------------------------
# Step 4: Upload backend JAR
# -------------------------------------------------------
print("\n[4/7] Uploading backend JAR (~80MB)...")
jar_local = BASE + "/sms-admin/target/sms-admin-2.0.0-SNAPSHOT.jar"
jar_remote = REMOTE_DIR + "/backend/sms-admin.jar"
run("mkdir -p " + REMOTE_DIR + "/backend " + REMOTE_DIR + "/logs")
sz = os.path.getsize(jar_local)
print("  Size: " + str(sz // 1024 // 1024) + " MB, uploading...")
sftp.put(jar_local, jar_remote)
print("  Upload done")

# -------------------------------------------------------
# Step 5: Write env + service files
# -------------------------------------------------------
print("\n[5/7] Writing config files...")
java_bin = java17 if java17 else "/usr/local/jdk1.8.0_211/bin/java"

env_content = (
    "DB_HOST=127.0.0.1\n"
    "DB_PORT=" + DB_PORT + "\n"
    "DB_NAME=" + DB_NAME + "\n"
    "DB_USER=" + DB_USER + "\n"
    "DB_PASSWORD=" + DB_PASS + "\n"
    "JWT_SECRET=" + JWT_SECRET + "\n"
)
with sftp.open(REMOTE_DIR + "/backend/app.env", "w") as f:
    f.write(env_content)

svc = (
    "[Unit]\n"
    "Description=SMS Admin Backend\n"
    "After=network.target\n\n"
    "[Service]\n"
    "Type=simple\n"
    "User=root\n"
    "WorkingDirectory=" + REMOTE_DIR + "/backend\n"
    "EnvironmentFile=" + REMOTE_DIR + "/backend/app.env\n"
    "ExecStart=" + java_bin + " -jar " + REMOTE_DIR + "/backend/sms-admin.jar"
    " --spring.profiles.active=prod --server.port=" + BACKEND_PORT + "\n"
    "Restart=always\n"
    "RestartSec=15\n"
    "StandardOutput=append:" + REMOTE_DIR + "/logs/backend.log\n"
    "StandardError=append:" + REMOTE_DIR + "/logs/backend.log\n\n"
    "[Install]\n"
    "WantedBy=multi-user.target\n"
)
with sftp.open("/etc/systemd/system/sms-backend.service", "w") as f:
    f.write(svc)

mysql_svc = (
    "[Unit]\n"
    "Description=MySQL 8\n"
    "After=network.target\n\n"
    "[Service]\n"
    "Type=forking\n"
    "ExecStart=/usr/local/mysql-8.0.37/support-files/mysql.server start\n"
    "ExecStop=/usr/local/mysql-8.0.37/support-files/mysql.server stop\n"
    "Restart=on-failure\n\n"
    "[Install]\n"
    "WantedBy=multi-user.target\n"
)
with sftp.open("/etc/systemd/system/mysql8.service", "w") as f:
    f.write(mysql_svc)

# -------------------------------------------------------
# Step 6: Upload frontend dist + Nginx config
# -------------------------------------------------------
print("\n[6/7] Uploading frontend files...")
def upload_dir(local_dir, remote_dir):
    for root, dirs, files in os.walk(local_dir):
        rel = os.path.relpath(root, local_dir).replace("\\", "/")
        rdir = remote_dir if rel == "." else remote_dir + "/" + rel
        try:
            sftp.mkdir(rdir)
        except:
            pass
        for fn in files:
            sftp.put(os.path.join(root, fn), rdir + "/" + fn)

run("rm -rf " + REMOTE_DIR + "/ui " + REMOTE_DIR + "/portal")
run("mkdir -p " + REMOTE_DIR + "/ui " + REMOTE_DIR + "/portal")
print("  Uploading sms-ui dist...")
upload_dir(BASE + "/sms-ui/dist", REMOTE_DIR + "/ui")
print("  Uploading sms-portal dist...")
upload_dir(BASE + "/sms-portal/dist", REMOTE_DIR + "/portal")

nginx_conf = (
    "server {\n"
    "    listen " + ADMIN_PORT + ";\n"
    "    server_name _;\n"
    "    root " + REMOTE_DIR + "/ui;\n"
    "    index index.html;\n\n"
    "    location / {\n"
    "        try_files $uri $uri/ /index.html;\n"
    "    }\n\n"
    "    location /v1/ {\n"
    "        proxy_pass http://127.0.0.1:" + BACKEND_PORT + ";\n"
    "        proxy_set_header Host $host;\n"
    "        proxy_set_header X-Real-IP $remote_addr;\n"
    "        proxy_read_timeout 60s;\n"
    "    }\n"
    "}\n\n"
    "server {\n"
    "    listen " + PORTAL_PORT + ";\n"
    "    server_name _;\n"
    "    root " + REMOTE_DIR + "/portal;\n"
    "    index index.html;\n\n"
    "    location / {\n"
    "        try_files $uri $uri/ /index.html;\n"
    "    }\n\n"
    "    location /v1/ {\n"
    "        proxy_pass http://127.0.0.1:" + BACKEND_PORT + ";\n"
    "        proxy_set_header Host $host;\n"
    "        proxy_set_header X-Real-IP $remote_addr;\n"
    "        proxy_read_timeout 60s;\n"
    "    }\n"
    "}\n"
)
with sftp.open("/etc/nginx/conf.d/sms.conf", "w") as f:
    f.write(nginx_conf)

run("nginx -t 2>&1")
run("systemctl reload nginx 2>/dev/null || systemctl restart nginx")

# -------------------------------------------------------
# Step 7: Start all services
# -------------------------------------------------------
print("\n[7/7] Starting services...")
run("systemctl daemon-reload")
run("systemctl enable mysql8 sms-backend nginx")
run("systemctl restart sms-backend")
time.sleep(20)

run("systemctl status sms-backend 2>&1 | head -12")
run("tail -30 " + REMOTE_DIR + "/logs/backend.log 2>/dev/null | tail -15")
run("ss -tlnp | grep -E '5800|5801|5802'")

# Open ports
run("iptables -I INPUT -p tcp --dport 5800 -j ACCEPT 2>/dev/null; true")
run("iptables -I INPUT -p tcp --dport 5801 -j ACCEPT 2>/dev/null; true")

sftp.close()
client.close()

print("\n" + "=" * 60)
print("  DEPLOY COMPLETE")
print("  Admin panel    : http://119.29.128.12:5800")
print("  Customer portal: http://119.29.128.12:5801")
print("=" * 60)
