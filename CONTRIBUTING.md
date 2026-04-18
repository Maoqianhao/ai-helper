# Contributing Guide

感谢你对 `ai-helper` 的关注，欢迎通过 Issue 和 Pull Request 参与共建。

## 开发前准备

- JDK 21
- Maven（推荐使用仓库自带 `mvnw`）
- MySQL 8+
- Chroma
- Node.js

## 分支与提交流程

1. Fork 仓库并创建分支：`feat/xxx`、`fix/xxx`
2. 保持提交原子性，提交信息建议遵循 Conventional Commits：
   - `feat: ...`
   - `fix: ...`
   - `docs: ...`
   - `refactor: ...`
   - `test: ...`
3. 提交前本地自检：
   - 可编译通过：`./mvnw clean verify`（Windows 使用 `mvnw.cmd`）
   - 核心流程不回归
   - 文档同步更新
4. 发起 PR，说明变更动机、实现方式、验证步骤和影响范围

## 代码规范

- 不提交密钥、Token、密码、个人隐私信息
- 不提交本地产物（日志、缓存、构建目录）
- 保持命名清晰、注释简洁、结构一致
- 变更应尽量不破坏现有业务行为

## Issue 反馈规范

请尽量包含：

- 运行环境（OS、JDK、数据库版本）
- 复现步骤
- 期望行为与实际行为
- 关键日志/报错截图（脱敏后）

## 法律与合规

- 仅提交你有权使用与分发的代码和资源
- 引入第三方依赖时请确认许可证兼容性
- 严禁提交商业密钥、生产数据、个人隐私数据
