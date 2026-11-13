# 单词记忆卡（WordsREApp · Android）

一款类多邻国风格的 Android 背单词应用：**Kotlin + Jetpack Compose + Room**，内置完整 **CET-4（4,544 词）/ CET-6（3,991 词）** 词库，核心闭环为「翻转卡片学习 → 标记学习状态 → 每日目标 / 学习记录统计 → 词库切换 → 发音」。

> 当前分支：`kotlin-refactor`（基于 `main@66b4c21`），5 个重构里程碑已完成并各自编译通过，**尚未合并 main，等待真机验收**。

## 功能（第一期核心闭环）

| 能力 | 说明 |
|---|---|
| 翻转卡片学习 | 正面：拼写 + 音标 + 发音按钮 + 学习状态徽章；背面：词性释义 + 双语例句 + 常用搭配。点击卡片做 3D 翻转（Compose GraphicsLayer） |
| 三档学习状态 | 未学习 / 学习中 / 已掌握；翻到背面后点「学习中 / 已掌握」才计为一次有效学习 |
| 筛选 chips | 全部 / 未学习 / 学习中 / 已掌握（SQL LEFT JOIN 过滤） |
| 本轮不重复抽词 | ViewModel 内存 Set 记录本轮已抽词；抽满一轮后自动重开一轮 |
| 今日目标 + 连续天数 | 按「不同单词」去重统计今日学习数；streak 按有学习记录的连续自然日计算 |
| 学习记录页 | 词库总数 / 已学 / 已掌握 / 待学四宫格；学习进度与掌握率双进度条；最近 7 天柱状图（Compose Canvas）；累计学习次数 |
| 词库切换 | CET-4 / CET-6 一键切换，两套词库的学习数据相互独立 |
| 每日目标设置 | 10 / 20 / 30 / 50 档位 + 自定义输入（5–200） |
| 发音 | 系统 TextToSpeech，按设置切换美式 US / 英式 UK；引擎不可用时提示 |
| 重置进度 | 二次确认后只重置当前词库的学习状态与记录，词库本身保留 |
| 许可与来源 | 设置页可查看词库 BSD-3 与字体 OFL 许可证全文 |
| 游戏页 | 4 个游戏入口占位，标注「第二期开放」 |

## 技术栈

| 项 | 选型 |
|---|---|
| 语言 | Kotlin 2.2.10（AGP 9 **内置 Kotlin**，不再单独应用 `org.jetbrains.kotlin.android`） |
| UI | Jetpack Compose（BOM 2025.10.01 + Material3）、Navigation Compose、单 Activity + 底部四页签 |
| 数据库 | Room 2.7.2 + **KSP**（suspend / Flow 查询），KSP 与 AGP 9 内置 Kotlin 的兼容开关见下文 |
| 异步 | Kotlin 协程 + StateFlow（ViewModel + `collectAsStateWithLifecycle`） |
| 设置存储 | DataStore Preferences（当前词库 / 每日目标 / 发音口音） |
| JSON | Gson 2.10.1（解析 assets 词库） |
| 依赖注入 | 手动 `AppContainer`（不引入 Hilt） |
| 构建 | AGP 9.1.1 + Gradle 9.3.1 + JDK 21；minSdk 26 / targetSdk 36 / applicationId `com.example.study` |

## 目录结构

```
Study/app/src/main/java/com/example/study/
├── StudyApp.kt / MainActivity.kt / AppContainer.kt     # 入口与手动依赖容器
├── data/db/        WordEntity / WordProgressEntity / StudyEventEntity
│                   WordDao / ProgressDao / StudyEventDao / StudyDatabase
├── data/seed/      WordBankDto / DatabaseSeeder        # 首启解析 assets 词库并事务入库
├── data/repo/      WordRepository / StudyRepository / SettingsRepository / TtsManager
├── data/stats/     StudyStats（streak 纯逻辑，可 JVM 单测）+ LibraryStats
├── ui/theme/       Color / Type（Nunito）/ Shape / Theme
├── ui/components/  DuoButton / ProgressTrack / FlipCard / StatusPill / FilterChips
│                   StatCard / WeekBarChart
├── ui/navigation/  AppNavHost（底部四页签）
├── ui/study/       StudyScreen / StudyViewModel
├── ui/records/     RecordsScreen / RecordsViewModel
├── ui/settings/    SettingsScreen / SettingsViewModel
└── ui/games/       GamesScreen（第二期占位）
```

资源：

- `assets/wordbank_cet4.json`（4,544 词）、`assets/wordbank_cet6.json`（3,991 词）
- `assets/licenses/wordbank-BSD-3.txt`、`assets/licenses/Nunito-OFL.txt`
- `res/font/nunito_regular.ttf / nunito_bold.ttf / nunito_extrabold.ttf`（由可变字体实例化生成）

## 数据库设计（Room, version 1）

| 表 | 字段 | 说明 |
|---|---|---|
| `words` | id, word, phonetic, pos, meaning, example, exampleZh, phrase, phraseZh, library | 词库；唯一索引 (word, library) |
| `word_progress` | wordId(PK), status(1 学习中 / 2 已掌握), firstLearnedAt, lastLearnedAt, studyCount, masteredAt | 只存学过的词 |
| `study_events` | id, date(yyyy-MM-dd), wordId, timestamp | 每次有效学习一行；今日目标按日期+单词去重，累计次数不去重 |

- 数据库文件：`wordcard.db`（新重构 schema 与旧 Java 版 `study.db` 不兼容，旧库直接废弃；保留 `fallbackToDestructiveMigration` 作开发期兜底）。
- 首次启动：`DatabaseSeeder` 在 IO 线程解析两份词库 JSON 并事务批量入库（幂等，已存在则跳过）。
- 统计口径：今日目标 = 当天学习过的不同单词数；累计学习次数 = 有效学习事件总数（复习重复计）；streak = 有学习记录的连续自然日（当天未学不清零，从昨天起算）。

## 构建与运行

```bash
# 编译 Debug APK
./gradlew :app:assembleDebug                # Windows: gradlew.bat :app:assembleDebug

# JVM 单元测试（streak / 日期去重等纯逻辑）
./gradlew :app:testDebugUnitTest

# 安装到设备 / 模拟器
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

开发环境说明：

1. **AGP 9 内置 Kotlin**：不要再应用 `org.jetbrains.kotlin.android` 插件（会报 `extension 'kotlin' already registered`）；Compose 仍需 `org.jetbrains.kotlin.plugin.compose`。
2. **KSP 兼容开关**：AGP 9 内置 Kotlin 默认禁止通过 `kotlin.sourceSets` 注册生成源，`gradle.properties` 中需保留 `android.disallowKotlinSourceSets=false`，否则 Room KSP 无法工作。
3. **国内镜像**：`settings.gradle.kts` 已配置 google / mavenCentral + 阿里云镜像兜底（`plugins.gradle.org` 在部分网络不可达）。

## 词库转换（可复现）

参考 Web 应用的词库数据 → Android assets 的转换脚本：

```bash
python -I WorkBuddy/wordbank/convert_ref_wordbank.py
```

脚本只把参考数据当纯文本解析（不执行任何 JS），校验词数 4,544 / 3,991 与字段完整性后输出 UTF-8 JSON。

## 许可

- 词库：整理自 [KyleBing/english-vocabulary](https://github.com/KyleBing/english-vocabulary)，**BSD 3-Clause License**。
- 字体：**Nunito**，**SIL Open Font License 1.1**。

两份许可证全文随 APK 打包（`assets/licenses/`），并在应用「词库与设置 → 关于与许可」中可查看。

## 第二期队列（不在本期范围）

4 个小游戏（消消乐 / 看词选译 / 听音辨词 / 拼写）、词典搜索、收藏集合、轮次持久化、Web 端追齐。
