# 技术债清单

本文档记录 MVP 开发过程中**已知但尚未修复**的问题。仅作跟踪，不代表当前迭代必须处理。

最后更新：2026-07-08

---

## 1. 检测报告上传缺少文件内容校验

| 项 | 说明 |
|----|------|
| **位置** | `POST /api/files/inspect-reports` → `FileController.uploadInspectReport` → `FileStorageService.storeInspectReport` |
| **现状** | 仅校验：非空、≤10MB、文件名后缀为 `pdf` / `jpg` / `jpeg` / `png`。未校验 MIME type，未校验文件头 magic bytes。 |
| **影响范围** | 入库录入页「生产信息（选填）」中的检测报告上传；上传文件存于本地 `uploads/inspect-reports/`，URL 写入 `inbound_records.inspect_file_url`。 |
| **触发条件** | 任意已登录用户将可执行文件（如 `.html`、`.exe`、脚本）改后缀为 `.pdf` / `.jpg` / `.png` 后上传；服务端会接受并持久化。 |
| **风险** | 中等。当前为内网部署、需 JWT 登录，文件名 UUID 随机，直接外链概率低；但若检测报告 URL 被写入页面或日志，其他已登录用户可下载并在浏览器内联打开（见条目 2），存在存储恶意内容、社工诱导的可能。 |
| **建议修复时机** | **阶段 4 做历史数据导入 / 文件相关功能前必须先修**（4.5 `POST /api/import/preview` 将引入更大体量文件上传）。修复方向：Apache Tika 或等价库做 magic bytes 校验；Content-Type 白名单；必要时病毒扫描或改用对象存储 + 内容安全策略。 |

---

## 2. 检测报告下载接口缺少角色与归属校验

| 项 | 说明 |
|----|------|
| **位置** | `GET /api/files/inspect-reports/{filename}` → `FileController.downloadInspectReport` |
| **现状** | Spring Security 仅要求 **JWT 已认证**（`SecurityConfig`：`anyRequest().authenticated()`），无 `@PreAuthorize`，不校验角色，不校验文件是否属于当前用户有权查看的入库记录。 |
| **谁能未授权访问（相对产品权限表）** | 按 `user_flow.md`，**仓管（WAREHOUSE）**、**主管（SUPERVISOR）** 不应访问财务/敏感导出，但当前与老板、财务一样，只要登录且知道 `{filename}`（UUID）即可下载任意检测报告。未登录用户无法访问（401）。 |
| **影响范围** | 所有已上传的检测报告；`ProductLedgerPage` / 流水详情通过 `inspect_file_url` 链接触发下载。 |
| **触发条件** | 已登录的仓管/主管/财务/老板，从入库记录、流水页、网络请求或日志中获得 `/api/files/inspect-reports/{uuid}.pdf` 后直接 GET。 |
| **风险** | 低–中。文件名不可猜测（UUID），内网场景下实际泄露面有限；但与角色权限设计不一致，后续若检测报告含质检敏感信息，权限缺口会放大。 |
| **建议修复时机** | **阶段 3 做权限控制（3.2 `@PreAuthorize` + 3.5/3.6 前端路由守卫）时一并处理**。修复方向：下载前联表 `inbound_records` 校验归属；按角色限制（至少：仅 OWNER / FINANCE / 录入相关 WAREHOUSE 可读）；或改为带签名的短期下载 URL。上传接口（条目 1）的角色限制应同步收紧。 |

---

## 3. 多币种：同客户多币种应收在列表/明细的展示口径错误

| 项 | 说明 |
|----|------|
| **位置** | 后端 `AccountReceivableRepository.findReceivableSummary`（按 `customer_id` + `customer_name` **GROUP BY 聚合**）；前端 `ReceivablePage`、`AccountDetailPage`、`PaymentModal` 对 `currency` 的单值假设。 |
| **现状** | 聚合 SQL 对同一客户的多条应收：`SUM(original_amount)`、`SUM(paid_amount)`、`SUM(remaining_amount)` **直接相加**（不区分 CNY/USD）；`currency` 取 `MAX(ar.currency)`（任取一种）；仅 `convertedAmount` / `totalUnpaidAmount` 使用 `remaining_amount × exchange_rate` 正确折算人民币。前端列表用单一 `row.currency` 格式化上述 SUM 后的金额（如 `$ 12,000.00` 或 `¥ 12,000.00`），**数字与币种符号可能不匹配**。 |
| **影响范围** | `GET /api/accounts/receivable` 列表；`ReceivablePage` 顶部 StatCard 的折算总额正确，但**行内**原始/已收/剩余金额列可能误导；从列表进入 `AccountDetailPage` 时 `location.state.currency` 传递的是聚合行的 `MAX(currency)`，明细页与 `PaymentModal` 登记还款时币种前缀可能与单条记录实际币种不一致（明细 API 按条返回原始金额，但 UI 层统一用一个 currency 渲染）。 |
| **触发条件** | 同一客户既有人民币出库应收，又有美元出库应收，且均存在未结清或部分结清记录。 |
| **风险** | 中–高（业务误导）。财务在列表/还款弹窗可能误判欠款金额与币种；`totalUnpaidAmount` 汇总仍正确，掩盖行级错误。单币种客户不受影响。 |
| **建议修复时机** | **阶段 3 做 Dashboard / 账款汇总（3.1）前或与之同期修复**，避免看板与列表口径分叉。修复方向（需产品确认后实施）：列表改为按 **客户 + 币种** 分行；或列表仅展示折算人民币、原始币种下沉到明细；`PaymentModal` / 明细页按单条记录的 `currency` 渲染，不再依赖列表传入的单一 currency。 |

**相关已知取舍（仍开放，但优先级低于上条）**

| 项 | 说明 |
|----|------|
| **部分还款后折算汇总轻微失真** | `database.md`：`converted_amount` 为创建时快照，还款不重新折算；Dashboard 应收汇总用 `remaining × exchange_rate` 实时计算，与快照字段在部分还款场景下可能不一致。 |
| **payment_logs 无币种字段** | `multi-currency-outbound.md` Later：跨币种还款/汇兑损益未建模。 |

---

## 4. AccountDetailPage 应付场景缺少登记付款入口

| 状态 | **已修复（2.11，commit `33f97ac`）** — 不再作为开放技术债。 |

| 项 | 说明 |
|----|------|
| **原问题** | 应付明细页无「登记付款」操作，仅能从 `PayablePage` 列表登记。 |
| **修复内容** | `AccountDetailPage` 对 receivable / payable 均展示操作列；`PaymentModal` 传入 `accountType`；应付行构造 `PayableRecord`（含 `inboundId`）。 |

---

## 处理优先级摘要

| 优先级 | 条目 | 建议阶段 |
|--------|------|----------|
| P1 | 3. 同客户多币种展示口径 | 阶段 3（Dashboard / 账款汇总） |
| P2 | 2. 下载接口角色与归属校验 | 阶段 3（权限体系 3.2） |
| P3 | 1. 上传内容校验 | 阶段 4 导入功能之前 |
| — | 4. AccountDetailPage 应付入口 | 已关闭 |

---

## 变更记录

| 日期 | 说明 |
|------|------|
| 2026-07-05 | 初版：条目 1、2（检测报告上传/下载安全） |
| 2026-07-08 | 重组为统一清单；新增条目 3（多币种同客户聚合）；条目 4 标记为 2.11 已修复 |
