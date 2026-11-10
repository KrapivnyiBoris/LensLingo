package com.example.lenslingo.ui.dictionary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lenslingo.R;
import com.example.lenslingo.data.local.WordEntity;

import java.util.Locale;
import java.util.Objects;

public class WordAdapter extends ListAdapter<WordEntity, WordAdapter.WordViewHolder> {

    public interface OnWordClickListener {
        void onWordClick(WordEntity word);
    }

    private final OnWordClickListener clickListener;

    public WordAdapter(OnWordClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    private static final DiffUtil.ItemCallback<WordEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<WordEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull WordEntity a, @NonNull WordEntity b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull WordEntity a, @NonNull WordEntity b) {
                    return a.word.equals(b.word)
                            && a.translation.equals(b.translation)
                            && a.sourceLanguage.equals(b.sourceLanguage)
                            && Objects.equals(a.photoUri, b.photoUri);
                }
            };

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_word, parent, false);
        return new WordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener);
    }

    static class WordViewHolder extends RecyclerView.ViewHolder {
        private final TextView wordText;
        private final TextView translationText;
        private final TextView languageText;
        private final ImageView thumb;

        WordViewHolder(@NonNull View itemView) {
            super(itemView);
            wordText = itemView.findViewById(R.id.text_word);
            translationText = itemView.findViewById(R.id.text_translation);
            languageText = itemView.findViewById(R.id.text_language);
            thumb = itemView.findViewById(R.id.image_thumb);
        }

        void bind(WordEntity word, OnWordClickListener listener) {
            wordText.setText(word.word);
            translationText.setText(word.translation);
            languageText.setText("auto".equals(word.sourceLanguage)
                    ? "—" : word.sourceLanguage.toUpperCase(Locale.ROOT));
            ThumbnailLoader.load(thumb, word.photoUri);
            itemView.setOnClickListener(v -> listener.onWordClick(word));
        }
    }
}