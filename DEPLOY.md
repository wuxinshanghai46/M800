# SMS Platform 部署指南

## 服务器要求

- Ubuntu 20.04+ (推荐 22.04)
- Docker 24+ & Docker Compose v2
- 最低配置：2核 4GB RAM 40GB SSD
- 开放端口：80 (HTTP)

## 部署步骤

### 1. 上传项目到服务器

```bash
# 在本地打包（先构建 Java 后端）
cd sms-platform
mvn clean package -DskipTests

# 上传整个 sms-platform 目录到服务器
scp -r sms-platform/ user@server:/opt/
```

### 2. 配置环境变量

```bash
cd /opt/sms-platform
cp .env.example .env
vi .env
```

必须修改的配置：
```
DB_PASSWORD=你的数据库强密码
JWT_SECRET=至少32字节的随机字符串
```

生成随机密钥：
```bash
openssl rand -base64 48
```

### 3. 启动服务

```bash
cd /opt/sms-platform
docker compose up -d
```

首次启动会：
- 拉取 MySQL 8.0 / Node 20 / Nginx / JRE 17 镜像
- 自动建库建表（doc/ 下的 SQL 脚本）
- 构建前端并部署到 Nginx
- 启动后端 Java 应用

### 4. 验证

```bash
# 查看容器状态
docker compose ps

# 查看后端日志
docker compose logs -f sms-admin

# 健康检查
curl http://localhost/actuator/health
```

### 5. 访问系统

浏览器打开 `http://服务器IP`

默认管理员账号：
- 用户名：admin
- 密码：admin123

**登录后请立即修改默认密码！**

## 常用运维命令

```bash
# 重启所有服务
docker compose restart

# 仅重启后端
docker compose restart sms-admin

# 查看日志
docker compose logs -f sms-admin --tail=200

# 停止所有服务
docker compose down

# 停止并删除数据（慎用！）
docker compose down -v

# 重新构建并启动
docker compose up -d --build
```

## 预置角色

| 角色 | 编码 | 权限范围 |
|------|------|----------|
| 超级管理员 | SUPER_ADMIN | 全部权限 |
| 运营管理员 | OPS_ADMIN | 除财务和系统配置外的所有功能 |
| 财务 | FINANCE | 概览 + 客户 + 统计 + 财务 |
| 只读 | VIEWER | 概览 + 发送记录 + 统计 |

## 目录结构

```
sms-platform/
├── docker-compose.yml      # 编排配置
├── .env                    # 环境变量（不要提交到 Git）
├── doc/                    # SQL 初始化脚本（MySQL 自动执行）
├── sms-admin/              # 后端 (Spring Boot)
│   ├── Dockerfile
│   └── target/sms-admin-*.jar
└── sms-ui/                 # 前端 (Vue 3 + Nginx)
    ├── Dockerfile
    └── nginx.conf
```
