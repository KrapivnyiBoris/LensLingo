package com.example.lenslingo.ui.result;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lenslingo.R;
import com.example.lenslingo.data.model.OcrWord;

import java.util.ArrayList;
import java.util.List;

public class OcrWordAdapter extends RecyclerView.Adapter<OcrWordAdapter.ViewHolder> {

    public interface OnWordClickListener {
        void onWordClick(OcrWord word);
    }

    private final List<OcrWord> words = new ArrayList<>();
    private final OnWordClickListener listener;

    public OcrWordAdapter(OnWordClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<OcrWord> newWords) {
        words.clear();
        words.addAll(newWords);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ocr_word, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OcrWord word = words.get(position);
        holder.wordText.setText(word.text);
        holder.translationText.setText(
                word.translation != null ? word.translation : "(no translation)");
        holder.itemView.setOnClickListener(v -> listener.onWordClick(word));
    }

    @Override
    public int getItemCount() {
        return words.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView wordText;
        final TextView translationText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            wordText = itemView.findViewById(R.id.text_ocr_word);
            translationText = itemView.findViewById(R.id.text_ocr_translation);
        }
    }
}