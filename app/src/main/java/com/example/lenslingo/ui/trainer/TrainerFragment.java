package com.example.lenslingo.ui.trainer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.lenslingo.R;
import com.example.lenslingo.data.local.WordEntity;

public class TrainerFragment extends Fragment {

    private TrainerViewModel viewModel;

    private TextView remainingText;
    private TextView wordText;
    private TextView translationText;
    private TextView statusText;
    private View ratingLayout;
    private Button showAnswerButton;
    private android.widget.ImageView contextImage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trainer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TrainerViewModel.class);
        contextImage = view.findViewById(R.id.image_context);

        remainingText = view.findViewById(R.id.text_remaining);
        wordText = view.findViewById(R.id.text_word);
        translationText = view.findViewById(R.id.text_translation);
        statusText = view.findViewById(R.id.text_status);
        ratingLayout = view.findViewById(R.id.layout_rating);
        showAnswerButton = view.findViewById(R.id.button_show_answer);

        Button backButton = view.findViewById(R.id.button_back);
        backButton.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        showAnswerButton.setOnClickListener(v -> revealAnswer());

        int[] buttonIds = {R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4, R.id.btn_5};
        for (int i = 0; i < buttonIds.length; i++) {
            int quality = i;
            Button button = view.findViewById(buttonIds[i]);
            button.setOnClickListener(v -> viewModel.submitAnswer(quality));
        }

        viewModel.getCurrentCard().observe(getViewLifecycleOwner(), this::showCard);
        viewModel.getRemainingCount().observe(getViewLifecycleOwner(), count ->
                remainingText.setText(count + " words left"));

        viewModel.getSessionFinished().observe(getViewLifecycleOwner(), finished -> {
            if (finished != null && finished) {
                showStatus("Session complete! Great job.");
            }
        });

        viewModel.getNoWordsToday().observe(getViewLifecycleOwner(), noWords -> {
            if (noWords != null && noWords) {
                showStatus("All words reviewed for today!");
            }
        });

        viewModel.startSession();
    }

    private void showCard(WordEntity word) {
        if (word == null) return;

        wordText.setText(word.word);
        translationText.setText(word.translation);

        if (word.photoUri != null && word.photoUri.startsWith("file:")) {
            contextImage.setVisibility(View.VISIBLE);
            com.example.lenslingo.ui.dictionary.ThumbnailLoader.load(contextImage, word.photoUri);
        } else {
            contextImage.setVisibility(View.GONE);
        }

        statusText.setVisibility(View.GONE);

        translationText.setVisibility(View.INVISIBLE);
        ratingLayout.setVisibility(View.GONE);
        showAnswerButton.setVisibility(View.VISIBLE);
    }

    private void revealAnswer() {
        translationText.setVisibility(View.VISIBLE);
        ratingLayout.setVisibility(View.VISIBLE);
        showAnswerButton.setVisibility(View.GONE);
    }

    private void showStatus(String message) {
        wordText.setText("");
        translationText.setText("");
        statusText.setText(message);
        statusText.setVisibility(View.VISIBLE);
        showAnswerButton.setVisibility(View.GONE);
        ratingLayout.setVisibility(View.GONE);
    }
}