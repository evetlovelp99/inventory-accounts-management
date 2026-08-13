# 项目目录结构

## 整体说明
前后端分离，前端 React + Ant Design，后端 Spring Boot，通过 REST API 通信。

```
beewax-system/
├── frontend/                          # React 前端
│   ├── public/
│   │   └── index.html
│   ├── src/
│   │   ├── main.tsx                   # 入口
│   │   ├── App.tsx                    # 路由配置
│   │   │
│   │   ├── assets/                    # 静态资源
│   │   │   └── logo.svg
│   │   │
│   │   ├── styles/                    # 全局样式
│   │   │   ├── theme.ts               # Ant Design token 覆盖（映射 theme.md 色彩系统）
│   │   │   ├── global.css             # CSS 变量、基础重置
│   │   │   └── typography.css         # 字体引入（IBM Plex Mono）
│   │   │
│   │   ├── layouts/                   # 布局组件
│   │   │   ├── AppLayout.tsx          # 主框架（TopBar + SideNav + Content）
│   │   │   ├── TopBar.tsx
│   │   │   └── SideNav.tsx
│   │   │
│   │   ├── components/                # 通用组件（可复用）
│   │   │   ├── StatCard/
│   │   │   │   └── StatCard.tsx
│   │   │   ├── DataTable/
│   │   │   │   └── DataTable.tsx
│   │   │   ├── StatusBadge/
│   │   │   │   └── StatusBadge.tsx
│   │   │   ├── EntryForm/
│   │   │   │   ├── EntryForm.tsx      # 通用表单容器
│   │   │   │   └── BatchRow.tsx       # 出库批次明细行
│   │   │   ├── Button/
│   │   │   │   └── Button.tsx         # 封装 Ant Design Button，应用自定义样式
│   │   │   ├── Toast/
│   │   │   │   └── Toast.tsx
│   │   │   ├── AlertBar/
│   │   │   │   └── AlertBar.tsx
│   │   │   └── FilterToolbar/
│   │   │       └── FilterToolbar.tsx  # 搜索框 + 筛选标签 + 导出按钮
│   │   │
│   │   ├── pages/                     # 页面组件（路由对应）
│   │   │   ├── Login/
│   │   │   │   └── LoginPage.tsx
│   │   │   ├── Dashboard/
│   │   │   │   └── DashboardPage.tsx
│   │   │   ├── Inventory/
│   │   │   │   ├── InboundEntryPage.tsx     # 录入入库
│   │   │   │   ├── OutboundEntryPage.tsx    # 录入出库
│   │   │   │   ├── StockOverviewPage.tsx    # 当前库存
│   │   │   │   └── ProductLedgerPage.tsx    # 单产品流水
│   │   │   ├── Accounts/
│   │   │   │   ├── ReceivablePage.tsx       # 应收账款
│   │   │   │   ├── PayablePage.tsx          # 应付账款
│   │   │   │   └── AccountDetailPage.tsx   # 单客户/供应商账款详情
│   │   │   └── Settings/
│   │   │       ├── ProductsPage.tsx         # 产品管理
│   │   │       ├── UsersPage.tsx            # 用户管理
│   │   │       ├── CustomersPage.tsx        # 客户管理
│   │   │       ├── SuppliersPage.tsx        # 供应商管理
│   │   │       └── ImportPage.tsx           # 历史数据导入
│   │   │
│   │   ├── hooks/                     # 自定义 Hook
│   │   │   ├── useAuth.ts             # 登录状态、角色
│   │   │   ├── useToast.ts            # 全局 Toast 控制
│   │   │   └── usePagination.ts       # 分页状态
│   │   │
│   │   ├── api/                       # API 请求层（axios 封装）
│   │   │   ├── client.ts              # axios 实例、拦截器、token 注入
│   │   │   ├── auth.ts
│   │   │   ├── inventory.ts
│   │   │   ├── accounts.ts
│   │   │   ├── settings.ts
│   │   │   ├── dashboard.ts
│   │   │   └── importExport.ts
│   │   │
│   │   ├── store/                     # 全局状态（Zustand）
│   │   │   ├── authStore.ts           # 当前用户、token
│   │   │   └── alertStore.ts          # AlertBar 内容
│   │   │
│   │   ├── types/                     # TypeScript 类型定义
│   │   │   ├── models.ts              # 与后端数据模型对应的接口
│   │   │   └── api.ts                 # 请求/响应通用类型
│   │   │
│   │   ├── utils/
│   │   │   ├── format.ts              # 金额格式化、日期格式化
│   │   │   ├── permission.ts          # 角色权限判断
│   │   │   └── constants.ts           # 枚举值、状态常量
│   │   │
│   │   └── routes/
│   │       └── ProtectedRoute.tsx     # 登录守卫 + 权限守卫
│   │
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts
│
└── backend/                           # Spring Boot 后端
    ├── pom.xml
    └── src/main/
        ├── java/com/beewax/
        │   ├── BeewaxApplication.java
        │   │
        │   ├── config/
        │   │   ├── SecurityConfig.java      # Spring Security，JWT 配置
        │   │   ├── CorsConfig.java
        │   │   └── JwtConfig.java
        │   │
        │   ├── controller/                  # REST 控制器
        │   │   ├── AuthController.java
        │   │   ├── DashboardController.java
        │   │   ├── InventoryController.java
        │   │   ├── AccountsController.java
        │   │   ├── SettingsController.java
        │   │   └── ImportExportController.java
        │   │
        │   ├── service/                     # 业务逻辑
        │   │   ├── AuthService.java
        │   │   ├── DashboardService.java
        │   │   ├── InventoryService.java
        │   │   ├── AccountsService.java
        │   │   ├── SettingsService.java
        │   │   └── ImportExportService.java
        │   │
        │   ├── repository/                  # JPA Repository
        │   │   ├── UserRepository.java
        │   │   ├── ProductRepository.java
        │   │   ├── CustomerRepository.java
        │   │   ├── SupplierRepository.java
        │   │   ├── InboundRepository.java
        │   │   ├── OutboundRepository.java
        │   │   ├── OutboundBatchLineRepository.java
        │   │   ├── AccountReceivableRepository.java
        │   │   └── AccountPayableRepository.java
        │   │
        │   ├── entity/                      # JPA 实体（对应数据库表）
        │   │   ├── User.java
        │   │   ├── Product.java
        │   │   ├── Customer.java
        │   │   ├── Supplier.java
        │   │   ├── InboundRecord.java
        │   │   ├── OutboundRecord.java
        │   │   ├── OutboundBatchLine.java
        │   │   ├── AccountReceivable.java
        │   │   ├── AccountPayableRecord.java
        │   │   ├── PaymentLog.java          # 收/付款流水
        │   │   └── OperationLog.java        # 操作日志
        │   │
        │   ├── dto/                         # 请求/响应 DTO
        │   │   ├── request/
        │   │   └── response/
        │   │
        │   ├── exception/
        │   │   ├── BusinessException.java
        │   │   └── GlobalExceptionHandler.java
        │   │
        │   └── util/
        │       ├── JwtUtil.java
        │       ├── ExcelUtil.java           # Apache POI 工具类
        │       └── PdfUtil.java             # iText/Flying Saucer 工具类
        │
        └── resources/
            ├── application.yml
            ├── application-dev.yml
            ├── application-prod.yml
            ├── templates/                   # 导入 Excel 模板
            │   ├── inbound_template.xlsx
            │   ├── outbound_template.xlsx
            │   └── accounts_template.xlsx
            └── db/
                └── migration/               # Flyway 数据库迁移脚本
                    ├── V1__create_tables.sql
                    ├── V2__seed_roles.sql
                    └── V3__add_indexes.sql
```

## 关键技术选型说明

| 技术点 | 选型 | 理由 |
|--------|------|------|
| 状态管理 | Zustand | 轻量，适合内部系统，无需 Redux 的复杂度 |
| HTTP 客户端 | Axios | 拦截器处理 token/错误，配合 TypeScript 类型良好 |
| 构建工具 | Vite | 比 CRA 快，HMR 体验好 |
| 数据库迁移 | Flyway | 版本化管理 DDL，避免手动跑 SQL |
| Excel 处理 | Apache POI（后端）| 生成模板、解析导入文件 |
| PDF 导出 | iText 7（后端）| 生成中文 PDF 对账单 |
| 身份认证 | JWT + Spring Security | 无状态，适合前后端分离 |
