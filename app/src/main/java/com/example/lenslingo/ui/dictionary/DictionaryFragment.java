package com.example.lenslingo.ui.dictionary;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lenslingo.R;

import java.util.Random;

public class DictionaryFragment extends Fragment {

    private DictionaryViewModel viewModel;
    private WordAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dictionary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DictionaryViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_words);
        adapter = new WordAdapter(word -> viewModel.deleteWord(word));
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        EditText searchEdit = view.findViewById(R.id.edit_search);
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        Button addTestButton = view.findViewById(R.id.button_add_test_word);
        addTestButton.setOnClickListener(v -> {
            int n = new Random().nextInt(1000);
            viewModel.addTestWord("word" + n, "переклад" + n, "en");
        });

        viewModel.getWords().observe(getViewLifecycleOwner(), adapter::submitList);

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        Button startTrainingButton = view.findViewById(R.id.button_start_training);
        startTrainingButton.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new com.example.lenslingo.ui.trainer.TrainerFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.button_home).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }
}