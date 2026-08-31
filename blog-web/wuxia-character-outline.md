# 首页剑客完整轮廓

用户要求人物边缘既不虚化也不截断。原来的 `object-fit: cover`、1.5 倍放大和底部 40% 裁切已移除；现在使用完整轮廓素材和 `object-fit: contain`，无缩放变换、无边缘渐隐、无矩形裁切。文字、按钮、天气卡及首屏布局没有修改。

目前人物为静态图。原 `public/media/wuxia-swordsman-intro.mp4`、`public/images/wuxia-swordsman-poster.webp` 均保留，未删除、未覆盖。

资源：`./blog-web/public/images/wuxia-swordsman-complete-v2.png`，1254 × 1254，1,453,089 字节。

该文件为 RGB 白底图，不是带 alpha 的 PNG。浏览器通过 SVG 颜色键将接近纯白的背景变为透明，只按像素颜色去底，不做空间模糊、外轮廓渐变或裁切。保留原有灰阶、墨色透明度和背景。

生成方式：Codex 内置 ImageGen，没有使用 CLI/API fallback。第一版生成了棋盘格背景且剑柄仍贴边，未接入项目；最终版改为纯白底并补齐剑柄、头发和短衣摆的完整轮廓。

最终编辑输入：`./local-workspace`。该输入由原视频海报衍生。最终生成原件保留于 `./local-workspace`，项目使用的是它的副本。

最终提示词：

> Edit the provided swordsman illustration for final website use. Preserve this exact same face, hair, horizontal sword pose, hands, armor and sharp monochrome drawing. Two necessary fixes: (1) the sword handle is clipped at the right edge: complete it and provide a clear safety margin around ALL hair, sword tip, handle, elbows and fabric, nothing touching any canvas edge; (2) remove the excessive long skirt/tail below the waist, ending the illustration just below the belt with a compact set of SHORT pointed fabric ends / crisp ink brush tips, each with a complete organic outline, not a straight crop. Keep a large head-and-torso portrait and make the face prominent; do not zoom out to a small full-body figure. Square 1:1 canvas, illustration occupies about 90% of canvas width and height, transparent-looking empty area to the lower left, body concentrated on the right. IMPORTANT OUTPUT BACKGROUND: use completely uniform PURE WHITE #FFFFFF around the silhouette and in all gaps; no checkerboard anywhere, no transparency-grid pattern, no gray texture, no paper texture, no gradient, no shadow. This white background will be removed by multiply blending in the website. Natural edges must be CRISP and fully drawn, with no feathering, no blur, no fade, no vignette, no fog, no white halo. Do not change the character or pose, do not add text, do not add any rectangular frame or border. No clipping on any side.

实际去底采用 SVG 颜色键，避免父级图层隔离导致叠乘后仍出现白色方框。

验证截图：`design-qa-assets/wuxia/character-complete-outline.png`。现有视觉检查覆盖六种屏幕宽度、主题切换、移动菜单、阅读按钮、城市选择器和动态偏好切换。
