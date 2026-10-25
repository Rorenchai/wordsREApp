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

        // 四个选项按钮统一挂监听，用下标区分点的是哪一个
        optionButtons = new Button[]{
                binding.option1, binding.option2, binding.option3, binding.option4
        };
        for (int i = 0; i < optionButtons.length; i++) {
            final int optionIndex = i;
            optionButtons[i].setOnClickListener(v -> onOptionSelected(optionIndex));
        }

        renderQuestion();
    }

    /**
     * 把 ViewModel 里的当前题目渲染到界面：单词、四个选项、进度统计。
     */
    private void renderQuestion() {
        binding.wordText.setText(quizViewModel.getCurrentWord().getSpelling());

        List<String> options = quizViewModel.getCurrentOptions();
        for (int i = 0; i < optionButtons.length; i++) {
            optionButtons[i].setText(options.get(i));
        }

        // 题号从 1 开始，所以用已答数 + 1
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
        renderQuestion();
    }
}
