# 马善政首页副标题字体子集

`ma-shan-zheng-motto.woff2` 从同目录的 `ma-shan-zheng.woff2` 提取，仅包含“把代码写成诗”六字，保留原有字形。用于首页预加载，减少首屏字体切换。

Copyright 2018 The Ma Shan Zheng Project Authors。沿用 SIL Open Font License 1.1，完整许可见 `MaShanZheng-OFL.txt`。

生成命令（需安装 fonttools 和 brotli，在 blog-web 目录执行）：

```sh
python -m fontTools.subset public/fonts/ma-shan-zheng.woff2 --text=把代码写成诗 --flavor=woff2 --output-file=public/fonts/ma-shan-zheng-motto.woff2
```
