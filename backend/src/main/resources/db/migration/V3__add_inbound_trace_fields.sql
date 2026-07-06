ALTER TABLE inbound_records
  ADD COLUMN origin_place     VARCHAR(100) COMMENT '产地/蜂场名称',
  ADD COLUMN harvest_date     DATE         COMMENT '生产/采集日期',
  ADD COLUMN inspect_no       VARCHAR(50)  COMMENT '检测报告编号',
  ADD COLUMN inspect_org      VARCHAR(100) COMMENT '检测机构',
  ADD COLUMN inspect_date     DATE         COMMENT '检测日期',
  ADD COLUMN inspect_file_url VARCHAR(255) COMMENT '检测报告文件地址（存OSS/本地路径）',
  ADD COLUMN expiry_date      DATE         COMMENT '保质期截止日期';
