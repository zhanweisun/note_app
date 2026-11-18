# NoteDraft (app.notedraft.noteall)

从 `NoteDraft_v1.4.1_Premium.apk` 反编译后，按原始业务逻辑正向重建的 Android Studio 工程。

## 环境

| 项 | 值 |
|---|---|
| JDK | 21 (`/home/jack/Software/jdk21`) |
| Android SDK | `/home/jack/Software/AndroidSDK`（`local.properties` 已配置） |
| Gradle | 8.13（wrapper 已生成） |
| AGP / Kotlin | 8.13.0 / 2.2.10（Kotlin 2.x Compose 编译器插件） |
| compileSdk / min / target | 36 / 24 / 36 |
| applicationId | `app.notedraft.noteall`（debug 带 `.debug` 后缀） |

```bash
export JAVA_HOME=/home/jack/Software/jdk21
export PATH=$JAVA_HOME/bin:$PATH
export ANDROID_HOME=/home/jack/Software/AndroidSDK
./gradlew :app:assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

## 反编译产物位置

```
/home/jack/Dev/NoteDraft_work/
├── apktool_out/    # apktool 2.11.1 解出的资源、Manifest、smali
└── jadx_out/       # jadx 1.5.6 解出的 Java 源码
```

## 还原依据（关键发现）

原始 APK 经 R8 full-mode 混淆，业务类被重命名为 `p000/a00.java` 这类名字，
因此无法直接拷贝代码；但下列信息完整保留，据此正向重建：

- **数据库 schema**：`p000/zw0.java`（SQLiteOpenHelper，version 4）中的 DDL 与 v1→v4 迁移语句逐字还原。
- **数据模型字段名**：kotlinx-serialization 的描述符字符串保留了真实字段名
  （`id / folderId / type / title / body / isPinned / createdAt ...`），据此重建全部 13 种笔记类型的模型。
- **加密参数**：PBKDF2WithHmacSHA1 / 100000 次 / 256 bit，AES-GCM 128 bit tag，
  `salt(16) || iv(12) || ciphertext` 的 Base64 负载，`encryption_salt` 为 `base64(salt):base64(key)`。
- **多笔记本**：`default / team / business` → `notedraft.db / notebook_team.db / notebook_business.db`。
- **依赖版本**：取自 APK 内 `META-INF/androidx.*.version`（Compose 1.10.6、Material3 1.4.0 等）。
- **UI 结构**：由 `strings.xml`（1663 条，含 103 种语言）与 Manifest 推导得出；
  原始界面为高度 Kotlin 化的 Compose 代码，此处按同等交互语义重写。

## 资源

资源直接取自 APK，保持原文件名：

- `res/drawable*`：`nd_*.xml`（13 种笔记类型图标）、`ph_*`（Phosphor 图标集）、`ic_*`
- `res/mipmap-*`：启动图标（含 adaptive / monochrome）
- `res/values*/strings.xml`：应用自有字符串 + 全部语言
- `res/values/colors.xml`、`themes.xml`（`Theme.OfflineNotepadPro` 继承链与原始一致）
- `res/xml`：shortcuts、backup_rules、data_extraction_rules、file_paths、network_security_config

## 工程结构

```
app/src/main/java/app/notedraft/noteall/
├── MainActivity.kt                  # 唯一入口
├── data/
│   ├── Models.kt                    # 13 种笔记 + 文件夹/修订/设置/备份
│   ├── Crypto.kt                    # PBKDF2 + AES-GCM
│   ├── NoteDatabase.kt              # schema 与迁移
│   └── DataRepository.kt            # 数据访问、搜索、回收站、修订、备份
└── ui/
    ├── NoteDraftApp.kt              # 路由（主页 / 编辑器 / 设置）
    ├── MainScreen.kt                # 抽屉导航、列表、多选、移动、回收站
    ├── CreateMenuSheet.kt           # 新建菜单
    ├── MainViewModel.kt             # 状态与动作
    ├── theme/Theme.kt               # 主题 / 字号 / 强调色
    ├── note/NoteEditorScreen.kt     # 纯文本 & 清单编辑（自动保存 + 修订）
    ├── editors/StructuredEditors.kt # 看板/计算/计数器/习惯/链接/名单/密码/购物/片段/笔记本/日程
    └── settings/SettingsScreen.kt   # 设置页
```

## 多语言

工程内置 **86 个语言**目录，中文来自原始 APK 且已完整覆盖：

| 目录 | 数量 | 说明 |
|---|---|---|
| `values-zh-rCN` | 1535 条 | 简体中文（完整） |
| `values-zh-rTW` | 1535 条 | 繁体中文（完整） |
| `values-zh-rHK` | 44 条 | 香港繁体（仅差异项，其余回退到 `zh-rTW`） |

仅 `app_name_short`（品牌名）与 `shortcut_target_package`（包名）不翻译，属预期。

额外补充了原始资源中缺失的一个键：

```xml
<!-- values/strings.xml        --> <string name="settings_theme_system">System</string>
<!-- values-zh-rCN/strings.xml --> <string name="settings_theme_system">跟随系统</string>
<!-- values-zh-rTW/strings.xml --> <string name="settings_theme_system">跟隨系統</string>
```

主题（System / Light / Dark）与结构化编辑器的占位文字（如 URL）已改用 `stringResource`，
不再硬编码英文。

## 已验证

在 **Redmi K30 5G（Android 11，系统语言中文）** 实机测试：

- 主页、抽屉导航、新建菜单、设置页、笔记编辑器、看板编辑器均为中文
- 新建 / 编辑 / 自动保存 / 重启后数据回显正常

## 修复记录

| 问题 | 修复 |
|---|---|
| **release 构建失败**：`lintVitalAnalyzeRelease` 抛 `IncompatibleClassChangeError: KaCallableMemberCall` | AGP 8.7.3 自带的 lint 与 lifecycle 2.9.4 的 detector 字节码不兼容。升级到 **AGP 8.13.0 + Gradle 8.13**（其 lint 基于新版 Kotlin Analysis API） |
| 看板、计数器等结构化笔记创建后从列表消失，无法再打开 | `MainViewModel.visibleItems` 的 NOTES 分区原先只放行 NOTE/LIST，改为放行全部类型（清单单独归到「任务」） |
| 打开结构化笔记时进度圈不消失 | `DataRepository.getNote` 改为直接按 id 查库，不再依赖内存缓存 |
| 设置页主题选项硬编码英文、中文被挤成两行 | 改用字符串资源 + 全宽分段按钮 |
| 新建菜单偶尔未进入编辑器 | BottomSheet 改为 `hide()` 完成后在同一协程内执行动作 |
| `themes.xml` 中 API 27/29 属性在 minSdk 24 下报 NewApi | 拆分到 `values-v27/` 与 `values-v29/` |

### 关于 lint 配置

`lintDebug` 原本会报 1541 个错误，绝大多数是资源噪音（APK 只带部分语言翻译、Compose 内联的
`PrivateResource`、`UnusedResources`），与正确性无关。已在 `app/build.gradle.kts` 中关闭这三项，
其余检查保持开启；`lintVital`（release 必过的致命项检查）全程未放宽。

### 依赖版本统一

Compose BOM `2025.10.01` 会把 lifecycle 提升到 2.9.4，因此显式依赖也已统一为 `2.9.4`，
避免声明版本与实际解析版本不一致。