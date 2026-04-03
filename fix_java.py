#!/usr/bin/env python
# Install Java 17 on CentOS 7 remote server and restart backend
import paramiko, time

HOST = "119.29.128.12"
ROOT_PASS = "_}V%n=J[ks?V|!"
REMOTE_DIR = "/opt/sms"
BACKEND_PORT = "5802"

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, username="root", password=ROOT_PASS, timeout=30)

def run(cmd, timeout=120):
    print("  $ " + cmd[:140])
    try:
        stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
        out = stdout.read().decode("utf-8", errors="replace").strip()
        err = stderr.read().decode("utf-8", errors="replace").strip()
        rc = stdout.channel.recv_exit_status()
    except Exception as e:
        print("    TIMEOUT: " + str(e))
        return 1, "", str(e)
    result = out or err
    if result:
        print("    => " + result[:400])
    return rc, out, err

# Check current backend log
print("[1] Checking backend status...")
run("systemctl status sms-backend 2>&1 | head -10")
run("tail -20 " + REMOTE_DIR + "/logs/backend.log 2>/dev/null | tail -10")

# Check Java 8 version
run("/usr/local/jdk1.8.0_211/bin/java -version 2>&1")

# Try multiple mirrors for Java 17
print("\n[2] Downloading Java 17 (China mirrors)...")
mirrors = [
    # Huawei mirror
    "https://repo.huaweicloud.com/java/jdk/17.0.2+8/openjdk-17.0.2_linux-x64_bin.tar.gz",
    # Tsinghua mirror
    "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/linux/OpenJDK17U-jdk_x64_linux_hotspot_17.0.13_11.tar.gz",
    # Adoptium direct
    "https://objects.githubusercontent.com/github-production-release-asset-2e65be/342158408/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_linux_hotspot_17.0.13_11.tar.gz",
]

# Remove broken partial download
run("rm -f /tmp/jdk17.tar.gz")

downloaded = False
for url in mirrors:
    print("  Trying: " + url[:80])
    rc, out, err = run(
        "wget -q --no-check-certificate --timeout=120 --tries=1 '"
        + url + "' -O /tmp/jdk17.tar.gz 2>&1 && ls -lh /tmp/jdk17.tar.gz",
        180
    )
    # Check size is > 100MB
    rc2, size, _ = run("stat -c%s /tmp/jdk17.tar.gz 2>/dev/null || echo 0")
    try:
        sz = int(size.strip())
    except:
        sz = 0
    if sz > 100000000:
        print("  Downloaded OK: " + str(sz) + " bytes")
        downloaded = True
        break
    else:
        print("  File too small (" + str(sz) + " bytes), trying next...")
        run("rm -f /tmp/jdk17.tar.gz")

if not downloaded:
    # Try curl
    for url in mirrors[:2]:
        rc, out, err = run(
            "curl -fsSL --max-time 120 '" + url + "' -o /tmp/jdk17.tar.gz 2>&1 && ls -lh /tmp/jdk17.tar.gz",
            180
        )
        rc2, size, _ = run("stat -c%s /tmp/jdk17.tar.gz 2>/dev/null || echo 0")
        try:
            sz = int(size.strip())
        except:
            sz = 0
        if sz > 100000000:
            downloaded = True
            break
        run("rm -f /tmp/jdk17.tar.gz")

if downloaded:
    print("\n[3] Extracting Java 17...")
    run("tar -xzf /tmp/jdk17.tar.gz -C /usr/local/ 2>&1 | head -5", 60)
    rc, java17, _ = run(
        "find /usr/local -maxdepth 4 -name 'java' -path '*/bin/java' 2>/dev/null"
        " | xargs -I{} sh -c '{} -version 2>&1 | grep -q 17 && echo {}' 2>/dev/null | head -1"
    )
    print("  Java 17 path: " + java17)

    if java17:
        print("\n[4] Updating backend service with Java 17...")
        sftp = client.open_sftp()
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
        sftp.close()

        run("systemctl daemon-reload")
        run("systemctl restart sms-backend")
        time.sleep(20)
        run("systemctl status sms-backend 2>&1 | head -12")
        run("tail -20 " + REMOTE_DIR + "/logs/backend.log 2>/dev/null")
    else:
        print("  ERROR: Java 17 binary not found after extraction!")
else:
    print("\n  All downloads failed. Checking if Java 8 works at all...")
    # Wait and check if Java 8 starts something
    time.sleep(10)
    run("systemctl status sms-backend 2>&1 | head -10")
    run("tail -10 " + REMOTE_DIR + "/logs/backend.log 2>/dev/null")
    print("\n  You may need to manually install Java 17 on the server.")
    print("  Commands to run on server:")
    print("    wget https://repo.huaweicloud.com/java/jdk/17.0.2+8/openjdk-17.0.2_linux-x64_bin.tar.gz -O /tmp/jdk17.tar.gz")
    print("    tar -xzf /tmp/jdk17.tar.gz -C /usr/local/")

print("\n[Final] Port check...")
run("ss -tlnp | grep -E '5800|5801|5802'")
client.close()
