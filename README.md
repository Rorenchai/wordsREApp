# 单词记忆卡（WordsREApp · Android）

一款温暖奶油系风格的 Android 背单词应用：**Kotlin + Jetpack Compose + Room**，内置 **PETS（3,664 词）/ CET-4（4,544 词）/ CET-6（3,991 词）** 三套词库，支持自定义词库导入、翻转卡片学习、专项复习计划、单词总览、每日打卡与内置离线女声。

> **当前版本：v1.2.1**（`versionCode 6`，2026-11-18）
>
> - GitHub Release：[v1.2.1 体验更新](https://github.com/Rorenchai/wordsREApp/releases/tag/v1.2.1)
> - 发布 APK：`app-release.apk`（约 58 MB，已签名）
> - 开发分支：`main` / `phase2`（同步）
> - 项目形态：**仅 Android**。原 Web 单页线已弃用，远程 `web` 分支已删除。

## 功能

### 学习闭环

| 能力 | 说明 |
|---|---|
| 翻转卡片 | 正面：单词 + 音标 + 发音 + 状态；背面：词性释义、双语例句、常用搭配；点击卡片 3D 翻转 |
| 单行单词 | 学习卡片与计划卡片中的单词强制单行，超长词按宽度自动缩小字号，保证完整显示 |
| 三档状态 | 未学习 / 学习中 / 已掌握；翻到背面后标记才计为有效学习 |
| 筛选 | 全部 / 未学习 / 学习中 / 已掌握 |
| 本轮去重 | 本轮已浏览词持久化到 `round_seen`，按词库 + 筛选隔离，重启后恢复 |
| 今日目标 | 按「不同单词」去重统计；目标可在设置中调整（5–200，预设 10/20/30/50） |
| 首页布局 | 顶部固定「备考计划 + 今日目标」紧凑栏；下方为筛选、单词卡片、操作按钮与小游戏区 |
| 自动朗读 | 听音辨词 / 单词拼写 / 单词消消乐进入下一词或配对成功时自动发音；设置 → 发音 →「自动朗读」可关闭 |

### 词库

| 能力 | 说明 |
|---|---|
| 内置词库 | **PETS 3,664 / CET-4 4,544 / CET-6 3,991**，首次启动自动入库 |
| 切换词库 | 设置 →「词库」；各词库学习数据相互独立 |
| 自定义词库 | 设置 →「自定义词库」导入 JSON / CSV / TXT，自动解析单词与释义，作为独立词库参与学习、计划、收藏和统计 |
| 单词总览 | 底部「总览」页：全部 / 未掌握 / 已掌握筛选，按单词或释义搜索，分页浏览，点击查看详情并发音 |
| 词典详情 | 音标、释义、双语例句、常用搭配；支持播放发音 |

### 专项复习计划

| 能力 | 说明 |
|---|---|
| 创建计划 | 选择词库、备考时长（7–365 天滑动条）、每日新词、每日复习上限、计划名称；实时预览考试日期与建议词量 |
| 今日任务 | 计划概览展示目标词数、已学/已掌握、今日新词与到期复习 |
| 复习节奏 | 新词学习后次日首次复习；复习按 1 / 2 / 4 / 7 / 15 天推进，答对推进、答错次日重试，越过考试日期自动收口 |
| 计划日历 | 月历展示每日新词/复习任务与完成情况，支持切月、点选日期、考试日标识 |
| 游戏联动 | 学习页小游戏可从计划词池抽题（到期复习优先），答对到期词推进复习 |

### 记录与激励

| 能力 | 说明 |
|---|---|
| 学习记录 | 词库总数 / 已学 / 已掌握 / 待学；学习进度、掌握率、最近 7 天柱状图、答题统计、学习时长 |
| 每日打卡 | 当天有学习记录即可打卡；奖励弹窗包含彩带、连续天数与学习能量；同一天只可领取一次 |
| 打卡日历 | 当月打卡日历，已打卡日期高亮 |
| 错题本 | 自动收集最近答错的词，支持错题复习，答对后移出 |
| 词汇量评测 | 20 题抽样四选一 + 猜测校正，结果持久化并展示在记录页 |
| 收藏夹 | 卡片星标收藏 / 取消；跨词库统一列表，支持按单词 / 释义 / 音标搜索、详情与发音 |

### 小游戏

游戏入口位于**学习页单词卡片下方**，不再占用底部页签：

- 看词选译（四选一）
- 听音辨词（自动发音 + 四选一）
- 单词拼写（按释义输入）
- 单词消消乐（词义配对）

答题结果计入记录页，并可选择「优先练计划词」。

### 设置与个性化

| 能力 | 说明 |
|---|---|
| 用户资料 | 自定义昵称（最多 20 字）、相册头像（持久化 URI）、恢复默认头像 |
| 发音 | 内置 Piper 神经网络女声（LJSpeech，22.05kHz），支持美式 / 英式语速，完全离线 |
| 外观 | 跟随系统 / 浅色 / 深色；5 套全局主题色预设（奶油金 / 蜜桃粉 / 鼠尾草 / 海盐蓝 / 薰衣草），深色模式同步映射 |
| 学习提醒 | 19:00 / 20:00 / 21:00 / 22:00，通知权限 + WorkManager 每日调度，可发送测试提醒 |
| 数据备份 | JSON 导出 / 覆盖式导入；按「单词 + 词库」还原，可重复导入 |
| 重置进度 | 二次确认后仅重置当前词库的学习状态与记录 |
| 关于与许可 | 词库、字体、Piper / eSpeak 许可全文 |

### 视觉与性能

- **Creamy Kawaii 主题**：奶油金 `#D9BB59`、杏仁白 `#FFF8E8`、蜜桃粉 `#F8D9CC`、焦糖棕 `#B99578`、可可深棕 `#57483B`、燕麦米色 `#EADCBF`；
- 深色模式：`#292522` 背景、`#39322D` 卡片、奶油金主色；
- 学习状态：未学习燕麦灰、学习中奶油金、已掌握鼠尾草绿、错题珊瑚红；
- 应用图标：金发 / 单词书 / 金色书签 / 星星插画，含自适应图标与全密度 mipmap；
- 小游戏图标：书本翻译 / 耳机 / 键盘 / 拼图四枚奶油极简线性图标，深浅色自适应；
- 卡片动效：换词时卡片滑入、缩放、微旋转，背后保留两层错位卡片形成卡堆层次；
- 奖励反馈：已掌握、小游戏每连对 5 题、消消乐过关时展示星星 + 彩带轻量动效；
- 内置 TTS 在**首次点击发音时**才初始化，启动阶段不加载模型；`tokens.txt` 固定 LF，避免原生解析闪退。

## 技术栈

| 项 | 选型 |
|---|---|
| 语言 | Kotlin 2.2.10（AGP 9 内置 Kotlin） |
| UI | Jetpack Compose（BOM 2025.10.01 + Material3）、Navigation Compose、单 Activity + 五页签 |
| 数据库 | Room 2.7.2 + KSP（schema v6） |
| 异步 | Kotlin 协程 + StateFlow / ViewModel |
| 设置存储 | DataStore Preferences |
| 后台任务 | WorkManager（学习提醒） |
| JSON | Gson 2.10.1 |
| 依赖注入 | 手动 `AppContainer` |
| 构建 | AGP 9.1.1 + Gradle 9.3.1 + JDK 21；minSdk 26 / targetSdk 36 |
| 测试 | JVM 单元测试 31 条（日期/streak、计划调度、自定义词库解析等） |

底部页签：**学习 / 收藏 / 记录 / 总览 / 设置**。

## 目录结构

```text
Study/app/src/main/java/com/example/study/
├── MainActivity.kt / AppContainer.kt
├── data/db/          Room 实体、DAO、数据库与迁移（v1–v6）
├── data/seed/        内置词库解析与首次入库、旧词库清理
├── data/repo/        词库 / 学习记录 / 收藏 / 设置 / 计划 / TTS 仓库
├── data/plan/        计划调度与仓库
├── ui/study/         首页学习卡片、今日目标、小游戏入口
├── ui/plan/          计划创建、概览、日历、计划学习/复习
├── ui/records/       学习记录、打卡、错题本入口、词汇量评测入口
├── ui/overview/      单词总览（筛选/搜索/详情）
├── ui/favorites/     收藏夹与搜索
├── ui/games/         四个小游戏与学习页游戏区
├── ui/settings/      设置、词库、资料、主题、备份、提醒
├── ui/theme/         Creamy Kawaii 色板、Light/Dark 主题、字体与形状
└── ui/components/    翻转卡片、按钮、状态徽章、图表等通用组件
```

内置词库位于 `app/src/main/assets/wordbank_*.json`；Piper 语音资源位于 `app/src/main/assets/tts/piper/`；图标源文件位于 `design/app_icon_source.png`。

## 数据库（Room v6）

| 表 | 说明 |
|---|---|
| `words` | 单词主表，唯一索引 `(word, library)` |
| `word_progress` | 每个单词的学习状态（未学习 / 学习中 / 已掌握） |
| `study_events` | 每次有效学习事件，含答题对错 |
| `favorites` | 收藏关系 |
| `round_seen` | 本轮已浏览词（词库 + 筛选隔离） |
| `study_duration` | 按日期 + 词库累计学习时长 |
| `plans` / `plan_words` | 专项复习计划与计划内单词阶段 |

数据库版本历史：v1 基础表 → v2 收藏 → v3 答题对错 → v4 本轮进度 → v5 学习时长 → v6 计划表；迁移保持既有数据。

## 构建与运行

```bash
# Debug APK
./gradlew :app:assembleDebug          # Windows: gradlew.bat :app:assembleDebug

# 单元测试
./gradlew :app:testDebugUnitTest

# 签名 Release APK
./gradlew :app:assembleRelease

# 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

环境说明：

1. **AGP 9 内置 Kotlin**：不要应用 `org.jetbrains.kotlin.android`；Compose 使用 `org.jetbrains.kotlin.plugin.compose`。
2. **KSP**：`gradle.properties` 保留 `android.disallowKotlinSourceSets=false`，否则 Room KSP 无法注册生成源。
3. **ABI**：release 内置 sherpa-onnx 语音原生库为 arm64-v8a；debug 额外包含 x86_64 供模拟器调试。

## 发布信息

| 项 | 值 |
|---|---|
| 版本 | `1.2.1`（versionCode 6） |
| Release | <https://github.com/Rorenchai/wordsREApp/releases/tag/v1.2.1> |
| APK | `app-release.apk`（61,102,512 字节 / 58.27 MB） |
| SHA-256 | `C6E74A7D6E943EE30B79E15E89E08336BB86CF96EE425542F9800A85134E0EC1` |
| 签名 | release keystore（`keystore/wordcard-release.jks`，不入库） |

## 词库与许可

- 词库：整理自 [KyleBing/english-vocabulary](https://github.com/KyleBing/english-vocabulary)，**BSD 3-Clause License**；
- 字体：**Nunito**，**SIL Open Font License 1.1**；
- 离线语音：**sherpa-onnx**（Apache-2.0）+ **Piper LJSpeech** 女声（数据集公有领域）+ **eSpeak NG** 音素数据（GPL）；
- 许可全文随 APK 打包在 `assets/licenses/`，可在应用「设置 → 关于与许可」查看。

## 版本历史

| 版本 | 主要变化 |
|---|---|
| v1.0.0 | Kotlin + Compose 重构完成；CET-4 / CET-6 词库、翻转卡片、每日目标、学习记录、收藏、四游戏、计划、备份、提醒、深色模式 |
| v1.1.0 | 计划时长滑动条、自定义词库、PETS 词库、单行单词、单词总览、每日打卡、设置页资料/头像、奶油金主题、内置 Piper 女声 |
| v1.1.1 | 修复 TTS 启动闪退（tokens LF + 延迟初始化）；清理计划说明中的专业术语文案；更新应用图标与主题 |
| v1.2.1 | 三小游戏下一词自动朗读（统一封装 + 开关）、奶油极简游戏图标、5 套全局主题色、卡片堆叠切换动效、连对/掌握奖励反馈 |

> 历史阶段说明：第二至第四期（小游戏 / 词典 / 收藏 / 答题 / 学习时长 / 错题本 / 词汇量 / 备份 / 提醒 / 深色模式 / 专项计划）均已完成；第五期 Web 线与 Android **不再同步维护**，Web 已弃用。
