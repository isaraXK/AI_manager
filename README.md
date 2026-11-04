# AI 管家

本仓库采用 Java 17、Spring Boot 3.5.16、MySQL 8.0 和 Next.js 16。项目配置与首条业务流程见 [技术设计](./首条流程技术设计.md)。

已实现接口的请求、响应和版本记录见[后端接口索引](./docs/backend-api/README.md)。

## Windows 本机验证

后端需要 Maven 3.6.3 或更高版本。本机已在 Maven Wrapper 缓存中验证 Maven 3.9.5 可使用 JDK 17 启动。可在普通 Windows PowerShell 中执行下方命令。其他机器可将已安装的 Maven 的 bin 目录加入 PATH。

```powershell
$env:JAVA_HOME = 'D:\Develop\Environment\Java\JDK17'
$maven = Get-ChildItem "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.9.5-bin" -Recurse -Filter mvn.cmd | Select-Object -First 1
if (-not $maven) { throw '请先安装 Maven 3.6.3 或更高版本，并将 bin 目录加入 PATH。' }
$env:Path = "$($maven.DirectoryName);$env:JAVA_HOME\bin;$env:Path"
mvn -version
Set-Location backend
mvn clean verify
$env:SPRING_PROFILES_ACTIVE = 'smoke'
mvn spring-boot:run
```

访问 http://127.0.0.1:8080/actuator/health 检查启动状态。smoke 配置仅用于验证应用启动。

首次构建需要访问 Maven Central。若报 `Unknown host repo.maven.apache.org`，先检查当前终端的 DNS 和仓库访问，再执行 `mvn -U clean verify`。

连接 MySQL 时，在另一个 PowerShell 中进入 backend，确保目标库已创建，并使用专用账号。下面是环境变量模板，密码从提示框读取。

```powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/ai_manager?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
$env:DB_USERNAME = 'ai_manager'
$env:DB_PASSWORD = Read-Host 'MySQL 项目账号密码' -MaskInput
mvn spring-boot:run
```

业务表通过 backend/src/main/resources/db/migration 下的 Flyway 脚本维护。首次迁移随账号和日记模块提交。

前端在另一个 PowerShell 中从项目根目录执行以下命令。已有锁文件可用 npm ci 安装依赖。验证网页登录时，后端需使用普通配置连接 MySQL。下面以 8083 为例，按后端实际端口设置地址。

```powershell
Set-Location frontend
$env:AI_MANAGER_BACKEND_URL = 'http://127.0.0.1:8083'
npm.cmd ci
npm.cmd run build
npm.cmd run dev
```

访问 http://127.0.0.1:3000，用应用主人账号登录。页面流程和错误处理见[网页登录与个人资料接入](./docs/frontend-integration/account-profile.md)。

## 初始化应用主人账号

在已设置 MySQL 连接变量的后端 PowerShell 中操作。应用主人账号用于网页登录，有自己的用户名和密码。先单独执行密码输入命令，输入至少 6 个字符。

```powershell
$ownerPassword = Read-Host '设置应用主人密码' -AsSecureString
```

输入完成后，再运行以下命令。使用普通配置启动时，Flyway 先准备数据表，随后首次初始化写入主人账号。已有账号时初始化会报错并退出。

```powershell
$env:AI_MANAGER_SETUP_USERNAME = 'owner'
try {
    $env:AI_MANAGER_SETUP_PASSWORD = [System.Net.NetworkCredential]::new('', $ownerPassword).Password
    mvn spring-boot:run
} finally {
    Remove-Item Env:AI_MANAGER_SETUP_USERNAME,Env:AI_MANAGER_SETUP_PASSWORD -ErrorAction SilentlyContinue
    Remove-Variable ownerPassword -ErrorAction SilentlyContinue
}
```

初始化成功后停止应用，再以普通配置启动。当前账号功能通过 `/api/v1/auth/login`、`/api/v1/auth/session` 和 `/api/v1/auth/logout` 提供。登录与退出请求需带 `X-AI-Manager-Request: 1`，前端接入时使用同源请求。HTTPS 部署时设置 `AI_MANAGER_AUTH_SECURE_COOKIE=true`。
