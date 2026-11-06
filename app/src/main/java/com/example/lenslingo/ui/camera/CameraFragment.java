package com.example.lenslingo.ui.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.lenslingo.R;
import com.example.lenslingo.ui.shared.SharedOcrViewModel;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraFragment extends Fragment {

    private CameraViewModel viewModel;
    private PreviewView previewView;
    private ProgressBar progressBar;
    private SharedOcrViewModel sharedViewModel;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CameraViewModel.class);
        previewView = view.findViewById(R.id.preview_view);
        progressBar = view.findViewById(R.id.progress_bar);
        cameraExecutor = Executors.newSingleThreadExecutor();
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedOcrViewModel.class);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) startCamera();
                    else Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_LONG).show();
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) viewModel.processImage(uri);
                });

        view.findViewById(R.id.button_capture).setOnClickListener(v -> takePhoto());
        view.findViewById(R.id.button_gallery).setOnClickListener(v -> galleryLauncher.launch("image/*"));

        viewModel.getIsProcessing().observe(getViewLifecycleOwner(), processing ->
                progressBar.setVisibility(Boolean.TRUE.equals(processing) ? View.VISIBLE : View.GONE));

        viewModel.getOcrResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                sharedViewModel.setResult(result);
                viewModel.consumeResult();
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new com.example.lenslingo.ui.result.ResultFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.button_home).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        checkPermissionAndStartCamera();
    }

    private void checkPermissionAndStartCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());

        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, selector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(requireContext(), "Could not start camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg";
        File photoFile = new File(requireContext().getCacheDir(), fileName);

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = Uri.fromFile(photoFile);
                        requireActivity().runOnUiThread(() -> viewModel.processImage(savedUri));
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Failed to capture photo", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
    }
}