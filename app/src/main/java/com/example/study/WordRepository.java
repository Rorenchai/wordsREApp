package com.example.study;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 词库仓库（阶段 2-2）：只负责「words 表为空时，把 assets/words.json 导入 Room」这一件事。
 * 答题页的数据来源仍是 QuizViewModel 里的硬编码词，本阶段不接入这里（阶段 2-3 才换）。
 */
public class WordRepository {

    private static final String TAG = "WordRepository";

    /** 词库 JSON 放在 assets 根目录 */
    private static final String ASSET_FILE = "words.json";

    /**
     * 单线程池做成静态的：页面旋转重建时会创建多个 Repository 实例，
     * 只有共用同一个线程串行执行，才能保证「先查空、再插入」不被并发打乱，
     * 否则可能重复入库变成 40 行。
     */
    private static final ExecutorService SEED_EXECUTOR = Executors.newSingleThreadExecutor();

    private final Context context;
    private final WordDao wordDao;

    public WordRepository(Context context) {
        // 用 ApplicationContext，避免持有 Activity 造成内存泄漏
        this.context = context.getApplicationContext();
        this.wordDao = AppDatabase.getInstance(this.context).wordDao();
    }

    /**
     * 对外入口：词库为空才导入，非空直接跳过（幂等）。
     * 整个流程在后台线程执行，不阻塞界面，也不影响阶段 1 的答题链路。
     */
    public void seedIfEmpty() {
        SEED_EXECUTOR.execute(() -> {
            try {
                // 已有数据说明之前导入过，直接返回，避免重复插入
                if (wordDao.countWords() > 0) {
                    return;
                }
                List<Word> words = readWordsFromAssets();
                if (words != null && !words.isEmpty()) {
                    wordDao.insertAll(words);
                    Log.i(TAG, "词库首次导入完成，共 " + words.size() + " 条");
                }
            } catch (Exception e) {
                // 导入失败只记日志，不能影响答题页正常使用
                Log.e(TAG, "词库导入失败", e);
            }
        });
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

    /** 对应 words.json 的顶层结构，只有一个 words 数组字段 */
    static class WordListBean {
        List<Word> words;
    }
}
