# Living Room TV · 客厅桌面

给安卓电视换一个干净的首页。用遥控器左右选择应用，按确定打开。壁纸已内置，首页会从电视上已安装的应用里选取最多六个入口。

**TCL / 雷鸟提示「无法安装」，先试[普通应用版（约 2 MB）](https://github.com/enzoyoyo/living-room-tv-home/releases/latest/download/LivingRoomTV-App.apk)。** 安装后在应用列表里打开「客厅应用」。界面与桌面版相同，按 Home 键仍使用系统选定的桌面。

想把它设为主页，且电视允许更换默认桌面，可以选[桌面版 LivingRoomTV.apk](https://github.com/enzoyoyo/living-room-tv-home/releases/latest/download/LivingRoomTV.apk)。原 1.0 用户可直接更新桌面版。两个版本任选一个即可。

## 用 U 盘安装

两个安装包的最低系统版本均为 Android 5.0（API 21），电视或盒子还需允许本地安装 APK。无需电脑脚本、AI 账号或开启电视调试。

1. 下载上方的 APK，原样复制到 U 盘。
2. 把 U 盘插到电视上，进入「文件管理」或「媒体中心」，选中刚下载的 APK。
3. 选择「安装」。如果电视询问是否允许此文件管理器安装应用，按屏幕提示授权。装好后点「打开」，以后也可以在应用列表里找到「客厅应用」或 Living Room TV。

侧栏的「应用」可以看全部软件。「设置 → 打开原厂桌面」可以临时回到原来的界面。按主页键会打开系统选定的默认桌面。只有提供默认桌面选项的机型才能在系统设置中更换。想移除时，可在电视的应用管理中卸载 Living Room TV。

## 电视仍提示无法安装

- 提示「不允许此来源」时，按电视自己的提示为正在使用的文件管理器允许安装。若固件只允许应用商店安装，本项目的 U 盘安装方式不适用。
- 提示「解析失败」时，先在电视的「关于」里确认 Android 版本，再从本页重新下载完整 APK 并复制到 U 盘。
- 提示「与现有软件包冲突」时，先核对已装版本的来源和签名。桌面版和普通应用版使用独立包名，可共存，无需卸载原厂桌面。
- 如果只有「无法安装」四个字，无法据此判定具体原因。电视完整型号、系统版本和报错原文有助于定位。TCL Roku TV 等非 Android 系统不能运行 Android APK。

USB 安装权限取决于电视型号与固件设置。

## 隐私与兼容

桌面不申请联网权限。应用列表来自本机，焦点位置与动画偏好也只保存在本机。电视开放哪些输入源、是否允许安装第三方应用，由电视系统决定。投屏入口需要电视已安装 AirScreen。

## 从源码构建

在 macOS 或 Linux 上准备 Python 3、JDK 17、Android SDK Platform 34 和 Build Tools 34.0.0。设置 ANDROID_HOME 和 JAVA_HOME。准备自己的发布签名，密钥存放在仓库外。以下为 Bash 命令，密码在本机交互输入。

```bash
export TV_KEYSTORE="$HOME/living-room-release.jks"
# 首次构建时生成一次。密钥密码与密钥库密码使用相同值。
keytool -genkeypair -keystore "$TV_KEYSTORE" -alias tvhome -keyalg RSA -keysize 3072 -validity 10950
read -rs -p 'Keystore password: ' TV_STOREPASS
export TV_STOREPASS
python3 build.py
unset TV_STOREPASS
```

两个签名安装包均输出到 `dist/`。后续更新使用同一把签名密钥。项目采用 MIT 许可证。

作者：Enzo · [博客](https://blog.enzocreative.me)。
