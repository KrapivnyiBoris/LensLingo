package com.example.lenslingo.ui.dictionary;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.lenslingo.R;
import com.example.lenslingo.data.local.WordEntity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class WordDetailsSheet extends BottomSheetDialogFragment {

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

        DateFormat fmt = DateFormat.getDateInstance(DateFormat.MEDIUM);
        long next = a.getLong("next");
        String nextText = next <= System.currentTimeMillis()
                ? getString(R.string.due_now) : fmt.format(new Date(next));
        int reps = a.getInt("reps");
        String info = "Added: " + fmt.format(new Date(a.getLong("added")))
                + "\nNext review: " + nextText
                + "\nSuccessful reviews in a row: " + reps
                + "\nCurrent interval: " + a.getInt("interval") + " day(s)";
        ((TextView) view.findViewById(R.id.text_info)).setText(info);

        int id = a.getInt(KEY_ID);
        TextView translationView = view.findViewById(R.id.text_translation);
        view.findViewById(R.id.button_edit).setOnClickListener(v -> {
            float d = getResources().getDisplayMetrics().density;
            int pad = (int) (24 * d);

            EditText input = new EditText(requireContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setSingleLine(true);
            input.setHint(R.string.translation_hint);
            input.setText(translationView.getText());
            input.setSelection(input.getText().length());

            FrameLayout container = new FrameLayout(requireContext());
            container.setPadding(pad, (int) (8 * d), pad, 0);
            container.addView(input);

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.edit_translation_title)
                    .setView(container)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.save, (dialog, which) -> {
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
        });

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
}