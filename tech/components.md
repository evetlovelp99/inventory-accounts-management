# 组件拆分规范

## 说明
- **通用组件**：可跨页面复用，放在 `src/components/`
- **页面组件**：对应一个路由，放在 `src/pages/`
- **布局组件**：全局框架，放在 `src/layouts/`

---

## 一、布局组件（Layouts）

### AppLayout
- **用途**：主框架，包含 TopBar + SideNav + 内容区，登录后所有页面的外壳
- **Props**：无（通过 React Router Outlet 渲染子页面）
- **行为**：根据 authStore 的角色决定 SideNav 显示哪些菜单项
- **复用性**：全局唯一

### TopBar
- **用途**：顶部 56px 固定栏，显示当前页面标题、用户信息、退出按钮
- **Props**：
  - `pageTitle: string` — 当前页面标题
- **行为**：读取 authStore 中的用户名和角色；点击退出清除 token
- **复用性**：全局唯一

### SideNav
- **用途**：左侧 220px 固定导航，Iron 底色，高亮当前激活路由
- **Props**：无（内部读取路由和角色）
- **行为**：
  - 系统设置模块仅「老板」角色可见
  - 激活项：左侧 3px 琥珀色竖线 + 白色文字
  - hover：`rgba(255,255,255,0.08)` 背景
- **复用性**：全局唯一

---

## 二、通用组件（Components）

### StatCard
- **用途**：首页看板的关键指标卡片（库存余量、应收总额等）
- **Props**：
  ```ts
  {
    label: string           // 卡片标签，如"应收总额"
    value: string | number  // 主数值，金额或数量
    unit?: string           // 单位，如"kg"或"元"
    trend?: string          // 可选：如"较上月 ▲ 12.3%"
    status?: 'normal' | 'warning' | 'success'  // 控制左侧竖线颜色
    loading?: boolean       // 骨架屏状态
    onClick?: () => void    // 点击下钻跳转
  }
  ```
- **行为**：
  - `onClick` 有值时，hover 显示 Amber 边框，cursor: pointer
  - `status === 'warning'` 时左侧 4px Brick 红色竖线
  - `loading` 时渲染骨架屏（灰色矩形动画）
  - 同行等高：父容器 grid + align-items: stretch；自身 flex column
- **复用性**：通用，DashboardPage 主要使用

### DataTable
- **用途**：系统中所有列表的通用表格（库存、账款、流水等）
- **Props**：
  ```ts
  {
    columns: ColumnDef[]      // 列定义（Ant Design Table columns 格式，扩展 align/width）
    dataSource: object[]      // 数据
    loading?: boolean         // 加载骨架屏
    rowKey: string            // 行 key 字段名
    onRowClick?: (row) => void // 行点击
    emptyText?: string        // 空状态说明文字
    emptyAction?: ReactNode   // 空状态操作按钮
    pagination?: PaginationConfig | false
    rowClassName?: (row) => string  // 警告行、已结清行样式
  }
  ```
- **行为**：
  - 表头底色 `#F0EDE8`，行交替色（奇 Paper / 偶 `#F9F7F4`）
  - 行高 48px，hover 背景 `#F0EDE8`
  - 操作列按钮：默认隐藏，行 hover 时出现
  - 数字列右对齐 + 等宽字体
  - 空状态：图标 + 说明文字 + 操作入口（不显示表头）
  - 骨架屏：每行用宽度随机 60–90% 的条形占位
- **复用性**：核心通用组件，几乎所有列表页使用

### StatusBadge
- **用途**：标注记录状态（已结清、未结清、入库、出库等）
- **Props**：
  ```ts
  {
    status: '已结清' | '未结清' | '部分还款' | '入库' | '出库' | '还款记录'
  }
  ```
- **行为**：根据 status 映射对应底色/文字色/圆点色（见 components.md 色彩表）
- **复用性**：通用，在所有列表页的状态列使用

### FilterToolbar
- **用途**：列表页的搜索 + 筛选 + 导出工具栏
- **Props**：
  ```ts
  {
    searchPlaceholder?: string
    onSearch: (value: string) => void
    filters?: FilterConfig[]        // 状态筛选、日期范围等
    onFilterChange: (filters) => void
    onExport?: () => void
    exportLabel?: string
    activeFilters: ActiveFilter[]   // 用于渲染 FilterTag
    onClearFilter: (key: string) => void
    onClearAll: () => void
  }
  ```
- **行为**：
  - 搜索框 220px，focus 时 Amber 边框 + 光晕；有内容时右侧出现 ✕ 清除按钮
  - 激活的筛选条件以 FilterTag 形式显示（Amber Light 底色）
  - 导出按钮：有筛选时导出当前结果，Toast 提示导出条数
- **复用性**：通用，所有列表页使用

### EntryForm（容器）
- **用途**：录入表单的外壳卡片，提供标准化布局（最大宽 640px，左对齐）
- **Props**：
  ```ts
  {
    title: string
    onSubmit: (values) => Promise<void>
    onCancel: () => void
    children: ReactNode         // 具体字段内容
    submitLabel?: string        // 如"提交入库"
    loading?: boolean
  }
  ```
- **行为**：提交中禁用所有字段；提交失败后恢复；成功后跳转由父页面处理
- **复用性**：入库页、出库页使用

### BatchRow（出库专用）
- **用途**：出库表单「批次明细」中的单行，选择入库批次 + 填写出库数量
- **Props**：
  ```ts
  {
    index: number
    batches: InboundBatch[]         // 该产品下可用批次列表
    disabledBatchIds: number[]      // 已在其他行选中的批次，置灰不可选
    value: { batchId: number; qty: number }
    onChange: (value) => void
    onDelete: () => void
    canDelete: boolean              // false 时删除按钮禁用（保留最后一行）
  }
  ```
- **行为**：
  - 选中批次后行底显示：入库日期、采购价、可用余量（Caption + Ash 色）
  - 数量超过批次余量时：输入框 Brick 红色边框 + 行内错误提示
- **复用性**：页面专用，仅 OutboundEntryPage 使用

### ExchangeRateInput（出库专用）
- **用途**：出库表单中，币种选择为美元时展示的汇率输入框，自动预填当日人行中间价（详见 `docs/multi-currency-outbound.md`）
- **Props**：
  ```ts
  {
    value: number | null       // 当前汇率值
    onChange: (rate: number) => void
    loading: boolean           // 自动获取汇率接口请求中
    autoFetchFailed: boolean   // 自动获取失败，需用户手动输入
  }
  ```
- **行为**：
  - `loading` 时输入框显示加载态（骨架/spinner），字段暂不可编辑
  - `autoFetchFailed` 时输入框清空，下方显示提示文案"自动获取汇率失败，请手动输入当日汇率"，但**不阻塞**表单其他字段填写和保存
  - 正常状态下预填当日中间价，用户可随时手动修改
  - 仅在币种选择为美元时渲染；人民币场景该组件不出现，无汇率相关字段
  - 空值保存校验（"请填写汇率"）由父表单（OutboundEntryPage）在提交时处理，本组件只负责输入交互
- **复用性**：页面专用，仅 OutboundEntryPage 使用

### Button
- **用途**：封装 Ant Design Button，应用自定义颜色系统（Primary/Secondary/Danger/Ghost/Link）
- **Props**：
  ```ts
  {
    variant: 'primary' | 'secondary' | 'danger' | 'ghost' | 'link'
    size?: 'standard' | 'compact'   // 40px / 32px
    loading?: boolean
    disabled?: boolean
    onClick?: () => void
    children: ReactNode
  }
  ```
- **复用性**：全局通用

### Toast / useToast
- **用途**：右上角操作反馈提示（成功 3 秒自动消失，失败需手动关闭）
- **Props**（组件）：无，由 useToast hook 驱动
- **useToast Hook**：
  ```ts
  showToast(message: string, type: 'success' | 'error'): void
  ```
- **行为**：最多叠放 3 条，从右侧滑入，淡出消失；错误 Toast 不自动关闭
- **复用性**：全局通用

### AlertBar
- **用途**：TopBar 下方全局警告条（服务器错误等严重情况）
- **Props**：由 alertStore 驱动，无直接 props
- **行为**：只显示一条，有新的则替换；Brick Light 底色 + 左侧 4px 红色竖线
- **复用性**：全局唯一

---

## 三、页面组件（Pages）

### LoginPage
- **用途**：登录页，账号 + 密码表单
- **行为**：提交后存储 JWT token 到 authStore，跳转首页
- **无 Layout**：登录页不使用 AppLayout

### DashboardPage（首页看板）
- **用途**：登录后首屏，4 张 StatCard + 库存概况 + 账款概况 + 最近动态
- **布局**：模式 A（看板布局）
- **数据**：调用 `GET /api/dashboard/summary`
- **专用子组件**：
  - `RecentActivity`：最近 10 笔操作的时间轴列表（纯展示，页面专用）

### InboundEntryPage（录入入库）
- **用途**：入库录入表单
- **布局**：模式 C（录入表单布局）
- **字段**：产品（Select）、数量、采购单价、入库日期、供应商（Select）、备注；自动计算总金额
- **专用逻辑**：提交时询问"该批货款是否已付"，若否则自动创建应付账款记录

### OutboundEntryPage（录入出库）
- **用途**：出库录入表单，含多行批次明细
- **布局**：模式 C
- **字段**：产品（Select，显示总余量）、币种选择（人民币/美元，单选）、汇率（仅美元时显示，见 `ExchangeRateInput`）、销售单价（按所选币种）、出库日期、客户（Select）、备注；批次明细区（BatchRow × N）；汇总计算区（只读，展示折算后的总销售额与预估毛利）
- **专用逻辑**：
  - 提交时询问"货款是否已收"，若否则自动创建应收账款记录，并透传币种/汇率
  - 美元记录提交前校验汇率非空，为空则阻塞提交并提示"请填写汇率"

### StockOverviewPage（当前库存）
- **用途**：所有产品当前余量列表
- **布局**：模式 B（列表布局）
- **行为**：行点击跳转至 ProductLedgerPage；余量为零时行文字变 Brick 红

### ProductLedgerPage（单产品流水）
- **用途**：某产品完整入库/出库流水
- **布局**：模式 B
- **路由参数**：`productId`
- **筛选**：日期范围

### ReceivablePage（应收账款）
- **用途**：客户欠款列表（按客户聚合）
- **布局**：模式 B
- **行为**：顶部显示应收总额 StatCard；行操作「登记还款」弹出 PaymentModal；行点击进入 AccountDetailPage

### PayablePage（应付账款）
- **用途**：供应商欠款列表（按供应商聚合）
- **结构**：与 ReceivablePage 对称

### AccountDetailPage（账款详情）
- **用途**：单客户或单供应商的欠款明细 + 完整收/付款流水
- **路由参数**：`type: 'receivable' | 'payable'`，`partyId: number`

### PaymentModal（弹窗，页面专用）
- **用途**：登记还款/付款的弹窗
- **Props**：
  ```ts
  {
    visible: boolean
    remainingAmount: number      // 显示当前剩余待收/待付
    onConfirm: (amount, date, remark) => void
    onCancel: () => void
    type: 'receivable' | 'payable'
  }
  ```
- **行为**：输入金额不可超过剩余欠款，超出时行内报错

### ProductsPage / UsersPage / CustomersPage / SuppliersPage（设置子页）
- **用途**：基础数据的 CRUD 管理页
- **布局**：模式 B（含「新增」按钮在筛选栏右侧）
- **共用模式**：列表 + 行内操作（编辑/停用） + 新增/编辑弹窗（Modal）
- **EditModal**（每个设置页各自一个，页面专用）：对应各自的字段

### ImportPage（历史数据导入）
- **用途**：三步式导入流程（下载模板 → 上传 → 预览确认 → 结果）
- **布局**：模式 C（卡片居左）
- **专用子组件**：
  - `ImportStepIndicator`：步骤进度指示器（页面专用）
  - `PreviewTable`：解析结果预览，异常行标红（基于 DataTable，页面专用扩展）
  - `ErrorReportDownload`：下载错误报告按钮（页面专用）

---

## 四、全局状态（Store）

### authStore（Zustand）
```ts
{
  token: string | null
  user: { id, name, role: 'OWNER' | 'FINANCE' | 'WAREHOUSE' | 'SUPERVISOR' } | null
  login: (token, user) => void
  logout: () => void
}
```

### alertStore（Zustand）
```ts
{
  alert: { message: string; visible: boolean } | null
  showAlert: (message: string) => void
  hideAlert: () => void
}
```

---

## 五、工具函数（Utils）

### formatCurrencyAmount
- **用途**：按币种格式化金额展示（人民币 ¥ 前缀 / 美元 $ 前缀），用于出库/应收列表页和详情页展示原始币种数据
- **签名**：`formatCurrencyAmount(amount: number, currency: 'CNY' | 'USD'): string`
- **复用性**：通用工具函数，非 React 组件，`DataTable` 的金额列渲染时调用
