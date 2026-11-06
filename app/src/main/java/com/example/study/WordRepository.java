package com.example.study;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 词库仓库（阶段 2-2 负责入库；阶段 2-3 起同时负责随机出题）。
 * 所有数据库访问都跑在同一个后台单线程池里：先保证 seed 完成，再查词出题；
 * 出题结果统一 post 回主线程，UI 线程只负责渲染。
 */
public class WordRepository {

    private static final String TAG = "WordRepository";

    /** 词库 JSON 放在 assets 根目录 */
    private static final String ASSET_FILE = "words.json";

    /** 四选一：1 个正确释义 + 3 个干扰释义 */
    private static final int OPTION_COUNT = 4;

    /**
     * 单线程池做成静态的：页面旋转重建时会创建多个 Repository 实例，
     * 只有共用同一个线程串行执行，才能保证「先查空、再插入」不被并发打乱（否则会重复入库），
     * 也保证「seed 先完成、再查词出题」的顺序。
     */
    private static final ExecutorService SEED_EXECUTOR = Executors.newSingleThreadExecutor();

    /** 回调统一切回主线程执行，便于 ViewModel / Activity 直接刷新界面 */
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private final Context context;
    private final WordDao wordDao;

    public WordRepository(Context context) {
        // 用 ApplicationContext，避免持有 Activity 造成内存泄漏
        this.context = context.getApplicationContext();
        this.wordDao = AppDatabase.getInstance(this.context).wordDao();
    }

    /**
     * 对外入口：词库为空才导入，非空直接跳过（幂等）。
     * 整个流程在后台线程执行，不阻塞界面。
     */
    public void seedIfEmpty() {
        SEED_EXECUTOR.execute(() -> {
            try {
                ensureSeeded();
            } catch (Exception e) {
                // 导入失败只记日志，不能影响答题页正常使用
                Log.e(TAG, "词库导入失败", e);
            }
        });
    }

    /**
     * 异步取一道四选一题目：随机主词 + 3 个非本题的干扰释义，打乱后回调到主线程。
     * 因为和 seedIfEmpty 共用同一个单线程池，所以一定是在「库里有词」之后才查库；
     * 这里再调一次 ensureSeeded() 作兜底，即使调用方忘了先 seed 也不会出不了题。
     */
    public void loadQuestion(QuestionCallback callback) {
        SEED_EXECUTOR.execute(() -> {
            Question question = null;
            try {
                ensureSeeded();
                question = buildRandomQuestion();
            } catch (Exception e) {
                Log.e(TAG, "随机出题失败", e);
            }
            final Question result = question;
            MAIN_HANDLER.post(() -> {
                if (result == null) {
                    callback.onQuestionLoadFailed();
                } else {
                    callback.onQuestionLoaded(result);
                }
            });
        });
    }

    /** 库为空时解析 assets/words.json 并批量入库；非空直接返回（幂等） */
    private void ensureSeeded() throws Exception {
        // 已有数据说明之前导入过，直接返回，避免重复插入
        if (wordDao.countWords() > 0) {
            return;
        }
        List<Word> words = readWordsFromAssets();
        if (words != null && !words.isEmpty()) {
            wordDao.insertAll(words);
            Log.i(TAG, "词库首次导入完成，共 " + words.size() + " 条");
        }
    }

    /**
     * 组装一道题：随机取 1 个主词，再从表里随机取 3 条「非本题拼写」的释义作干扰项。
     * 表内释义本身互不重复，这里去重只是防止脏数据；凑不满 4 个选项就视为出题失败。
     */
    private Question buildRandomQuestion() {
        Word word = wordDao.getRandomWord();
        if (word == null) {
            return null;
        }
        List<String> distractors = wordDao.getRandomMeanings(word.getSpelling(), OPTION_COUNT - 1);

        List<String> options = new ArrayList<>();
        options.add(word.getMeaning());
        for (String meaning : distractors) {
            if (meaning != null && !options.contains(meaning)) {
                options.add(meaning);
            }
        }
        if (options.size() < OPTION_COUNT) {
            return null;
        }
        Collections.shuffle(options);
        return new Question(word, options, options.indexOf(word.getMeaning()));
    }

    /**
     * 读取 assets/words.json 并用 Gson 反序列化为单词列表。
     * JSON 顶层结构为 { "words": [ ... ] }，对应下面的 WordListBean。
     */
    private List<Word> readWordsFromAssets() throws Exception {
        try (InputStream inputStream = context.getAssets().open(ASSET_FILE);
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            WordListBean bean = new Gson().fromJson(reader, WordListBean.class);
            return bean == null ? null : bean.words;
        }
    }

    /** 出题回调：两个方法都在主线程触发 */
    public interface QuestionCallback {

        void onQuestionLoaded(Question question);

        void onQuestionLoadFailed();
    }

    /** 一道四选一题目：主词、打乱后的四个释义、正确答案在选项中的下标 */
    public static class Question {

        private final Word word;
        private final List<String> options;
        private final int answerIndex;

        Question(Word word, List<String> options, int answerIndex) {
            this.word = word;
            this.options = options;
            this.answerIndex = answerIndex;
        }

        public Word getWord() {
            return word;
        }

        public List<String> getOptions() {
            return options;
        }

        public int getAnswerIndex() {
            return answerIndex;
        }
    }

    /** 对应 words.json 的顶层结构，只有一个 words 数组字段 */
    static class WordListBean {
        List<Word> words;
    }
}
