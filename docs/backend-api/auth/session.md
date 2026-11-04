# 查询登录会话

- 当前版本 `V1.0.0`
- 接口 `GET /api/v1/auth/session`
- 验证状态：已通过本机 MySQL 联调，包括首次保存个人资料后原会话返回 `profileReady=true`。

查询当前登录状态及个人资料是否已填写。页面打开或刷新时可调用。

## 请求

可携带登录时取得的 `ai_manager_session` Cookie。无请求体。

## 响应

返回 HTTP 200。会话有效时，`loggedIn` 为 `true`，`profileReady` 根据个人资料是否存在计算。

```json
{"loggedIn":true,"profileReady":false}
```

Cookie 缺失、无效或会话过期时返回：

```json
{"loggedIn":false,"profileReady":false}
```

## 版本历史

| 版本 | 日期 | 变更 | 调用方影响 |
| --- | --- | --- | --- |
| V1.0.0 | 2026-11-04 | 首次记录有效会话、未登录及资料完成状态的响应。 | 建立初始契约。 |
