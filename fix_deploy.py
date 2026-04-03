#!/usr/bin/env python
# -*- coding: utf-8 -*-
import paramiko, time, os

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
BACKEND_PORT = "5802"
JWT_SECRET = "BoRuiSmsPlatformJwtSecretKey2024MustBe32BytesLong!"
BASE = "D:/boruiworld-m800/sms-platform"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, username="root", password=ROOT_PASS, timeout=30)

def run(cmd, timeout=120):
    print("  $ " + cmd[:130])
    try:
        stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
        out = stdout.read().decode("utf-8", errors="replace").strip()
        err = stderr.read().decode("utf-8", errors="replace").strip()
        rc = stdout.channel.recv_exit_status()
    except Exception as e:
        print("    TIMEOUT/ERR: " + str(e))
        return 1, "", str(e)
    result = out or err
    if result:
        print("    => " + result[:300])
    return rc, out, err

MYSQL_CMD = MYSQL + " -S " + MYSQL_SOCK + " -u root -p'" + DB_ROOT_PASS + "'"

# ---- Step 1: Start MySQL ----
print("\n[1/5] Starting MySQL...")
run("/usr/local/mysql-8.0.37/support-files/mysql.server start", 60)
time.sleep(5)
run(MYSQL_CMD + " -e 'SELECT VERSION();'")

# ---- Step 2: Setup DB ----
print("\n[2/5] Setup database...")
sqls = [
    "CREATE DATABASE IF NOT EXISTS " + DB_NAME + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
    "CREATE USER IF NOT EXISTS '" + DB_USER + "'@'localhost' IDENTIFIED BY '" + DB_PASS + "'",
    "CREATE USER IF NOT EXISTS '" + DB_USER + "'@'127.0.0.1' IDENTIFIED BY '" + DB_PASS + "'",
    "GRANT ALL PRIVILEGES ON " + DB_NAME + ".* TO '" + DB_USER + "'@'localhost'",
    "GRANT ALL PRIVILEGES ON " + DB_NAME + ".* TO '" + DB_USER + "'@'127.0.0.1'",
    "FLUSH PRIVILEGES",
]
for sql in sqls:
    run(MYSQL_CMD + " -e \"" + sql + ";\"")

print("  Running SQL migrations...")
rc, files, _ = run("ls /tmp/*.sql 2>/dev/null | sort")
for sf in files.split('\n'):
    sf = sf.strip()
    if sf:
        run(MYSQL_CMD + " " + DB_NAME + " < " + sf + " 2>&1 | grep -i error | grep -v '1060\\|1062\\|1050\\|1061' | head -2")

# ---- Step 3: Install Java 17 ----
print("\n[3/5] Installing Java 17...")
rc, out, _ = run("ls /usr/local/ | grep 'jdk-17\\|jdk17\\|temurin'")
if not out:
    print("  Downloading Java 17 from Adoptium...")
    dl_url = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_linux_hotspot_17.0.13_11.tar.gz"
    run("wget -q --no-check-certificate '" + dl_url + "' -O /tmp/jdk17.tar.gz 2>&1 | tail -3", 300)
    run("ls -lh /tmp/jdk17.tar.gz")
    run("tar -xzf /tmp/jdk17.tar.gz -C /usr/local/ 2>&1", 60)

rc, java17, _ = run("find /usr/local -maxdepth 3 -name 'java' -path '*/bin/java' 2>/dev/null | xargs -I{} sh -c '{} -version 2>&1 | grep -q 17 && echo {}' 2>/dev/null | head -1")
if not java17:
    java17 = "/usr/local/jdk1.8.0_211/bin/java"
    print("  WARNING: Using Java 8 fallback")
print("  Java: " + java17)

# ---- Step 4: Write service + env files ----
print("\n[4/5] Writing service config...")
sftp = client.open_sftp()

env_content = (
    "DB_HOST=127.0.0.1\n"
    "DB_PORT=" + DB_PORT + "\n"
    "DB_NAME=" + DB_NAME + "\n"
    "DB_USER=" + DB_USER + "\n"
    "DB_PASSWORD=" + DB_PASS + "\n"
    "JWT_SECRET=" + JWT_SECRET + "\n"
)
with sftp.open(REMOTE_DIR + "/backend/app.env", 'w') as f:
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
    "ExecStart=" + java17 + " -jar " + REMOTE_DIR + "/backend/sms-admin.jar"
    " --spring.profiles.active=prod --server.port=" + BACKEND_PORT + "\n"
    "Restart=always\n"
    "RestartSec=15\n"
    "StandardOutput=append:" + REMOTE_DIR + "/logs/backend.log\n"
    "StandardError=append:" + REMOTE_DIR + "/logs/backend.log\n\n"
    "[Install]\n"
    "WantedBy=multi-user.target\n"
)
with sftp.open("/etc/systemd/system/sms-backend.service", 'w') as f:
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
with sftp.open("/etc/systemd/system/mysql8.service", 'w') as f:
    f.write(mysql_svc)
sftp.close()

run("systemctl daemon-reload")
run("systemctl enable mysql8")
run("systemctl enable sms-backend")
run("systemctl restart sms-backend")
time.sleep(15)
run("systemctl status sms-backend 2>&1 | head -15")
run("tail -15 " + REMOTE_DIR + "/logs/backend.log 2>/dev/null")

# ---- Step 5: Final check ----
print("\n[5/5] Port check...")
run("ss -tlnp | grep -E '5800|5801|5802'")

client.close()
print("\n========================================")
print("Admin panel    : http://119.29.128.12:5800")
print("Customer portal: http://119.29.128.12:5801")
print("Backend (int)  : 127.0.0.1:5802")
print("========================================")
