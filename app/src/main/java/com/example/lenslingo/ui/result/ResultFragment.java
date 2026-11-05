package com.example.lenslingo.ui.result;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lenslingo.R;
import com.example.lenslingo.data.model.OcrResult;
import com.example.lenslingo.data.model.OcrWord;
import com.example.lenslingo.ui.shared.SharedOcrViewModel;

public class ResultFragment extends Fragment {

    private ResultViewModel viewModel;
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

        RecyclerView recyclerView = view.findViewById(R.id.recycler_ocr_words);
        adapter = new OcrWordAdapter(this::showSaveDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        TextView languageText = view.findViewById(R.id.text_source_language);

        Button backButton = view.findViewById(R.id.button_back);
        backButton.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack(
                        null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE));

        sharedViewModel.getResult().observe(getViewLifecycleOwner(), (OcrResult result) -> {
            if (result == null) return;

            currentSourceLanguage = result.sourceLanguage;
            languageText.setText("Detected: " + result.sourceLanguage
                    + (result.fromOnlineMode ? " (online)" : " (offline)"));
            adapter.submitList(result.words);
        });

        viewModel.getSaveMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    private void showSaveDialog(OcrWord word) {
        new AlertDialog.Builder(requireContext())
                .setTitle(word.text)
                .setMessage(word.translation != null ? word.translation : "No translation available")
                .setPositiveButton("Save", (dialog, which) ->
                        viewModel.saveWord(word, currentSourceLanguage))
                .setNegativeButton("Close", null)
                .show();
    }
}