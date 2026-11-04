# 登录

- 当前版本 `V1.0.0`
- 接口 `POST /api/v1/auth/login`
- 验证状态：已通过本机 MySQL 联调。

验证应用主人账号，创建一个有效期为 7 天的登录会话。首次使用前需完成本机账号初始化。

## 请求

| 请求头 | 值 |
| --- | --- |
| Content-Type | `application/json` |
| X-AI-Manager-Request | `1` |

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | string | 是 | 应用主人用户名，例如 `owner`。 |
| password | string | 是 | 应用主人密码。 |

```json
{"username":"owner","password":"<应用主人密码>"}
```

## 响应

成功返回 HTTP 200。响应头 `Set-Cookie` 设置 `ai_manager_session`，含 `HttpOnly`、`SameSite=Lax`、`Path=/` 和 `Max-Age=604800`。服务端开启安全 Cookie 配置时会附加 `Secure`。

```json
{"loggedIn":true,"profileReady":false}
```

`profileReady` 为 `false` 时进入首次资料填写，为 `true` 时进入首页入口。后续请求携带该 Cookie。

| HTTP 状态 | 错误码 | 原因 |
| --- | --- | --- |
| 401 | INVALID_CREDENTIALS | 用户名或密码无效。 |
| 403 | REQUEST_MARKER_REQUIRED | 请求标记缺失或值不为 `1`。 |

业务错误响应包含 `code`、`message` 和 `details`。

## 版本历史

| 版本 | 日期 | 变更 | 调用方影响 |
| --- | --- | --- | --- |
| V1.0.0 | 2026-11-04 | 首次记录主人登录、会话 Cookie 和资料完成状态。 | 建立初始契约。 |
