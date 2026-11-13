# 单词记忆卡（WordsREApp · Android）

一款类多邻国风格的 Android 背单词应用：**Kotlin + Jetpack Compose + Room**，内置完整 **CET-4（4,544 词）/ CET-6（3,991 词）** 词库，核心闭环为「翻转卡片学习 → 标记学习状态 → 每日目标 / 学习记录统计 → 词库切换 → 发音」。

> 状态：重构已完成并通过真机验收，**已合并 `main`**（`4413cbe`，2026-11-13）。当前开发进入第二期（小游戏 / 词典搜索 / 收藏集合等）。

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
| 词典查询 | 学习页右上角搜索入口：按英文单词 / 中文释义检索完整词库，可切换当前词库 / CET-4 / CET-6 / 全部范围；点击结果查看音标、释义、双语例句、常用搭配并播放发音 |
| 收藏夹 | 卡片（正反面）右上角星标一键收藏 / 取消；「收藏」页签跨词库统一展示，支持详情弹窗与发音 |
| 看词选译 | 「游戏」页签进入四选一答题：随机主词 + 3 个干扰释义，点选即判对错（答错显示正确答案），答对 0.9s / 答错 2.2s 后自动换题；会话统计已答 / 答对 / 正确率，答题结果落库并计入记录页「答题统计」 |
| 听音辨词 | 「游戏」页签进入：自动播放目标单词发音（可点喇叭重听），从 4 个单词中选出正确拼写；判对错与自动换题逻辑同看词选译，结果同样落库计入答题统计 |
| 单词拼写 | 「游戏」页签进入：展示中文释义与字母数，输入正确拼写后提交；不区分大小写，答错显示正确答案；结果同样落库计入答题统计 |
| 单词消消乐 | 「游戏」页签进入：从当前词库随机抽 6 个词，词卡与释义卡打乱成 3×4 网格；点单词再点释义配对成功即消除，配错红色提示后翻回；全部配对完成弹结算，可再来一局 |
| 轮次持久化 | 学习页「本轮已浏览 / 本轮内不重复抽词」持久化到数据库（`round_seen`，按词库 + 筛选隔离）；杀进程、重启后自动恢复本轮进度；重置词库进度时一并清空 |
| 学习时长 + 打卡日历 | 学习页可见期间自动计时（切后台/离开页面时上报），按「日期 + 词库」累计；记录页展示今日/累计学习时长与当月打卡日历（已打卡日期绿色高亮，今天蓝色描边） |
| 错题本 + 错题复习 | 自动收集「最近一次答题答错」的词（含累计答错次数、最近答错日期）；记录页「错题本」入口进入列表，一键开始错题复习；答对后该词自动移出错题本，完成显示本轮复习统计 |
| 词汇量评测 | 记录页入口；从 CET-4 + CET-6 共 8,535 词中随机抽 20 词做四选一，按正确率做 1/4 猜测校正后估算词汇量，结果与日期持久化并展示在记录页卡片 |
| 数据备份 | 设置页「数据备份」：导出学习进度 / 学习记录 / 收藏 / 本轮进度 / 学习时长与设置到 JSON（系统文件选择器）；导入前二次确认，按「单词 + 词库」还原并覆盖旧数据，可重复导入不产生重复记录 |
| 每日学习提醒 | 设置页「学习提醒」：开关 + 19:00/20:00/21:00/22:00 时间档位；Notification 权限申请，WorkManager 每 24 小时调度，点击通知打开学习页；提供「发送测试提醒」 |
| 深色模式 | 设置页「外观」：跟随系统 / 浅色 / 深色；主题色通过 CompositionLocal 切换，全部页面适配，DataStore 持久化，重启后保留 |
| 游戏页 | 4 个游戏入口全部上线：看词选译 / 听音辨词 / 单词拼写 / 单词消消乐 |

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
├── ui/dictionary/  DictionaryScreen / DictionaryViewModel（词典搜索）
├── ui/favorites/   FavoritesScreen / FavoritesViewModel（收藏夹）
├── ui/quiz/        QuizScreen / QuizViewModel（看词选译）
├── ui/listening/   ListeningScreen / ListeningViewModel（听音辨词）
├── ui/spelling/    SpellingScreen / SpellingViewModel（单词拼写）
├── ui/match/       MatchScreen / MatchViewModel（单词消消乐）
└── ui/games/       GamesScreen（游戏入口，4 个模式全部上线）
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
| `study_events` | id, date(yyyy-MM-dd), wordId, timestamp, answerCorrect | 每次有效学习一行；answerCorrect 1/0 = 答题对错，null = 翻卡标记；今日目标按日期+单词去重，累计次数不去重 |
| `favorites` | id, wordId(唯一), createdAt | 收藏夹（v2 新增） |
| `round_seen` | library, filter, wordId, seenAt（联合主键） | 学习页本轮已浏览词（v4 新增，按词库+筛选隔离） |
| `study_duration` | date, library, durationMs（联合主键） | 学习时长（v5 新增，按日期+词库累计） |

- 数据库文件：`wordcard.db`（新重构 schema 与旧 Java 版 `study.db` 不兼容，旧库直接废弃；保留 `fallbackToDestructiveMigration` 作开发期兜底）。
- 版本历史：v1 = 词库 / 学习进度 / 学习事件；v2 新增 `favorites` 收藏表；v3 学习事件新增 `answerCorrect` 答题对错列；v4 新增 `round_seen` 轮次持久化表；**v5 新增 `study_duration` 学习时长表**（`Migration(1,2)` 至 `Migration(4,5)` 增量迁移，既有数据保留）。
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

## 第二期队列（Android 端全部完成，Web 端待追齐）

- ✅ 词典搜索（学习页搜索入口；单词 / 中文释义检索，支持范围切换与详情发音）
- ✅ 收藏集合（卡片星标收藏 / 取消；「收藏」页签跨词库统一管理，支持详情与发音；数据库 v2 迁移）
- ✅ 看词选译（四选一答题、判对错、自动换题、会话统计；答题结果落库 + 记录页正确率；数据库 v3 迁移）
- ✅ 听音辨词（自动播放发音 + 四选一选单词、重听、判对错与自动换题；复用答题统计）
- ✅ 单词拼写（按释义输入拼写、不区分大小写、判对错与自动换题；复用答题统计）
- ✅ 单词消消乐（6 对词义词卡打乱网格、配对消除、错误提示、结算与再来一局）
- ✅ 轮次持久化（学习页本轮进度跨进程恢复，按词库+筛选隔离，数据库 v4 迁移）
- ⬜ Web 端追齐（独立技术线，按双端对齐原则另行排期）

## 第三期队列（Android 端全部完成）

- ✅ 学习时长统计 + 打卡日历（学习页计时、记录页展示今日/累计时长与当月打卡网格；数据库 v5 迁移）
- ✅ 错题本 / 复习模式（自动收集最近答错的词，错题复习答对后自动移出）
- ✅ 词汇量评测（20 题抽样四选一 + 猜测校正估算，结果持久化展示在记录页）
- ✅ 数据导出与备份导入（JSON 导出 / 覆盖式导入，SAF 文件选择，幂等可重复导入）
- ✅ 每日学习提醒（19/20/21/22 点档位、通知权限、WorkManager 每日调度、测试提醒）
- ✅ 深色模式（跟随系统 / 浅色 / 深色，CompositionLocal 主题色全页面适配 + 持久化）
- ⬜ Web 端追齐（独立技术线，按双端对齐原则另行排期）

## 第四期队列（进行中：艾宾浩斯专项计划）

设计依据：复习点采用 [新东方艾宾浩斯背词法](https://mtoutiao.xdf.cn/kaoyan/201703/10625559.html) 的 5 分钟 / 30 分钟 / 12 小时 / 1 天 / 2 天 / 4 天 / 7 天 / 15 天（应用中长期复习取 **1/2/4/7/15 天**，短期巩固以「当天再练」体现）；每日新词量与复习比例参考 [四六级备考安排](https://cet4-6.xdf.cn/202610/15436210.html)：新词 40–60/天、新词与复习约 1:2、单日新词上限 80、时间不足时优先保复习。

- ✅ 计划数据模型与艾宾浩斯调度算法（数据库 v6：`plans` / `plan_words`；学习次日首复习，答对按 1/2/4/7/15 天推进、答错次日重试、越过考试日期自动截断；10 条单测）
- ✅ 计划创建向导与概览（学习页计划卡片入口；词库 / 备考时长 30–120 天 / 每日新词 / 每日复习上限 / 计划名称；考试日期与建议量预览、可行性提示；计划概览含进度、今日新词与到期复习列表）
- ✅ 计划学习 / 复习流程（新词卡片学习后自动安排次日首复习；复习按「翻面回想 → 记住了/没记住」推进艾宾浩斯阶段：答对 +1 轮、答错次日重试；完成页展示本次统计）
- ⬜ 小游戏与计划联动（计划词池优先、游戏结果推进复习）
- ⬜ 小游戏与计划联动（优先练计划词池、游戏结果推进复习）
- ⬜ 计划日历 / 收尾文档
