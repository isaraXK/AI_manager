# 退出登录

- 当前版本 `V1.0.0`
- 接口 `POST /api/v1/auth/logout`
- 验证状态：已通过本机 MySQL 联调。

退出当前会话，并清除浏览器对应的会话 Cookie。

## 请求

携带 `X-AI-Manager-Request: 1` 及当前的 `ai_manager_session` Cookie。无请求体。

## 响应

成功返回 HTTP 204，无响应体。响应头 `Set-Cookie` 将会话 Cookie 的 `Max-Age` 设为 `0`。当 Cookie 缺失或对应会话已失效时，同样返回 HTTP 204。

| HTTP 状态 | 错误码 | 原因 |
| --- | --- | --- |
| 403 | REQUEST_MARKER_REQUIRED | 请求标记缺失或值不为 `1`。 |

业务错误响应包含 `code`、`message` 和 `details`。退出后可调用[查询会话](session.md)检查登录状态。

## 版本历史

| 版本 | 日期 | 变更 | 调用方影响 |
| --- | --- | --- | --- |
| V1.0.0 | 2026-11-04 | 首次记录当前会话注销和 Cookie 清除行为。 | 建立初始契约。 |
