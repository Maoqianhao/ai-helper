# Configuration Guide

该目录用于存放部署层配置说明，不建议在此提交真实密钥。

## 推荐做法

1. 参考 `src/main/resources/application-example.yaml`
2. 在运行环境中通过环境变量注入敏感配置
3. 本地开发使用 `application-local.yaml`（已在 `.gitignore` 忽略）

## 核心环境变量

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DASHSCOPE_API_KEY`
- `ZHIPU_API_KEY`
- `AMAP_API_KEY`
- `CHROMA_BASE_URL`
- `NODE_HOME`
