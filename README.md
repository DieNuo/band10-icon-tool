# 环10改图标工具

小米手环 10（Xiaomi Smart Band 10）图标自定义工具：把手机相册里的任意图片，变成手环上的应用列表图标、控制中心图标和 NFC 卡包封面，生成可安装的 `.face` 表盘文件。兼容 **Android 8.0 及以上**。

基于 [KernelSU-Style-UI-Kit](https://github.com/chenaizhang/KernelSU-Style-UI-Kit) 的 Miuix 米柚风格界面构建。

**[⬇️ 最新版下载（Releases）](https://github.com/DieNuo/band10-icon-tool/releases/latest)** · 当前版本 **v2.2.7**

## 功能一览

- 应用列表图标自定义（**38 个**）
- 控制中心图标自定义（勿扰开/关、找手机、手电筒、设置，共 **5 个**），自动裁剪为圆形
- NFC 卡包封面自定义（**21 张**，自动切圆角，和手环原生卡片外观一致）
- 未更换的图标自动保留原版样式，可随时还原
- 支持 JPG / PNG / WebP / TGA 输入，自动居中裁剪到目标尺寸
- 换装表盘手环端内置 **建副本 / 删副本 / 体检状态**，无需单独的副本工具表盘
- 长按表盘弹出表盘选择菜单，像普通表盘一样切换
- 手环端「关于」页含开发者真实头像；「更多」页可查看存储占用并一键清理缓存
- 深浅双主题 + 动态取色，Miuix / Material 两种界面风格

## 使用方法

1. 从 [Releases](https://github.com/DieNuo/band10-icon-tool/releases/latest) 下载 APK 附件，安装到手机上
2. 打开应用，切换到想要更换的分类，添加好图片后点「生成 .face」
3. 把生成到下载目录的 `.face` 表盘文件，用 AstroBox、OronBox 等工具安装到手环上（安装过程中尽量保持手环亮屏！）
4. 安装好表盘之后请先点击 **「建副本」** 按钮（建副本时间较长，请耐心等待~），显示 **「副本就绪」** 后，即可点击 **「应用封面」** 按钮，并显示 **「写入...条」** 则代表修改成功，左滑控制面板设置里切换表盘即可生效~

## 注意事项

- 生成的表盘约 3MB（实际可能更大）
- 在建副本后会在 `/data/fpatch` 目录产生一个 **42MB 左右的副本**，建一次即可，后续都不用再建
- 如需卸载，请点击表盘里的 **「删副本」** 按钮删除副本。v2.2.2 起删副本会自动解除挂载占用并真正释放空间；如果之前点过「应用封面」，删完如存储数字未变化，重启手环后再看
- 想完全恢复原厂：点表盘里的「还原原厂」按钮，或还原后删除副本

## 已测试

- 小米手环 10（NFC 版）固件 3.1.28 可用（理论上小米手环 10 全系固件均适用）

**环 10 标准版不建议使用！除非你的手环内存很充裕！**

## 从源码构建

```bash
git clone https://github.com/DieNuo/band10-icon-tool.git
cd band10-icon-tool
./gradlew :app:assembleRelease
```

要求 JDK 21、Android SDK 37。若 `keystore/band10.jks` 不存在，release 构建会自动使用 debug 签名；正式发布请用 `keytool` 自行生成密钥。

## 目录说明

| 路径 | 内容 |
|---|---|
| `app/src/main/assets/tool.face` | 应用图标 + 卡包封面模板（含原厂备份与手环端换装脚本） |
| `app/src/main/assets/cc.face` | 控制中心图标模板（含原厂备份与手环端换装脚本） |
| `app/src/main/assets/mapping.json` / `cc_mapping.json` | 模板条目标注 |
| `app/src/main/java/.../ui/tool/` | 核心逻辑：模板解析、TGA 解码、索引色/真彩编码、圆角圆形裁剪 |
| `scripts/watch_ui/` | 手环端换装脚本源码（LVGL Lua） |
| `scripts/patch_watch_ui.py` | 把手环端脚本等长写回模板 |

## 免责声明

本人是深度米环 10 NFC 用户，该表盘以及产生的副本不会触发内存问题（重启掉字库等），请放心使用。**操作有风险！因操作不当造成的任何后果，本人概不负责！！!**

## 许可证

[GPL-3.0](LICENSE)，继承自上游项目 [KernelSU-Style-UI-Kit](https://github.com/chenaizhang/KernelSU-Style-UI-Kit)。
