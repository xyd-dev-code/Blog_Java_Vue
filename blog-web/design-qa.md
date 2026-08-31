# 本次检查：全站背景补充（2026-08-31）

- final result: passed
- P0/P1/P2：0（本次背景改动范围）。应用内浏览器缺失运行模块，已使用项目现有隔离式 Headless Edge/CDP 检查方式完成截图与交互验证，不使用个人浏览器会话，不写后台数据。
- 本次范围：仅 `src/styles/themes.scss` 的背景绘制。首页首屏保持原样，首页后续分区、前台内页、后台和登录页复用已有水墨山水、客栈与纸纹素材。阅读页和后台使用更淡的背景罩染。
- 源视觉：`./local-workspace`，1910 × 873 像素。源图用于约束布局、字体、卡片和内容，不是要求保留其中的单色背景。
- 实现地址：`http://localhost:5173/articles`；首页 `http://localhost:5173/` 滚动到精选文章。
- 实现截图：`design-qa-assets/background-only/after-1440-_-featured.png`、`after-1440-_articles-top.png`、`after-390-_-featured.png`、`after-390-_articles-top.png`，以及同目录各内页截图。
- 视口/密度：1440 × 900 和 390 × 844 CSS px，DPR 1，原始截图像素与视口一致。前后对照始终使用相同数据、滚动位置、字体加载状态与视口；用户截图与测试数据不同，不据此宣称逐像素复刻用户截图。
- 对照基线：本次改动前保存的 `themes.before.scss`，只在隔离测试页替换主题 CSS，工作区源码不回滚。测试数据由本地固定 API 响应提供，截图中的文章示例不写入真实站点。
- 全视图证据：`compare-1440-_-featured.png`、`compare-1440-_articles-top.png`、`compare-390-_-featured.png`、`compare-390-_articles-top.png`；图片为前后同框对照，等密度并排，已打开检查。
- 局部证据：`compare-card-detail.png`，核对封面、标签、正文换行、卡片尺寸与间距；阅读页证据 `compare-1440-_articles_layout_1-top.png`；滚动证据 `home-scrolled-desktop.png`。

## 已完成的静态验证

- `npm run test:theme-layout` 通过；该次未传 git baseline，`originalLayoutFilesCompared` 为 0，不将其当作浏览器几何比较。
- `npm run build` 通过。仅出现依赖已有的 Rollup PURE 注释警告。
- 与本次开始时快照逐一核对 112 个源码文件，仅背景样式文件发生变化。
- 编译前后 SCSS 后比较 CSS：209 条非背景声明完全相同；增量仅为 `background-*` 及两个背景透明度变量。
- 三张复用素材已实际打开查看，且本地服务均返回 HTTP 200。未新增图片、字体、依赖或业务逻辑。
- 浏览器对照：48 个视口/主题/页面状态、532 个内容框的实际坐标和排版指标全部一致，页面文字一致，0 横向溢出、0 运行异常。报告：`design-qa-assets/background-only/background-report.json`。
- 首页首屏像素核对：桌面 1440 × 714 的首屏区域、手机 390 × 844 可见首屏区域前后像素完全相同。报告：`home-hero-pixel-report.json`。
- 交互：桌面文章导航、移动菜单展开/跳转/关闭通过；分类、标签、归档、搜索背景加载与无溢出检查通过；首页各 section 使用相同固定背景坐标，长页滚动截图无重起画卷接缝。报告：`interaction-report.json`。

## 必查表面

- 字体与排版：声明和组件均未改动；同框全图和局部对照中标题、正文换行保持一致。
- 间距与布局：没有增加节点，没有修改尺寸、定位、网格、边距、圆角；桌面/移动端实际几何比较全部通过。
- 色彩与视觉变量：只改变水墨主题页面背景。边缘保留明显松林、客栈、山门和剑客，中间纸色留白；文章阅读内容表面保持原样；晴天主题保持原背景。
- 图片质量：复用透明画卷、山峦、纸纹，保持原比例，移动端向右侧山门取景；全图和局部检查未发现拉伸、矩形底板或新增内容遮挡。
- 文案与内容：所有 Vue、JS、路由和业务数据均未修改。

## 比较历史与限制

1. 已定位首页内容区禁用全局背景、内页缺少明显实景画作的问题，并完成背景代码调整。
2. 应用内浏览器连接缺失模块，使用现有项目检查脚本的隔离 Edge/CDP 方式补齐前后截图与布局证据。该故障不涉及页面源码。
3. 初次前后视觉比较未发现本次新增的 P0/P1/P2，无需视觉返工。交互脚本修正了等待移动抽屉动画完成及文章页选择器后复验通过，没有修改应用交互代码。
4. 这是背景专项检查，未重做原有 UI、内容或其他既存视觉问题；未测试真实后台提交、Safari/iOS 真机或外部资源网络波动。浏览器验收采用固定测试数据，线上内容仍保持原样。

---

# 历史记录：首页 Product Design QA（以下通过结论不代表本次背景验收）

## 结论

- final result: passed
- P0：0
- P1：0
- P2：0
- P3：0

这版没有把参考画面作为整张首屏背景；参考图仅用于提取宣纸底色、墨山层次、朱砂枝叶框景、中央留白与清峻壮阔的气势。实际页面由独立图片层与真实 UI 组件重新构图。

## 对照输入

- 参考图：`./local-workspace`
- 参考图像素：7992 × 4484
- 实现截图：`design-qa-assets/home-desktop-final.png`
- 实现截图像素：1894 × 953
- 同画框对照：`design-qa-assets/reference-vs-implementation.png`
- 浏览器：Microsoft Edge，桌面视口 1894 × 953 CSS px，DPR 1
- 页面状态：`ink` 主题、首页、天气定位未授权、顶部导航可见

## 视觉核对

### 全视图

通过同一 16:9 画框中的并排对照完成判断：

- 保留了参考图的深墨主峰、灰墨远山、朱砂框景和中央呼吸感。
- 没有复刻参考图中的湖面、亭台和完整场景；它们不再作为产品背景图出现。
- 标题区和天气卡是独立组件，分别落在左右视觉支点上，背景山脉仍保持主体气势。
- 参考图偏粉白的艺术留白，在产品中调整为更温和的宣纸色，以保证正文、导航和后续内容区的阅读一致性。

### 重点区域

在 1:1 桌面截图中核对了导航、标题题签、主按钮和天气卡：文字层级清楚，卡片边界不过度圆润，朱砂只用于印章、焦点和细线强调。该截图的分辨率足以判断重点区域，因此未额外制造会改变缩放关系的局部裁切。

### 响应式与动效

- 桌面端为左右双支点构图；窄屏改为纵向堆叠，并减弱或隐藏右侧枝叶，避免遮挡信息。
- 远山仅做 18 秒极慢呼吸，枝叶仅做 14 秒轻微漂移；卡片不持续浮动。
- `prefers-reduced-motion` 下关闭山体、枝叶与揭示动效。

## 交互与运行核对

- 首屏四个图片层全部加载完成，具有非零自然尺寸。
- 主按钮“开始阅读”点击后进入 `/articles`，页面标题变为“文章 · 个人博客”。
- HTML 为 `display: block`、`visibility: visible`、`opacity: 1`，白屏问题已消除。
- Edge 控制台异常与 warning：0。
- `npm run build`：通过。

## 比较与修正历史

1. P0：复杂 scoped 主题选择器被编译到 `html` 后触发隐藏，造成白屏。主题覆盖已移动到全局主题层。
2. P1：首版把参考图作为完整 hero 背景，偏离“用视觉语言设计 UI”的目标。完整场景资产已删除。
3. Final：改为宣纸、远山、左枝叶、右枝叶四个独立资源，标题、天气、导航保持真实可交互组件，并通过同画框视觉对照。

## 生成素材记录

模式：Codex 内置 ImageGen。

- `public/images/ink-paper-texture-v2.png`
  - Prompt：`Use case: stylized-concept. A seamless, subtle warm ivory Xuan-paper texture only, with extremely restrained natural fibers and soft tonal variation. No mountains, foliage, calligraphy, stamps, objects, text, watermark, shadows, gradients, or folds.`
- `public/images/ink-mountain-layer-v2.png`
  - Prompt：`Transparent isolated Chinese ink-wash mountain layer for a responsive website hero: one clear charcoal central ridge, receding gray peaks, a few pale blush side peaks, mist-soft lower edge, contemporary expressive ink, monumental and clean. No paper background, trees, buildings, birds, text, watermark, frame, or UI.`
- `public/images/ink-foliage-left-v2.png`
  - Prompt：`Transparent left-edge foreground foliage layer for a modern Chinese ink-wash website hero: asymmetric vermilion and orange maple branches growing from the lower-left and framing inward, dark expressive branch lines, painterly leaf clusters with varied density, open center. No background, mountains, buildings, text, watermark, frame, or UI.`
- `public/images/ink-foliage-right-v2.png`
  - Prompt：`Transparent right-edge foreground foliage layer for a modern Chinese ink-wash website hero: asymmetric vermilion and orange maple branches growing from the lower-right and framing inward, dark expressive branch lines, painterly leaf clusters with varied density, open center. No background, mountains, buildings, text, watermark, frame, or UI.`

## 实现位置

- 分层首屏：`src/components/SkyHero.vue`
- 水墨主题与响应式：`src/styles/themes.scss`
- 主题色板：`src/themes/registry.js`
- Edge 回归脚本：`scripts/edge-cdp-qa.mjs`
- 同画框比较脚本：`scripts/edge-cdp-comparison.mjs`
