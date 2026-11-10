package com.example.lenslingo.ui.dictionary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lenslingo.R;
import com.example.lenslingo.data.local.WordEntity;

public class WordAdapter extends ListAdapter<WordEntity, WordAdapter.WordViewHolder> {

    public interface OnWordDeleteListener {
        void onDelete(WordEntity word);
    }

    private final OnWordDeleteListener deleteListener;

    public WordAdapter(OnWordDeleteListener deleteListener) {
        super(DIFF_CALLBACK);
        this.deleteListener = deleteListener;
    }

    private static final DiffUtil.ItemCallback<WordEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<WordEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull WordEntity oldItem, @NonNull WordEntity newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull WordEntity oldItem, @NonNull WordEntity newItem) {
                    return oldItem.word.equals(newItem.word)
                            && oldItem.translation.equals(newItem.translation)
                            && oldItem.sourceLanguage.equals(newItem.sourceLanguage)
                            && java.util.Objects.equals(oldItem.photoUri, newItem.photoUri);
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
        WordEntity word = getItem(position);
        holder.bind(word, deleteListener);
    }

    static class WordViewHolder extends RecyclerView.ViewHolder {
        private final TextView wordText;
        private final TextView translationText;
        private final TextView languageText;
        private final View deleteButton;
        private final android.widget.ImageView thumb;

        WordViewHolder(@NonNull View itemView) {
            super(itemView);
            wordText = itemView.findViewById(R.id.text_word);
            translationText = itemView.findViewById(R.id.text_translation);
            languageText = itemView.findViewById(R.id.text_language);
            deleteButton = itemView.findViewById(R.id.button_delete);
            thumb = itemView.findViewById(R.id.image_thumb);
        }

        void bind(WordEntity word, OnWordDeleteListener listener) {
            ThumbnailLoader.load(thumb, word.photoUri);
            wordText.setText(word.word);
            translationText.setText(word.translation);
            languageText.setText("auto".equals(word.sourceLanguage)
                    ? "—" : word.sourceLanguage.toUpperCase(java.util.Locale.ROOT));
            deleteButton.setOnClickListener(v -> listener.onDelete(word));
        }
    }
}
