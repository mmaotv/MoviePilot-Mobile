# 贡献指南

感谢你对 MoviePilot Mobile 的兴趣！我们欢迎各种形式的贡献。

## 如何贡献

### 报告 Bug

如果你发现了 Bug，请通过 [GitHub Issues](https://github.com/mmaotv/MoviePilot-Mobile/issues) 报告，并包含以下信息：

- 问题的清晰描述
- 复现步骤
- 预期行为与实际行为
- 设备信息（型号、Android 版本、App 版本）
- 相关截图（如有）

### 提交功能请求

如果你有新功能的想法，请：

1. 先搜索现有 Issues，避免重复
2. 创建新的 Issue，使用 "Feature request" 模板
3. 清晰描述功能和使用场景

### 提交代码

1. Fork 本仓库
2. 创建你的功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开一个 Pull Request

#### 代码规范

- 使用 TypeScript 编写代码
- 遵循现有的代码风格
- 确保通过 ESLint 检查：`yarn lint`
- 提交前进行类型检查：`yarn type-check`

### 开发环境设置

```bash
# 克隆仓库
git clone https://github.com/mmaotv/MoviePilot-Mobile.git
cd MoviePilot-Mobile

# 安装依赖
yarn install

# 启动开发服务器
yarn dev

# 构建 Android 项目
yarn build
npx cap sync android
npx cap open android
```

## 行为准则

- 使用友善和包容的语言
- 尊重不同的观点和经验
- 优雅地接受建设性批评
- 关注对社区最有利的事情

## 许可证

通过贡献代码，你同意你的贡献将在 MIT 许可证下发布。
