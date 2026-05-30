# ling-xi-zhi-jian-frontend

一个基于 Vue 3.5 的现代化前端项目，集成了多种强大的框架和工具，适用于构建高效、可扩展的 Web 应用。

## 项目介绍

`ling-xi-zhi-jian-frontend` 是一个基于 Vue 3.5 的前端项目，使用了 Element Plus、Ant Design Vue 等流行的 UI 框架，以及 Pinia 状态管理库和 Vue Router 路由管理库。此外，项目还集成了 ECharts 和 Three.js 等可视化库，支持国际化（Vue I18n），并使用了 TypeScript 进行类型安全的开发。

## 技术栈

- **框架**：Vue 3.5
- **UI 框架**：Element Plus、Ant Design Vue
- **状态管理**：Pinia
- **路由管理**：Vue Router
- **国际化**：Vue I18n
- **富文本编辑器**：WangEditor for Vue
- **可视化库**：ECharts、Three.js
- **构建工具**：Vite
- **代码规范**：ESLint
- **类型检查**：TypeScript、Vue-TSC

## 项目结构

```
ling-xi-zhi-jian-frontend/
├── public/                   # 静态资源目录
├── src/                      # 源代码目录
│   ├── api/                  # API 接口
│   ├── assets/               # 静态资源（如图片、样式文件等）
│   ├── components/           # 全局组件（ 项目作者开发的UI组件库，现已整合至 [Exploria UI](https://www.npmjs.com/package/exploria-ui) ）
│   ├── constants/            # 自定义常量
│   ├── directives/           # 自定义指令
│   ├── style/                # 全局样式
│   ├── views/                # 页面组件
│   ├── store/                # Pinia 状态管理
│   ├── router/               # 路由配置
│   ├── i18n/                 # 国际化配置
│   ├── types/                # 类型定义
│   ├── utils/                # 工具函数
│   ├── App.vue               # 根组件
│   └── main.ts               # 入口文件
├── eslint.config.ts          # ESLint 配置文件
├── prettier.config.ts        # Prettier 配置文件
├── .env.dev                  # 开发环境下的环境变量
├── .env.prod                 # 生产环境下的环境变量
├── .gitignore                # Git 忽略文件
├── package.json              # 项目依赖配置
├── tsconfig.json             # TypeScript 配置文件
└── vite.config.ts            # Vite 配置文件
```

## 依赖说明

- `vue`: Vue 3.5 核心库
- `@wangeditor/editor-for-vue`: WangEditor 的 Vue 组件
- `animate.css`: CSS 动画库
- `ant-design-vue`: Ant Design Vue 组件库
- `element-plus`: Element Plus 组件库
- `axios`: HTTP 客户端
- `echarts`: 数据可视化库
- `three`: 3D 图形库
- `marked`: Markdown 解析库
- `pinia`: Vue 3 的状态管理库
- `sass`: CSS 预处理器
- `vue-i18n`: Vue 的国际化插件
- `vue-router`: Vue 的路由管理库

## 开发环境

- Node.js: 20.x
- pnpm: 10.x
- TypeScript: 5.x
- Vite: 5.x

## pnpm指令

### 项目初始化

```sh
pnpm i
```

### 项目启动

```sh
pnpm run dev
```

### 为生产进行类型检查、编译和最小化

```sh
pnpm run build
```

### 使用 [ESLint](https://eslint.org/) 进行Lint

```sh
pnpm run lint
```
