## 检测报告文件安全（1.24-1.26，记录于 2026-07-05）

**1. 文件上传仅校验扩展名，未校验文件真实内容**
- 位置：FileStorageService.storeInspectReport
- 现状：仅通过文件名后缀判断是否为 pdf/jpg/jpeg/png，未做 MIME type 
  或文件头 magic bytes 校验
- 风险：已登录用户可将任意文件改扩展名后上传
- 建议修复：后续引入文件头校验（如 Apache Tika）或改用云存储的
  内容安全扫描

**2. 检测报告下载接口无角色限制**
- 位置：FileController.downloadInspectReport（GET /api/files/
  inspect-reports/{filename}）
- 现状：仅要求登录（JWT），未限制角色，任意已认证用户（包括仓管/
  主管等）只要知道文件名即可下载
- 风险：低（文件名为UUID随机生成，且当前系统内网使用，敏感度不高）
- 建议修复：后续做文件权限模块时，按角色或按记录归属校验下载权限

**处理优先级**：MVP阶段暂不修复，标记为技术债，待后续统一处理
