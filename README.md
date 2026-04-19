# 🌟 星裁系统 (Star Choice System)

> 🎤 专为大型校园活动（“十佳歌手”比赛）量身定制的全栈实时评分与现场调度平台。

## 📖 项目简介
本项目旨在解决传统线下赛事中纸质打分效率低、统分易错、大屏数据展示不同步等痛点。通过前后端分离架构，实现了评委移动端实时打分、后台数据毫秒级汇总计算，以及现场大屏动态数据可视化展示，保障了大型赛事的数字化平稳运行。

## ✨ 核心功能
* **评委端 (前端)**：支持多评委扫码登录，提供直观的滑动/键盘打分界面，防误触设计。
* **大屏端 (前端)**：采用毛玻璃 (Glassmorphism) UI 设计风格，实时动态展示选手得分、排名及现场互动状态。
* **后台管理 (后端)**：基于 Spring Boot 提供高并发的数据处理接口，支持赛事轮次管理、评委权重配置及成绩导出。
* **高可用保障**：优化了接口响应速度，确保在比赛现场百人级网络环境下，系统数据互通无卡顿。

## 🛠️ 技术栈
* **前端**：Vue3, JavaScript, HTML/CSS, TailwindCSS (根据实际情况修改)
* **后端**：Java, Spring Boot, Maven
* **数据库**：MySQL
* **部署/运维**：Linux (Ubuntu/CentOS), Nginx, 1Panel 面板管理

## 🚀 本地运行指南

### 1. 环境准备
* JDK 1.8+ / Node.js 16+ / MySQL 8.0+

### 2. 后端启动 (scoring-system)
1. 在 MySQL 中创建名为 `scoring_system` 的数据库，并导入数据表。
2. 打开 `scoring-system/src/main/resources/application.properties`，修改你的数据库账号和密码。
3. 进入后端目录，使用 Maven 运行：
   \`\`\`bash
   cd scoring-system
   mvn spring-boot:run
   \`\`\`

### 3. 前端启动 (frontend)
1. 进入前端目录，安装依赖：
   \`\`\`bash
   cd frontend_bak
   npm install
   \`\`\`
2. 启动前端服务：
   \`\`\`bash
   npm run dev
   \`\`\`

## 📦 生产环境部署方案（实施指南）
本项目支持标准化的服务器部署，推荐使用 Linux 环境：
1. **后端构建**：使用 `mvn clean package` 打包出 jar 文件，通过 `nohup java -jar xxx.jar &` 后台运行。
2. **前端构建**：使用 `npm run build` 打包静态文件，放置于服务器目录。
3. **反向代理**：配置 Nginx（或通过 1Panel 面板快速配置），将域名请求代理至前端静态目录及后端 API 接口。

## 👨‍💻 开发者
* **GitHub**: [@XUANFU-h](https://github.com/XUANFU-h)
* **主导工作**：全栈代码开发、系统架构设计、Linux 服务器环境配置与上线部署、比赛现场技术场控。
