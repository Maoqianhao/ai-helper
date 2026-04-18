# AI Helper

`AI Helper` 是一个基于 `Spring Boot + LangChain4j` 的多智能体后端服务，提供流式对话、智能路由、会话记忆、SQL/RAG 检索与小说创作编排能力。

## 项目简介

本项目面向需要构建 AI 助手后端能力的开发者，核心目标是：

- 提供可扩展的多智能体编排框架
- 统一 Prompt 与工具调用治理
- 支持记忆持久化与可控遗忘
- 兼顾开发调试效率与开源可维护性

## 功能亮点

- 双编排策略：`routing`（低延迟）与 `orchestrator`（复杂任务）
- SSE 流式响应：支持按 chunk 实时输出
- 多专家代理：SQL、地图、面试问答等
- Chroma 记忆系统：会话上下文加载、持久化、遗忘
- SQL 安全护栏：默认拦截高风险写操作语义
- Prompt 资产注册：统一追踪模板来源与用途

## 技术栈

- Java 21
- Spring Boot 3.5.x
- Maven Wrapper
- LangChain4j（Agentic / MCP / Chroma / SQL）
- MyBatis + MySQL
- Chroma Vector Database
- Reactor（Flux 流式输出）

## 快速开始

### 环境要求

- JDK 21
- MySQL 8+
- Chroma（默认地址 `http://localhost:8000`）
- Node.js（用于 MCP 地图工具）

### 安装与启动

1. 克隆仓库并进入目录：

```bash
git clone <your-repo-url>
cd ai-helper
```

2. 初始化数据库（执行 `src/main/resources/sql/hmdp.sql`）

3. 配置环境变量（参考 `.env.example`）或修改本地私有配置文件（不要提交到仓库）

4. 启动服务：

Windows:

```powershell
.\scripts\start.ps1
```

Linux/macOS:

```bash
chmod +x ./scripts/start.sh
./scripts/start.sh
```

服务默认地址：`http://localhost:8081/api`

## 核心功能说明

- `routing`：面向通用问题，优先响应速度与调用成本
- `orchestrator`：面向复杂创作，支持多步骤状态推进
- `memory`：按会话隔离上下文并持久化到 Chroma
- `rag`：提取数据库元数据并向量化检索
- `mcp`：接入外部工具（如地图服务、Web 搜索）

## 目录结构

```text
ai-helper
├─ src/
│  ├─ main/
│  │  ├─ java/com/example/aihelper/      # 核心业务代码
│  │  └─ resources/
│  │     ├─ application.yaml             # 默认配置（无真实密钥）
│  │     ├─ application-example.yaml     # 示例配置模板
│  │     ├─ templates/                   # Prompt 模板
│  │     ├─ static/chat-debug.html       # 联调页面
│  │     └─ sql/hmdp.sql                 # 初始化脚本
├─ scripts/
│  ├─ start.ps1                          # Windows 启动脚本
│  ├─ start.sh                           # Linux/macOS 启动脚本
│  └─ run-regression.ps1                 # 回归脚本
├─ docs/
│  └─ OPEN_SOURCE_RELEASE_CHECKLIST.md   # 发布前检查清单
├─ .github/                              # Issue/PR 模板
├─ SECURITY.md
├─ CODE_OF_CONDUCT.md
├─ CONTRIBUTING.md
├─ LICENSE
└─ README.md
```

## 使用示例

流式对话（routing）：

```http
GET /api/ai/chat/routing?memoryId=1&message=你好
Accept: text/event-stream
```

流式对话（orchestrator）：

```http
GET /api/ai/chat/orchestrator?memoryId=1&message=帮我写一篇科幻小说大纲
Accept: text/event-stream
```

会话记忆管理：

```http
DELETE /api/memory/clear/{sessionId}
GET /api/memory/history/{sessionId}
```

## 配置与安全

- 仓库已移除硬编码密钥，统一改为环境变量占位符
- 本地私有配置建议使用 `application-local.yaml`（已在 `.gitignore` 忽略）
- 敏感信息不得提交到仓库（API Key、数据库密码、个人隐私数据）
- 建议在 CI 启用 Secret Scan（如 `gitleaks`）

## 贡献指南

- 贡献流程：`CONTRIBUTING.md`
- 安全上报：`SECURITY.md`
- 社区行为准则：`CODE_OF_CONDUCT.md`

## 许可证

本项目采用 `MIT License`，详见 `LICENSE`。

