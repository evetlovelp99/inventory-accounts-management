-- V1: Create all application tables (see tech/database.md)

CREATE TABLE users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(50) NOT NULL COMMENT '姓名',
  username      VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
  password_hash VARCHAR(100) NOT NULL COMMENT 'bcrypt 哈希',
  role          ENUM('OWNER','FINANCE','WAREHOUSE','SUPERVISOR') NOT NULL,
  status        ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE products (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL COMMENT '产品名称',
  spec        VARCHAR(100) COMMENT '规格',
  unit        VARCHAR(20) NOT NULL COMMENT '计量单位，如 kg、桶',
  status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_product_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE outbound_records (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id      BIGINT NOT NULL COMMENT '关联产品',
  customer_id     BIGINT COMMENT '关联客户',
  customer_name   VARCHAR(100) COMMENT '冗余客户名称',
  outbound_date   DATE NOT NULL,
  total_qty       DECIMAL(15, 3) NOT NULL COMMENT '出库总数量（所有批次行汇总）',
  unit            VARCHAR(20) NOT NULL,
  sale_unit_price DECIMAL(15, 2) NOT NULL COMMENT '销售单价',
  total_sale_amount  DECIMAL(15, 2) NOT NULL COMMENT '总销售额',
  weighted_cost   DECIMAL(15, 2) NOT NULL COMMENT '加权采购成本总额',
  gross_profit    DECIMAL(15, 2) NOT NULL COMMENT '预估毛利 = total_sale_amount - weighted_cost',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE account_receivables (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id       BIGINT COMMENT '关联客户（可为空，历史导入）',
  customer_name     VARCHAR(100) NOT NULL COMMENT '冗余',
  outbound_id       BIGINT COMMENT '关联出库单（可为空，手动创建或历史导入）',
  original_amount   DECIMAL(15, 2) NOT NULL COMMENT '原始应收金额',
  paid_amount       DECIMAL(15, 2) NOT NULL DEFAULT 0 COMMENT '已收金额',
  remaining_amount  DECIMAL(15, 2) NOT NULL COMMENT '剩余应收 = original - paid',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
