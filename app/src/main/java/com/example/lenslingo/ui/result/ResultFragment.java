package com.example.lenslingo.ui.result;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.lenslingo.R;
import com.example.lenslingo.data.model.OcrResult;
import com.example.lenslingo.data.model.OcrWord;
import com.example.lenslingo.ocr.OcrLog;
import com.example.lenslingo.ui.shared.SharedOcrViewModel;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ResultFragment extends Fragment {

    private ResultViewModel viewModel;
    private SharedOcrViewModel sharedViewModel;

    private BoundingBoxImageView imageBoxes;
    private ChipGroup chipGroup;
    private View popup;
    private TextView popupWord;
    private TextView popupTranslation;
    private TextView popupLang;
    private View imageTab;
    private View textTab;

    private OcrWord popupTarget;
    private String currentLang;
    private Bitmap currentImage;

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

        imageBoxes = view.findViewById(R.id.image_boxes);
        chipGroup = view.findViewById(R.id.chip_group);
        popup = view.findViewById(R.id.popup_word_card);
        popupWord = view.findViewById(R.id.popup_word);
        popupTranslation = view.findViewById(R.id.popup_translation);
        popupLang = view.findViewById(R.id.popup_lang);
        imageTab = view.findViewById(R.id.layout_image_tab);
        textTab = view.findViewById(R.id.layout_text_tab);
        TextView languageText = view.findViewById(R.id.text_source_language);
        TextView notice = view.findViewById(R.id.text_notice);

        view.findViewById(R.id.button_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack(
                        null, FragmentManager.POP_BACK_STACK_INCLUSIVE));

        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_tabs);
        toggle.check(R.id.tab_image);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            boolean image = checkedId == R.id.tab_image;
            imageTab.setVisibility(image ? View.VISIBLE : View.GONE);
            textTab.setVisibility(image ? View.GONE : View.VISIBLE);
            dismissPopup();
        });

        imageBoxes.setOnWordTapListener(this::selectWord);
        imageBoxes.setOnEmptyTapListener(this::dismissPopup);

        view.findViewById(R.id.popup_save).setOnClickListener(v -> {
            if (popupTarget != null) {
                viewModel.saveWord(popupTarget, currentLang, currentImage);
            }
            dismissPopup();
        });

        sharedViewModel.getResult().observe(getViewLifecycleOwner(), (OcrResult result) -> {
            if (result == null) return;

            currentLang = result.sourceLanguage;
            currentImage = result.image;

            languageText.setText(result.sourceLanguage.toUpperCase(Locale.ROOT)
                    + (result.fromOnlineMode ? " · online" : " · offline"));
            if (result.notice != null) {
                notice.setText(result.notice);
                notice.setVisibility(View.VISIBLE);
            } else {
                notice.setVisibility(View.GONE);
            }

            List<OcrWord> visible = result.visibleWords();
            imageBoxes.setImage(result.image);
            imageBoxes.setWords(visible);
            fillChips(visible);
            fillTexts(view, result);
        });

        viewModel.getSaveMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    private void fillChips(List<OcrWord> words) {
        chipGroup.removeAllViews();

        int selectedBg = ContextCompat.getColor(requireContext(), R.color.white);
        int normalBg = ContextCompat.getColor(requireContext(), R.color.green_light);
        int selectedText = ContextCompat.getColor(requireContext(), R.color.ink);
        int normalText = ContextCompat.getColor(requireContext(), R.color.white);
        int[][] states = {{android.R.attr.state_checked}, {}};
        ColorStateList bg = new ColorStateList(states, new int[]{selectedBg, normalBg});
        ColorStateList fg = new ColorStateList(states, new int[]{selectedText, normalText});

        Set<String> seen = new HashSet<>();
        for (OcrWord w : words) {
            String label = w.shownText();
            if (!seen.add(label.toLowerCase(Locale.ROOT))) continue;

            Chip chip = new Chip(requireContext());
            chip.setText(label);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setChipBackgroundColor(bg);
            chip.setTextColor(fg);
            chip.setChipStrokeWidth(0f);
            chip.setTag(w);
            chip.setOnClickListener(v -> selectWord(w));
            chipGroup.addView(chip);
        }
    }

    private void fillTexts(View root, OcrResult result) {
        Map<Integer, StringBuilder> original = new LinkedHashMap<>();
        for (OcrWord w : result.words) {
            if (w.kind == OcrWord.KIND_NOISE) continue;
            StringBuilder sb = original.get(w.lineIndex);
            if (sb == null) {
                sb = new StringBuilder();
                original.put(w.lineIndex, sb);
            }
            if (sb.length() > 0) sb.append(' ');
            sb.append(w.shownText());
        }

        StringBuilder originalText = new StringBuilder();
        StringBuilder translatedText = new StringBuilder();

        for (Map.Entry<Integer, StringBuilder> e : original.entrySet()) {
            appendLine(originalText, e.getValue().toString());

            String translated;
            if (result.lineTranslations != null) {
                translated = result.lineTranslations.get(e.getKey());
            } else {
                translated = joinWordTranslations(result.words, e.getKey());
            }
            if (translated != null && !translated.isEmpty()) appendLine(translatedText, translated);
        }

        ((TextView) root.findViewById(R.id.text_original)).setText(originalText);
        ((TextView) root.findViewById(R.id.text_translation_full)).setText(translatedText);
        ((TextView) root.findViewById(R.id.text_translation_label)).setText(
                result.lineTranslations != null
                        ? R.string.translation_label : R.string.translation_label_offline);
    }

    private static void appendLine(StringBuilder sb, String line) {
        if (sb.length() > 0) sb.append('\n');
        sb.append(line);
    }

    private static String joinWordTranslations(List<OcrWord> words, int lineIndex) {
        StringBuilder sb = new StringBuilder();
        for (OcrWord w : words) {
            if (w.lineIndex != lineIndex || w.translation == null) continue;
            if (w.translation.startsWith("(")) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(w.translation);
        }
        return sb.toString();
    }

    private void selectWord(OcrWord word) {
        OcrLog.d("TAP \"" + word.text + "\" shown=\"" + word.shownText()
                + "\" kind=" + word.kind + " box=" + OcrLog.fmt(word.box()));

        popupTarget = word;
        imageBoxes.setSelectedWord(word);
        syncChip(word);

        popupWord.setText(word.shownText());
        popupTranslation.setText(word.translation != null ? word.translation : "—");
        popupLang.setText(currentLang == null || "auto".equals(currentLang)
                ? "—" : currentLang.toUpperCase(Locale.ROOT));

        popup.setVisibility(View.INVISIBLE);
        popup.post(() -> positionPopup(word));
    }

    private void positionPopup(OcrWord word) {
        RectF r = imageBoxes.getWordRect(word);
        if (r == null || popupTarget != word) return;

        float gap = 8 * getResources().getDisplayMetrics().density;
        int maxWidth = (int) (imageBoxes.getWidth() - 2 * gap);
        popup.measure(View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.UNSPECIFIED);
        int pw = popup.getMeasuredWidth();
        int ph = popup.getMeasuredHeight();

        float x = r.centerX() - pw / 2f;
        x = Math.max(gap, Math.min(x, imageBoxes.getWidth() - pw - gap));

        float y = r.top - ph - gap;
        if (y < gap) y = r.bottom + gap;
        if (y + ph > imageBoxes.getHeight() - gap) {
            y = Math.max(gap, imageBoxes.getHeight() - ph - gap);
        }

        popup.setX(x);
        popup.setY(y);
        popup.setVisibility(View.VISIBLE);
    }

    private void syncChip(OcrWord word) {
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            OcrWord tagged = (OcrWord) chip.getTag();
            if (tagged != null && tagged.shownText().equalsIgnoreCase(word.shownText())) {
                chip.setChecked(true);
                return;
            }
        }
        chipGroup.clearCheck();
    }

    private void dismissPopup() {
        popupTarget = null;
        popup.setVisibility(View.GONE);
        imageBoxes.clearSelection();
        chipGroup.clearCheck();
    }
}