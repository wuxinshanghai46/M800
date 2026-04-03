#!/usr/bin/env python
# -*- coding: utf-8 -*-
import paramiko, subprocess, os

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect("119.29.128.12", username="root", password="_}V%n=J[ks?V|!", timeout=30)
sftp = client.open_sftp()

def run(cmd, timeout=60):
    stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", errors="replace").strip()
    err = stderr.read().decode("utf-8", errors="replace").strip()
    stdout.channel.recv_exit_status()
    return out or err

MYSQL_LOCAL = "C:/Program Files/MySQL/MySQL Server 9.6/bin/mysqldump.exe"
MYSQL_ARGS = [
    MYSQL_LOCAL, "-u", "root", "-p12345678",
    "--no-create-info", "--skip-triggers",
    "--complete-insert", "--single-transaction",
    "sms_platform"
]

print("Exporting local data...")
result = subprocess.run(MYSQL_ARGS, capture_output=True)
if result.returncode != 0:
    print("Export error:", result.stderr.decode("utf-8", errors="replace")[:200])
else:
    sql_data = result.stdout
    print("Export size:", len(sql_data), "bytes")
    print("Uploading to server...")
    with sftp.open("/tmp/sms_data_export.sql", "wb") as f:
        f.write(sql_data)
    print("Upload done")

sftp.close()

MYSQL = "/usr/local/mysql-8.0.37/bin/mysql -S /tmp/mysql.sock -u root -p'BwhhNA3pw!mQ},'"
print("\nImporting data...")
print(run(MYSQL + " sms_platform < /tmp/sms_data_export.sql 2>&1 | grep -i error | grep -v '1062\\|1060\\|1050\\|Duplicate' | head -10"))

print("\nData check:")
print(run(MYSQL + " sms_platform -e \"SELECT 'customers' as tbl, COUNT(*) as cnt FROM customer UNION SELECT 'sys_user', COUNT(*) FROM sys_user UNION SELECT 'sms_template', COUNT(*) FROM sms_template;\" 2>&1 | grep -v Warning"))

print("\nPortal login test:")
print(run("curl -s http://127.0.0.1:5802/v1/portal/auth/login -X POST -H 'Content-Type: application/json' -d '{\"account\":\"C2558\",\"password\":\"Sms@C25582024\"}'"))

client.close()
