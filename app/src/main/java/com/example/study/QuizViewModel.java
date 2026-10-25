package com.example.study;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 答题页的状态持有者（阶段 1）。
 * 单词暂时硬编码在内存里，不接数据库；页面旋转重建时题目与答题统计由 ViewModel 保留。
 */
public class QuizViewModel extends ViewModel {

    /** 四选一，固定 4 个选项 */
    private static final int OPTION_COUNT = 4;

    /** 阶段 1 的硬编码词库：先跑通「出题 - 判断 - 换词」链路，阶段 2 再换成 Room + 词库 JSON */
    private final List<Word> words = Arrays.asList(
            new Word("abandon", "v. 放弃；抛弃"),
            new Word("absorb", "v. 吸收；使专心"),
            new Word("accurate", "adj. 准确的；精确的"),
            new Word("adapt", "v. 适应；改编"),
            new Word("benefit", "n. 益处 v. 受益"),
            new Word("capable", "adj. 有能力的"),
            new Word("concentrate", "v. 集中；专心"),
            new Word("convince", "v. 使确信；说服"),
            new Word("decline", "v. 下降；拒绝"),
            new Word("efficient", "adj. 高效的；有效率的"),
            new Word("essential", "adj. 必不可少的；本质的"),
            new Word("expand", "v. 扩大；膨胀"),
            new Word("familiar", "adj. 熟悉的"),
            new Word("generate", "v. 产生；发电"),
            new Word("identical", "adj. 完全相同的"),
            new Word("maintain", "v. 维持；保养"),
            new Word("obvious", "adj. 明显的"),
            new Word("participate", "v. 参加；参与"),
            new Word("reduce", "v. 减少；降低"),
            new Word("sufficient", "adj. 足够的；充分的")
    );

    private final Random random = new Random();

    private Word currentWord;
    private List<String> currentOptions = new ArrayList<>();
    private int answerIndex;
    private int answeredCount;
    private int correctCount;

    public QuizViewModel() {
        nextQuestion();
    }

    /**
     * 换下一题：随机抽一个单词，再随机挑 3 个其它单词的释义当干扰项，
     * 打乱顺序后作为四个选项，并记下正确答案在选项中的位置。
     */
    public void nextQuestion() {
        currentWord = words.get(random.nextInt(words.size()));

        List<String> options = new ArrayList<>();
        options.add(currentWord.getMeaning());
        // 干扰项从词库随机取，去重后凑满 4 个
        while (options.size() < OPTION_COUNT) {
            String meaning = words.get(random.nextInt(words.size())).getMeaning();
            if (!options.contains(meaning)) {
                options.add(meaning);
            }
        }
        Collections.shuffle(options, random);

        currentOptions = options;
        answerIndex = options.indexOf(currentWord.getMeaning());
    }

    /**
     * 提交用户选择的选项下标。
     * 这里只做判断与计数，不负责换题，换题由调用方决定。
     *
     * @param optionIndex 用户点的是第几个选项
     * @return 是否答对
     */
    public boolean submitAnswer(int optionIndex) {
        boolean correct = optionIndex == answerIndex;
        answeredCount++;
        if (correct) {
            correctCount++;
        }
        return correct;
    }

    public Word getCurrentWord() {
        return currentWord;
    }

    public List<String> getCurrentOptions() {
        return currentOptions;
    }

    public int getAnsweredCount() {
        return answeredCount;
    }

    public int getCorrectCount() {
        return correctCount;
    }
}
