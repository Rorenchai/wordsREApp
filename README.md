# 背单词 App（WordsREApp）

一款简洁易用的 Android 背单词应用，类多邻国风格，覆盖 **三级 / 四级 / 六级** 词库，通过多种背题模式帮助高效记单词，并量化学习成果。

## 功能进度

| 功能 | 状态 |
|---|---|
| 看词选义（四选一） | ✅ 已完成（硬编码 20 词） |
| Room 数据层（Entity / Dao / Database） | ✅ 已完成 |
| 词库导入（三 / 四 / 六级 JSON） | ⬜ 待做 |
| 收藏夹 | ⬜ 待做 |
| 背词频率统计（LearningRecord） | ⬜ 待做 |
| 学习时长统计 + 打卡日历 | ⬜ 待做 |
| 单词拼写模式 | ⬜ 待做 |
| 单词消消乐 | ⬜ 待做 |
| 词汇量评测 + 个性化记忆方案 | ⬜ 待做 |

> 详细路线见 [TODO.md](TODO.md)

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
│   └── src/main/
│       ├── java/com/example/study/
│       │   ├── MainActivity.java      # 答题页
│       │   ├── QuizViewModel.java     # 题目与答题状态
│       │   ├── Word.java              # 单词实体（Room @Entity）
│       │   ├── WordDao.java            # 单词表数据访问
│       │   └── AppDatabase.java       # Room 数据库单例
│       └── res/layout/
│           └── activity_main.xml       # 答题页布局
└── build.gradle.kts
```

## 快速开始

1. Clone 仓库：`git clone https://github.com/Rorenchai/wordsREApp.git`
2. 用 Android Studio（建议最新稳定版）打开 `Study` 目录
3. 等待 Gradle 同步完成（首次需联网下载依赖）
4. 连接模拟器或真机，点击 Run

## 开发协作方式

采用「任务卡」开发循环：

```
豆包工作出任务卡  →  DeepSeek Harness 写代码  →  程序员 AS 验收  →  通过才 commit
```

每个功能只做一个，跑通验收后再进下一个；Git 每完成一个可运行功能提交一次。

## 开发铁律

1. 每个里程碑以「能跑」验收，不以「写完」为准
2. 纯 Java 项目，不生成 Kotlin 文件
3. Room 固定 2.6.1 + annotationProcessor，禁止 kapt / ksp
4. Room 查询必须异步（Executor），禁止主线程访问数据库
5. 需求变更先改文档，再改代码
