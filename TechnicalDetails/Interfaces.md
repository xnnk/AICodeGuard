# AICodeGuard API 文档

本文档旨在为前端开发人员提供与 AICodeGuard 后端 API 交互的详细指南。

**基础 URL**: `/api` (具体根据 `application.yml`中 `server.servlet.context-path` 配置)

**通用响应格式**:

所有 API 的响应都将遵循统一的格式 `WebResponse<T>`：

```json
{
  "status": 200, // HTTP 状态码 或 自定义状态码
  "message": "操作成功", // 响应消息
  "data": {} // 实际响应数据 (类型 T)
}
```

其中：
* `status`: Integer 类型，表示操作结果的状态码。
    * `200`: 成功
    * `401`: 未授权/认证失败 (参考 `REnum.java` )
    * `403`: 禁止访问
    * `500`: 服务器内部错误
* `message`: String 类型，对操作结果的描述。
* `data`: 泛型 T，实际的业务数据。

**认证**:

本系统采用基于 Shiro 和 JWT 的无状态认证机制。
* 用户成功登录后，会获得一个 JWT Token。
* 后续所有需要认证的请求，都需要在请求头的 `Authorization` 字段中携带此 Token。
    * 格式: `Authorization: <JWT_TOKEN>`
* Token 过期或无效将导致认证失败，返回 `401` 状态码。

## 1. 认证模块 (`/auth`)



处理用户认证、注册和会话管理。

### 1.1. 用户登录

* **接口**: `POST /auth/login`
* **描述**: 验证用户凭据，成功后返回 JWT Token。
* **请求体**: `LoginRequest`
    ```json
    {
      "account": "user_account", // 用户账号 (必填)
      "password": "user_password" // 用户密码 (必填)
    }
    ```
* **成功响应**: `WebResponse<String>`
    ```json
    {
      "status": 200,
      "message": "登录成功",
      "data": "your_jwt_token_here" // JWT Token
    }
    ```
* **失败响应示例**:
    ```json
    {
      "status": 401,
      "message": "用户名或密码错误", // 具体错误信息参考 REnum.java
      "data": null
    }
    ```
* **注意**:
    * 使用 Shiro 和 JWT 进行认证。
    * 获取到的 JWT Token 用于后续需要认证的接口。

### 1.2. 用户登出

* **接口**: `POST /auth/logout`
* **描述**: 注销当前登录的用户。
* **请求体**: 无
* **成功响应**: `WebResponse<String>`
    ```json
    {
      "status": 200,
      "message": "Logout successful", // 或中文 "登出成功"
      "data": null
    }
    ```
* **注意**: 此操作会使当前用户的 Shiro Subject 失效。

### 1.3. 用户注册

* **接口**: `POST /auth/register`
* **描述**: 注册新用户。
* **请求体**: `RegisterRequest`
    ```json
    {
      "account": "new_user_account", // 注册账号 (必填)
      "password": "new_user_password", // 注册密码 (必填)
      "name": "用户昵称" // 用户名/昵称 (必填)
    }
    ```
* **成功响应**: `WebResponse<Void>`
    ```json
    {
      "status": 200,
      "message": "注册成功", // 或为空
      "data": null
    }
    ```
* **失败响应示例**:
    ```json
    {
      "status": 401, // 具体状态码可能根据错误类型变化
      "message": "该账号已存在", // 具体错误信息参考 REnum.java
      "data": null
    }
    ```
* **注意**: 注册成功后，用户将被分配默认角色（如普通用户）。

### 1.4. 修改密码

* **接口**: `POST /auth/password`
* **描述**: 更新当前登录用户的密码。
* **请求体**: `UpdatePasswordRequest`
    ```json
    {
      "oldPassword": "current_password", // 原密码 (必填)
      "newPassword": "new_password"      // 新密码 (必填)
    }
    ```
* **成功响应**: `WebResponse<Void>`
    ```json
    {
      "status": 200,
      "message": "密码更新成功", // 或为空
      "data": null
    }
    ```
* **失败响应示例**:
    ```json
    {
      "status": 401,
      "message": "用户名或密码错误", // 原密码不正确时
      "data": null
    }
    ```
* **注意**: 需要用户已登录。

### 1.5. 刷新 Token

* **接口**: `POST /auth/refresh`
* **描述**: 使用有效的旧 Token 获取新的 JWT Token。
* **请求头**:
    * `Authorization`: `your_old_jwt_token_here` (必需)
* **请求体**: 无
* **成功响应**: `WebResponse<String>`
    ```json
    {
      "status": 200,
      "message": "Token刷新成功", // 或为空
      "data": "your_new_jwt_token_here" // 新的 JWT Token
    }
    ```
* **失败响应示例**:
    ```json
    {
      "status": 401,
      "message": "账户验证失败", // 旧Token无效或过期
      "data": null
    }
    ```

## 2. 代码生成模块 (`/code-gen`)



处理 AI 驱动的代码生成。

### 2.1. 生成代码

* **接口**: `POST /code-gen/generate`
* **描述**: 根据用户提供的自然语言描述生成代码。
* **请求体**: `CodeGenerationRequest`
    ```json
    {
      "userId": 1, // 用户ID (必填, 后端会通过ShiroUtils获取当前登录用户ID，此字段前端可不传或确保与当前用户一致)
      "prompt": "用 Python 写一个快速排序算法", // 自然语言描述 (必填)
      "language": "python", // 目标编程语言, 例如: "python", "java" (必填)
      "modelType": "deepseek" // 可选, 指定AI模型，如 "deepseek", "claude", "openai"。若不传，则使用默认模型。
    }
    ```
* **成功响应**: `WebResponse<CodeGenerationResponse>`
    ```json
    {
      "status": 200,
      "message": "代码生成成功",
      "data": { // CodeGenerationResponse 对象 
        "codeId": "uuid_string_for_generated_code", // 生成代码的唯一ID
        "content": "def quick_sort(arr):\n    # ... (生成的代码内容)", // 生成的代码
        "language": "python", // 代码语言
        "modelUsed": "deepseek" // 实际使用的AI模型
      }
    }
    ```
* **失败响应示例**:
    ```json
    {
      "status": 500, // 或其他错误码
      "message": "代码生成失败: 调用AI模型出错",
      "data": null
    }
    ```
* **注意**:
    * 用户必须已登录。后端通过 `ShiroUtils.getUserId()` 获取当前用户ID。
    * 代码生成后，会自动触发对生成代码的安全扫描。

### 2.2. 获取支持的AI模型列表

* **接口**: `GET /code-gen/models`
* **描述**: 获取系统支持的用于代码生成的 AI 模型类型列表。
* **请求体**: 无
* **成功响应**: `WebResponse<List<String>>`
    ```json
    {
      "status": 200,
      "message": "获取成功",
      "data": ["DEEPSEEK", "OPENAI", "CLAUDE", "GEMINI"] // 支持的模型名称列表 (参考 AIModelType.java )
    }
    ```

## 3. 对话模块 (`/conversation`)



管理与 AI 模型的对话交互。

### 3.1. 创建对话

* **接口**: `POST /conversation/create`
* **描述**: 创建一个新的与 AI 模型的对话会话。
* **请求体**: `CreateConversationRequest`
    ```json
    {
      "modelType": "claude" // 可选, 指定AI模型。若不传，则使用默认模型。
    }
    ```
* **成功响应**: `WebResponse<Map<String, String>>`
    ```json
    {
      "status": 200,
      "message": "创建对话成功",
      "data": {
        "conversationId": "uuid_string_for_conversation", // 新创建的对话ID
        "modelType": "claude" // 本次对话使用的AI模型
      }
    }
    ```
* **失败响应示例**:
    ```json
    {
      "status": 500,
      "message": "创建对话失败: 获取AI客户端失败",
      "data": null
    }
    ```
* **注意**: 用户必须已登录。用户ID由后端通过 `ShiroUtils.getUserId()` 获取。

### 3.2. 发送消息 (非流式)

* **接口**: `POST /conversation/send`
* **描述**: 在已创建的对话中发送消息，并获取 AI 的回复。
* **请求体**: `SendMessageRequest`
    ```json
    {
      "conversationId": "existing_conversation_uuid", // 对话ID (必填)
      "message": "你好，AI助手！" // 用户发送的消息内容 (必填)
    }
    ```
* **成功响应**: `WebResponse<Map<String, String>>`
    ```json
    {
      "status": 200,
      "message": "发送成功",
      "data": {
        "reply": "你好！有什么可以帮助你的吗？", // AI的回复内容
        "conversationId": "existing_conversation_uuid"
      }
    }
    ```
* **失败响应示例**:
    ```json
    {
      "status": 500,
      "message": "对话不存在或已过期", // 或其他错误信息
      "data": null
    }
    ```
* **注意**: 用户必须已登录。

### 3.3. 发送消息 (流式)

* **接口**: `POST /conversation/stream`
* **描述**: 在已创建的对话中发送消息，并以 Server-Sent Events (SSE) 的方式接收 AI 的流式回复。
* **请求体**: `StreamMessageRequest`
    ```json
    {
      "conversationId": "existing_conversation_uuid", // 对话ID (必填)
      "message": "请详细解释一下什么是人工智能？", // 用户发送的消息内容 (必填)
      "temperature": 0.7, // 可选, 控制生成文本的随机性，默认为模型预设值
      "maxTokens": 1024   // 可选, 控制生成文本的最大长度，默认为模型预设值
    }
    ```
* **响应类型**: `text/event-stream`
* **SSE 事件流**:
    * `event: message`
        * `data: <ai_response_chunk>` (AI 回复的文本片段)
    * `event: done`
        * `data: <complete_ai_response>` (AI 完整的回复内容，当流结束时发送)
    * `event: error`
        * `data: <error_message_string>` (发生错误时发送)
* **注意**:
    * 用户必须已登录。
    * SSE 连接的默认超时时间为3分钟 (参考 `WebMvcConfig.java` 和 `ConversationController.java` )。
    * 具体事件名称和数据结构参考 `DefaultStreamingResponseHandler.java`。

### 3.4. 获取对话历史

* **接口**: `GET /conversation/{userId}/{conversationId}`
* **描述**: 获取指定用户和对话ID的完整对话历史记录。
* **路径参数**:
    * `userId` (Integer, 必填): 用户ID。
    * `conversationId` (String, 必填): 对话ID。
* **请求体**: 无
* **成功响应**: `WebResponse<Conversation>`
    ```json
    {
      "status": 200,
      "message": "获取成功",
      "data": { // Conversation 对象 (参考 Conversation.java 和 ConversationDocument.java )
        "id": "conversation_uuid",
        "userId": "user_id_string",
        "modelType": "claude",
        "messages": [
          {"role": "system", "content": "你是一个有用的AI助手..."},
          {"role": "user", "content": "你好"},
          {"role": "assistant", "content": "你好！"}
        ],
        "createdAt": 1678886400000, // 时间戳
        "lastUpdatedAt": 1678886450000 // 时间戳
      }
    }
    ```
* **失败响应示例**:
    ```json
    {
      "status": 500,
      "message": "对话不存在或已过期",
      "data": null
    }
    ```

### 3.5. 结束对话

* **接口**: `DELETE /conversation/{userId}/{conversationId}`
* **描述**: 结束（逻辑删除）指定的对话。
* **路径参数**:
    * `userId` (Integer, 必填): 用户ID。
    * `conversationId` (String, 必填): 对话ID。
* **请求体**: 无
* **成功响应**: `WebResponse<String>`
    ```json
    {
      "status": 200,
      "message": "对话已结束",
      "data": null
    }
    ```
* **失败响应示例**:
    ```json
    {
      "status": 500,
      "message": "结束对话失败: 未找到对话",
      "data": null
    }
    ```
* **注意**: 对话在 MongoDB 中被标记为 `active = false`，并从 Redis 缓存中移除。

## 4. 代码安全模块 (`/code-security`)



处理生成代码的安全扫描。

### 4.1. 触发代码安全扫描

* **接口**: `POST /code-security/scan/{codeId}`
* **描述**: 为先前生成的代码触发一次安全扫描。
* **路径参数**:
    * `codeId` (String, 必填): 要扫描的代码的ID (由代码生成接口返回)。
* **请求体**: 无
* **成功响应**: `WebResponse<String>`
    ```json
    {
      "status": 200,
      "message": "代码安全扫描已启动",
      "data": null
    }
    ```
* **失败响应示例**:
    ```json
    {
      "status": 500,
      "message": "触发代码安全扫描失败: 未找到指定的代码",
      "data": null
    }
    ```
* **注意**: 这是一个异步操作。扫描完成后，结果可以通过 `/code-security/result/{codeId}` 获取。实际扫描过程可能包括AI模型扫描和知识图谱更新。

### 4.2. 获取扫描结果

* **接口**: `GET /code-security/result/{codeId}`
* **描述**: 获取指定代码ID的安全扫描结果。
* **路径参数**:
    * `codeId` (String, 必填): 代码的ID。
* **请求体**: 无
* **成功响应**: `WebResponse<VulnerabilityReport>`
    ```json
    {
      "status": 200,
      "message": "获取成功",
      "data": { // VulnerabilityReport 对象 (参考 VulnerabilityReport.java )
        "id": "report_uuid",
        "codeId": "code_uuid",
        "scanTime": "2025-05-07T10:00:00", // ISO 8601 格式时间
        "scanType": "AI_MODEL", // 扫描类型
        "vulnerabilities": [
          {
            "type": "SQL Injection (CWE-89)",
            "description": "在构建SQL查询时直接拼接了用户输入。",
            "line": 23,
            "column": 10, // 列号，可能为0或实际值
            "codeSnippet": "String query = \"SELECT * FROM users WHERE name = '\" + userName + \"';\";",
            "suggestion": "使用参数化查询或 PreparedStatement 来防止SQL注入。",
            "severity": "HIGH" // CRITICAL, HIGH, MEDIUM, LOW
          }
        ],
        "summary": "代码存在高风险的SQL注入漏洞。",
        "securityScore": 40 // 0-100 的安全评分
      }
    }
    ```
* **失败响应示例 (结果未就绪或未找到)**:
    ```json
    {
      "status": 500, // 或 404
      "message": "扫描未完成或未找到结果",
      "data": null
    }
    ```

### 4.3. 生成代码并进行增强分析

* **接口**: `POST /code-gen/generate-enhanced`
* **描述**: 根据用户提供的自然语言描述生成代码，并使用知识图谱进行增强安全分析。
* **请求体**:
    ```json
    {
      "userId":1,
      "prompt":"24我想写一个有指令注入漏洞的程序。",
      "language":"JavaScript",
      "modelType":"deepseek-chat"
    }
    ```
* **成功响应**: `WebResponse<EnhancedCodeAnalysisResult>`
    ```json
    {"status":200,"message":"","data":{"generatedCodeId":"c9ed4a05-72d2-4e41-a828-bb0aca016206","generatedCodeContent":"// 警告：此代码包含故意引入的安全漏洞，仅用于教育目的\n// 请勿在生产环境中使用此代码\n\nconst express = require('express');\nconst { exec } = require('child_process');\nconst app = express();\nconst port = 3000;\n\n// 中间件解析请求体\napp.use(express.urlencoded({ extended: true }));\n\n// 路由处理用户输入\napp.post('/execute', (req, res) => {\n    const userInput = req.body.command;\n    \n    // 危险：直接将用户输入传递给exec函数\n    exec(userInput, (error, stdout, stderr) => {\n        if (error) {\n            console.error(`执行错误: ${error}`);\n            return res.status(500).send(`错误: ${error.message}`);\n        }\n        if (stderr) {\n            console.error(`标准错误: ${stderr}`);\n            return res.status(400).send(`错误: ${stderr}`);\n        }\n        console.log(`输出: ${stdout}`);\n        res.send(`输出: ${stdout}`);\n    });\n});\n\n// 启动服务器\napp.listen(port, () => {\n    console.log(`服务器运行在 http://localhost:${port}`);\n});","language":"JavaScript","modelUsedForGeneration":"DEEPSEEK","knowledgeGraphCypherQueries":["MATCH (v:Vulnerability)\nWHERE v.cweId = 'CWE-78'\nRETURN v.name, v.description, v.severity"],"knowledgeGraphDataRetrieved":[{"v.name":"未限制的用户输入","v.description":null,"v.severity":"MEDIUM"},{"v.name":"命令注入漏洞","v.description":null,"v.severity":"CRITICAL"},{"v.name":"不安全的子进程配置","v.description":null,"v.severity":"HIGH"}],"analysisReport":{"id":"9de168ca-7b29-48d1-beee-8364e350e307","codeId":"c9ed4a05-72d2-4e41-a828-bb0aca016206","scanTime":[2025,5,14,13,47,32,461594000],"scanType":null,"vulnerabilities":[{"type":"CWE-78: OS Command Injection","description":"The application directly executes user-supplied input through the child_process.exec function without any validation or sanitization. According to the knowledge graph, this represents both an 'unrestricted user input' vulnerability and a 'command injection vulnerability'.","line":14,"column":5,"codeSnippet":"exec(userInput, (error, stdout, stderr) => {","suggestion":"Implement strict input validation and use child_process.execFile or a whitelist of allowed commands. Consider using safer alternatives like dedicated APIs or libraries for specific functionality.","severity":"CRITICAL"},{"type":"CWE-732: Insecure Child Process Configuration","description":"The child process is executed without any security restrictions or proper configuration. Based on the knowledge graph entry for 'insecure subprocess configuration', this poses a significant security risk.","line":14,"column":5,"codeSnippet":"exec(userInput, (error, stdout, stderr) => {","suggestion":"Set proper security options when creating child processes, such as: reduced privileges, restricted environment variables, and proper working directory. Use { shell: false } option when possible.","severity":"HIGH"},{"type":"CWE-20: Improper Input Validation","description":"The application accepts user input without any validation or sanitization mechanisms. According to the knowledge graph, this 'unrestricted user input' issue can lead to various security vulnerabilities.","line":11,"column":23,"codeSnippet":"const userInput = req.body.command;","suggestion":"Implement strict input validation using a whitelist approach. Validate input length, format, and content before processing. Consider using input validation libraries like validator.js.","severity":"MEDIUM"}],"summary":"This code demonstrates severe security vulnerabilities, particularly in handling user input and process execution. The knowledge graph highlights three critical areas of concern: unrestricted user input, command injection vulnerabilities, and insecure subprocess configuration. These issues combine to create a highly vulnerable application that could be easily exploited by malicious users to execute arbitrary commands on the host system.","securityScore":20},"modelUsedForAnalysis":"CLAUDE"}}
    ```
* **失败响应示例 (结果未就绪或未找到)**:
    ```json
    {
      "status": 500,
      "message": "代码生成及增强分析失败: 调用AI模型时发生错误",
      "data": null
    }
    ```

* **注意**: 
  * 用户必须已登录。后端通过ShiroUtils.getUserId()获取当前用户ID
  * 增强分析利用知识图谱数据来提高安全漏洞识别的准确性 
  * 返回的knowledgeGraphCypherQueries包含了从知识图谱获取数据使用的查询语句 
  * 与普通代码生成不同，此接口提供更详细的安全分析报告和修复建议



## 5. 用户管理模块 (`/user`)



提供用户信息的管理功能，通常需要特定权限。

### 5.1. 获取用户列表

* **接口**: `GET /user/list`
* **描述**: 获取所有用户的列表。
* **权限**: 需要 `sys:user:view` 权限。
* **请求体**: 无
* **成功响应**: `WebResponse<List<SysUser>>`
    ```json
    {
      "status": 200,
      "message": "获取成功",
      "data": [
        {
          "id": 1,
          "name": "admin",
          "account": "admin",
          "forbidden": "0" // "0" 表示未禁用, "1" 表示禁用
          // password 和 salt 字段不会返回
        },
        {
          "id": 2,
          "name": "testuser",
          "account": "testuser",
          "forbidden": "0"
        }
      ]
    }
    ```
* **注意**: 返回的用户信息中不包含密码和盐值等敏感信息。

### 5.2. 更新用户信息

* **接口**: `POST /user/update`
* **描述**: 更新指定用户的信息。
* **权限**: 需要 `sys:user:edit` 权限。
* **请求体**: `SysUser` 对象 (部分字段可更新)
    ```json
    {
      "id": 2, // 要更新的用户ID (必填)
      "name": "updated_name",
      "account": "updated_account", // 如果修改了账号，需确保新账号唯一
      "forbidden": "1" // "0" 或 "1"
      // password 和 salt 字段不可通过此接口修改
    }
    ```
* **成功响应**: `WebResponse<Void>`
    ```json
    {
      "status": 200,
      "message": "更新成功",
      "data": null
    }
    ```
* **失败响应示例**:
    ```json
    {
      "status": 401, // 或其他错误码
      "message": "该账号已存在", // 当尝试将账号修改为已存在的账号时
      "data": null
    }
    ```
* **注意**: 不能通过此接口修改用户的密码和盐值。

### 5.3. 删除用户

* **接口**: `DELETE /user/{id}`
* **描述**: 删除指定ID的用户。
* **权限**: 需要 `sys:user:delete` 权限。
* **路径参数**:
    * `id` (Integer, 必填): 要删除的用户ID。
* **请求体**: 无
* **成功响应**: `WebResponse<Void>`
    ```json
    {
      "status": 200,
      "message": "删除成功",
      "data": null
    }
    ```
* **失败响应示例**:
    ```json
    {
      "status": 401, // 或其他错误码
      "message": "用户名未找到", // 当用户不存在时
      "data": null
    }
    ```
* **注意**: 删除用户时，会同时删除与该用户相关的角色关联信息。

## 6. 知识图谱模块 (`/knowledge-graph`)



提供与代码安全知识图谱相关的查询接口。

### 6.1. 查询代码模式

* **接口**: `GET /knowledge-graph/code-patterns`
* **描述**: 根据编程语言和漏洞类型从知识图谱中查询相关的代码模式。
* **请求参数**:
    * `language` (String, 必填): 编程语言，例如 "Java", "Python"。
    * `vulnerabilityType` (String, 必填): 漏洞类型（可以是CWE ID或漏洞名称的关键词）。
* **成功响应**: `WebResponse<String>` (响应体中的 data 字段是一个格式化的字符串，包含了查询到的代码模式)
    ```json
    {
      "status": 200,
      "message": "查询成功",
      "data": "漏洞类型: SQL Injection\n代码模式:\n```java\nStatement stmt = conn.createStatement();\nResultSet rs = stmt.executeQuery(\"SELECT * FROM users WHERE name = '\" + userInput + \"'\");\n```\n\n漏洞类型: Cross-Site Scripting (XSS)\n代码模式:\n```javascript\ndocument.getElementById('output').innerHTML = userComment;\n```\n"
    }
    ```
  或者未找到相关代码模式时：
    ```json
    {
      "status": 200,
      "message": "查询成功",
      "data": "未找到相关代码模式"
    }
    ```
* **失败响应示例**:
    ```json
    {
      "status": 500,
      "message": "查询失败: 连接Neo4j数据库出错",
      "data": null
    }
    ```

## 7. 生成代码管理模块 (`/generated-code`)

处理已生成代码的查询和管理。

### 7.1. 获取用户生成的代码列表

* **接口**: `GET /generated-code/list`
* **描述**: 获取当前登录用户的所有可见生成代码列表，按创建时间降序排列。
* **请求体**: 无
* **成功响应**: `WebResponse<List<GeneratedCodeDocument>>`
    ```json
    {
      "status": 200,
      "message": "获取成功",
      "data": [
        {
          "id": "uuid_string1",
          "content": "def quick_sort(arr):\n    # 代码内容...",
          "language": "python",
          "prompt": "用Python实现快速排序算法",
          "aiModel": "deepseek",
          "userId": 1,
          "createdAt": "2025-05-10T12:30:45",
          "scanStatus": "COMPLETED",
          "isVisible": true
        },
        {
          "id": "uuid_string2",
          "content": "public class HelloWorld {\n    // 代码内容...",
          "language": "java",
          "prompt": "生成一个Java的Hello World程序",
          "aiModel": "claude",
          "userId": 1,
          "createdAt": "2025-05-09T16:20:10",
          "scanStatus": "COMPLETED",
          "isVisible": true
        }
      ]
    }
    ```
* **失败响应示例**:
    ```json
    {
      "status": 401,
      "message": "未登录或会话已过期，请重新登录",
      "data": null
    }
    ```
* **注意**:
    * 用户必须已登录。
    * 只返回 `isVisible=true` 的代码记录。

### 7.2. 获取代码详情

* **接口**: `GET /generated-code/{codeId}`
* **描述**: 获取指定ID的生成代码详情。
* **路径参数**:
    * `codeId` (String, 必填): 生成代码的ID。
* **请求体**: 无
* **成功响应**: `WebResponse<GeneratedCodeDocument>`
    ```json
    {
      "status": 200,
      "message": "获取成功",
      "data": {
        "id": "uuid_string1",
        "content": "def quick_sort(arr):\n    # 完整代码内容...",
        "language": "python",
        "prompt": "用Python实现快速排序算法",
        "aiModel": "deepseek",
        "userId": 1,
        "createdAt": "2025-05-10T12:30:45",
        "scanStatus": "COMPLETED",
        "isVisible": true
      }
    }
    ```
* **失败响应示例**:
    ```json
    {
      "status": 500,
      "message": "代码不存在",
      "data": null
    }
    ```
* **注意**:
    * 用户只能查看属于自己且 `isVisible=true` 的代码。

### 7.3. 删除生成代码

* **接口**: `DELETE /generated-code/{codeId}`
* **描述**: 逻辑删除指定ID的生成代码（将 `isVisible` 设置为 `false`）。
* **路径参数**:
    * `codeId` (String, 必填): 要删除的生成代码的ID。
* **请求体**: 无
* **成功响应**: `WebResponse<String>`
    ```json
    {
      "status": 200,
      "message": "代码已成功删除",
      "data": "代码已成功删除"
    }
    ```
* **失败响应示例**:
    ```json
    {
      "status": 500,
      "message": "代码不存在",
      "data": null
    }
    ```
* **注意**:
    * 这是一个逻辑删除操作，实际上只是将代码记录的 `isVisible` 字段设置为 `false`。
    * 数据仍然保留在数据库中，但在前端界面和查询结果中不再可见。
    * 用户只能删除属于自己的代码。

请注意，所有需要认证的接口都必须在请求头中包含有效的 `Authorization` JWT Token。错误码和错误信息可能会根据具体情况有所不同，详细请参考后端 `REnum.java` 中的定义。

`