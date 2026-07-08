ALTER TABLE outbound_records
  ADD COLUMN currency ENUM('CNY','USD') NOT NULL DEFAULT 'CNY' COMMENT '结算币种',
  ADD COLUMN exchange_rate DECIMAL(10, 4) NULL COMMENT '美元汇率（人行中间价，可手动修改），人民币记录为空',
  ADD COLUMN converted_sale_amount DECIMAL(15, 2) NULL COMMENT '折算人民币总额';

UPDATE outbound_records
SET converted_sale_amount = total_sale_amount;

ALTER TABLE outbound_records
  MODIFY COLUMN converted_sale_amount DECIMAL(15, 2) NOT NULL COMMENT '折算人民币总额：人民币记录 = total_sale_amount，美元记录 = total_sale_amount × exchange_rate；用于仪表盘/报表统一汇总，gross_profit 基于此字段计算';

ALTER TABLE account_receivables
  ADD COLUMN currency ENUM('CNY','USD') NOT NULL DEFAULT 'CNY' COMMENT '继承自出库记录的结算币种',
  ADD COLUMN exchange_rate DECIMAL(10, 4) NULL COMMENT '继承自出库记录，创建时快照一次，人民币记录为空',
  ADD COLUMN converted_amount DECIMAL(15, 2) NULL COMMENT '折算人民币金额';

UPDATE account_receivables
SET converted_amount = original_amount;

ALTER TABLE account_receivables
  MODIFY COLUMN converted_amount DECIMAL(15, 2) NOT NULL COMMENT '折算人民币金额 = original_amount × exchange_rate，仅用于仪表盘应收汇总展示';
