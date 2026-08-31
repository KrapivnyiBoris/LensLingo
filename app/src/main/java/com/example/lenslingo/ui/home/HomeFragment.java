package com.example.lenslingo.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lenslingo.R;
import com.example.lenslingo.ui.camera.CameraFragment;
import com.example.lenslingo.ui.dictionary.DictionaryFragment;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.button_scan_text).setOnClickListener(v ->
                navigateTo(new CameraFragment()));

        view.findViewById(R.id.button_my_dictionary).setOnClickListener(v ->
                navigateTo(new DictionaryFragment()));

        view.findViewById(R.id.button_stats).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Stats screen not built yet", Toast.LENGTH_SHORT).show());
    }

    private void navigateTo(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}