# RingerModeTile 1.0

PixelOS / Android 15 独立 LSPosed 模块。快速设置中增加铃声模式磁贴：正常 → 振动 → 静音 → 正常；长按请求系统原生音量浮窗，不调高或调低音量。无桌面图标、无障碍、额外 root 授权、常驻轮询或前台服务。仅在 SystemUI 中 Hook 本模块自己的 CustomTile；普通磁贴不受影响。仅在磁贴 listening 时接收系统模式广播。

## 使用
1. 安装签名 APK，LSPosed 启用 RingerModeTile，勾选系统界面（com.android.systemui）。
2. 重启手机。打开快速设置编辑页，将 Ringer mode / 铃声模式 拖入磁贴列表。
3. 单击循环，长按呼出音量面板；在系统音量面板切换铃声模式后重新检查磁贴。
4. 静音为灰色，正常和振动为激活颜色。中英日文按系统语言显示。不带振动器的设备跳过振动。

## 构建及签名
GitHub Actions 自动构建。为避免每次 CI debug 证书变化，不发布临时 debug 签名 APK。设置仓库 Actions secrets：
- RINGER_SIGNING_KEYSTORE_B64：私有 PKCS#12 密钥库的 Base64 文本，alias 为 ringermodetile。
- RINGER_SIGNING_PASSWORD：密钥库及 alias 密码。

设置后手动运行 Build RingerModeTile APK，下载 RingerModeTile-signed-APK。保留并私下备份同一私钥供所有版本使用。缺少 secrets 时只上传未签名 APK 及 apksigner.jar，可用自己的固定私钥签名。切勿把私钥提交到公开仓库。

## 诊断
在 LSPosed 日志中搜索 [RingerModeTile]：
```
su -c 'grep -hF "[RingerModeTile]" /data/adb/lspd/log/verbose_*.log 2>/dev/null | tail -n 40'
```
应出现 Installed hooks、Mode ... 或 Native volume panel requested。若 Hook 失败，普通磁贴不受影响，模块磁贴点击不执行切换。禁用模块并重启可撤回 Hook。

实现基于 AOSP CustomTile、QSTileImpl 与 AudioManager 内部铃声 API。实际 PixelOS SystemUI 私有方法可能因 ROM 编译优化而异；首次版本需要真机验证，尤其是长按浮窗和勿扰模式下的行为。铃声静音不代表媒体或闹钟全部静音。不主动修改勿扰模式策略。

参考：
- https://android.googlesource.com/platform/frameworks/base/+/6c2f890bd761b5531d9c928d935bb6f5fc38343f/packages/SystemUI/src/com/android/systemui/qs/external/CustomTile.java
- https://android.googlesource.com/platform/frameworks/base/+/a41d75968d7d5b0468cb01276d108b73823d7cb3/packages/SystemUI/src/com/android/systemui/qs/tileimpl/QSTileImpl.java
