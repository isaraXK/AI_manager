# AI 管家

本仓库采用 Java 17、Spring Boot 3.5.16、MySQL 8.0 和 Next.js 16。项目配置与首条业务流程见 [技术设计](./首条流程技术设计.md)。

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

前端在另一个 PowerShell 中从项目根目录执行以下命令。首次 npm install 会生成锁文件，之后可用 npm ci 重现安装。

```powershell
Set-Location frontend
npm.cmd install
npm.cmd run build
npm.cmd run dev
```

访问 http://127.0.0.1:3000 查看启动验证页。
