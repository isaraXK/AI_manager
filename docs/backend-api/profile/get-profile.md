# 读取个人资料

- 当前版本 `V1.0.0`
- 接口 `GET /api/v1/profile`
- 验证状态：已通过接口自动化测试。2026-11-04 在本机 MySQL 验证首次读取返回空资料，保存后能正确读回中文资料、生日、年龄和版本。

读取当前登录主人的个人资料。服务端从会话取得账号。

## 请求

携带有效的 `ai_manager_session` Cookie。无查询参数或请求体。

## 响应

返回 HTTP 200。首次填写前的响应如下。

```json
{
  "profileReady": false,
  "version": 0,
  "profile": null,
  "age": null,
  "updatedAt": null
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| profileReady | boolean | 是否已经保存个人资料。 |
| version | integer | 资料版本。首次填写前为 `0`，保存时作为版本校验值提交。 |
| profile | object 或 null | 资料对象。字段与[保存个人资料](save-profile.md#资料字段)一致。 |
| age | integer 或 null | 根据生日及北京时间当天计算的年龄。 |
| updatedAt | string 或 null | UTC 的 ISO 8601 最后更新时间。 |

会话缺失、无效或已过期时返回 HTTP 401，错误码为 `UNAUTHORIZED`，错误体包含 `code`、`message` 和 `details`。

## 版本历史

| 版本 | 日期 | 变更 | 调用方影响 |
| --- | --- | --- | --- |
| V1.0.0 | 2026-11-04 | 首次记录空资料、已有资料及版本字段的响应。 | 建立初始契约。 |
