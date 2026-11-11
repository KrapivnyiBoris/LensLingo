package com.example.lenslingo.ui.trainer;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.lenslingo.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class PhotoSheet extends BottomSheetDialogFragment {

    private static final String KEY_URI = "uri";

    public static PhotoSheet newInstance(String uri) {
        Bundle b = new Bundle();
        b.putString(KEY_URI, uri);
        PhotoSheet sheet = new PhotoSheet();
        sheet.setArguments(b);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_photo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String uri = requireArguments().getString(KEY_URI);
        if (uri == null) return;
        try {
            ((ImageView) view.findViewById(R.id.image_full)).setImageURI(Uri.parse(uri));
        } catch (Exception ignored) {
        }
    }
}