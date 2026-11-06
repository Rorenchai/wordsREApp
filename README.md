# 背单词 App（WordsREApp）

一款简洁易用的 Android 背单词应用，类多邻国风格，覆盖 **PETS / 四级 / 六级** 词库（2026-11 口径调整：原「三级」更名 PETS），通过多种背题模式帮助高效记单词，并量化学习成果。

> 当前状态（2026-11-07）：阶段 2-2「词库 JSON 首次入库」已验收通过并提交（`7fe4124`）；阶段 2-3「Room 随机抽词接入答题页」进行中（任务卡 11）。词库口径为 PETS×7 / CET4×7 / CET6×6，共 20 词。

## 功能进度

| 功能 | 状态 |
|---|---|
| 看词选义（四选一） | ✅ 已完成（阶段 1，硬编码 20 词） |
| Room 数据层（Entity / Dao / Database） | ✅ 已完成（阶段 2-1） |
| 词库导入（PETS / 四 / 六级 JSON） | ✅ 已完成（阶段 2-2，2026-11-07 验收通过） |
| Room 随机抽词接入答题页 | 🔄 进行中（阶段 2-3，任务卡 11） |
| 收藏夹 | ⬜ 待做 |
| 背词频率统计（LearningRecord） | ⬜ 待做 |
| 学习时长统计 + 打卡日历 | ⬜ 待做 |
| 单词拼写模式 | ⬜ 待做 |
| 单词消消乐 | ⬜ 待做 |
| 词汇量评测 + 个性化记忆方案 | ⬜ 待做 |

> 详细路线见项目根目录 `TODO.md`（位于本仓库之外）；Web 线 W-1 交付物在 `Web/index.html`。

## 技术栈

| 项 | 选型 |
|---|---|
| 语言 | Java（不使用 Kotlin / Compose） |
| UI | XML + ViewBinding |
| 数据库 | Room 2.6.1（annotationProcessor） |
| JSON 解析 | Gson |
| 架构 | MVVM：Activity + ViewModel + Repository + Room |

## 项目结构

```
Study/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── assets/
│       │   └── words.json          # 20 词词库（PETS / CET4 / CET6）
│       ├── java/com/example/study/
│       │   ├── MainActivity.java      # 答题页
│       │   ├── QuizViewModel.java     # 题目与答题状态
│       │   ├── Word.java              # 单词实体（Room @Entity）
│       │   ├── WordDao.java           # 单词表数据访问
│       │   ├── AppDatabase.java       # Room 数据库单例
│       │   └── WordRepository.java    # 首次启动导入词库 / 抽词（阶段 2-3 扩展中）
│       └── res/
│           ├── layout/activity_main.xml
│           └── values/strings.xml
└── gradle/libs.versions.toml
```

## 快速开始

1. Clone 仓库：`git clone https://github.com/Rorenchai/wordsREApp.git`（本仓库内容在 `main` 分支）
2. 用 Android Studio（建议最新稳定版）打开 `Study` 目录
3. 等待 Gradle 同步完成（首次需联网下载依赖）
4. 连接模拟器或真机，点击 Run
5. 首次启动自动导入 `assets/words.json` 到 Room（`seedIfEmpty` 幂等，重复启动不会重复入库）

## 开发协作方式

采用「任务卡」开发循环（2026-11-06 起豆包下线，项目大脑职责由 WorkBuddy 兼任）：

```
WorkBuddy（PM）出任务卡  →  DeepSeek Harness 写代码  →  Kimi 静态审查
      →  程序员按测试清单验收  →  通过后由 WorkBuddy 提交并回报哈希
```

每个功能只做一个，跑通验收后再进下一个；Git 每完成一个可运行功能提交一次。

仓库形态：远程 `Rorenchai/wordsREApp` 单仓库三分支——`main`（本工程）、`web`（Web 单页）、`workbuddy`（助理产出），三者独立历史、互不合并。

## 开发铁律

1. 每个里程碑以「能跑」验收，不以「写完」为准
2. 纯 Java 项目，不生成 Kotlin 文件
3. Room 固定 2.6.1 + annotationProcessor，禁止 kapt / ksp
4. Room 查询必须异步（Executor），禁止主线程访问数据库
5. 需求变更先改文档，再改代码
