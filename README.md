# AICodeGuard: AI 驱动的代码安全与生成平台

[![语言](https://img.shields.io/badge/language-Java-orange.svg)](https://www.java.com)
[![框架](https://img.shields.io/badge/framework-Spring%20Boot-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![构建工具](https://img.shields.io/badge/build-Maven-red.svg)](https://maven.apache.org/)
欢迎来到 AICodeGuard！本项目是一个基于 AI 大模型的智能应用，旨在提供代码生成、代码安全漏洞检测以及知识图谱构建与查询等功能。

## ✨ 主要功能

* **🤖 AI 代码生成**:
    * 支持通过与多种 AI 大模型（如 OpenAI GPT 系列、Anthropic Claude 系列、Deepseek 等）进行交互来生成代码片段或完整程序。
    * 提供流式（Streaming）和非流式响应。
    * 支持多轮对话管理，保留上下文信息。
* **🛡️ 代码安全扫描**:
    * 利用 AI 模型分析代码，识别潜在的安全漏洞和风险。
    * 生成漏洞报告，并存储于 MongoDB。
* **🕸️ 知识图谱**:
    * 构建代码模式、漏洞信息及其关系的知识图谱（使用 Neo4j）。
    * 提供知识图谱查询接口，用于分析和理解代码结构与安全问题。
    * 包含定时任务维护知识图谱。
* **👤 用户认证与授权**:
    * 基于 Apache Shiro 和 JWT 实现用户注册、登录、密码更新功能。
    * 实现基于角色的访问控制（RBAC），保护 API 接口。
* **💾 数据持久化**:
    * 使用 MySQL 存储用户信息、角色、权限等关系型数据。
    * 使用 MongoDB 存储对话记录、生成的代码文档、漏洞报告等非结构化数据。
    * 使用 Neo4j 存储知识图谱数据。
    * 使用 Redis 进行会话缓存等。

## 🛠️ 技术栈

* **后端**: Java, Spring Boot
* **数据库**: MySQL, MongoDB, Neo4j, Redis
* **安全框架**: Apache Shiro, JWT
* **AI 集成**:
    * [openai-java](https://github.com/TheoKanning/openai-java) (或类似 SDK)
    * [anthropic-sdk-java](https://github.com/anthropic-ai/anthropic-sdk-java) (或类似 SDK)
    * 自定义 Deepseek API 客户端
* **构建工具**: Maven
* **其他**: Lombok, Spring Data JPA, Spring Data MongoDB, Spring Data Neo4j, Spring Data Redis, RestTemplate

## 🚀 快速开始

### 1. 环境准备

* JDK 17 或更高版本
* Maven 3.6+
* MySQL 8.0+
* MongoDB 4.4+
* Neo4j 4.x+
* Redis 6.0+
* 有效的 OpenAI, Anthropic, Deepseek API 密钥 (如果需要使用对应的 AI 模型)

### 2. 配置

* **数据库配置**:
    * 修改 `src/main/resources/application.yml` 文件，配置 MySQL, MongoDB, Neo4j, Redis 的连接信息（地址、端口、用户名、密码等）。
* **AI 模型配置**:
    * 在 `application.yml` 中配置 `ai.models` 部分，填入你的 OpenAI, Anthropic, Deepseek 的 API 密钥和基础 URL (如果需要代理)。
    * ```yaml
      ai:
        models:
          openai:
            api-key: "sk-YOUR_OPENAI_API_KEY"
            base-url: "[https://api.openai.com/v1/](https://api.openai.com/v1/)" # 或者你的代理地址
          claude:
            api-key: "YOUR_ANTHROPIC_API_KEY"
            base-url: "[https://api.anthropic.com/v1/](https://api.anthropic.com/v1/)" # 或者你的代理地址
          deepseek:
            api-key: "sk-YOUR_DEEPSEEK_API_KEY"
            base-url: "[https://api.deepseek.com/](https://api.deepseek.com/)" # 或者你的代理地址
      ```
* **JWT 配置**:
    * 在 `application.yml` 中配置 `shiro.jwt.secret`，设置一个安全的密钥。
    * ```yaml
      shiro:
        jwt:
          secret: "YOUR_SECURE_JWT_SECRET" # 替换为一个强随机密钥
          expire-time-in-minute: 60 # Token 过期时间（分钟）
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
    java -jar target/aicodeguard-*.jar
    ```
    或者在你的 IDE 中直接运行 `com.ai.aicodeguard.AiCodeGuardApplication` 类。

### 5. 访问应用

应用启动后，可以通过 API 工具（如 Postman, Insomnia）或自定义前端访问以下主要端点：

* **认证**: `/api/auth/register`, `/api/auth/login`, `/api/auth/update-password`
* **代码生成**: `/api/codegen/generate`, `/api/codegen/generate-stream`
* **对话管理**: `/api/conversation/create`, `/api/conversation/{conversationId}/send`, `/api/conversation/{conversationId}/send-stream`
* **代码安全**: `/api/security/scan` (具体端点可能需要查看 `CodeSecurityController`)
* **知识图谱**: `/api/graph/query` (具体端点可能需要查看 `KnowledgeGraphController`)
* **用户管理**: `/api/users/...` (具体端点可能需要查看 `UserController`)

*注意*: 大部分接口需要通过登录获取 JWT Token，并在请求头中携带 `Authorization: Bearer <YOUR_TOKEN>`。

## 📚 技术文档

项目包含详细的技术设计文档，位于 `TechnicalDetails` 目录下，涵盖了认证、代码生成、知识图谱等模块的设计思路和实现细节。

* `TechnicalDetails/auth.md`, `auth_2.md`: 认证授权设计。
* `TechnicalDetails/code_generation.md`, `code_generation_2.md`: 代码生成流程。
* `TechnicalDetails/code-guard.md`, `code-guard-graph.md`: 代码安全与知识图谱设计。
* `TechnicalDetails/openai-java-README.md`, `anthropic-sdk-java-README.md`: 使用的 AI SDK 相关信息。

## 🔭 未来展望

项目未来的发展方向可能包括：

* 更智能的代码漏洞修复建议。
* 支持更多编程语言的代码生成和安全扫描。
* 集成静态代码分析工具 (SAST)。
* 优化知识图谱的构建和查询效率。
* 开发更完善的前端用户界面。
* 增强 AI 模型的微调能力。

## 🙌 贡献

欢迎对 AICodeGuard 项目做出贡献！你可以通过以下方式参与：

1.  报告 Bug 或提出功能建议：在 GitHub Issues 中创建新的 issue。
2.  提交代码：Fork 本仓库，创建新的分支进行开发，然后提交 Pull Request。

请确保遵循良好的编码规范和提交信息格式。

## 📄 许可证

[请在此处添加你的开源许可证信息，例如 MIT, Apache 2.0 等]

---

*这个 README 文件是根据项目代码和文档自动生成的。*
