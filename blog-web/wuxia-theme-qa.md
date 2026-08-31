# 武侠水墨主题切换

最新人物轮廓修正：为同时满足“不虚化、不截断”，首页已使用补齐轮廓的静态人物素材，取消放大裁切和边缘渐隐。原视频、海报保留；下文关于视频自动播放的描述是历史记录，当前资源、去底方式和生成提示词见 `wuxia-character-outline.md`。

布局恢复：已撤销错误增加的字号、行高、字距和换行覆盖；仅首页首屏主副标题换字体，其他区域保留原字体与布局。缓存版本为 7，当前范围见 `wuxia-typography.md`。验证直接比较真实几何，不归一化 CSS 参数。

设计判断：个人博客的保留式改版，以灰白宣纸、冷灰墨黑、朱砂和剑影切换气质。沿用 Vue / Element Plus 与已有水墨渲染；构图、动效、密度分别延续为 6 / 4 / 3，不新增内容分区。

## 改动范围

- 保留 `ink` 主题 ID 与管理员设置流程，展示名称更新为“武侠水墨”；缓存版本同步为 5。
- 保留导航、站点名称、后台座右铭、文章、标签数据、天气功能及全部路由。首页第二行主题题字改为“仗剑天涯”，仅展示层变化，不写站点配置。
- 保留原有水墨纸纹、远近晕染和留白。红枫框景替换为客栈、山门、剑客与对峙剪影，增加装饰性的江湖舆图和三式剑谱。
- 展示标题使用本地书法字体，正文、字号、间距、容器和组件形状不变。字体行框按已有 Noto Serif SC 的 ascent/descent 校准，避免 `line-height: normal` 改变页面高度；文章页打字光标保持原字体宽度。
- 用户视频仅用作经过渐隐蒙版的背景画层，4.5 秒静音播放后停止。减少动态效果、省流量或 2G 连接使用静态海报；页面不可见或首屏离开视口时暂停。所有画作与图谱均 `aria-hidden`，不截获点击。
- 晴天主题保持原有风格与文案；后台主题预览使用实际客栈画作。
- 已撤销为避让人物而错误新增的文案左边距和手机端顶部边距，标题、说明、按钮和天气卡保持原布局。按最新红框标记，人物移至标题右侧、天气卡左侧，画层下缘在说明文字前渐隐；1200px 以下继续使用首屏原有的 104px 顶部空白，不新增空间、不挤动内容。

## 资源

配色修正：根据“为什么还是黄色的配色”的反馈，移除全局背景、卡片、导航、编辑区域的暖黄基色，改为 `#f2f3f4` 灰白宣纸。纸纹用 luminosity 混合保留纤维明暗、去除原图黄色；视频和天气图标取消 sepia，保留朱砂点缀。本次仅修改颜色与混合方式，不改布局。

| 文件 | 来源 / 用途 | 大小 |
| --- | --- | --- |
| `public/media/wuxia-swordsman-intro.mp4` | 用户提供的 `D:/Download/【哲风壁纸】刀剑-剑士-古风.mp4`，1280 × 720、24 fps、静音 4.5 秒片段 | 395,566 B |
| `public/images/wuxia-swordsman-poster.webp` | 用户视频第 2 秒静态海报，原视频未修改 | 69,938 B |
| `public/images/wuxia-jianghu-border.webp` | Codex 内置 ImageGen 生成，保留透明通道后转 WebP | 531,744 B |
| `public/fonts/ma-shan-zheng.woff2` | [Google Fonts / Ma Shan Zheng](https://github.com/google/fonts/tree/main/ofl/mashanzheng)，完整字库本地托管 | 3,250,344 B |
| `public/fonts/MaShanZheng-OFL.txt` | 随字体保留的 SIL OFL 许可证 | 4,397 B |

视频原文件约 34.6 MB，没有直接放进网页。完整书法字库保留对后台自定义中文名称的支持，使用 `font-display: swap`；字体不可用时回退到楷体。没有新增运行时 npm 依赖，没有发布或修改后端数据。

## 验证

- 最新红框移位：在 1520 × 696 CSS 像素、1.25 倍截图比例下，人物画层位于 `(472, 180)`、尺寸 `350 × 400`，对应用户标记的中间区域。移动前后导航、首屏、标题、说明、按钮和天气卡的坐标及尺寸完全一致，见 `design-qa-assets/wuxia/marked-position-before.json`、`marked-position-after.json` 及同名截图。
- `npm run test:theme-layout -- --baseline=HEAD`：54 个原有组件/样式文件受原始布局声明检查。仅允许明确标题选择器的 `font-family` 变化，不允许新增文案边距或放宽字号、行高、容器及形状约束。
- `npm run test:theme-layout:browser`：通过真实主题 store 切换，首页及 11 个内页 × 3 个视口均逐元素比较位置、尺寸与形状，必须零布局差异；首页另检查文案无额外边距、人物不与标题实际字形及按钮、天气卡相交、页面无横向溢出。标题所在列的空白区域允许放置人物，不把整列空白当成文字。
- `node scripts/wuxia-visual-qa.mjs`：1900、1440、1024、768、520、390 六种宽度的实际本地页面截图和人物/标题字形、按钮、天气卡不相交检查；说明文字区域另通过截图检查下缘渐隐。同时检查无横向溢出、素材成功加载、移动菜单、阅读按钮、城市选择器、晴天与武侠主题往返、减少动态效果以及视频静音/时长/播完停止。
- `npm run build`：通过，保留依赖原有的 PURE 注释警告；字体和新图片、视频均被复制进产物。
- `git diff --check`：通过。

自动布局回归使用固定 API 数据且不写后端；视觉和交互检查访问本地开发站点，不提交表单、不保存管理员主题设置。天气服务依赖网络，截图中为未授权状态，未伪造天气信息。

截图与记录：`design-qa-assets/wuxia/`。布局回归明细：`design-qa-assets/theme-layout-regression/report.json`。

## 生成画作的最终提示词

模式：Codex 内置 ImageGen；不是 CLI / API fallback。生成原图保留在 Codex generated_images 目录，项目实际使用的是上表中的 WebP 文件。

> Use case: stylized-concept. Create one ultra-wide 3:1 Chinese wuxia ink-wash border landscape illustration for the bottom layer of an existing website. True transparent alpha background. Composition: painting is confined to the bottom 55% and the far left and far right, upper half and central third left completely transparent for real website content. Lower left: small rugged riverside martial-arts roadside inn with a weathered double-eaved tiled roof, a tiny vermilion hanging banner, dark gnarled pine, a lone conical-hatted swordsman silhouette walking toward the inn. Far right: steep angular ink mountain crags with a small fortress-like sect gate and two distant tiny rival martial-artist silhouettes with swords. Middle bottom: wispy pale gray ink mountains and sparse mist connecting the edges, fading fully to transparent. Mood: austere, weathered jianghu, cinematic old wuxia book illustration, energetic dry-brush strokes, sumi-e ink bleeding, rough brush texture. Palette charcoal black, warm gray wash, just one tiny muted cinnabar red banner, no other colors. Beautiful hand-painted traditional Chinese ink, NOT photorealistic, NOT 3D, NOT flat vector, NOT anime. No text, no calligraphy, no watermark, no UI, no panels, no paper rectangle or full-page background. All edges dissolve into genuine transparency. Resolution as wide and detailed as possible.
