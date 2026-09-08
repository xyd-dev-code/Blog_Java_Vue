# 上传前隐私检查

本项目使用本地 Git 钩子阻止问题进入提交或推送，再由 GitHub Actions 复查。Python 3.11+ 和 Git 是必需依赖；检查器本身不联网、不登录服务、不打印命中的敏感值。

## 每台电脑安装一次

在仓库根目录执行：

```sh
python scripts/privacy_guard.py install
python scripts/privacy_guard.py identity
```

安装只修改当前克隆的 `core.hooksPath`，不会修改全局 Git 设置。有其他 hooks 或自定义 hooksPath 时会拒绝覆盖，应先人工整合。

配置允许公开的昵称及 GitHub 提供的隐私邮箱（从 GitHub 邮箱设置复制自己的地址，不要直接使用下面的示例）：

```sh
python scripts/privacy_guard.py install --name "Demo Developer" --email "12345+demo@users.noreply.github.com"
```

安装后正常使用 `git commit`、`git push`，包括调用 Git 的 IDE。新的克隆不会自动继承本地 Git 配置，必须重新安装。不要使用跳过 hooks 的选项；本地 hooks 是防误操作工具，不能约束有意绕过的人。

## 自动检查什么

| 阶段 | 范围与行为 |
|---|---|
| pre-commit / pre-merge-commit / pre-applypatch | 从 Git 暂存区读取新增、修改、重命名文件的完整对象，检查待提交身份；不会拿工作区里尚未暂存的修正冒充安全版本 |
| commit-msg / applypatch-msg | 检查提交说明和身份，防止脱敏说明再次复述旧秘密 |
| pre-push | 读取 Git 传来的所有待推送分支/标签对象，扫描每个 tip 的全部祖先、文件版本、作者、提交者、消息和附注标签；新分支、强制更新、多 ref 推送同样检查 |
| GitHub Actions | 完整 checkout，运行防护回归测试，再检查当前文件及全部已获取历史；它是上传后检查，不能代替本地拦截 |

推送检查**不把远端已经存在的历史当作安全基线**。早期提交泄露、后来删掉，仍会失败。纯删除 ref 的推送不新增对象，允许执行。无法读取对象、缺 Python/策略文件、浅克隆、冲突索引、超限文件等情况均阻断，不静默跳过。

规则覆盖常见密钥/token/私钥前缀、BCrypt 字面量、带凭据 URL、短密码与不安全环境变量默认值、非示例邮箱、手机号、IPv4、常见括号/引号包裹的 IPv6、本机用户路径、项目已知个人标识及敏感文件名。测试目录没有整体豁免；明确的 IP 分类测试值仅按路径与字面值豁免。

已知标识在 `.privacy-policy.json` 中保存规范化后的 SHA-256 指纹，避免把需要删除的个人资料再写进规则源码。指纹只是匹配手段，不能让低熵个人信息获得密码学意义上的保密；也不能识别所有变体、未知姓名或嵌入长句中的名字。

## 图片、Word 和其他二进制

新增或改变的图片在提交前尝试本地 OCR：Windows 使用系统 OCR；安装了 Tesseract 时使用 `eng+chi_sim`。其他平台可自行安装 Tesseract 与相应语言包。缺少 OCR、语言包或识别失败时不会假定安全。

**所有图片和其他二进制都需要人工审核记录，即使 OCR 未命中。** 对 PDF、字体、视频、压缩包等同样要求审核；不会把无法解析的内容当作已检查。DOCX/XLSX/PPTX 会额外解包扫描 XML、关系地址、作者与自定义属性，有风险的元数据即便存在审核记录也会拦截。LFS 指针、符号链接和子模块由于无法确认实际载荷，直接拦截。

辅助检查一个资源（不会自动批准）：

```sh
python scripts/privacy_guard.py inspect-asset blog-web/public/images/example.png
```

该命令只打印问题位置和文件 SHA-256，不输出 OCR 文本。审核图片像素、个人头像、二维码、EXIF/GPS、文档作者/自定义属性、嵌入文件等；删除或脱敏后，在 `.privacy-policy.json` 的 `reviewed_assets` 中添加精确记录，例如：

```json
{
  "path": "blog-web/public/images/example.png",
  "sha256": "填写检查器输出的64位十六进制摘要",
  "reason": "已查看图片及元数据，仅包含通用装饰，无个人资料",
  "reviewer": "公开审核昵称"
}
```

上例的摘要是说明文字，不能直接复制使用。将资源与策略记录一起暂存。摘要必须与实际文件逐字节匹配，文件更新后旧批准自动失效。不支持目录通配批准，不要批量批准旧截图来绕过历史问题。

正常推送/CI 对已明确人工批准的图片按摘要验证，不重复 OCR 全部历史图片；未批准的图片始终阻断。人工审核不能通过字段记录本身得到证明，仍需要认真查看资源。生成或清洗元数据之后，应重新计算摘要。

## 手动复查

```sh
python scripts/privacy_guard.py worktree
python scripts/privacy_guard.py worktree --tracked-only
python scripts/privacy_guard.py staged
python scripts/privacy_guard.py history
python -m unittest discover -s scripts/tests -p test_privacy_guard.py -v
```

原来的 PowerShell 入口继续可用：

```powershell
pwsh ./scripts/privacy-scan.ps1 -TrackedOnly -History
```

返回码：`0` 通过，`1` 发现待处理内容，`2` 检查或配置失败。输出最多 100 个位置并按规则汇总，不含匹配值。暂存检查和提交消息检查使用暂存区的策略，避免未暂存的豁免误放行；安装、工作区/历史检查使用当前策略文件。

## 本仓库现有历史

本次已在独立备份克隆中清理 45 次历史提交：移除旧私密配置、原始 QA 附件和内部需求文档，替换个人标识、部署地址及历史口令，统一提交身份并清理提交消息。保留的运行时素材逐个复核，并记录精确 SHA-256。详见 `HISTORY-CLEANUP.md`。远端是否已替换，应以 GitHub 的实际提交为准；安装防护本身不执行远端历史替换。曾使用的凭据仍需按 `PRIVACY-MIGRATION.md` 轮换。

GitHub Actions 配置提交并推送后才会生效。本地安装不会自动更改 GitHub 服务端设置；仓库管理员还应核实 Push protection，并在分支保护/规则集中要求 `privacy-guard` 检查。网页上传不运行本地 hooks，因此应使用安装了防护的克隆提交。对规则、hook 或扫描器的修改也需要代码审查；有写权限的人可以主动关闭或修改检查，无法仅靠仓库内脚本实现不可绕过的服务端安全边界。

官方行为参考：[Git hooks](https://git-scm.com/docs/githooks)、[GitHub Push protection](https://docs.github.com/en/code-security/concepts/secret-security/push-protection)。
