# 后端接口索引

按业务功能划分目录，每个 HTTP 方法和路径各有一份文档。下表收录当前代码已实现的接口。

| 功能 | 接口 | 文档 |
| --- | --- | --- |
| 账号与会话 | `POST /api/v1/auth/login` | [登录](auth/login.md) |
| 账号与会话 | `GET /api/v1/auth/session` | [查询会话](auth/session.md) |
| 账号与会话 | `POST /api/v1/auth/logout` | [退出登录](auth/logout.md) |
| 个人资料 | `GET /api/v1/profile` | [读取个人资料](profile/get-profile.md) |
| 个人资料 | `PUT /api/v1/profile` | [保存个人资料](profile/save-profile.md) |
| 运行检查 | `GET /actuator/health` | [健康检查](operations/health.md) |

默认本机后端地址为 `http://127.0.0.1:8080`。如果启动时指定了其他端口，请使用对应地址。网页接入时使用同源请求，由浏览器保存并发送登录 Cookie。首次创建主人账号通过[本机初始化步骤](../../README.md#初始化应用主人账号)完成。

网页的调用位置、同源转发和异常处理见[网页登录与个人资料接入](../frontend-integration/account-profile.md)。

## 维护约定

- 每个接口的初始版本为 `V1.0.0`。版本号记录单个接口的契约演进，URL 中的 `/api/v1` 表示路由分组。
- 当请求字段、响应结构、状态码、权限或调用行为有变化时，同步更新该接口文档。每次迭代在文末保留版本号、日期、变化内容和调用方影响。
- 同一路径的不同 HTTP 方法分别维护。实现进度写在对应文档里，未实现接口的设计仍见[技术设计](../../首条流程技术设计.md)。
