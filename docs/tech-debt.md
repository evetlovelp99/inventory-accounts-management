# 技术债清单

本文档记录 MVP 代码库中**已知但尚未修复**的问题。条目均来自 2026-07-08 对当前分支的代码排查，非对话摘要。

最后更新：2026-07-08

---

## 排查方法与范围

### 使用的搜索关键词

| 类别 | 关键词 / 模式 |
|------|----------------|
| 权限 | `@PreAuthorize`、`@Secured`、`hasRole`、`EnableMethodSecurity` |
| 文件 I/O | `MultipartFile`、`upload`、`download`、`inspect-reports`、`FileStorage`、`transferTo`、`getOriginalFilename`、`Paths.get`、`normalize` |
| 金额口径 | `SUM(`、`converted_amount`、`convertedAmount`、`converted_sale_amount`、`remaining_amount`、`sumRemaining`、`gross_profit` |
| 前端权限 | `role`、`OWNER`、`FINANCE`、`WAREHOUSE`、`SUPERVISOR`、`authStore`、`ProtectedRoute` |
| 数据安全 | `password`、`password_hash`、`bcrypt`、`token`、`JWT_SECRET` |
| 事务 | `@Transactional` |
| 功能缺口 | `createPayable`、`delete`、`updateInbound` |

### 阅读 / 检查的主要文件

**后端 Controller（7 个，全部通读）**

- `AuthController.java`
- `FileController.java`
- `AccountsController.java`
- `InventoryController.java`
- `SettingsController.java`（`/api/settings/products`）
- `CustomerController.java`
- `SupplierController.java`

**后端安全 / 存储 / 业务**

- `SecurityConfig.java`、`JwtAuthenticationFilter.java`
- `FileStorageService.java`
- `AccountService.java`、`InventoryService.java`
- `AccountReceivableRepository.java`、`AccountPayableRepository.java`
- `InventoryLedgerRepository.java`、`ProductRepository.java`
- `AuthService.java`、`User.java`、`PaymentLog.java`
- `application.yml`、`application-prod.yml`
- `V1__create_tables.sql`、`V2__seed_roles.sql`

**前端（15 个页面 + 路由 / 导航）**

- `App.tsx`、`ProtectedRoute.tsx`、`SideNav.tsx`、`navConfig.ts`、`authStore.ts`
- 全部 `frontend/src/pages/**/*.tsx`

**产品权限对照**

- `project.md/user_flow.md` 角色权限表

---

## A. 全局：后端接口无方法级权限（P0）

### 结论

全仓库 **0 处** `@PreAuthorize` / `@Secured` / `@EnableMethodSecurity`。`SecurityConfig` 仅区分：

- `POST /api/auth/login` → `permitAll`
- 其余 `/api/**` → **JWT 已认证即可**（不校验角色）

### 接口权限矩阵（29 个已实现端点）

| Controller | 方法 | 路径 | @PreAuthorize | 实际门槛 |
|------------|------|------|---------------|----------|
| AuthController | POST | `/api/auth/login` | 无 | 匿名可访问（设计如此） |
| FileController | POST | `/api/files/inspect-reports` | 无 | 任意已登录用户 |
| FileController | GET | `/api/files/inspect-reports/{filename}` | 无 | 任意已登录用户 |
| AccountsController | GET | `/api/accounts/receivable` | 无 | 任意已登录用户 |
| AccountsController | POST | `/api/accounts/receivable` | 无 | 任意已登录用户 |
| AccountsController | GET | `/api/accounts/receivable/detail` | 无 | 任意已登录用户 |
| AccountsController | POST | `/api/accounts/receivable/{id}/payment` | 无 | 任意已登录用户 |
| AccountsController | GET | `/api/accounts/payable` | 无 | 任意已登录用户 |
| AccountsController | POST | `/api/accounts/payable` | 无 | 任意已登录用户 |
| AccountsController | GET | `/api/accounts/payable/detail` | 无 | 任意已登录用户 |
| AccountsController | POST | `/api/accounts/payable/{id}/payment` | 无 | 任意已登录用户 |
| InventoryController | POST | `/api/inventory/inbound` | 无 | 任意已登录用户 |
| InventoryController | GET | `/api/inventory/stock` | 无 | 任意已登录用户 |
| InventoryController | GET | `/api/inventory/stock/{productId}/ledger` | 无 | 任意已登录用户 |
| InventoryController | GET | `/api/inventory/inbound/{productId}/batches` | 无 | 任意已登录用户 |
| InventoryController | POST | `/api/inventory/outbound` | 无 | 任意已登录用户 |
| InventoryController | GET | `/api/inventory/exchange-rate/cny-usd` | 无 | 任意已登录用户 |
| SettingsController | GET/POST/PUT×2 | `/api/settings/products` 及 `/{id}`、`/{id}/status` | 无 | 任意已登录用户 |
| CustomerController | GET/POST/PUT×2 | `/api/settings/customers` 及 `/{id}`、`/{id}/status` | 无 | 任意已登录用户 |
| SupplierController | GET/POST/PUT×2 | `/api/settings/suppliers` 及 `/{id}`、`/{id}/status` | 无 | 任意已登录用户 |

**影响范围**：全部 API；与 `user_flow.md` 角色表全面不符（见条目 6）。

**建议修复时机**：**阶段 3.2**（后端 `@PreAuthorize`）+ **3.5 / 3.6**（前端 SideNav / 路由守卫）一并完成。

---

## B. 文件上传 / 下载安全

### B1. 上传缺少文件内容校验（原条目 1，范围确认无误）

| 项 | 说明 |
|----|------|
| **接口** | `POST /api/files/inspect-reports` |
| **调用链** | `FileController.uploadInspectReport` → `FileStorageService.storeInspectReport` |
| **已有防护** | 非空；`MAX_FILE_SIZE_BYTES = 10MB`；Spring `multipart.max-file-size: 10MB`；后缀白名单 `pdf/jpg/jpeg/png`；存储文件名改为 **服务端 UUID**，不使用用户原始文件名（降低路径注入风险） |
| **缺失** | 无 MIME type 校验；无 magic bytes / 内容嗅探（如 Tika） |
| **前端** | `InboundEntryPage.tsx` 仅 `accept=".pdf,.jpg,.jpeg,.png"`（可绕过） |
| **触发条件** | 已登录用户将非图片/PDF 文件改后缀上传 |
| **风险** | 中。恶意内容可入库；若配合 `Content-Disposition: inline` 下载（见 B2），浏览器可能执行 HTML/SVG 等 |
| **建议修复时机** | **阶段 4 导入功能（4.5 大文件上传）之前必须修** |

### B2. 下载缺少角色与文件归属校验（原条目 2，范围比初版更大）

| 项 | 说明 |
|----|------|
| **接口** | `GET /api/files/inspect-reports/{filename}` |
| **已有防护** | `Paths.get(filename).getFileName()` + `normalize()` + `startsWith(inspectReportDir)` → **路径穿越已缓解**（排查确认，非开放债） |
| **缺失** | 无 `@PreAuthorize`；不校验 `{filename}` 是否属于某条 `inbound_records`；不校验请求者角色 |
| **相对权限表的越权** | **仓管、主管**（不应「登记/查看账款」）与 **财务、老板** 权限相同：知 UUID 即可下载任意检测报告 |
| **响应头** | `Content-Disposition: inline` — 增加浏览器内联渲染风险 |
| **建议修复时机** | **阶段 3.2 权限控制时一并处理** |

### B3. 【新增】入库接口可注入任意 `inspectFileUrl`（上传链路绕过）

| 项 | 说明 |
|----|------|
| **接口** | `POST /api/inventory/inbound` |
| **代码** | `InventoryService.createInbound` 直接 `record.setInspectFileUrl(trimToNull(request.getInspectFileUrl()))`，**不校验** URL 是否来自本系统 `/api/files/inspect-reports/{uuid}.ext` |
| **影响范围** | 流水页 `ProductLedgerPage` 展示并链到该 URL；其他用户点击后可能访问外部恶意链接或伪造路径 |
| **触发条件** | 已登录用户直接调 API，或在将来前端被篡改时提交非本系统 URL |
| **风险** | 中。与 B1/B2 独立：即使上传校验完善，仍可通过入库 API 绕过 |
| **建议修复时机** | 与 B1/B2 同期（**阶段 3–4 之间**，最迟导入前） |

---

## C. 多币种与金额计算口径

### 排查基准（任务 1.32 目标口径）

- **应收汇总**（`totalUnpaidAmount`、行内 `convertedAmount`）：`SUM(remaining_amount × COALESCE(exchange_rate, 1))`
- **出库毛利**：基于 `converted_sale_amount - weighted_cost`（`InventoryService.createOutbound` 已按此实现）
- **列表行内原始金额**：应按**单币种**展示，不应跨币种直接 SUM

### C1. 同客户多币种应收聚合错误（原条目 3，范围扩大）

| 项 | 说明 |
|----|------|
| **位置** | `AccountReceivableRepository.findReceivableSummary` / `sumRemainingAmount` |
| **正确** | `convertedAmount`、`totalUnpaidAmount` 使用 `remaining × exchange_rate` 折算 |
| **错误** | 同一客户 CNY + USD 记录：`SUM(original_amount)`、`SUM(paid_amount)`、`SUM(remaining_amount)` **跨币种直接相加**；`currency = MAX(ar.currency)` |
| **前端** | `ReceivablePage` 用 `row.currency` 格式化上述 SUM 值；API 返回的 `convertedAmount` **未在列表 UI 使用** |
| **明细 API 缺口** | `ReceivableRecordResponse` **无 `currency` / `exchangeRate` 字段**；`AccountDetailPage` / `PaymentModal` 依赖列表传入的单一 `currency`（`location.state`） |
| **触发条件** | 同一客户存在 CNY 与 USD 两条及以上未结清/部分结清应收 |
| **风险** | 高（业务误导）。单币种客户不受影响 |
| **建议修复时机** | **阶段 3.1 Dashboard 应收汇总上线前或同期**（需产品确认：按「客户+币种」分行 vs 列表只展示折算人民币） |

### C2. 【新增】应收明细/还款弹窗缺少 per-record 币种

| 项 | 说明 |
|----|------|
| **位置** | `AccountService.toRecordResponse` → `ReceivableRecordResponse`；`AccountDetailPage`、`PaymentModal` |
| **现状** | 明细每条记录有独立 `originalAmount`（原始币种值），但 UI 统一用列表带来的一个 `currency` 渲染 |
| **与 C1 关系** | C1 的子问题；混合币种客户下明细页和还款弹窗币种前缀可能错误 |
| **建议修复时机** | 与 C1 同一批次修复 |

### C3. 【新增】手动创建应收固定为 CNY

| 项 | 说明 |
|----|------|
| **位置** | `AccountService.createReceivable`（`POST /api/accounts/receivable`） |
| **代码** | `SettlementCurrency currency = SettlementCurrency.CNY` 写死 |
| **影响** | 无法通过 API 手动补录美元应收；与出库自动创建（可 USD）行为不一致 |
| **风险** | 低–中（功能缺口，非计算错误） |
| **建议修复时机** | 若产品需要手动补录美元应收，在 C1 修复时一并扩展 DTO |

### C4. 部分还款后 `converted_amount` 快照不更新（已知取舍，仍开放）

| 项 | 说明 |
|----|------|
| **位置** | `AccountReceivable.converted_amount`；`registerReceivablePayment` 只更新 `paid/remaining/status`，**不更新** `converted_amount` |
| **影响** | 单条记录快照与 `remaining × rate` 实时折算不一致；若未来有接口直接读快照字段会出错。当前列表汇总 SQL 用实时公式，**汇总不受影响** |
| **文档** | `tech/database.md` 已说明为 MVP 取舍 |
| **建议修复时机** | **Later**（或阶段 3.1 做 Dashboard 前确认是否读快照） |

### C5. `payment_logs` 无币种字段（已知 Later，仍开放）

| 项 | 说明 |
|----|------|
| **位置** | `payment_logs` 表、`PaymentLog` 实体 |
| **影响** | 跨币种还款、汇兑损益无法追溯（`multi-currency-outbound.md` Later） |
| **建议修复时机** | **Later**（明确不在 MVP） |

### 口径排查：已确认一致的位置

| 位置 | 口径 | 结论 |
|------|------|------|
| `AccountReceivableRepository.sumRemainingAmount` | `SUM(remaining × COALESCE(rate,1))` | ✅ 与 1.32 一致 |
| `findReceivableSummary.convertedAmount` | 同上 | ✅ |
| `InventoryService.createOutbound` 毛利 | `convertedSaleAmount - weightedCost` | ✅ |
| `AccountPayableRepository` 汇总 | 仅 CNY，直接 `SUM(remaining_amount)` | ✅（应付无多币种） |
| `ProductRepository.findStockOverview` | `SUM(remaining_qty)` | ✅（库存，非金额） |
| `InventoryLedgerRepository` 出库 `amount` | `total_sale_amount` + `currency` 字段 | ✅（明细展示原始币种，符合 1.35） |
| `GET /api/dashboard/summary` | **尚未实现**（3.1） | — 暂无运行代码 |

---

## D. 前端角色权限（对照 `user_flow.md` 逐条）

前端 **无任何角色过滤**：`SideNav.tsx` 渲染全部 `NAV_SECTIONS`；`ProtectedRoute.tsx` 只检查 token；`App.tsx` 所有业务路由对已登录用户开放。

### 逐权限表核对

| 权限（user_flow） | 允许角色 | 当前前端可达路径 / 行为 | 越权角色 |
|-------------------|----------|-------------------------|----------|
| 录入入库/出库 | 老板、仓管 | `/inventory/inbound`、`/inventory/outbound` | **财务、主管** 可见导航且可访问 |
| 查看库存 | 全部 | `/inventory/stock`、`/inventory/stock/:id` | 无越权 |
| 登记/查看账款 | 老板、财务 | `/accounts/receivable`、`/accounts/payable`、`/accounts/:type/:id` | **仓管、主管** 可见「财务管理」且可访问、可登记收/付款 |
| 导出对账单 | 老板、财务 | 导出按钮尚未实现（4.x） | 暂无 UI；API 未来需限制 |
| 修改历史记录 | 老板、财务 | **无修改 API / 页面** | 暂无法行使（功能未做，非权限已修） |
| 产品/客户/供应商/用户管理 | 仅老板 | `/settings/products`、`customers`、`suppliers`；`users` 为占位页 | **财务、仓管、主管** 可见「系统设置」整组且前三项可 CRUD |

### 按页面汇总（15 个页面组件）

| 页面 | 路径 | 应限角色 | 当前 |
|------|------|----------|------|
| LoginPage | `/login` | 匿名 | ✅ |
| （看板占位） | `/` | 全部 | ✅ 无敏感数据 |
| InboundEntryPage | `/inventory/inbound` | OWNER, WAREHOUSE | ❌ 全部可访问 |
| OutboundEntryPage | `/inventory/outbound` | OWNER, WAREHOUSE | ❌ 全部可访问 |
| StockOverviewPage | `/inventory/stock` | 全部 | ✅ |
| ProductLedgerPage | `/inventory/stock/:id` | 全部 | ✅（含检测报告下载链接触发 B2） |
| ReceivablePage | `/accounts/receivable` | OWNER, FINANCE | ❌ 全部可访问 |
| PayablePage | `/accounts/payable` | OWNER, FINANCE | ❌ 全部可访问 |
| AccountDetailPage | `/accounts/:type/:id` | OWNER, FINANCE | ❌ 全部可访问 |
| ProductsPage | `/settings/products` | OWNER | ❌ 全部可访问 |
| CustomersPage | `/settings/customers` | OWNER | ❌ 全部可访问 |
| SuppliersPage | `/settings/suppliers` | OWNER | ❌ 全部可访问 |
| UsersPage（占位） | `/settings/users` | OWNER | ❌ 导航可见（占位无 CRUD） |

**建议修复时机**：**阶段 3.5 + 3.6**（与后端 3.2 同步）。

---

## E. 数据库与事务

### E1. 敏感字段存储（排查结论）

| 字段 | 存储方式 | 结论 |
|------|----------|------|
| `users.password_hash` | bcrypt（`AuthService` + `BCryptPasswordEncoder`；`V2__seed_roles.sql` 为哈希） | ✅ 符合要求 |
| JWT | **不落库**；`JwtUtil` 签发无状态 token | ✅ |
| JWT 密钥 | `application.yml` 默认 `dev-only-change-me-...`；`application-prod.yml` 依赖 `${JWT_SECRET}` 环境变量 | ⚠️ **部署债**：生产未设 env 时仍可能用 dev 默认值（见 E3） |
| 前端 token | `localStorage`（`authStore.ts`） | ⚠️ 标准 SPA 做法；XSS 可窃取 token（无 HttpOnly cookie）— **低优先级**，阶段 5 安全走查时可评估 |

### E2. 关键写操作事务覆盖

| 操作 | 方法 | @Transactional | 结论 |
|------|------|----------------|------|
| 入库 | `InventoryService.createInbound` | ✅ | 单表写入 + 操作日志同事务 |
| 出库 + 应收 | `createOutbound` → `createReceivableFromOutbound` | ✅ 均为 `@Transactional`，默认传播合并 | ✅ |
| 入库 + 应付 | `createInbound` 应调 `createPayableFromInbound` | — | ❌ **未实现**（见 F1） |
| 登记应收/应付款 | `registerReceivablePayment` / `registerPayablePayment` | ✅ | 更新账款 + `payment_logs` + `operation_log` 同事务 |
| 产品/客户/供应商 CRUD | 各 `*Service` 写方法 | ✅ | ✅ |
| 文件上传 | `FileStorageService.storeInspectReport` | ❌ | 单文件写入，无 DB 同事务；失败时可能留 orphan 文件 — **低** |

**删除 / 修改历史金额**：全仓库 **无** `delete` 相关 Java 代码；无入库/出库 UPDATE API。`user_flow.md`「修改历史记录」尚未实现，不属于当前事务缺口。

### E3. 【新增】生产 JWT 密钥依赖环境变量但未强制

| 项 | 说明 |
|----|------|
| **位置** | `application.yml` → `jwt.secret: ${JWT_SECRET:dev-only-change-me-...}` |
| **风险** | 生产部署若未注入 `JWT_SECRET`，使用可预测的 dev 默认值 |
| **建议修复时机** | **阶段 0.20 部署 / 阶段 5 上线前** 强制校验 |

---

## F. 功能实现缺口（代码排查新发现）

### F1. 【新增】入库 `createPayable` 未接线（implementation_plan 2.6 与代码不符）

| 项 | 说明 |
|----|------|
| **文档/ DTO** | `InboundCreateRequest.createPayable` 存在；`InboundCreateResponse.payableId` 存在；`tech/api.md` 描述 `createPayable: true` |
| **代码** | `InventoryService.createInbound` **从未读取** `createPayable`，始终 `return new InboundCreateResponse(..., null)`；无 `createPayableFromInbound` 方法 |
| **前端** | `InboundEntryPage` **无**「货款未付」勾选（对比 `OutboundEntryPage.createReceivable`） |
| **影响** | 入库无法自动生成应付；与产品流程「入库勾选该批货款未付」不符 |
| **建议修复时机** | **应尽快修复**（阶段 2 验收范围）；属功能债，可与权限分开 PR |

---

## G. 已关闭 / 已验证非问题

| 项 | 结论 |
|----|------|
| AccountDetailPage 应付「登记付款」 | **已修复**（2.11，`33f97ac`） |
| 下载路径穿越 | **已缓解**（`FileStorageService.loadInspectReport` 文件名规范化 + 目录边界检查） |
| 上传路径穿越 | **已缓解**（存储名 UUID，不用用户输入路径） |
| 应付账款多币种混用 | **不适用**（应付仅 CNY） |

---

## 处理优先级摘要

| 优先级 | ID | 摘要 | 建议阶段 |
|--------|-----|------|----------|
| P0 | A | 全部 API 无 `@PreAuthorize` | 3.2 + 3.5/3.6 |
| P0 | F1 | 入库 createPayable 未实现 | 立即 / 阶段 2 补验收 |
| P1 | C1–C2 | 同客户多币种应收展示 | 3.1 前 |
| P1 | D | 前端角色越权（见逐页表） | 3.5/3.6 |
| P2 | B2–B3 | 文件下载 ACL + inspectFileUrl 校验 | 3.2 或 4 前 |
| P2 | B1 | 上传内容校验 | 4 导入前 |
| P3 | C3 | 手动创建应收仅 CNY | 按需 |
| P3 | E3 | 生产 JWT_SECRET 强制 | 部署 / 5 |
| — | C4–C5 | 已知 MVP 取舍 / Later | Later |
| — | G | 已修复 / 非问题 | 关闭 |

---

## 变更记录

| 日期 | 说明 |
|------|------|
| 2026-07-05 | 初版：检测报告上传/下载（条目 1、2） |
| 2026-07-08 | 对话摘要版：多币种、AccountDetailPage |
| 2026-07-08 | **代码排查重写**：全 Controller 权限矩阵；新增 B3、C2–C3、E3、F1；前端 15 页逐条核对；金额 SQL 全量对照；AccountDetailPage 标记已关闭 |
