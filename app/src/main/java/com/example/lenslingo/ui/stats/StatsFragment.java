package com.example.lenslingo.ui.stats;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.lenslingo.R;
import com.example.lenslingo.data.local.LanguageCount;
import com.example.lenslingo.data.model.StatsData;

import java.util.Locale;

public class StatsFragment extends Fragment {

    // Довге натискання на заголовок додає демо-історію. Вимкнути перед здачею
    private static final boolean DEBUG_TOOLS = true;

    private StatsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StatsViewModel.class);

        View content = view.findViewById(R.id.layout_content);
        TextView emptyText = view.findViewById(R.id.text_empty);
        TextView learned = view.findViewById(R.id.text_learned);
        TextView streak = view.findViewById(R.id.text_streak);
        TextView languages = view.findViewById(R.id.text_languages);
        TextView streakMessage = view.findViewById(R.id.text_streak_message);
        WeekBarChartView chart = view.findViewById(R.id.chart_week);
        LinearLayout byLanguage = view.findViewById(R.id.layout_by_language);

        view.findViewById(R.id.button_home).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        if (DEBUG_TOOLS) {
            view.findViewById(R.id.text_title).setOnLongClickListener(v -> {
                viewModel.seedDemo();
                Toast.makeText(requireContext(), "Demo history added", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        viewModel.getStats().observe(getViewLifecycleOwner(), (StatsData data) -> {
            if (data == null) return;

            if (data.isEmpty()) {
                content.setVisibility(View.GONE);
                emptyText.setVisibility(View.VISIBLE);
                return;
            }
            emptyText.setVisibility(View.GONE);
            content.setVisibility(View.VISIBLE);

            learned.setText(String.valueOf(data.learned));
            streak.setText(String.valueOf(data.streak));
            languages.setText(String.valueOf(data.languageCount()));

            if (data.daysSinceLast == 0) {
                streakMessage.setText("Great, you have practiced today!");
            } else if (data.daysSinceLast == 1) {
                streakMessage.setText("Practice today to keep your streak going.");
            } else if (data.daysSinceLast > 1) {
                streakMessage.setText("Last activity was " + data.daysSinceLast
                        + " days ago. Resume your streak today!");
            }
            streakMessage.setVisibility(data.daysSinceLast >= 0 ? View.VISIBLE : View.GONE);

            chart.setData(data.dayLabels, data.dayCounts);

            byLanguage.removeAllViews();
            for (int i = 0; i < data.languages.size(); i++) {
                byLanguage.addView(languageRow(data.languages.get(i), i));
            }
        });

        viewModel.load();
    }

    private static final int[] DOT_COLORS = {
            R.color.dot_1, R.color.dot_2, R.color.dot_3, R.color.dot_4, R.color.dot_5};

    private View languageRow(LanguageCount lc, int index) {
        float d = getResources().getDisplayMetrics().density;

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, (int) (8 * d), 0, (int) (8 * d));

        View dot = new View(requireContext());
        LinearLayout.LayoutParams dotParams =
                new LinearLayout.LayoutParams((int) (10 * d), (int) (10 * d));
        dotParams.setMarginEnd((int) (12 * d));
        dot.setLayoutParams(dotParams);
        dot.setBackgroundResource(R.drawable.bg_circle);
        dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(
                        requireContext(), DOT_COLORS[index % DOT_COLORS.length])));

        TextView name = new TextView(requireContext());
        name.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        name.setTextSize(15);
        name.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.ink));
        name.setText(languageName(lc.language));

        TextView count = new TextView(requireContext());
        count.setGravity(Gravity.END);
        count.setTextSize(15);
        count.setTypeface(count.getTypeface(), android.graphics.Typeface.BOLD);
        count.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.ink));
        count.setText(String.valueOf(lc.cnt));

        row.addView(dot);
        row.addView(name);
        row.addView(count);
        return row;
    }

    private String languageName(String code) {
        if (code == null || code.equals("auto")) return "Unknown";
        String name = new Locale(code).getDisplayLanguage(Locale.ENGLISH);
        return name.isEmpty() ? code : name;
    }
}