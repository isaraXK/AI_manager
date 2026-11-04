# 保存个人资料

- 当前版本 `V1.0.0`
- 接口 `PUT /api/v1/profile`
- 验证状态：已通过接口自动化测试。2026-11-04 在本机 MySQL 验证首次保存返回版本 1，随后能读回资料。版本冲突响应由接口自动化测试覆盖。

创建或更新当前登录主人的个人资料。提交的是整份资料，省略的选填字段按空值保存。

## 请求

携带有效的 `ai_manager_session` Cookie，以及 `Content-Type: application/json` 和 `X-AI-Manager-Request: 1`。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| version | integer | 是 | 首次创建传 `0`，编辑传读取到的版本。 |
| profile | object | 是 | 完整资料字段。 |

### 资料字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| nickname | string | 是 | 称呼，最多 100 个字符。 |
| gender | string | 是 | 性别，最多 32 个字符。 |
| birthDate | string | 是 | 生日，格式为 `YYYY-MM-DD`，年份至少为 1000，日期不能晚于北京时间当天。 |
| occupation | string | 否 | 职业，最多 200 个字符。 |
| routine | string | 否 | 作息，最多 65,535 UTF-8 字节。 |
| interests | string | 否 | 兴趣爱好，最多 65,535 UTF-8 字节。 |
| mbti | string | 否 | MBTI，最多 16 个字符。 |
| zodiac | string | 否 | 星座，最多 32 个字符。 |
| bloodType | string | 否 | 血型，最多 16 个字符。 |
| selfIntro | string | 否 | 自我介绍，最多 65,535 UTF-8 字节。 |

字符串会去掉首尾空白，空白内容按空值处理。年龄由服务端计算。

```json
{
  "version": 0,
  "profile": {"nickname":"小林","gender":"女","birthDate":"1998-05-20"}
}
```

## 响应

返回 HTTP 200，结构与[读取个人资料](get-profile.md#响应)一致。首次保存后 `profileReady=true`、`version=1`，每次更新成功后版本递增。

| HTTP 状态 | 错误码 | 原因 |
| --- | --- | --- |
| 401 | UNAUTHORIZED | 登录会话缺失、无效或已过期。 |
| 403 | REQUEST_MARKER_REQUIRED | 请求标记缺失或值不为 `1`。 |
| 409 | VERSION_CONFLICT | 资料已存在但请求按首次创建提交，或更新时版本已失效。 |
| 422 | PROFILE_VALIDATION_FAILED | 字段校验失败，`details` 按字段名给出原因。 |

发生版本冲突时，页面应保留用户输入，并重新读取资料再继续编辑。业务错误响应包含 `code`、`message` 和 `details`。无法解析的 JSON 请求由框架返回 HTTP 400。

## 版本历史

| 版本 | 日期 | 变更 | 调用方影响 |
| --- | --- | --- | --- |
| V1.0.0 | 2026-11-04 | 首次记录资料创建与更新、字段校验及版本冲突。 | 建立初始契约。 |
