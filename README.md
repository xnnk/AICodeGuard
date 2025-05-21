# AICodeGuard: AI 驱动的代码安全与生成平台 (实现了自驱型知识图谱AKG - Automated Knowledge Graph)

[![语言](https://img.shields.io/badge/language-Java-orange.svg)](https://www.java.com)
[![框架](https://img.shields.io/badge/framework-Spring%20Boot-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![构建工具](https://img.shields.io/badge/build-Maven-red.svg)](https://maven.apache.org/)

欢迎来到 AICodeGuard！本项目是一个基于 AI 大模型的智能应用，旨在提供代码生成、代码安全漏洞检测、知识图谱构建与查询以及代码的增强分析等功能。

## ✨ 主要功能

* **🤖 AI 代码生成**:
    * 支持通过与多种 AI 大模型（如 OpenAI GPT 系列、Anthropic Claude 系列、Deepseek 等）进行交互来生成代码片段或完整程序。
    * 提供普通（非流式）和流式（Streaming SSE）响应。
    * 支持多轮对话管理，保留上下文信息，并可获取对话历史。
    * 支持获取系统支持的 AI 模型列表。
* **🛡️ 代码安全扫描**:
    * 利用 AI 模型分析代码，识别潜在的安全漏洞和风险。
    * 生成漏洞报告，并存储于 MongoDB。
    * 支持对已生成的代码异步触发安全扫描并获取结果。
* **💡 代码增强分析**:
    * 提供代码生成并结合知识图谱进行增强安全分析的功能。
    * LLM 辅助生成 Cypher 查询语句从知识图谱获取相关安全信息。
    * 结合代码本身和知识图谱数据，生成更全面的安全分析报告。
* **🕸️ 知识图谱 (AKG - Automated Knowledge Graph)**:
    * 自动及通过 AI 辅助构建代码模式、漏洞信息及其关系的知识图谱（使用 Neo4j）。
    * 提供知识图谱查询接口，例如根据语言和漏洞类型查询相关代码模式。
    * 包含定时任务维护和优化知识图谱。
* **👤 用户认证与授权**:
    * 基于 Apache Shiro 和 JWT 实现用户注册、登录、登出、密码更新及 Token 刷新功能。
    * 实现基于角色的访问控制（RBAC），保护 API 接口。
* **💾 数据持久化**:
    * 使用 MySQL 存储用户信息、角色、权限等关系型数据。
    * 使用 MongoDB 存储对话记录、生成的代码文档、漏洞报告等非结构化数据。
    * 使用 Neo4j 存储知识图谱数据。
    * 使用 Redis 进行会话缓存（如对话记录）等。
* **📂 生成代码管理**:
    * 支持获取用户生成的代码列表。
    * 支持查看特定代码的详情。
    * 支持逻辑删除（隐藏）用户生成的代码。

## 🛠️ 技术栈

* **后端**: Java, Spring Boot (v3.0.2)。
* **数据库**: MySQL, MongoDB, Neo4j, Redis。
* **安全框架**: Apache Shiro (v1.12.0), JWT (jjwt v0.11.5)。
* **AI 集成**:
    * 通过自定义客户端或 SDK 与 OpenAI, Anthropic Claude, Deepseek 等大模型 API 交互。
    * 使用 OkHttp, RestTemplate, WebClient 进行 API 调用。
* **构建工具**: Maven。
* **其他**: Lombok, Spring Data JPA, Spring Data MongoDB, Spring Data Neo4j, Spring Data Redis, Hibernate Validator, Jackson。
* **API 文档**: `TechnicalDetails/Interfaces.md` 提供了详细的 API 交互指南。

## 🚀 快速开始

### 1. 环境准备

* JDK 17 或更高版本。
* Maven 3.6+。
* MySQL 8.0+。
* MongoDB 4.4+。
* Neo4j 4.x+。
* Redis 6.0+。
* 有效的 OpenAI, Anthropic, Deepseek API 密钥 (如果需要使用对应的 AI 模型，请参照 `application-dev.yml` 中的配置项)。

### 2. 配置

* **数据库配置**:
    * 修改 `src/main/resources/application-dev.yml` 文件（或您环境对应的配置文件），配置 MySQL, MongoDB, Neo4j, Redis 的连接信息（地址、端口、用户名、密码等）。
* **AI 模型配置**:
    * 在 `application-dev.yml` 中配置 `ai.models` 部分，填入你的 OpenAI, Anthropic, Deepseek 的 API 密钥和基础 URL。代理配置也在 `ai.proxy` 中。
    * 示例 (`application-dev.yml`):
      ```yaml
      ai:
        proxy:
          enabled: true # أو false
          host: localhost
          port: 7890
        models:
          deepseek:
            api-key: "sk-YOUR_DEEPSEEK_API_KEY" # 必须填写
            endpoints:
              completion: [https://api.deepseek.com/chat/completions](https://api.deepseek.com/chat/completions)
              conversation: [https://api.deepseek.com/chat/completions](https://api.deepseek.com/chat/completions)
            model-name: deepseek-chat
          openai:
            api-key: "sk-YOUR_OPENAI_API_KEY" # 必须填写
            endpoints:
              completion: [https://api.openai.com/v1/chat/completions](https://api.openai.com/v1/chat/completions)
              conversation: [https://api.openai.com/v1/chat/completions](https://api.openai.com/v1/chat/completions)
            model-name: o4-mini-2025-04-16 # 请根据实际使用的模型调整
          claude:
            api-key: "YOUR_ANTHROPIC_API_KEY" # 必须填写
            endpoints:
              completion: [https://api.anthropic.com/v1/messages](https://api.anthropic.com/v1/messages)
              conversation: [https://api.anthropic.com/v1/messages](https://api.anthropic.com/v1/messages)
            model-name: claude-3-5-sonnet-20241022 # 请根据实际使用的模型调整
        default-model: deepseek # 指定默认模型
      ```
* **JWT 配置**:
    * 在 `application-dev.yml` 中配置 `jwt.secret` 和 `jwt.expire`。
    * ```yaml
      jwt:
        secret: "YOUR_SECURE_JWT_SECRET" # 替换为一个强随机密钥，例如：符合JWT规范的密钥
        expire: 86400000 # Token 过期时间（毫秒，例如24小时）
      ```

### 3. 数据库初始化

* 连接到你的 MySQL 数据库。
* 执行 `src/main/resources/static/sql.sql` 文件中的 SQL 脚本，创建必要的表和初始数据（例如管理员用户、角色、权限等）。

### 4. 构建与运行

* **使用 Maven 构建**:
    ```bash
    mvn clean install -DskipTests
    ```
   
* **运行应用**:
    ```bash
    java -jar target/AICodeGuard-0.0.1-SNAPSHOT.jar
    ```
    (请将 `AICodeGuard-0.0.1-SNAPSHOT.jar` 替换为 `pom.xml` 中定义的实际 `artifactId` 和 `version` 生成的 jar 包名称)
    或者在你的 IDE 中直接运行 `com.ai.aicodeguard.AiCodeGuardApplication` 类。

### 5. 访问应用

应用启动后，默认 API 基础路径为 `/api` (具体参考 `application-dev.yml` 中 `server.servlet.context-path` 配置)。 可以通过 API 工具（如 Postman, Insomnia）或自定义前端访问以下主要端点 (详细请求/响应格式请参考 `TechnicalDetails/Interfaces.md`):

* **认证模块 (`/auth`)**:
    * `POST /auth/login`
    * `POST /auth/register`
    * `POST /auth/logout`
    * `POST /auth/password` (修改密码)
    * `POST /auth/refresh` (刷新Token)
* **代码生成模块 (`/code-gen`)**:
    * `POST /code-gen/generate` (普通生成)
    * `POST /code-gen/generate-enhanced` (增强分析生成)
    * `GET /code-gen/models` (获取支持的AI模型)
* **对话模块 (`/conversation`)**:
    * `POST /conversation/create`
    * `POST /conversation/send` (非流式发送消息)
    * `POST /conversation/stream` (流式发送消息 SSE)
    * `GET /conversation/{userId}/{conversationId}` (获取对话历史)
    * `DELETE /conversation/{userId}/{conversationId}` (结束对话)
* **代码安全模块 (`/code-security`)**:
    * `POST /code-security/scan/{codeId}` (触发扫描)
    * `GET /code-security/result/{codeId}` (获取扫描结果)
* **知识图谱模块 (`/knowledge-graph`)**:
    * `GET /knowledge-graph/code-patterns` (查询代码模式)
* **用户管理模块 (`/user`)**:
    * `GET /user/list` (需要 `sys:user:view` 权限)
    * `POST /user/update` (需要 `sys:user:edit` 权限)
    * `DELETE /user/{id}` (需要 `sys:user:delete` 权限)
* **已生成代码管理模块 (`/generated-code`)**:
    * `GET /generated-code/list` (获取用户代码列表)
    * `GET /generated-code/{codeId}` (获取代码详情)
    * `DELETE /generated-code/{codeId}` (删除代码)

*注意*: 大部分接口需要通过登录获取 JWT Token，并在请求头中携带 `Authorization: <JWT_TOKEN>`。

## 📚 技术文档

项目包含详细的 API 设计文档，位于 `TechnicalDetails/Interfaces.md`，涵盖了各模块的接口定义、请求参数、响应格式等。

(其他在旧 README 中提及的技术细节文档如 `auth.md`, `code_generation.md` 等未在本次提供的文件列表中，请按需更新此部分。)

## 🔭 未来展望

项目未来的发展方向可能包括：

* 更智能的代码漏洞自动修复建议。
* 支持更多编程语言的代码生成和安全扫描。
* 集成更多类型的静态代码分析工具 (SAST) 和动态分析工具 (DAST)。
* 持续优化知识图谱的自动构建、推理能力和查询效率。
* 开发功能更完善、用户体验更佳的前端用户界面。
* 增强 AI 模型的微调 (Fine-tuning) 能力，以适应特定领域或项目需求。
* 提升对复杂项目和代码仓库的分析能力。

## 🙌 贡献

欢迎对 AICodeGuard 项目做出贡献！你可以通过以下方式参与：

1.  报告 Bug 或提出功能建议：在 GitHub Issues 中创建新的 issue。
2.  提交代码：Fork 本仓库，创建新的分支进行开发，然后提交 Pull Request。

请确保遵循良好的编码规范和提交信息格式。

## 📄 许可证

[MIT LICENSE](https://github.com/xnnk/AICodeGuard/blob/master/LICENSE)

---

*此 README 文件已根据当前项目代码和文档进行更新。*
