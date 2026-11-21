# 环10改图标工具

小米手环 10（Xiaomi Smart Band 10）图标自定义工具：把手机相册里的任意图片，变成手环上的应用列表图标、控制中心图标和 NFC 卡包封面，生成可刷入的 `.face` 表盘文件。兼容 **Android 8.0 及以上**，低版本自动关闭毛玻璃等特效，功能不受影响。

基于 [KernelSU-Style-UI-Kit](https://github.com/chenaizhang/KernelSU-Style-UI-Kit) 的 Miuix 米柚风格界面构建。

## 功能

- **应用列表图标**：38 个可换（小爱同学、支付宝、微信支付、米家等）
- **控制中心图标**：5 个可换（勿扰开/关、找手机、手电筒、设置），自动裁剪为圆形
- **NFC 卡包封面**：21 张可换（车钥匙、门禁卡、银行卡等），自动裁剪圆角，与手环原生卡片外观一致
- 未更换的条目自动保留模板原图，不想用了刷回原厂备份即可
- 支持 JPG / PNG / WebP / TGA 输入，自动居中裁剪到目标尺寸
- 深浅双主题 + 动态取色，Miuix / Material 两种界面风格

## 使用方法

1. 安装 APK（Android 8+，低版本可正常安装使用），打开应用
2. 切换到想要更换的分类，点按方块挑选图片；已选的方块点右上角 × 可移除
3. 点「生成 .face」，保存生成的表盘文件
4. 用 AstroBox / OronBox 等工具把 `.face` 安装到手环（安装过程中尽量保持手环亮屏）

> 应用后可能在手环 `/data/fpatch` 产生约 42MB 的固件副本，属正常现象。

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
| `app/src/main/assets/tool.face` | 应用图标 + 卡包封面模板（含原厂备份） |
| `app/src/main/assets/cc.face` | 控制中心图标模板（含原厂备份） |
| `app/src/main/assets/mapping.json` / `cc_mapping.json` | 模板条目标注 |
| `app/src/main/java/.../ui/tool/` | 核心逻辑：模板解析、TGA 解码、索引色/真彩编码、圆角圆形裁剪 |

## 已测试

- 小米手环 10（NFC 版）固件 3.1.28

## 免责声明

修改手环资源有风险。因操作不当造成的任何后果，本项目概不负责。

## 许可证

[GPL-3.0](LICENSE)，继承自上游项目 [KernelSU-Style-UI-Kit](https://github.com/chenaizhang/KernelSU-Style-UI-Kit)。
