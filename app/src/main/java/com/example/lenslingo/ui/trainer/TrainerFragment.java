package com.example.lenslingo.ui.trainer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.lenslingo.R;
import com.example.lenslingo.data.local.WordEntity;
import com.example.lenslingo.ui.dictionary.ThumbnailLoader;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Locale;

public class TrainerFragment extends Fragment {

    private TrainerViewModel viewModel;

    private TextView remainingText;
    private TextView wordText;
    private TextView translationText;
    private TextView languageBadge;
    private TextView statusText;
    private View ratingLayout;
    private View wordCard;
    private View photoCard;
    private View showAnswerButton;
    private ImageView contextImage;
    private LinearProgressIndicator progress;
    private int total = 0;

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

        remainingText = view.findViewById(R.id.text_remaining);
        wordText = view.findViewById(R.id.text_word);
        translationText = view.findViewById(R.id.text_translation);
        languageBadge = view.findViewById(R.id.text_language);
        statusText = view.findViewById(R.id.text_status);
        ratingLayout = view.findViewById(R.id.layout_rating);
        wordCard = view.findViewById(R.id.card_word);
        photoCard = view.findViewById(R.id.card_photo);
        showAnswerButton = view.findViewById(R.id.button_show_answer);
        contextImage = view.findViewById(R.id.image_context);
        progress = view.findViewById(R.id.progress_bar);

        view.findViewById(R.id.button_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        showAnswerButton.setOnClickListener(v -> revealAnswer());

        int[] buttonIds = {R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4, R.id.btn_5};
        for (int i = 0; i < buttonIds.length; i++) {
            int quality = i;
            view.findViewById(buttonIds[i]).setOnClickListener(v -> viewModel.submitAnswer(quality));
        }

        viewModel.getTotalCount().observe(getViewLifecycleOwner(), t -> total = t == null ? 0 : t);

        viewModel.getRemainingCount().observe(getViewLifecycleOwner(), count -> {
            remainingText.setText(getString(R.string.words_left, count));
            if (total > 0) {
                progress.setProgressCompat(100 * (total - count) / total, true);
            }
        });

        viewModel.getCurrentCard().observe(getViewLifecycleOwner(), this::showCard);

        viewModel.getSessionFinished().observe(getViewLifecycleOwner(), finished -> {
            if (finished != null && finished) {
                progress.setProgressCompat(100, true);
                showStatus(String.valueOf(R.string.session_complete));
            }
        });

        viewModel.getNoWordsToday().observe(getViewLifecycleOwner(), noWords -> {
            if (noWords != null && noWords) {
                remainingText.setText(getString(R.string.words_left, 0));
                showStatus(String.valueOf(R.string.all_reviewed));
            }
        });

        viewModel.startSession();
    }

    private void showCard(WordEntity word) {
        if (word == null) return;

        statusText.setVisibility(View.GONE);
        wordCard.setVisibility(View.VISIBLE);

        wordText.setText(word.word);
        translationText.setText(word.translation);
        translationText.setVisibility(View.INVISIBLE);
        languageBadge.setText("auto".equals(word.sourceLanguage)
                ? "—" : word.sourceLanguage.toUpperCase(Locale.ROOT));

        if (word.photoUri != null && word.photoUri.startsWith("file:")) {
            photoCard.setVisibility(View.VISIBLE);
            ThumbnailLoader.load(contextImage, word.photoUri);
        } else {
            photoCard.setVisibility(View.GONE);
        }

        ratingLayout.setVisibility(View.GONE);
        showAnswerButton.setVisibility(View.VISIBLE);
    }

    private void revealAnswer() {
        translationText.setVisibility(View.VISIBLE);
        ratingLayout.setVisibility(View.VISIBLE);
        showAnswerButton.setVisibility(View.GONE);
    }

    private void showStatus(String message) {
        wordCard.setVisibility(View.GONE);
        photoCard.setVisibility(View.GONE);
        showAnswerButton.setVisibility(View.GONE);
        ratingLayout.setVisibility(View.GONE);
        statusText.setText(message);
        statusText.setVisibility(View.VISIBLE);
    }
}