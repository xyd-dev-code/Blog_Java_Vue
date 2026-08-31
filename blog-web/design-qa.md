# 本次检查：水墨场景加强与留言标题去重（2026-08-31）

- final result: 本轮背景与标题修复通过；不代表全站 WCAG AA 已通过。
- 应用源码只改动 `src/styles/themes.scss`，112 个源文件快照复核中仅该文件变化。没有修改 Vue、业务逻辑、前景字色或布局，没有新增图片/依赖。
- 背景改为复用已有山水、客栈山门、左右枝叶，纸纹继续用 CSS 绘制；文章、项目、工具、友链、留言、关于使用不同取景。现代几何底纹退出前台，原晴天背景装饰在水墨内页透明度为 0；首页与晴天主题不变。
- 留言标题根因：`.hg-title` 的 `background-clip:text` 仍绘制来源字形，叠加书法图形成重影。仅在题字 `.is-ready` 后清除旧渐变背景，保留原文字、字体尺寸、语义、占位与未加载时的回退。

## 可复核证据

- 基线：`./local-workspace`；同一隔离页面、同一数据和视口替换前后 CSS，不回滚工作区，不提交后台数据。
- 浏览器：项目已有的隔离 Headless Edge/CDP 工作流；应用内浏览器运行模块缺失，未使用用户登录会话。
- 52 组最终有效状态、664 个布局框，前后几何与文字排版一致，0 横向溢出。覆盖 1910×873 DPR 1 浅色六页、1440×900 DPR 2 / 390×844 DPR 3 夜间前台与后台代表页，以及晴天首页/留言页。
- 扩展初测中手机标签页发生一次 12px 的页头高度变化；没有对应 CSS 几何声明变化。在每次替换 CSS 后重新等待 `document.fonts.ready`，标签页两个尺寸均复测通过，符合字体加载时序波动。初始记录保留于 `extended/background-report.json`，最终替代证据在 `recheck/background-report.json`。
- 关于页最终调整为左侧枝影，避免与工具页相似；浅色最终证据位于 `about-final/`，夜间最终证据位于 `recheck/`，汇总报告使用这些最终记录替代初稿。
- 首页首屏和精选分区：1440/DPR2、390/DPR3 共 4 对水墨截图逐像素一致。
- 编译前后 CSS 比较：所有已有非背景声明保留；唯一新增非 background 声明是既有背景装饰组的 `opacity:0`。尺寸、定位、边距、字体、层级、阴影均未变。
- `npm run build` 与 `npm run test:theme-layout` 通过（布局脚本未传 git baseline，`originalLayoutFilesCompared:0`）；最后的取景与夜间透明度参数调整另经 Sass 编译和浏览器复验。构建仅有已有 Rollup PURE 注释警告。

## 标题专项验证

- 前后标题矩形均为 `[605.75, 114, 220.5, 51]` CSS px；原文字为“留言板”，来源文字 opacity=0，书法 SVG 数量=1。
- 在隔离测试页临时隐藏书法 SVG：修改前仍留下 3280 个深色字形像素，修改后为 0，证明旧渐变字形已清除；测试后已恢复 SVG。
- 桌面与手机题字实际截图均为单层；“落笔留书”按钮正常打开留言弹窗，没有提交留言。
- 证据：`title-report.json`、`title-ghost-pixel-report.json`、`title-without-art-before.png`、`title-without-art-after.png`、`guestbook-title-final.png`。

## 视觉比较与限制

- 已打开并排比较 `compare-projects-1910.png`、`compare-guestbook-390.png`，核对内容位置、字号、卡片尺寸、按钮位置、背景差异和标题重影。
- `scene-overview.png` 为当前六页总览；各页原始截图保留在 `design-qa-assets/ink-scenes/`。
- 初稿有近景/枝叶图片边缘落在画布内部的截断，已扩大取景使边缘落出视口，并重新截图检查。画作保持原始比例，不改图片文件。
- 夜间仍是深墨外侧与浅纸阅读面的背景适配，不是完整反色主题。前景色未获修改授权，既有浅灰辅助文字 AA 问题保持上一轮记录；本轮不声称全面对比度认证。
- 固定数据截图只用于隔离检查，示例文章/头像不写入站点。未测试真实后台提交或 Safari/iOS 真机。
- 汇总报告：`design-qa-assets/ink-scenes/summary-report.json`；源码边界报告：`static-report.json`。

---

# 历史检查：内页差异化纯 CSS 背景

- final result: 背景与布局检查通过；完整 WCAG AA 验收未通过，存在既有前景色问题。
- 范围：26 类非首页页面，背景色、CSS 渐变/纹理、背景变量；应用代码只由本任务修改 `src/styles/themes.scss`。没有新增运行时图片、字体或依赖。完整设计表：`page-background-design.md`。
- Product Design 视觉约束：用户提供的截图与现有项目布局；此次明确要求只改背景，且禁止新图片，因此使用纯 CSS 纹理实现，不生成新素材、不重做组件。
- 本次基线：`./local-workspace`。使用相同数据、主题、视口和已加载字体，仅在隔离测试页面替换前后 CSS；没有回滚工作区或写入后台数据。
- 浏览器：项目现有隔离 Headless Edge/CDP 方式；应用内浏览器运行模块仍不可用。没有使用用户浏览器资料或真实账号会话。

## 布局与实现边界

- 前台 44 组：11 条路由 × 1440/390 CSS px × light/dark，DPR 1。
- 补充 40 组：首页首屏/精选分区、晴天/水墨、登录、404、后台全部主要路由，1440 × 900 DPR 2 与 390 × 844 DPR 3。
- 合计 **84 组状态、784 个布局框**。前后文字、坐标、尺寸、字体、间距、网格、边框宽度、圆角指标一致；0 横向溢出，0 捕获到的运行异常。
- 首页水墨首屏和精选分区：两种测试尺寸共 4 对截图逐像素完全一致。晴天首页几何与排版也一致。
- 编译 CSS 对比：206 条非背景声明完全相同，0 新图片 URL。新增声明仅涉及 `background-*` 和 `--wuxia-*` 背景/层级说明变量；没有改写 z-index。
- `npm run test:theme-layout` 通过；未传 git baseline，因此 `originalLayoutFilesCompared:0`，不把它作为独立几何证据。`npm run build` 通过，仅有依赖已有的 Rollup PURE 注释警告。
- 112 个源文件快照检查期间，还检测到 App.vue、useWuxiaCopy.js、router/index.js、Home.vue 的其他工作区变动；本任务没有写入或回滚它们。不能将工作区总 diff 都归为本次背景修改。

## 视觉证据

- `design-qa-assets/page-backgrounds/page-background-overview.png`：文章、项目、工具、友链、关于、搜索六页总览，已打开核对。
- `compare-1440-projects.png`：同状态前后并排，斜向淡墨带与原卡片布局的关系清楚。
- `compare-390-tools.png`：同状态手机前后并排，检查网格、导航、搜索框、卡片尺寸与换行。
- 桌面项目、工具、文章详情，手机项目、登录，后台概览与夜间工具页等原始截图已打开检查；装饰没有新增内容遮挡。图像查看器曾缩小高 DPI 图用于展示，文件保留原始像素。
- DPR 2/3 的截图见 `extra/`；测试范围是模拟设备密度，不代表 Safari/iOS 真机验收。桌面背景 fixed，手机 scroll；纹理间距以 CSS px 稳定，细线随 DPR 收细。
- 夜间是深墨外侧 + 明亮阅读纸面的背景适配。现有前景色未反转，因此不能声称交付了完整深色 UI。

## 对比度与尚未满足的要求

- 已在背景范围内修复：无封面文章不再使用照片暗遮罩；有封面文章原遮罩加深；白字分类/标签/归档页头继续采用深底。
- 浏览器辅助检查：11 个前台页面 × light/dark × before/after，隐藏普通文字绘制后，每个可见文字矩形取 10 个背景样本，结合计算后前景色和祖先透明度计算对比度。该抽样未发现从通过变为不通过的新增阈值跨越。
- 该方法不是完整 WCAG 认证：不覆盖图形题字、渐变文字、占位字、屏外内容、所有交互状态或任意用户上传图片；部分装饰符号/复杂背景产生的报警需要人工判读，不将其全部计为真实可访问性缺陷。
- 可以确认的既有问题：项目卡描述约 3.76:1，工具卡描述约 3.80:1，留言时间/操作浅灰文字约 2.19:1；原先即不满足普通字号 4.5:1。色板中 #737b83 在纯白上也只有 4.29:1，#a0a7ae 在纯白上仅 2.43:1。
- 这些前景色不适合靠继续减淡浅背景解决。为遵守用户“仅背景样式”的限制，本次保留原前景色，不声称全站 AA 达标；完整通过需要额外授权调整相关文字颜色/透明度，并复查组件各状态。
- 规则与计算口径见 `page-background-design.md`，标准依据 [WCAG 2.2 对比度最低要求](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html)。

## 机器报告

- `design-qa-assets/page-backgrounds/background-report.json`
- `design-qa-assets/page-backgrounds/extra/background-report.json`
- `design-qa-assets/page-backgrounds/static-report.json`
- `design-qa-assets/page-backgrounds/home-pixel-report.json`
- `design-qa-assets/page-backgrounds/contrast-report.json`

---

# 历史检查：全站背景补充（本节不是本次验收结论）

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
