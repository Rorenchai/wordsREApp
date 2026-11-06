package com.example.study;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.study.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private QuizViewModel quizViewModel;
    private Button[] optionButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 阶段 2-2：词库为空时把 assets/words.json 导入 Room（后台执行，界面无感知）
        new WordRepository(this).seedIfEmpty();
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 旋转屏幕等页面重建场景下复用同一个 ViewModel，题目和答题统计不丢
        quizViewModel = new ViewModelProvider(this).get(QuizViewModel.class);

        // 阶段 2-3：题目改为异步从 Room 取，取到后回调刷新界面
        quizViewModel.setQuestionListener(new QuizViewModel.QuestionListener() {
            @Override
            public void onQuestionReady() {
                renderQuestion();
            }

            @Override
            public void onQuestionLoadFailed() {
                Toast.makeText(MainActivity.this, R.string.quiz_load_failed, Toast.LENGTH_SHORT).show();
                renderQuestion();
            }
        });

        // 四个选项按钮统一挂监听，用下标区分点的是哪一个
        optionButtons = new Button[]{
                binding.option1, binding.option2, binding.option3, binding.option4
        };
        for (int i = 0; i < optionButtons.length; i++) {
            final int optionIndex = i;
            optionButtons[i].setOnClickListener(v -> onOptionSelected(optionIndex));
        }

        // 先渲染「加载中」占位，再按需取第一题（旋转重建时不会重复取题）
        renderQuestion();
        quizViewModel.loadQuestionIfNeeded();
    }

    @Override
    protected void onDestroy() {
        // ViewModel 会跨旋转存活，销毁前清掉界面回调，避免持有已销毁的 Activity
        if (quizViewModel != null) {
            quizViewModel.clearQuestionListener();
        }
        super.onDestroy();
    }

    /**
     * 把 ViewModel 里的当前题目渲染到界面：单词、四个选项、进度统计。
     * 题目未就绪时显示加载提示并禁用选项，避免空指针或误作答。
     */
    private void renderQuestion() {
        boolean loading = quizViewModel.isLoading();
        if (!quizViewModel.isQuestionReady()) {
            // 首题还没取到：显示加载提示并禁用选项
            binding.wordText.setText(R.string.quiz_loading);
            for (Button button : optionButtons) {
                button.setText("");
                button.setEnabled(false);
            }
        } else {
            binding.wordText.setText(quizViewModel.getCurrentWord().getSpelling());

            List<String> options = quizViewModel.getCurrentOptions();
            for (int i = 0; i < optionButtons.length; i++) {
                optionButtons[i].setText(options.get(i));
                // 换题加载中沿用旧题内容但暂时禁用，防止重复作答
                optionButtons[i].setEnabled(!loading);
            }
        }

        // 题号从 1 开始，所以用已答数 + 1；进度统计不受加载状态影响
        binding.progressText.setText(getString(R.string.quiz_progress,
                quizViewModel.getAnsweredCount() + 1,
                quizViewModel.getAnsweredCount(),
                quizViewModel.getCorrectCount()));
    }

    /**
     * 用户点选某个选项：先判对错并提示，再自动换下一题。
     *
     * @param optionIndex 用户点的是第几个选项
     */
    private void onOptionSelected(int optionIndex) {
        if (!quizViewModel.isQuestionReady() || quizViewModel.isLoading()) {
            // 题未就绪或正在换题时忽略点击（按钮已禁用，这里再兜底一次）
            return;
        }
        boolean correct = quizViewModel.submitAnswer(optionIndex);
        if (correct) {
            Toast.makeText(this, R.string.toast_answer_correct, Toast.LENGTH_SHORT).show();
        } else {
            // 答错时把正确答案的释义一起告诉用户
            Toast.makeText(this,
                    getString(R.string.toast_answer_wrong, quizViewModel.getCurrentWord().getMeaning()),
                    Toast.LENGTH_SHORT).show();
        }
        quizViewModel.nextQuestion();
        renderQuestion();   // 立即禁用选项并保留旧题，取到新题后由回调刷新
    }
}
