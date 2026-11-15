package com.example.lenslingo.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import com.example.lenslingo.R;
import com.example.lenslingo.data.Prefs;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.button_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        RadioGroup group = view.findViewById(R.id.radio_language);

        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        boolean ukrainian = current.startsWith("uk")
                || (current.isEmpty() && getResources().getConfiguration()
                .getLocales().get(0).getLanguage().equals("uk"));
        group.check(ukrainian ? R.id.radio_uk : R.id.radio_en);

        group.setOnCheckedChangeListener((g, checkedId) -> {
            String tag = checkedId == R.id.radio_uk ? "uk" : "en";
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
        });

        MaterialSwitch translation = view.findViewById(R.id.switch_online_translation);
        translation.setChecked(Prefs.onlineTranslation(requireContext()));
        translation.setOnCheckedChangeListener((b, checked) ->
                Prefs.setOnlineTranslation(requireContext(), checked));

        MaterialSwitch suggestions = view.findViewById(R.id.switch_online_suggestions);
        suggestions.setChecked(Prefs.onlineSuggestions(requireContext()));
        suggestions.setOnCheckedChangeListener((b, checked) ->
                Prefs.setOnlineSuggestions(requireContext(), checked));
    }
}