package com.example.lenslingo.ui.dictionary;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.lenslingo.R;
import com.example.lenslingo.data.local.WordEntity;
import com.example.lenslingo.ocr.RemoteDataSource;
import com.example.lenslingo.ocr.TranslationSuggester;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WordDetailsSheet extends BottomSheetDialogFragment {
    private final TranslationSuggester suggester = new TranslationSuggester();

    public static final String RESULT_DELETE = "word_delete";
    public static final String KEY_ID = "id";
    public static final String RESULT_EDIT = "word_edit";
    public static final String KEY_TRANSLATION = "translation_new";

    public static WordDetailsSheet newInstance(WordEntity w) {
        Bundle b = new Bundle();
        b.putInt(KEY_ID, w.id);
        b.putString("word", w.word);
        b.putString("translation", w.translation);
        b.putString("lang", w.sourceLanguage);
        b.putString("photo", w.photoUri);
        b.putLong("added", w.dateAdded);
        b.putInt("reps", w.repetitionCount);
        b.putInt("interval", w.intervalDays);
        b.putLong("next", w.nextReviewDate);
        WordDetailsSheet sheet = new WordDetailsSheet();
        sheet.setArguments(b);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_word_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle a = requireArguments();

        ((TextView) view.findViewById(R.id.text_word)).setText(a.getString("word"));
        ((TextView) view.findViewById(R.id.text_translation)).setText(a.getString("translation"));

        String lang = a.getString("lang", "auto");
        ((TextView) view.findViewById(R.id.text_language)).setText(
                "auto".equals(lang) ? "—" : lang.toUpperCase(Locale.ROOT));

        String photo = a.getString("photo");
        if (photo != null && photo.startsWith("file:")) {
            ImageView image = view.findViewById(R.id.image_photo);
            try {
                image.setImageURI(Uri.parse(photo));
                image.setVisibility(View.VISIBLE);
            } catch (Exception ignored) {
            }
        }

        DateFormat fmt = DateFormat.getDateInstance(DateFormat.MEDIUM,
                getResources().getConfiguration().getLocales().get(0));
        long next = a.getLong("next");
        String nextText = next <= System.currentTimeMillis()
                ? getString(R.string.due_now) : fmt.format(new Date(next));
        String info = getString(R.string.info_added, fmt.format(new Date(a.getLong("added"))))
                + "\n" + getString(R.string.info_next, nextText)
                + "\n" + getString(R.string.info_reps, a.getInt("reps"))
                + "\n" + getString(R.string.info_interval, a.getInt("interval"));
        ((TextView) view.findViewById(R.id.text_info)).setText(info);

        int id = a.getInt(KEY_ID);
        TextView translationView = view.findViewById(R.id.text_translation);
        view.findViewById(R.id.button_edit).setOnClickListener(v ->
                showEditDialog(id, translationView, a.getString("word"), a.getString("lang", "auto")));

        view.findViewById(R.id.button_delete).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.delete_confirm_title)
                        .setMessage(R.string.delete_confirm_message)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.delete, (d, w) -> {
                            Bundle result = new Bundle();
                            result.putInt(KEY_ID, id);
                            getParentFragmentManager().setFragmentResult(RESULT_DELETE, result);
                            dismiss();
                        })
                        .show());
    }

    @Override
    public void onStart() {
        super.onStart();
        View parent = (View) requireView().getParent();
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void showEditDialog(int id, TextView translationView, String word, String lang) {
        float d = getResources().getDisplayMetrics().density;

        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSingleLine(true);
        input.setHint(R.string.translation_hint);
        input.setText(translationView.getText());
        input.setSelection(input.getText().length());

        TextView status = new TextView(requireContext());
        status.setTextSize(12);
        status.setPadding(0, (int) (12 * d), 0, (int) (4 * d));

        ChipGroup chips = new ChipGroup(requireContext());

        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding((int) (24 * d), (int) (8 * d), (int) (24 * d), 0);
        box.addView(input);
        box.addView(status);
        box.addView(chips);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.edit_translation_title)
                .setView(box)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dlg, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) return;
                    if (value.length() > 100) value = value.substring(0, 100);

                    translationView.setText(value);
                    Bundle result = new Bundle();
                    result.putInt(KEY_ID, id);
                    result.putString(KEY_TRANSLATION, value);
                    getParentFragmentManager().setFragmentResult(RESULT_EDIT, result);
                })
                .show();

        if (!isOnline()) {
            status.setText(R.string.suggestions_offline);
            return;
        }

        status.setText(R.string.suggestions_loading);
        suggester.suggest(word, lang, new RemoteDataSource.SuggestionCallback() {
            @Override
            public void onSuccess(List<String> variants) {
                runIfShowing(dialog, () -> {
                    status.setText(variants.isEmpty()
                            ? R.string.suggestions_failed : R.string.suggestions_title);
                    for (String variant : variants) {
                        Chip chip = new Chip(requireContext());
                        chip.setText(variant);
                        chip.setOnClickListener(c -> {
                            input.setText(variant);
                            input.setSelection(variant.length());
                        });
                        chips.addView(chip);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runIfShowing(dialog, () -> status.setText(R.string.suggestions_failed));
            }
        });
    }

    private void runIfShowing(AlertDialog dialog, Runnable action) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (dialog.isShowing()) action.run();
        });
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}