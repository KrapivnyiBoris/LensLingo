package com.example.lenslingo.ui.result;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lenslingo.R;
import com.example.lenslingo.data.model.OcrResult;
import com.example.lenslingo.data.model.OcrWord;
import com.example.lenslingo.ocr.OcrLog;
import com.example.lenslingo.ui.shared.SharedOcrViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class ResultFragment extends Fragment {

    private ResultViewModel viewModel;
    private Bitmap currentImage;
    private SharedOcrViewModel sharedViewModel;
    private OcrWordAdapter adapter;

    private String currentSourceLanguage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ResultViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedOcrViewModel.class);

        BoundingBoxImageView imageBoxes = view.findViewById(R.id.image_boxes);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_ocr_words);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        TextView languageText = view.findViewById(R.id.text_source_language);

        adapter = new OcrWordAdapter(this::showSaveDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        imageBoxes.setOnWordTapListener(this::showSaveDialog);

        tabLayout.addTab(tabLayout.newTab().setText("Image"));
        tabLayout.addTab(tabLayout.newTab().setText("Text"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean imageTab = tab.getPosition() == 0;
                imageBoxes.setVisibility(imageTab ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(imageTab ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        view.findViewById(R.id.button_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack(
                        null, FragmentManager.POP_BACK_STACK_INCLUSIVE));

        sharedViewModel.getResult().observe(getViewLifecycleOwner(), (OcrResult result) -> {
            if (result == null) return;

            currentSourceLanguage = result.sourceLanguage;
            currentImage = result.image;

            languageText.setText("Detected: " + result.sourceLanguage
                    + (result.fromOnlineMode ? " (online)" : " (offline)"));

            if (result.notice != null) {
                Toast.makeText(requireContext(), result.notice, Toast.LENGTH_LONG).show();
            }

            List<OcrWord> visible = result.visibleWords();
            imageBoxes.setImage(result.image);
            imageBoxes.setWords(visible);
            adapter.submitList(visible);
        });

        viewModel.getSaveMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    private void showSaveDialog(OcrWord word) {
        OcrLog.d("TAP \"" + word.text + "\" shown=\"" + word.shownText()
                + "\" kind=" + word.kind + " box=" + OcrLog.fmt(word.box()));

        new AlertDialog.Builder(requireContext())
                .setTitle(word.shownText())
                .setMessage(word.translation != null ? word.translation : "No translation available")
                .setPositiveButton("Save", (dialog, which) ->
                        viewModel.saveWord(word, currentSourceLanguage, currentImage))
                .setNegativeButton("Close", null)
                .show();
    }
}