# 数据库设计

## 说明
- 数据库：MySQL 8.0+
- 字符集：`utf8mb4`（支持中文及特殊字符）
- 所有表包含：`created_at DATETIME NOT NULL`，`updated_at DATETIME NOT NULL`
- 金额字段统一使用 `DECIMAL(15, 2)`（15位数字，2位小数）
- 软删除：通过 `status` 字段标记停用，不物理删除数据

---

## 表一览

| 表名 | 说明 |
|------|------|
| `users` | 系统用户 |
| `products` | 产品品类 |
| `customers` | 客户 |
| `suppliers` | 供应商 |
| `inbound_records` | 入库记录（每批次一条） |
| `outbound_records` | 出库记录（整笔出库一条） |
| `outbound_batch_lines` | 出库批次明细（每行一条，对应一个入库批次） |
| `account_receivables` | 应收账款（按客户聚合，每客户一条活动记录） |
| `account_payables` | 应付账款（按供应商聚合，每供应商一条活动记录） |
| `payment_logs` | 收/付款流水（每次还款/付款一条） |
| `operation_logs` | 操作日志（记录修改历史） |

---

## 详细表结构

### users（用户）
```sql
CREATE TABLE users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(50) NOT NULL COMMENT '姓名',
  username      VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
  password_hash VARCHAR(100) NOT NULL COMMENT 'bcrypt 哈希',
  role          ENUM('OWNER','FINANCE','WAREHOUSE','SUPERVISOR') NOT NULL,
  status        ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### products（产品）
```sql
CREATE TABLE products (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL COMMENT '产品名称',
  spec        VARCHAR(100) COMMENT '规格',
  unit        VARCHAR(20) NOT NULL COMMENT '计量单位，如 kg、桶',
  status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_product_name (name)
);
```

### customers（客户）
```sql
CREATE TABLE customers (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100) NOT NULL COMMENT '客户名称',
  country       VARCHAR(50) COMMENT '国家/地区',
  contact_name  VARCHAR(50) COMMENT '联系人姓名',
  contact_info  VARCHAR(100) COMMENT '电话或邮箱',
  remark        TEXT,
  status        ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_customer_name (name)
);
```

### suppliers（供应商）
```sql
CREATE TABLE suppliers (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100) NOT NULL COMMENT '供应商名称',
  contact_name  VARCHAR(50),
  contact_info  VARCHAR(100),
  remark        TEXT,
  status        ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_supplier_name (name)
);
```

### inbound_records（入库记录）
```sql
CREATE TABLE inbound_records (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id      BIGINT NOT NULL COMMENT '关联产品',
  supplier_id     BIGINT COMMENT '关联供应商（可为空，历史导入数据）',
  supplier_name   VARCHAR(100) COMMENT '冗余供应商名称，防止供应商被停用后显示异常',
  inbound_date    DATE NOT NULL COMMENT '入库日期',
  quantity        DECIMAL(15, 3) NOT NULL COMMENT '入库数量',
  unit            VARCHAR(20) NOT NULL COMMENT '单位（冗余）',
  unit_price      DECIMAL(15, 2) NOT NULL COMMENT '采购单价（元）',
  total_amount    DECIMAL(15, 2) NOT NULL COMMENT '总金额 = quantity × unit_price，由系统计算',
  remaining_qty   DECIMAL(15, 3) NOT NULL COMMENT '当前剩余数量，出库时扣减',
  remark          TEXT,
  created_by      BIGINT NOT NULL COMMENT '录入人 user_id',
  is_imported     TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为导入数据',
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (product_id) REFERENCES products(id),
  FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
  FOREIGN KEY (created_by) REFERENCES users(id),
  INDEX idx_product_date (product_id, inbound_date),
  INDEX idx_inbound_date (inbound_date)
);
```

**注意**：`remaining_qty` 是核心字段，每次出库录入时由后端事务性更新，必须加锁防并发超卖。

**溯源相关字段（选填，用于后续对接外部溯源系统）**：
```sql
ALTER TABLE inbound_records
  ADD COLUMN origin_place     VARCHAR(100) COMMENT '产地/蜂场名称',
  ADD COLUMN harvest_date     DATE         COMMENT '生产/采集日期',
  ADD COLUMN inspect_no       VARCHAR(50)  COMMENT '检测报告编号',
  ADD COLUMN inspect_org      VARCHAR(100) COMMENT '检测机构',
  ADD COLUMN inspect_date     DATE         COMMENT '检测日期',
  ADD COLUMN inspect_file_url VARCHAR(255) COMMENT '检测报告文件地址（存OSS/本地路径）',
  ADD COLUMN expiry_date      DATE         COMMENT '保质期截止日期';
```
说明：这些字段只存"真实生产数据"，供人工导出给外部溯源系统使用。**不需要单独的批次编号字段**——记录本身的 `id` 主键就是导出数据时对应每条记录的标识。溯源码的生成、绑定、公开查询、二维码，均由外部溯源系统负责，与本 ERP 系统无关。

### outbound_records（出库记录）
```sql
CREATE TABLE outbound_records (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id      BIGINT NOT NULL COMMENT '关联产品',
  customer_id     BIGINT COMMENT '关联客户',
  customer_name   VARCHAR(100) COMMENT '冗余客户名称',
  outbound_date   DATE NOT NULL,
  total_qty       DECIMAL(15, 3) NOT NULL COMMENT '出库总数量（所有批次行汇总）',
  unit            VARCHAR(20) NOT NULL,
  sale_unit_price DECIMAL(15, 2) NOT NULL COMMENT '销售单价（原始币种，见 currency 字段）',
  total_sale_amount  DECIMAL(15, 2) NOT NULL COMMENT '总销售额（原始币种，对账开票依据，不可被折算金额取代）',
  weighted_cost   DECIMAL(15, 2) NOT NULL COMMENT '加权采购成本总额（恒为人民币，采购环节不涉及多币种）',
  gross_profit    DECIMAL(15, 2) NOT NULL COMMENT '预估毛利 = converted_sale_amount - weighted_cost（口径统一为折算人民币值）',
  remark          TEXT,
  created_by      BIGINT NOT NULL,
  is_imported     TINYINT(1) NOT NULL DEFAULT 0,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (product_id) REFERENCES products(id),
  FOREIGN KEY (customer_id) REFERENCES customers(id),
  FOREIGN KEY (created_by) REFERENCES users(id),
  INDEX idx_product_date (product_id, outbound_date),
  INDEX idx_outbound_date (outbound_date)
);
```

**多币种支持字段（新增，详见 `docs/multi-currency-outbound.md`）**：
```sql
ALTER TABLE outbound_records
  ADD COLUMN currency ENUM('CNY','USD') NOT NULL DEFAULT 'CNY' COMMENT '结算币种',
  ADD COLUMN exchange_rate DECIMAL(10, 4) NULL COMMENT '美元汇率（人行中间价，可手动修改），人民币记录为空',
  ADD COLUMN converted_sale_amount DECIMAL(15, 2) NOT NULL COMMENT '折算人民币总额：人民币记录 = total_sale_amount，美元记录 = total_sale_amount × exchange_rate；用于仪表盘/报表统一汇总，gross_profit 基于此字段计算';
```
说明：`sale_unit_price` / `total_sale_amount` 始终保留原始币种数据，供客户对账、开票使用，不因折算而被覆盖或丢失。

### outbound_batch_lines（出库批次明细行）
```sql
CREATE TABLE outbound_batch_lines (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  outbound_id       BIGINT NOT NULL COMMENT '关联出库记录',
  inbound_id        BIGINT NOT NULL COMMENT '关联入库批次',
  qty               DECIMAL(15, 3) NOT NULL COMMENT '本行出库数量',
  unit_cost         DECIMAL(15, 2) NOT NULL COMMENT '该批次采购单价（冗余快照）',
  line_cost         DECIMAL(15, 2) NOT NULL COMMENT '本行采购成本 = qty × unit_cost',
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (outbound_id) REFERENCES outbound_records(id),
  FOREIGN KEY (inbound_id) REFERENCES inbound_records(id),
  INDEX idx_outbound (outbound_id),
  INDEX idx_inbound (inbound_id)
);
```

### account_receivables（应收账款）
```sql
CREATE TABLE account_receivables (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id       BIGINT COMMENT '关联客户（可为空，历史导入）',
  customer_name     VARCHAR(100) NOT NULL COMMENT '冗余',
  outbound_id       BIGINT COMMENT '关联出库单（可为空，手动创建或历史导入）',
  original_amount   DECIMAL(15, 2) NOT NULL COMMENT '原始应收金额（原始币种，见 currency 字段）',
  paid_amount       DECIMAL(15, 2) NOT NULL DEFAULT 0 COMMENT '已收金额（原始币种，本期不做汇兑损益核算）',
  remaining_amount  DECIMAL(15, 2) NOT NULL COMMENT '剩余应收 = original - paid（原始币种）',
  occur_date        DATE NOT NULL COMMENT '发生日期',
  status            ENUM('UNPAID','PARTIAL','PAID') NOT NULL DEFAULT 'UNPAID',
  remark            TEXT,
  created_by        BIGINT NOT NULL,
  is_imported       TINYINT(1) NOT NULL DEFAULT 0,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (customer_id) REFERENCES customers(id),
  FOREIGN KEY (outbound_id) REFERENCES outbound_records(id),
  FOREIGN KEY (created_by) REFERENCES users(id),
  INDEX idx_customer_status (customer_id, status)
);
```

**多币种支持字段（新增，详见 `docs/multi-currency-outbound.md`）**：
```sql
ALTER TABLE account_receivables
  ADD COLUMN currency ENUM('CNY','USD') NOT NULL DEFAULT 'CNY' COMMENT '继承自出库记录的结算币种',
  ADD COLUMN exchange_rate DECIMAL(10, 4) NULL COMMENT '继承自出库记录，创建时快照一次，人民币记录为空',
  ADD COLUMN converted_amount DECIMAL(15, 2) NOT NULL COMMENT '折算人民币金额 = original_amount × exchange_rate，仅用于仪表盘应收汇总展示';
```
说明：`converted_amount` 在创建时按 `original_amount` 全额快照一次，后续 `payment_logs` 还款不重新折算——汇兑损益单独核算已明确排除在本期范围（见 multi-currency-outbound.md「Later」）。客户部分还款后，Dashboard 汇总的应收折算值会有轻微失真，属于已知的范围内取舍，暂不处理。

### account_payables（应付账款）
```sql
CREATE TABLE account_payables (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  supplier_id       BIGINT COMMENT '关联供应商（可为空）',
  supplier_name     VARCHAR(100) NOT NULL COMMENT '冗余',
  inbound_id        BIGINT COMMENT '关联入库单（可为空）',
  original_amount   DECIMAL(15, 2) NOT NULL,
  paid_amount       DECIMAL(15, 2) NOT NULL DEFAULT 0,
  remaining_amount  DECIMAL(15, 2) NOT NULL,
  occur_date        DATE NOT NULL,
  status            ENUM('UNPAID','PARTIAL','PAID') NOT NULL DEFAULT 'UNPAID',
  remark            TEXT,
  created_by        BIGINT NOT NULL,
  is_imported       TINYINT(1) NOT NULL DEFAULT 0,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
  FOREIGN KEY (inbound_id) REFERENCES inbound_records(id),
  FOREIGN KEY (created_by) REFERENCES users(id),
  INDEX idx_supplier_status (supplier_id, status)
);
```

### payment_logs（收/付款流水）
```sql
CREATE TABLE payment_logs (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_type    ENUM('RECEIVABLE','PAYABLE') NOT NULL,
  account_id      BIGINT NOT NULL COMMENT '关联应收或应付 id',
  amount          DECIMAL(15, 2) NOT NULL COMMENT '本次还款/付款金额',
  payment_date    DATE NOT NULL,
  remark          TEXT,
  created_by      BIGINT NOT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (created_by) REFERENCES users(id),
  INDEX idx_account (account_type, account_id)
);
```

### operation_logs（操作日志）
```sql
CREATE TABLE operation_logs (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  operator_id     BIGINT NOT NULL COMMENT '操作人',
  operator_name   VARCHAR(50) NOT NULL COMMENT '冗余',
  action          VARCHAR(50) NOT NULL COMMENT '操作类型：INBOUND_CREATE / OUTBOUND_CREATE / PAYMENT_LOG 等',
  entity_type     VARCHAR(50) NOT NULL COMMENT '实体类型：inbound_records / outbound_records 等',
  entity_id       BIGINT NOT NULL,
  before_value    JSON COMMENT '修改前快照',
  after_value     JSON COMMENT '修改后快照',
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_entity (entity_type, entity_id),
  INDEX idx_operator (operator_id),
  INDEX idx_created (created_at)
);
```

---

## 关键关系图

```
products ──┬── inbound_records ──┬── outbound_batch_lines ──── outbound_records
           │                    │                                    │
           │                    └── account_payables                │
           │                                                         │
           └── outbound_records ────────────────────────── account_receivables

customers ─── outbound_records ─── account_receivables ─── payment_logs
suppliers ─── inbound_records  ─── account_payables    ─── payment_logs
users     ─── (操作任何表时记录 created_by) ─── operation_logs
```

---

## 库存余量计算逻辑

**每个产品的当前总余量** = `SUM(remaining_qty)` FROM `inbound_records` WHERE `product_id = ?`

`remaining_qty` 在每次出库时由后端事务性扣减（悲观锁 `SELECT ... FOR UPDATE`），保证不超卖。

**应收/应付账款按客户/供应商聚合展示**（前端视图逻辑）：
- 列表页显示：客户名 + `SUM(original_amount)` + `SUM(paid_amount)` + `SUM(remaining_amount)`（仅 status != PAID 的记录）
- 详情页显示：该客户下所有 `account_receivables` 记录 + 每条记录的 `payment_logs`

## 历史导入数据处理

- 导入数据设置 `is_imported = 1`
- 账款历史导入时 `outbound_id` / `inbound_id` 允许为空（孤立账款，无关联单据）
- 导入时的产品名/客户名/供应商名：需与系统内已有名称精确匹配；不匹配则标记为错误行
