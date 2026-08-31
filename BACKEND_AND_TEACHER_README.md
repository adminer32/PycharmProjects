# GoBackend 与教师端 代码说明文档

> 适用代码范围：
> - `GoBackend/` — 统一后端服务（Go 1.23 + Gin + GORM）
> - `教师端/` — 教师端前后端（Vue 3 前端 + Spring Boot 3 多模块后端）

---

## 一、整体定位

翎翼毽球教学平台由两套前后端组成，本文档覆盖其中两段：

| 模块 | 端口 | 角色 |
| --- | --- | --- |
| `GoBackend` | `8001` | 统一后端：主要服务学生端（AI、动作分析、视频、课程、体质、训练计划等），同时暴露 `/api/v0/teacher/*` 给教师端的轻量查询 |
| `ling-xi-zhi-jian-frontend` | `5173` | 学生端 Vue 3 前端（本文档不展开） |
| `教师端/my-vue-app` | `5173`（开发） | 教师端 Vue 3 前端 |
| `教师端/teacher-java` | `9002` | 教师端 Spring Boot 后端（提供 home / homework / learning / classes / classroom / AI 教学建议 等业务） |

> 历史后端 `FastAPIBackend/` 与 `Springboot/` 已废弃，不再维护。

---

## 二、GoBackend

### 2.1 技术栈

| 层 | 选型 |
| --- | --- |
| 语言 / 框架 | Go 1.23 + Gin 1.10 |
| ORM | GORM 1.25 + MySQL 驱动 |
| 缓存 | go-redis v9 |
| 鉴权 | JWT HS512（`golang-jwt/jwt/v5`）+ bcrypt |
| WebSocket | gorilla/websocket |
| AI | DeepSeek（`deepseek-chat`）/ DashScope（占位） |
| 配置 | `configs/config.json` + `internal/config` |
| 静态资源 | `/videos/*` 课程视频、`/uploads/*` 用户上传 |

### 2.2 目录结构

```
GoBackend/
├── cmd/server/main.go           # 入口：初始化 DB/Redis/JWT，组装所有路由
├── configs/config.json          # 配置文件
├── internal/
│   ├── config/                  # 配置加载（基于 encoding/json）
│   ├── database/                # GORM 初始化 + AutoMigrate + 事务封装 (tx.go)
│   ├── cache/                   # Redis 客户端
│   ├── model/                   # 全部 GORM 模型
│   ├── middleware/auth.go       # JWT 中间件
│   ├── router/router.go         # Deps{DB, RDB, Config} 依赖注入
│   ├── handler/                 # 业务处理器（functional style）
│   │   ├── user/                # 登录、Token、游客 Token、文件上传
│   │   ├── ai/                  # AI 聊天、教练、笔记管理、模型列表
│   │   ├── content/             # 动作分析保存/历史/转录
│   │   ├── course/              # 课程查询、教师课程
│   │   ├── video/               # 播放进度、历史
│   │   ├── schedule/            # 日程
│   │   ├── student/             # 学习统计、体质、可视化
│   │   └── transcribe/          # 视频转录 + AI 分析
│   ├── service/                 # 业务服务层（按需新增）
│   └── websocket/               # WebSocket：AI 聊天、教师通知
├── pkg/                         # 通用组件
│   ├── jwt/, response/, util/, userutil/
├── scripts/                     # 工具：init_database.sql、gen_hash、test_login
└── server.exe                   # 本地编译产物
```

### 2.3 常用命令

```sh
cd GoBackend

# 编译
go build ./...

# 开发运行（默认监听 :8001）
go run cmd/server/main.go

# 静态检查
go vet ./...

# 测试
go test -race ./...
```

> 入口直接读取 `configs/config.json`；如需本地覆盖，可临时把 `password`、`jwt.secret`、`deepseek.api_key` 改为本地值。生产请通过环境变量/密钥管理注入，**不要把真实密钥提交到仓库**。

### 2.4 关键路由

`main.go` 集中注册所有路由，分四组：

#### 公开路由（无 JWT）

| Method | Path | Handler |
| --- | --- | --- |
| POST | `/token` | `user.Login`（账号密码登录，返回 access + refresh） |
| GET  | `/token` | `user.CheckToken` |
| POST | `/token/refresh` | `user.RefreshToken` |
| GET  | `/vtoken` | `user.CheckVToken`（游客 token） |
| POST | `/vtoken` | `user.CreateVToken` |
| GET  | `/captcha` | `user.GetCaptcha` |
| POST | `/file/operation/upload` | `user.UploadFile` |
| GET  | `/videos/*`, `/uploads/*` | 静态资源 |

#### `/api/*`（需 JWT）

动作分析 / 课程 / 视频 / 笔记润色 / 可视化 / 内容等：

```
POST   /api/analysis/save
GET    /api/analysis/history
GET    /api/analysis/queryByTaskId
GET    /api/analysis/generateTrainingPlan

GET    /api/physique/query
POST   /api/physique/create
PUT    /api/physique/update

POST   /api/course/query
GET    /api/course/getNoteById

GET    /api/video/progress
POST   /api/video/progress
POST   /api/video/history/save | query
GET    /api/video/history/detail
DELETE /api/video/history/del

POST   /api/ai/NotePolishing

GET    /api/visualization/getMotionNameAnalysis
GET    /api/visualization/queryHistoricalTrends
GET    /api/visualization/getStatsByCreateBy

GET    /api/content/queryCategory
POST   /api/content/insert
POST   /api/system/user/personalCenterSave
```

#### `/api/v0/ai/*`（需 JWT，AI 对话）

```
POST   /api/v0/ai/chat
POST   /api/v0/ai/coach/chat
GET    /api/v0/ai/models
GET    /api/v0/ai/health
POST   /api/v0/ai/queryPersonalNotes
POST   /api/v0/ai/addTranscriptionTask
POST   /api/v0/ai/updateNote
DELETE /api/v0/ai/del
GET    /api/v0/ai/queryTaskStatus
```

#### `/api/v0/student/*`（需 JWT，学生学习域）

```
GET    /api/v0/student/learning/stats/:studentId
GET    /api/v0/student/learning/action-scores/:studentId
GET    /api/v0/student/learning/shuttlecock-skills/:studentId
GET    /api/v0/student/learning/weekly-trend/:studentId
GET    /api/v0/student/learning/checkin-records/:studentId
POST   /api/v0/student/learning/practice-record
GET    /api/v0/student/learning/practice-records/:studentId
GET    /api/v0/student/learning/practice-record/:recordId
DELETE /api/v0/student/learning/practice-record/:recordId

GET    /api/v0/student/schedule/query
POST   /api/v0/student/schedule/save
DELETE /api/v0/student/schedule/del

GET/POST/PUT  /api/v0/student/physique/
GET/POST      /api/v0/student/auth/
GET    /api/v0/student/users/profile
```

> 教学端 token 校验走 `/api/v0/student/auth/token`（不走 `jwtAuth` 中间件，自行返回 `valid/invalid`）。

#### `/api/v0/teacher/*`（需 JWT，教师端轻量接口）

```
GET    /api/v0/teacher/lessons
GET    /api/v0/teacher/lessons/:lessonId
```

> 当前仅暴露“教师课程列表 + 课程详情”。重业务（homework、classroom、learning、home、profile 等）由 `教师端/teacher-java` 提供。

#### `/api/v0/transcribe/*`（需 JWT）

```
POST   /api/v0/transcribe/transcribe
POST   /api/v0/transcribe/analyze
POST   /api/v0/transcribe/full-process
GET    /api/v0/transcribe/health
```

#### WebSocket

| Path | 处理 |
| --- | --- |
| `/ws/ai` | `websocket.HandleAIChat`（AI 流式对话） |
| `/ws/teacher/notification` | `websocket.HandleTeacherNotification`（ping/pong 通道） |

### 2.5 Handler 风格

所有 handler 使用 functional style，**通过 `router.Deps` 注入依赖**：

```go
func SaveAnalysis(deps *router.Deps) gin.HandlerFunc {
    return func(c *gin.Context) {
        // ...
    }
}
```

路由注册：

```go
api.POST("/analysis/save", content.SaveAnalysis(deps))
v0.POST("/chat", ai.Chat(deps))
```

### 2.6 配置项速查（`configs/config.json`）

| 字段 | 用途 |
| --- | --- |
| `server_addr` | 监听地址，默认 `:8001` |
| `video_base_path` | 课程视频根目录，映射到 `/videos/*` |
| `uploads_path` | 用户上传目录，映射到 `/uploads/*` |
| `database` | MySQL 连接（`aiidecn` 库已弃用，新库 `lingxi_jianqiu`） |
| `redis` | Redis 连接 |
| `jwt.secret` / `jwt.access_expiry` / `jwt.refresh_expiry` | 签名密钥与有效期 |
| `deepseek.api_key` / `deepseek.base_url` / `deepseek.model` | DeepSeek 配置 |
| `dashscope.api_key` | DashScope（占位） |
| `sendgrid.api_key` | 邮件（暂未启用） |
| `cloudflare_turnstile.*` | Cloudflare Turnstile 验证 |

### 2.7 数据库初始化

`scripts/init_database.sql` 含完整建表语句；`internal/database/init.go` 也会调用 `AutoMigrate` 兜底。建议首次部署：

```sh
mysql -uroot -p < scripts/init_database.sql
go run cmd/server/main.go   # AutoMigrate 兜底
```

---

## 三、教师端（`教师端/`）

`教师端/` 下并存两个子项目，是**前端 + 后端分离**的教师侧应用：

```
教师端/
├── my-vue-app/    # Vue 3 + Vite + Element Plus + ECharts 教师前端
└── teacher-java/  # Spring Boot 3.4 + MyBatis-Plus + Sa-Token 教师后端
```

### 3.1 `my-vue-app`（教师前端）

#### 技术栈

| 类别 | 选型 |
| --- | --- |
| 框架 | Vue 3.5 + TypeScript 5.9 + Vite 7 |
| 路由 | vue-router 5（`createWebHistory`） |
| 状态 | Pinia 3 |
| UI 库 | Element Plus 2.13 + `@element-plus/icons-vue` |
| 图表 | ECharts 6 + `echarts-liquidfill` + `vue-echarts` |
| 样式 | Tailwind CSS 4 + `@tailwindcss/postcss` |
| HTTP | Axios 1.13 |
| Mock | mockjs |
| 部署 | Cloudflare Pages（`wrangler`） |
| 检测 | `yolov8n.pt` + `yolo_server.py`（项目内带 YOLOv8 推理脚本） |

#### 常用命令

```sh
cd 教师端/my-vue-app
npm install           # 依赖已锁定（package-lock.json）
npm run dev           # 开发：默认 5173，代理 /api -> :9002
npm run build         # 类型检查 + 生产构建
npm run preview       # 本地预览（基于 wrangler pages dev）
npm run deploy        # 构建 + 推送到 Cloudflare Pages
npm run lint          # oxlint + eslint
npm run format        # prettier
npm run type-check    # vue-tsc --build
```

> Node 版本要求：^20.19.0 || >=22.12.0

#### 目录结构

```
my-vue-app/
├── src/
│   ├── api/           # 按业务域封装的 axios 请求
│   ├── assets/        # 静态资源
│   ├── components/    # 通用组件
│   ├── constants/     # 常量
│   ├── hooks/         # 组合式函数
│   ├── router/        # 路由
│   ├── store/, stores/# Pinia 状态
│   ├── types/         # 全局类型
│   ├── utils/         # 工具
│   ├── views/         # 页面级组件
│   │   ├── Home/index.vue          # 教师工作台首页
│   │   ├── Classroom/index.vue     # 课堂管理（视频回放）
│   │   ├── Learning/index.vue      # 学习管理（AI 课堂）
│   │   ├── analysis/index.vue      # 学情分析
│   │   ├── Homework/               # 作业管理 + 批改
│   │   ├── Profile.vue             # 个人信息
│   │   └── Login.vue / ForgotPasswordView.vue
│   └── App.vue, main.ts
├── public/
├── vite.config.ts
├── wrangler.jsonc
├── yolo_server.py     # YOLOv8 推理服务（毽球检测）
└── yolov8n.pt
```

#### 路由与权限

`src/router/index.ts` 中 `requireAuth: true` 的路由统一由 Pinia `userStore.token` 守门：

```
/login            登录（公开）
/                 重定向到 /home
/home             首页 - 我的学习工作台
/classroom        课堂管理 - 视频回放
/learning         学习管理 - AI 课堂
/analysis         学情分析
/homework         作业管理
/homework/grade   批改作业
/profile          个人信息
```

#### 代理配置（`vite.config.ts`）

```ts
server: {
  proxy: {
    '/api':    { target: 'http://localhost:9002', changeOrigin: true },
    '/videos': { target: 'http://localhost:9002', changeOrigin: true }
  }
}
```

> `/api` 与 `/videos` 都转发到 `localhost:9002`，对应 `teacher-java` 的默认端口。如 `teacher-java` 端口变更，需同步修改这里。

### 3.2 `teacher-java`（教师后端）

#### 技术栈

| 类别 | 选型 |
| --- | --- |
| 语言 / 框架 | Java 21 + Spring Boot 3.4.4 |
| 构建 | Maven（多模块） |
| 持久层 | MyBatis-Plus 3.5.9 + MyBatis 3.0.5 + 自研 `BaseJdbcDao` |
| 工具库 | Guava 33、Hutool 5.8、Fastjson2 2.0.54 |
| 鉴权 | sa-token 1.39（`sa-token-spring-boot3-starter`） |
| 媒体 | JavaCV 1.5.11（含 FFmpeg） |
| API 文档 | Knife4j 4.5（`knife4j-openapi3-jakarta-spring-boot-starter`） |
| AI | 自封装 `DeepSeekClient` |

#### 模块划分（Maven 多模块）

```
teacher-java/
├── common/                # 公共依赖（pom 聚合）
│   ├── common-core/       # 实体、DTO、DAO 基础、Sa-Token 拦截器、全局异常、Web 层封装
│   ├── common-jdbc/       # JDBC 工具、SQL 执行器（MySQL/PostgreSQL）
│   └── common-knife4j/    # Knife4j 配置
└── service/               # 业务服务主模块
    └── src/main/java/com/system/service/
        ├── auth/          # 登录、UserController、UserMapper
        ├── ai/            # DeepSeek 客户端、AI 控制器、教学建议（TeachingAdvice）
        ├── classes/       # 班级管理
        ├── classroom/     # 课堂（视频回放）
        ├── common/        # 文件上传
        ├── home/          # 教师工作台首页统计
        ├── homework/      # 作业创建/批改/提交/反馈
        ├── learning/      # 学习管理：动作视频、AI 训练计划
        └── profile/       # 教师个人信息
```

每个业务子模块遵循 `controller / service / service.impl / mapper / entity / dto / vo` 的标准分层。

#### 常用命令

```sh
cd 教师端/teacher-java

# 编译
mvn -q -DskipTests clean package

# 本地启动 service 模块
mvn -pl service -am spring-boot:run

# 运行测试
mvn test
```

> `service` 模块通常才是可启动的 Spring Boot 应用入口；`common/*` 是被依赖的公共库。
> 默认端口可在 `service/src/main/resources/application*.yml` 中调整，前端代理目标也需同步（`vite.config.ts` 中 `localhost:9002`）。

#### 鉴权

- 基于 `sa-token` 的无状态登录：`StpInterfaceImpl` 实现权限/角色装载；
- `WebMvcInterceptorConfig` + `MyInterceptor` 注册拦截器；
- 登录、刷新、注销等接口放在 `auth/controller/UserController.java`。

#### AI 教学建议

`service/ai/` 内含：

- `DeepSeekClient` — DeepSeek HTTP 客户端封装
- `DeepSeekConfig` — 读取 `deepseek.api-key` 等配置
- `AiController` — `/ai/generatePlan`、`/ai/generateHomework` 等
- `AiTeachingAdviceService` — 教学建议持久化与查询

输入/输出 VO 见 `ai/vo/`：`AiGeneratePlanInput`、`AiPlanResult`、`AiHomeworkResult`、`AiTeachingAdvice` 等。

---

## 四、协作关系

```
+-------------------+         HTTP/WS          +-------------------------+
| 学生端 Vue (5173) |  ─────────────────────► |  GoBackend  (8001)      |
+-------------------+                         +-------------------------+

+-------------------+         HTTP             +-------------------------+
| 教师端 Vue (5173) |  ─────────────────────► |  teacher-java  (9002)   |
|   (my-vue-app)    |  /api, /videos 代理     +-------------------------+
+-------------------+

                                                ┌── GoBackend  /api/v0/teacher/*（轻量教师接口）
                                                │
                                                └── teacher-java（重业务）
```

- **学生侧**：学生端 Vue → GoBackend（8001）。包含 AI 聊天（WebSocket）、动作分析、视频转录、训练计划、学习统计等。
- **教师侧**：教师端 Vue → teacher-java（9002）。包含教师工作台、作业、课堂回放、学情、AI 教学建议等。GoBackend 内的 `/api/v0/teacher/*` 作为**只读课程目录**的轻量旁路。
- **静态视频**：`GoBackend` 暴露 `/videos/*`（映射到 `video_base_path`），可同时服务学生与教师侧的视频播放。
- **AI**：
  - 学生对话 / 教练 → DeepSeek（`internal/handler/ai/ai.go`）
  - 教师训练计划 / 作业生成 → DeepSeek（`teacher-java/service/ai`）

---

## 五、本地启动顺序

1. **启动 MySQL + Redis**（端口分别 3306 / 6379）。
2. **导入数据**（首次）：
   ```sh
   mysql -uroot -p < GoBackend/scripts/init_database.sql
   # teacher-java 使用的库如需初始化，按 service 模块 README / SQL 脚本执行
   ```
3. **启动 GoBackend**：
   ```sh
   cd GoBackend && go run cmd/server/main.go
   ```
4. **启动 teacher-java**（教师后端）：
   ```sh
   cd 教师端/teacher-java && mvn -pl service -am spring-boot:run
   ```
5. **启动教师前端**：
   ```sh
   cd 教师端/my-vue-app && npm run dev
   ```
6. 访问 `http://localhost:5173`（教师）或 `http://localhost:5173`（学生端启动后）即可。

---

## 六、后续待办（建议）

- [ ] `teacher-java` 端口与数据库配置与前端代理保持一致（目前 Vite 代理写死 9002）。
- [ ] GoBackend 的教师侧能力（`/api/v0/teacher/*`）与 `teacher-java` 的边界需明确：是否长期保留两套后端，还是逐步把教师重业务迁回 GoBackend。
- [ ] `GoBackend` 内 `password / api_key / jwt.secret` 当前明文写在 `configs/config.json`，**生产前必须改为环境变量注入**。
- [ ] 给 GoBackend 与 `teacher-java` 分别补齐单元/集成测试（`go test -race` 与 JUnit 5）。
- [ ] 完善教师端 E2E（Playwright 覆盖登录、作业批改、AI 教学建议等关键流程）。

---

## 七、参考脚本与文档

- `GoBackend/scripts/init_database.sql` — 数据库建表
- `GoBackend/scripts/gen_hash*.go` — 密码哈希生成器（用于初始化用户）
- `GoBackend/scripts/test_login.go` — 登录冒烟测试
- `GoBackend/server.exe` — 本地已编译产物（请勿提交二进制到生产仓库）
- `教师端/my-vue-app/yolo_server.py` — YOLOv8 毽球检测推理脚本（开发期本地跑）

---

> 维护者请在改动路由或拆/合模块时同步更新本文档对应小节。
