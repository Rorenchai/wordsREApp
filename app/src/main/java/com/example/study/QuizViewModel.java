package com.example.study;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 答题页的状态持有者（阶段 2-3）。
 * 题目数据源改为 Room（经 WordRepository 异步取题），不再持有硬编码词库；
 * 页面旋转重建时 ViewModel 保留，当前题与答题统计不丢，也不会重新抽题。
 */
public class QuizViewModel extends AndroidViewModel {

    /** 界面回调：由 Activity 注册，回调在主线程触发 */
    public interface QuestionListener {

        /** 题目已就绪，可以刷新界面 */
        void onQuestionReady();

        /** 取题失败（例如词库为空），界面需要给出提示 */
        void onQuestionLoadFailed();
    }

    private final WordRepository repository;

    private Word currentWord;
    private List<String> currentOptions = new ArrayList<>();
    private int answerIndex = -1;
    private int answeredCount;
    private int correctCount;

    /** 是否正在取题：防止重复请求（旋转重建时不会重复抽题的关键之一） */
    private boolean loading;

    private QuestionListener listener;

    public QuizViewModel(@NonNull Application application) {
        super(application);
        repository = new WordRepository(application);
    }

    /** 注册界面回调（Activity 在 onCreate 里设置） */
    public void setQuestionListener(QuestionListener listener) {
        this.listener = listener;
    }

    /** 清除界面回调，避免 ViewModel 持有已销毁的 Activity */
    public void clearQuestionListener() {
        this.listener = null;
    }

    /**
     * 没有当前题时才向 Repository 要一道新题。
     * 旋转重建后 ViewModel 保留、currentWord 非空，所以不会重复查库换题。
     */
    public void loadQuestionIfNeeded() {
        if (currentWord != null || loading) {
            return;
        }
        requestQuestion();
    }

    /**
     * 换下一题：保留当前题直到新题就绪（期间界面只禁用选项），避免每题闪出加载占位；
     * 新题取到后由回调通知界面刷新。
     */
    public void nextQuestion() {
        if (loading) {
            return;
        }
        requestQuestion();
    }

    /** 真正发起一次异步取题；loading 在取题期间置 true，回调回来再复位 */
    private void requestQuestion() {
        loading = true;
        repository.loadQuestion(new WordRepository.QuestionCallback() {
            @Override
            public void onQuestionLoaded(WordRepository.Question question) {
                loading = false;
                currentWord = question.getWord();
                currentOptions = question.getOptions();
                answerIndex = question.getAnswerIndex();
                if (listener != null) {
                    listener.onQuestionReady();
                }
            }

            @Override
            public void onQuestionLoadFailed() {
                loading = false;
                if (listener != null) {
                    listener.onQuestionLoadFailed();
                }
            }
        });
    }

    /**
     * 提交用户选择的选项下标。
     * 这里只做判断与计数，不负责换题，换题由调用方决定。
     *
     * @param optionIndex 用户点的是第几个选项
     * @return 是否答对
     */
    public boolean submitAnswer(int optionIndex) {
        if (currentWord == null || loading) {
            // 题未就绪或正在换题时忽略，避免统计到无效作答
            return false;
        }
        boolean correct = optionIndex == answerIndex;
        answeredCount++;
        if (correct) {
            correctCount++;
        }
        return correct;
    }

    /** 当前题是否已就绪（未就绪时界面显示加载提示，不得访问题目字段） */
    public boolean isQuestionReady() {
        return currentWord != null;
    }

    /** 是否正在取题（换题瞬间界面只禁用选项，保留上一题内容） */
    public boolean isLoading() {
        return loading;
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
