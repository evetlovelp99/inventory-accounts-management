# API 规范

## 通用约定

### 请求格式
- Base URL：`/api`
- Content-Type：`application/json`
- 认证：`Authorization: Bearer <JWT token>`（登录接口除外）

### 统一响应结构
```json
// 成功
{ "code": 200, "message": "ok", "data": { ... } }

// 失败
{ "code": 400, "message": "错误描述", "data": null }
```

### 通用错误码
| code | 含义 |
|------|------|
| 400 | 参数错误（含业务校验失败） |
| 401 | 未登录或 token 过期 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 冲突（如名称重复） |
| 500 | 服务器内部错误 |

### 分页参数（列表接口通用）
Query: `page=1&size=20`
Response data 结构：
```json
{ "list": [...], "total": 100, "page": 1, "size": 20 }
```

---

## 一、认证模块 `/api/auth`

### POST `/api/auth/login`
**用途**：用户登录

**Request Body**
```json
{ "username": "admin", "password": "123456" }
```

**Response**
```json
{
  "token": "eyJ...",
  "user": { "id": 1, "name": "张老板", "role": "OWNER" }
}
```

**错误**
- 400：账号或密码错误
- 403：账号已停用

---

### POST `/api/auth/logout`
**用途**：退出登录（前端清除 token，后端可选择加入黑名单）

---

## 二、首页看板 `/api/dashboard`

### GET `/api/dashboard/summary`
**用途**：一次性返回看板所需全部数据

**权限**：所有角色

**Response**
```json
{
  "stockSummary": [
    { "productId": 1, "productName": "精蜡", "unit": "kg", "totalRemaining": 850.0 }
  ],
  "receivableTotal": 128450.00,
  "payableTotal": 56200.00,
  "currentMonth": {
    "inboundAmount": 230000.00,
    "outboundAmount": 310000.00,
    "grossProfit": 80000.00
  },
  "recentActivities": [
    {
      "time": "2025-06-18T10:30:00",
      "type": "INBOUND",
      "description": "精蜡入库 500kg",
      "amount": 9250.00,
      "partyName": "成都蜂蜜厂"
    }
  ]
}
```
说明：`outboundAmount`、`grossProfit`、`receivableTotal` 统一按折算人民币金额（`converted_sale_amount` / `converted_amount`）汇总，人民币记录该值等于原始金额，美元记录已折算，前端展示统一口径，无需二次换算。

**错误**：500 数据聚合失败

---

## 三、仓库模块 `/api/inventory`

### GET `/api/inventory/stock`
**用途**：当前库存总览列表

**权限**：所有角色

**Query**：`keyword=` (产品名搜索)，`page`, `size`

**Response**（分页）
```json
{
  "list": [
    {
      "productId": 1, "productName": "精蜡", "spec": "一级", "unit": "kg",
      "totalRemaining": 850.0, "lastUpdated": "2025-06-18"
    }
  ],
  "total": 5
}
```

---

### GET `/api/inventory/stock/{productId}/ledger`
**用途**：单产品完整流水

**权限**：所有角色

**Query**：`startDate=2025-01-01&endDate=2025-06-30&page&size`

**Response**（分页）
```json
{
  "productName": "精蜡",
  "unit": "kg",
  "list": [
    {
      "id": 10, "type": "OUTBOUND", "date": "2025-06-18",
      "qty": 200.0, "unitPrice": 28.50, "amount": 5700.00,
      "partyName": "上海贸易有限公司", "remark": ""
    }
  ],
  "total": 30
}
```

---

### POST `/api/inventory/inbound`
**用途**：录入入库

**权限**：OWNER, WAREHOUSE

**Request Body**
```json
{
  "productId": 1,
  "supplierId": 2,
  "inboundDate": "2025-06-18",
  "quantity": 500.0,
  "unitPrice": 18.50,
  "remark": "",
  "createPayable": true   // 是否生成应付账款记录
}
```

**Response**
```json
{
  "inboundId": 15,
  "totalAmount": 9250.00,
  "payableId": 8          // 若 createPayable=true，返回创建的应付 id
}
```

**错误**
- 400：产品或供应商不存在 / 数量或单价为负
- 400：产品已停用

---

### GET `/api/inventory/inbound/{productId}/batches`
**用途**：获取某产品有余量的入库批次列表（出库录入时用于批次选择下拉）

**权限**：OWNER, WAREHOUSE

**Response**
```json
[
  {
    "inboundId": 10,
    "inboundDate": "2025-06-10",
    "unitPrice": 18.50,
    "remainingQty": 350.0,
    "unit": "kg",
    "supplierName": "成都蜂蜜厂"
  }
]
```

---

### GET `/api/inventory/exchange-rate/cny-usd`
**用途**：获取人民币兑美元汇率（人行中间价），出库录入选择美元时前端自动调用

**权限**：OWNER, WAREHOUSE

**Query**：`date=2025-06-18`（可选，默认当日）

**Response（成功，200）**
```json
{
  "success": true,
  "rate": 7.1523,
  "date": "2025-06-18",
  "source": "PBOC"
}
```

**Response（失败，仍返回 200，不是 4xx/5xx，避免前端将其当作阻塞性错误处理）**
```json
{
  "success": false,
  "rate": null,
  "message": "汇率获取失败，请手动输入"
}
```

**说明**：内部对接第三方汇率数据源（供应商待定），当日结果做缓存；失败原因统一归一化为 `success: false`，前端收到后清空汇率输入框、展示提示文案，不阻塞出库表单其他字段的填写与提交。

---

### POST `/api/inventory/outbound`
**用途**：录入出库（含多行批次明细，支持人民币/美元双币种）

**权限**：OWNER, WAREHOUSE

**Request Body**
```json
{
  "productId": 1,
  "customerId": 3,
  "outboundDate": "2025-06-18",
  "currency": "USD",
  "exchangeRate": 7.1523,
  "saleUnitPrice": 4.20,
  "remark": "",
  "createReceivable": false,
  "batchLines": [
    { "inboundId": 10, "qty": 200.0 },
    { "inboundId": 11, "qty": 100.0 }
  ]
}
```
字段说明：
- `currency`：`CNY` | `USD`，默认 `CNY`
- `exchangeRate`：仅 `currency = USD` 时必填（人行中间价或手动修改值），`CNY` 时忽略该字段

**Response**
```json
{
  "outboundId": 22,
  "totalQty": 300.0,
  "currency": "USD",
  "totalSaleAmount": 1260.00,
  "convertedSaleAmount": 9011.90,
  "weightedCost": 5550.00,
  "grossProfit": 3461.90,
  "receivableId": null
}
```
- `totalSaleAmount`：原始币种总额（USD 记录即为美元金额，对账开票依据）
- `convertedSaleAmount`：折算人民币总额，`CNY` 记录等于 `totalSaleAmount`
- `grossProfit`：计算口径统一为 `convertedSaleAmount - weightedCost`

**错误**
- 400：batchLines 为空
- 400：某行 qty 超过该批次 remainingQty（返回哪个 inboundId 超量）
- 400：产品或客户不存在/已停用
- 400：`currency = USD` 且 `exchangeRate` 为空（提示"请填写汇率"，服务端二次校验，不能仅依赖前端）
- 409：并发导致余量变化（提示用户刷新批次列表重试）

**副作用**：若创建应收账款（`createReceivable`），透传 `currency` / `exchangeRate`，应收记录同步计算 `convertedAmount`

---

### PUT `/api/inventory/inbound/{id}`
**用途**：修改入库记录

**权限**：OWNER, FINANCE

**Request Body**：与 POST 相同字段（均可修改）

**错误**
- 400：修改后数量小于已出库量（`quantity < 原数量 - remaining_qty`）

---

### PUT `/api/inventory/outbound/{id}`
**用途**：修改出库记录

**权限**：OWNER, FINANCE

**错误**
- 400：修改后数量导致某批次 remaining_qty 变为负数

---

## 四、账款模块 `/api/accounts`

### GET `/api/accounts/receivable`
**用途**：应收账款列表（按客户聚合）

**权限**：OWNER, FINANCE

**Query**：`status=UNPAID|PARTIAL|PAID`，`keyword=`（客户名），`page`, `size`

**Response**（分页）
```json
{
  "totalUnpaidAmount": 128450.00,
  "list": [
    {
      "customerId": 3,
      "customerName": "上海贸易",
      "currency": "USD",
      "originalAmount": 6990.00,
      "convertedAmount": 50000.00,
      "paidAmount": 2795.00,
      "remainingAmount": 4195.00,
      "oldestUnpaidDate": "2025-04-10",
      "daysSinceOldest": 69,
      "status": "PARTIAL"
    }
  ],
  "total": 12
}
```
说明：`originalAmount` / `paidAmount` / `remainingAmount` 为原始币种数值（对账依据）。`convertedAmount` 与 `totalUnpaidAmount` 均为**剩余未收**的人民币折算金额，计算公式为 `SUM(remaining_amount × COALESCE(exchange_rate, 1))`（人民币记录 `exchange_rate` 为空时按 1 处理），会随还款实时减少；不是创建时的全额 `converted_amount` 快照。单条明细的 `converted_amount` 字段仍保留创建时快照，供对账/明细页使用。人民币记录 `currency` 为 `CNY`，折算后 `convertedAmount` 等于 `remainingAmount`。

---

### GET `/api/accounts/receivable/detail`
**用途**：某客户的全部应收账款明细 + 还款流水

**Query**：`customerId=3&startDate=&endDate=`

**Response**
```json
{
  "customerName": "上海贸易",
  "records": [
    {
      "id": 5,
      "originalAmount": 30000.00,
      "paidAmount": 0.00,
      "remainingAmount": 30000.00,
      "occurDate": "2025-06-01",
      "status": "UNPAID",
      "outboundId": 22,
      "remark": "",
      "paymentLogs": [
        { "id": 1, "amount": 20000.00, "paymentDate": "2025-05-10", "remark": "" }
      ]
    }
  ]
}
```

---

### POST `/api/accounts/receivable/{id}/payment`
**用途**：登记还款

**权限**：OWNER, FINANCE

**Request Body**
```json
{ "amount": 10000.00, "paymentDate": "2025-06-18", "remark": "" }
```

**Response**
```json
{ "remainingAmount": 20000.00, "status": "PARTIAL" }
```

**错误**
- 400：amount <= 0
- 400：amount > remainingAmount（提示"还款金额不能超过剩余欠款 XX 元"）

---

### GET `/api/accounts/payable` / GET `/api/accounts/payable/detail`
**用途**：与应收账款接口完全对称，方向为供应商

---

### POST `/api/accounts/payable/{id}/payment`
与应收对称

---

### POST `/api/accounts/receivable`（手动新增）
**权限**：OWNER, FINANCE

**Request Body**
```json
{
  "customerId": 3,
  "originalAmount": 15000.00,
  "occurDate": "2025-06-01",
  "outboundId": null,
  "remark": ""
}
```

---

### POST `/api/accounts/payable`（手动新增）
与应收对称

---

## 五、设置模块 `/api/settings`

### 产品管理
| Method | Path | 说明 | 权限 |
|--------|------|------|------|
| GET | `/api/settings/products` | 列表（支持 keyword, status 过滤） | OWNER |
| POST | `/api/settings/products` | 新增 | OWNER |
| PUT | `/api/settings/products/{id}` | 编辑 | OWNER |
| PUT | `/api/settings/products/{id}/status` | 启用/停用 | OWNER |

**POST 新增 Request Body**
```json
{ "name": "精蜡", "spec": "一级", "unit": "kg" }
```
**错误**：409 名称重复

**停用错误**：400 + 提示"该产品仍有余量，停用后将不再显示在录入选项中"（仅警告，前端确认后继续）

---

### 用户管理
| Method | Path | 说明 | 权限 |
|--------|------|------|------|
| GET | `/api/settings/users` | 列表 | OWNER |
| POST | `/api/settings/users` | 新增 | OWNER |
| PUT | `/api/settings/users/{id}` | 编辑（含角色修改） | OWNER |
| PUT | `/api/settings/users/{id}/status` | 停用/启用 | OWNER |

**停用错误**：403 不能停用当前登录账号

---

### 客户管理
| Method | Path | 说明 | 权限 |
|--------|------|------|------|
| GET | `/api/settings/customers` | 列表 | OWNER |
| POST | `/api/settings/customers` | 新增 | OWNER |
| PUT | `/api/settings/customers/{id}` | 编辑 | OWNER |
| PUT | `/api/settings/customers/{id}/status` | 停用 | OWNER |

**停用警告**（同产品，前端二次确认）：有未结清应收账款时提示

---

### 供应商管理
与客户管理接口完全对称，路径为 `/api/settings/suppliers`

---

## 六、导入模块 `/api/import`

### GET `/api/import/template/{type}`
**用途**：下载标准导入模板

**Path 参数**：`type = inbound | outbound | accounts`

**Response**：文件流（application/vnd.openxmlformats-officedocument.spreadsheetml.sheet）

**文件名**：`入库导入模板.xlsx` / `出库导入模板.xlsx` / `账款导入模板.xlsx`

---

### POST `/api/import/preview`
**用途**：上传 Excel 文件，返回解析预览（前 20 条 + 错误行）

**Request**：`multipart/form-data`，字段 `file`（.xlsx），字段 `type`（inbound|outbound|accounts）

**Response**
```json
{
  "totalRows": 85,
  "validRows": 82,
  "errorRows": 3,
  "preview": [
    {
      "rowNum": 1,
      "data": { "inboundDate": "2024-01-05", "productName": "精蜡", ... },
      "hasError": false,
      "errorMessage": null
    },
    {
      "rowNum": 4,
      "data": { "inboundDate": "", "productName": "未知产品X", ... },
      "hasError": true,
      "errorMessage": "产品名称'未知产品X'在系统中不存在"
    }
  ]
}
```

**错误**
- 400：文件格式不是 .xlsx
- 400：文件无法解析（非标准模板）
- 400：所有行均有错误

---

### POST `/api/import/confirm`
**用途**：确认导入（跳过错误行，导入有效行）

**Request Body**
```json
{ "type": "inbound", "skipErrors": true, "previewToken": "abc123" }
```
（`previewToken` 用于关联上一步的预览缓存，避免重新解析文件）

**Response**
```json
{
  "successCount": 82,
  "failCount": 3,
  "errorReportUrl": "/api/import/error-report/abc123"
}
```

---

### GET `/api/import/error-report/{token}`
**用途**：下载错误报告（Excel）

**Response**：文件流

---

## 七、导出模块 `/api/export`

### GET `/api/export/inbound`
**用途**：导出入库流水

**权限**：OWNER, FINANCE

**Query**：`startDate`, `endDate`, `productId`（可多个）, `format=excel|pdf`

**Response**：文件流

**错误**：400 筛选结果为空

---

### GET `/api/export/outbound`
**用途**：导出出库流水

**Query**：`startDate`, `endDate`, `productId`（多个）, `customerId`, `format=excel|pdf`

---

### GET `/api/export/receivable-statement`
**用途**：客户对账单（PDF 含签字区域 / Excel 原始数据）

**Query**：`customerId`, `startDate`, `endDate`, `format=excel|pdf`

**文件名**：`对账单_张三_2025年6月.pdf`

---

### GET `/api/export/payable-statement`
**用途**：供应商对账单，与应收对称

---

### GET `/api/export/receivable-summary`
**用途**：应收账款汇总（Excel）

**Query**：`status=UNPAID|PARTIAL|PAID`, `startDate`, `endDate`, `format=excel|pdf`

---

### GET `/api/export/payable-summary`
**用途**：应付账款汇总，与应收对称

---

## 八、操作日志（内部使用）

操作日志由后端 AOP 切面自动记录，无需前端主动调用。需记录的操作：
- 入库录入 / 修改
- 出库录入 / 修改
- 还款/付款登记
- 账款手动新增
- 产品/用户/客户/供应商的创建、编辑、停用

日志查看接口（后期功能，MVP 不做前端页面）：
`GET /api/logs?entityType=&entityId=&page&size`（仅 OWNER 权限）
