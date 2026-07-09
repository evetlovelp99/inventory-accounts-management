# 实施计划

## 阶段说明
按里程碑分组，每个阶段结束时有可验收的运行结果。
优先级规则：基础设施 → 数据模型 → 仓库核心流程 → 账款 → 看板 → 设置 → 导入导出

---

## 阶段 0：项目骨架（对应 M0）
_目标：前后端本地可跑通，部署到阿里云可访问_

- [x] 0.1 后端：用 Spring Initializr 创建项目，选依赖（Web, JPA, MySQL, Security, Lombok, Validation）
- [x] 0.2 后端：配置 `application.yml`（dev / prod 两套），连接本地 MySQL
- [x] 0.3 后端：编写 `V1__create_tables.sql`（按 database.md 创建所有表），接入 Flyway 自动执行
- [x] 0.4 后端：编写 `V2__seed_roles.sql`，插入一条初始老板账号（用于首次登录）
- [x] 0.5 后端：实现 JWT 工具类（生成、解析、过期校验）
- [x] 0.6 后端：配置 Spring Security（白名单 `/api/auth/login`，其余需 JWT）
- [x] 0.7 后端：实现 `POST /api/auth/login`（bcrypt 密码校验，返回 token + 用户信息）
- [x] 0.8 后端：实现全局异常处理器（`GlobalExceptionHandler`），统一返回 `{ code, message, data }` 结构
- [x] 0.9 后端：配置 CORS（允许前端开发端口访问）
- [x] 0.10 前端：用 Vite 创建 React + TypeScript 项目
- [x] 0.11 前端：安装依赖（Ant Design, Axios, Zustand, React Router v6）
- [x] 0.12 前端：配置 `theme.ts`，将 theme.md 色彩系统映射为 Ant Design token（Amber 主色，4px 圆角等）
- [x] 0.13 前端：引入 IBM Plex Mono 字体，编写 `global.css`（CSS 变量、基础重置）
- [x] 0.14 前端：创建 `api/client.ts`（axios 实例，请求拦截器注入 token，响应拦截器处理 401 跳转登录）
- [x] 0.15 前端：创建 `authStore.ts`（Zustand，存 token + user）
- [x] 0.16 前端：实现 `LoginPage`（账号密码表单，调用登录 API，成功后存 token 并跳转首页）
- [x] 0.17 前端：实现 `ProtectedRoute`（检查 token，未登录跳转登录页）
- [x] 0.18 前端：实现 `AppLayout`（TopBar + SideNav + Outlet，硬编码导航项，先不做权限过滤）
- [ ] 0.19 部署：购买阿里云 2核4G 服务器，安装 Java 17, MySQL 8, Nginx
- [ ] 0.20 部署：配置 Nginx 反向代理（前端静态文件 + `/api` 转发后端），验证可通过 IP 访问登录页

**✅ 阶段 0 验收**：从浏览器打开云服务器 IP，能看到登录页，用初始账号登录成功后进入空白主框架。

---

## 阶段 1：仓库模块（对应 M2）
_目标：仓管可完整录入入库、出库，老板可查库存和流水_

### 后端 — 设置基础数据接口（依赖入库/出库录入的下拉选项）

- [x] 1.1 后端：实现产品 CRUD 接口（`GET/POST/PUT /api/settings/products`，含停用）
- [x] 1.2 后端：实现供应商 CRUD 接口（`/api/settings/suppliers`）
- [x] 1.3 后端：实现客户 CRUD 接口（`/api/settings/customers`）

### 后端 — 仓库核心接口

- [x] 1.4 后端：实现 `POST /api/inventory/inbound`（含事务：插入 inbound_records，更新无需改 remaining_qty — 初始值等于 quantity；记录 operation_log）
- [x] 1.5 后端：实现 `GET /api/inventory/inbound/{productId}/batches`（返回有余量的批次列表）
- [x] 1.6 后端：实现 `POST /api/inventory/outbound`
  - 后端事务：`SELECT ... FOR UPDATE` 锁定各批次行，逐行校验 qty <= remaining_qty，校验通过后扣减 remaining_qty，插入 outbound_records + outbound_batch_lines，记录 operation_log
  - 并发冲突时返回 409
- [x] 1.7 后端：实现 `GET /api/inventory/stock`（聚合查询每个产品的 `SUM(remaining_qty)`）
- [x] 1.8 后端：实现 `GET /api/inventory/stock/{productId}/ledger`（联表查 inbound + outbound，按日期倒序分页）

### 前端 — 通用组件（先把核心组件做好，后续页面直接复用）

- [x] 1.9 前端：实现 `StatusBadge` 组件（按 components.md 色彩表渲染所有状态）
- [x] 1.10 前端：实现 `Button` 组件（5 种 variant，2 种 size，loading/disabled 状态）
- [x] 1.11 前端：实现 `DataTable` 组件（表头样式、行交替色、hover 效果、操作列 hover 显示、空状态、骨架屏）
- [x] 1.12 前端：实现 `Toast` + `useToast`（右上角叠放，滑入动画，成功 3s 消失）
- [x] 1.13 前端：实现 `AlertBar`（TopBar 下方，alertStore 驱动）
- [x] 1.14 前端：实现 `FilterToolbar`（搜索框 + FilterTag + 导出按钮）
- [x] 1.15 前端：实现 `EntryForm` 容器（640px 卡片，提交中禁用，按钮组）
- [x] 1.16 前端：实现 `BatchRow`（批次下拉 + 数量输入 + 行内信息展示 + 超量校验红框 + 删除按钮）

### 前端 — 设置基础数据页（给仓库录入提供下拉数据）

- [x] 1.17 前端：实现 `ProductsPage`（列表 + 新增/编辑 Modal + 停用确认）
- [x] 1.18 前端：实现 `SuppliersPage`
- [x] 1.19 前端：实现 `CustomersPage`

### 前端 — 仓库页面

- [x] 1.20 前端：实现 `InboundEntryPage`（产品/供应商下拉读接口，自动计算总金额，提交后 Toast 反馈）
- [x] 1.21 前端：实现 `OutboundEntryPage`（先选产品 → 调批次接口 → BatchRow 列表；汇总计算区实时更新；提交含 createReceivable 选项）
- [x] 1.22 前端：实现 `StockOverviewPage`（DataTable + 搜索 + 余量为零行变红，行点击跳流水页）
- [x] 1.23 前端：实现 `ProductLedgerPage`（日期范围筛选 + 入库/出库流水合并列表）

### 后端/前端 — 溯源信息（选填，供后续人工导出给外部溯源系统）

- [x] 1.24 数据库：`inbound_records` 新增溯源相关字段（`origin_place`、`harvest_date`、`inspect_no`、`inspect_org`、`inspect_date`、`inspect_file_url`、`expiry_date`），不新增批次编号字段，沿用现有 `id` 主键
- [x] 1.25 前端：`InboundEntryPage` 增加「生产信息（选填）」折叠区块，含检测报告文件上传
- [x] 1.26 前端：`ProductLedgerPage` / 流水详情展示已录入的生产信息

### 后端/前端 — 出库多币种支持（依据 `docs/multi-currency-outbound.md`）

- [x] 1.27 数据库：`outbound_records` 新增 `currency` / `exchange_rate` / `converted_sale_amount` 字段；`account_receivables` 新增 `currency` / `exchange_rate` / `converted_amount` 字段
- [x] 1.28 后端：`ExchangeRateService` 对接第三方汇率数据源（供应商待定），当日结果做缓存，失败降级返回 `success:false` 而非抛异常
- [x] 1.29 后端：实现 `GET /api/inventory/exchange-rate/cny-usd`
- [x] 1.30 后端：扩展 `POST /api/inventory/outbound`，支持 `currency` / `exchangeRate` 入参；`currency=USD` 时服务端二次校验汇率非空；计算 `converted_sale_amount`；`gross_profit` 计算口径切换为基于折算值
- [x] 1.31 后端：应收账款创建逻辑（2.7 任务内）透传出库记录的 `currency` / `exchange_rate`，计算 `converted_amount`（此任务与阶段 2 的 2.7 存在依赖，需协调顺序）
- [x] 1.32 后端：`GET /api/dashboard/summary` 与 `GET /api/accounts/receivable` 聚合查询切换到 `converted_*` 字段求和（本次仅完成 receivable 部分；dashboard/summary 留待 3.1）
- [x] 1.33 前端：实现 `ExchangeRateInput` 组件（loading / 自动获取失败提示 / 正常可编辑三态）
- [x] 1.34 前端：`OutboundEntryPage` 接入币种单选 + 条件渲染汇率框 + 提交前校验（USD 必填汇率）
- [x] 1.35 前端：实现 `formatCurrencyAmount` 工具函数；`StockOverviewPage` / `ProductLedgerPage` 出库列表、`ReceivablePage` 应收列表按原始币种展示金额（¥ / $ 前缀）
- [x] 1.36 联调测试：美元出库全链路（录入 → 应收生成 → Dashboard/应收汇总口径正确）；汇率接口失败场景确认不阻塞保存；人民币出库场景回归测试（后端单元测试覆盖折算与应收创建逻辑；dashboard 汇总留待 3.1）

**范围外（不在本阶段做，已记录于 multi-currency-outbound.md「Later」）**：其他币种支持、汇率历史查询、对接客户实际结汇价、汇兑损益单独科目核算（含 `payment_logs` 按币种记录还款）、入库/采购环节多币种支持。

**✅ 阶段 1 验收**：仓管可录入一笔入库 → 查看库存余量增加 → 录入出库（跨批次）→ 余量正确扣减 → 查看该产品完整流水；美元出库可正确预填/编辑汇率并生成折算金额。

---

## 阶段 2：账款模块（对应 M3）

- [x] 2.1 后端：实现 `GET /api/accounts/receivable`（按客户聚合，含账龄天数计算）
- [x] 2.2 后端：实现 `GET /api/accounts/receivable/detail`（单客户明细 + 还款流水）
- [x] 2.3 后端：实现 `POST /api/accounts/receivable/{id}/payment`（超额校验，更新 paid_amount / remaining_amount / status，插入 payment_log）
- [x] 2.4 后端：实现 `POST /api/accounts/receivable`（手动新增）
- [x] 2.5 后端：应付账款完全对称实现（2.1–2.4 对应的 payable 版本）
- [x] 2.6 后端：修改 `POST /api/inventory/inbound`：支持 `createPayable` 参数，入库时自动创建应付账款记录
- [x] 2.7 后端：修改 `POST /api/inventory/outbound`：支持 `createReceivable` 参数，出库时自动创建应收账款记录（含币种/汇率透传，见 1.31）
- [x] 2.8 前端：实现 `ReceivablePage`（顶部应收总额 StatCard + DataTable；账龄着色：<15天 Clay / 15–30天 Clay Dark / >30天 Brick；行操作「登记还款」）
- [x] 2.9 前端：实现 `PaymentModal`（弹窗：显示剩余金额，超额行内报错，确认后 Toast 提示）
- [x] 2.10 前端：实现 `AccountDetailPage`（单客户/供应商：明细列表 + 还款流水，按时间倒序）
- [x] 2.11 前端：实现 `PayablePage`（与 ReceivablePage 对称）

**✅ 阶段 2 验收**：录入一笔出库时勾选"货款未收" → 应收账款列表出现该条记录 → 登记部分还款 → 剩余金额正确更新 → 全额还清后状态变"已结清"。

---

## 阶段 3：首页看板 + 完整权限（对应 M4）

- [ ] 3.1 后端：实现 `GET /api/dashboard/summary`（多表聚合：库存余量、本月入/出库金额、毛利、未结清应收/应付总额、最近 10 条操作日志）
- [ ] 3.2 后端：完善权限控制——在各 Controller 方法上加 `@PreAuthorize` 注解，严格按 user_flow.md 角色权限表执行
- [ ] 3.3 前端：实现 `StatCard` 组件（等高网格，warning/success 竖线，hover Amber 边框，loading 骨架屏）
- [ ] 3.4 前端：实现 `DashboardPage`（4 列 StatCard；库存概况 DataTable；账款概况区；最近动态 `RecentActivity` 列表；各卡片点击下钻跳转）
- [ ] 3.5 前端：完善 `SideNav` 权限过滤（系统设置仅 OWNER 可见；财务模块仅 OWNER/FINANCE 可见账款；按角色隐藏无权菜单项）
- [ ] 3.6 前端：完善 `ProtectedRoute`（支持按角色守卫，访问无权页面时跳转 403 提示页或首页）

**✅ 阶段 3 验收**：老板账号登录看到完整看板数据可下钻；仓管账号登录看不到财务模块；主管账号只能查看，无法进入录入页。

---

## 阶段 4：用户管理 + 历史导入 + 导出（对应 M5）

### 用户管理

- [ ] 4.1 后端：实现用户 CRUD 接口（`/api/settings/users`），密码存储使用 bcrypt
- [ ] 4.2 前端：实现 `UsersPage`（列表 + 新增弹窗含角色选择 + 停用；不能停用自己）

### 历史数据导入

- [ ] 4.3 后端：创建三种导入 Excel 模板文件，放 `resources/templates/`
- [ ] 4.4 后端：实现 `GET /api/import/template/{type}`（返回文件流下载）
- [ ] 4.5 后端：实现 `POST /api/import/preview`（Apache POI 解析 .xlsx，按各类型校验规则检查每行，返回预览 + 错误列表；解析结果缓存到 Redis 或内存 Map，返回 previewToken）
- [ ] 4.6 后端：实现 `POST /api/import/confirm`（通过 previewToken 取缓存结果，批量插入有效行，跳过错误行，生成错误报告 Excel）
- [ ] 4.7 后端：实现 `GET /api/import/error-report/{token}`（返回错误报告文件流）
- [ ] 4.8 前端：实现 `ImportPage`（步骤指示器；三类模板下载按钮；拖拽上传区；预览 DataTable 含错误行标红；确认导入；结果页含错误报告下载）

### 导出功能

- [ ] 4.9 后端：引入 Apache POI，封装 `ExcelUtil`（通用 Excel 生成：表头 + 数据行 + 合计行，按 theme.md 色彩系统设置表头样式）
- [ ] 4.10 后端：引入 iText 7，封装 `PdfUtil`（中文字体嵌入，对账单模板：抬头 + 明细表 + 合计行 + 签字区域）
- [ ] 4.11 后端：实现 `GET /api/export/inbound`（入库流水 Excel + PDF）
- [ ] 4.12 后端：实现 `GET /api/export/outbound`（出库流水 Excel + PDF）
- [ ] 4.13 后端：实现 `GET /api/export/receivable-statement`（客户对账单 PDF + Excel）
- [ ] 4.14 后端：实现 `GET /api/export/payable-statement`（供应商对账单 PDF + Excel）
- [ ] 4.15 后端：实现 `GET /api/export/receivable-summary` + `payable-summary`（账款汇总 Excel）
- [ ] 4.16 前端：在 `StockOverviewPage` / `ProductLedgerPage` 的 FilterToolbar 导出按钮接入导出接口（弹出格式选择 Excel/PDF → 触发浏览器下载）
- [ ] 4.17 前端：在 `AccountDetailPage` 添加「导出对账单」按钮（Excel/PDF 选择）
- [ ] 4.18 前端：在 `ReceivablePage` / `PayablePage` 添加账款汇总导出按钮

**✅ 阶段 4 验收**：完成导入历史数据（入库 + 出库 + 账款三类）；导出一份客户 PDF 对账单，包含厂名抬头、明细、合计行、签字区域；导出入库流水 Excel，数据完整无合并单元格。

---

## 阶段 5：收尾与上线准备

- [ ] 5.1 后端：编写 `V3__add_indexes.sql`（为高频查询字段补充索引，见 database.md）
- [ ] 5.2 后端：压力测试主要接口（出库并发超卖场景重点测试）
- [ ] 5.3 前端：补全所有空状态、骨架屏 loading 状态、网络错误 AlertBar 提示
- [ ] 5.4 前端：全面验证角色权限（4 种角色逐一登录走查所有页面）
- [ ] 5.5 前端：响应式适配平板端（768–1023px：SideNav 收起为图标栏，看板 2 列，表格隐藏次要列）
- [ ] 5.6 部署：配置 HTTPS（Let's Encrypt 或阿里云证书）
- [ ] 5.7 部署：配置 MySQL 定时备份（每日 crontab dump）
- [ ] 5.8 培训：为老板、财务、仓管各准备一份简版操作说明（1–2 页 A4）

**✅ 阶段 5 验收（总体）**：四种角色账号均可在生产环境正常登录使用；历史数据已导入；打印一张对账单交客户签字确认；老板可在首页看板一屏掌握当天库存和资金状况。
