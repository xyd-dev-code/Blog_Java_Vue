# 历史隐私清理记录

本次在独立克隆中重写全部 45 次提交，保留开发顺序及提交时间。原仓库与未提交修改已有离线备份。

- 删除所有历史中的真实 application.yml、server-config、内部需求 DOCX、内部设计规格和 design-qa-assets 原始截图/报告。
- 个人邮箱、昵称、部署域名/IP、个人工作路径统一替换为示例值；作者和提交者统一使用公开昵称及 GitHub noreply 邮箱；提交说明同步脱敏。
- 已暴露的数据库口令、管理员口令/摘要和固定 JWT 密钥从历史删除或替换为不可登录的初始化标记。实际运行环境仍必须轮换旧凭据。
- 保留界面所需图片、视频、字体与导出模板；逐个复核内容和元数据，并按路径及 SHA-256 精确批准。第三方字体版权声明保留。
- QA 输出目录加入忽略清单；本地提交/推送检查与 GitHub Actions 检查所有可达历史。

## 验证与使用

运行 `python scripts/privacy_guard.py history` 和 `python -m unittest discover -s scripts/tests`。后端测试前按 README 复制 application-example.yml 为被忽略的 application.yml；不使用生产配置。前端运行 `npm ci`、`npm run build`，部署模板运行 `node scripts/check-deployment-config.mjs`。

扫描通过表示已覆盖的规则及已复核素材未发现遗留项，不构成所有未知个人信息均不存在的保证。

远端替换需要使用指定旧 main 提交的 force-with-lease。若远端期间有新提交，必须停止并重新合并清理。完成后重新克隆并安装 hooks，旧克隆不要继续推送或合并回新历史。备份及包含原值的审计材料仅供本地回退，不能重新上传。

历史重写不能撤回已下载的副本、fork 或 GitHub 缓存；需要时联系 GitHub Support 清理旧引用/缓存。历史被重写的提交签名无法保留有效性。
