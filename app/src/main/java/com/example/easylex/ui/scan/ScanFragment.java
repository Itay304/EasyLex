package com.example.easylex.ui.scan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.easylex.R;
import com.example.easylex.data.Word;
import com.example.easylex.ui.mywords.MyWordsViewModel;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * =====================================================================
 * ScanFragment — מסך הסריקה (OCR + תרגום + שמירה)
 * =====================================================================
 *
 * מה עושה מסך זה?
 * ----------------
 * מאפשר למשתמש לצלם עמוד טקסט ולקבל רשימת מילים אנגליות מתורגמות.
 * זרימה מלאה: צילום → OCR → תיקון סיבוב → זיהוי מילים → תרגום → שמירה.
 *
 * שני מצבי סריקה:
 * ---------------
 * ● ספר (Book mode) — OCR ברמת Element: מזהה מילים בודדות עם סוגריים (n) (v)
 * ● טקסט חופשי (Free Text) — OCR ברמת Line: מזהה שורות שלמות
 *
 * שני מצבי מסך:
 * --------------
 * ● CAPTURE — תצוגת מצלמה חיה + כפתור "סרוק"
 * ● REVIEW  — תמונה + רשימת מילים + כפתורי "שמור" / "סרוק שוב"
 *
 * תיקון סיבוב תמונה:
 * -------------------
 * CameraX שומר תמונה לפי כיוון המכשיר.
 * loadRotatedBitmap() קורא את ה-EXIF tag ומסובב את ה-Bitmap בהתאם
 * לפני שמעבירים ל-OCR — כדי שה-ML Kit יקבל תמונה ישרה.
 *
 * תרגום offline:
 * ---------------
 * ML Kit Translate מוריד מודל אנגלית→עברית (~30MB) פעם אחת.
 * לאחר מכן — כל התרגומים מתבצעים על המכשיר עצמו, ללא אינטרנט.
 *
 * שמירת מילים:
 * ------------
 * כל מילה נשמרת עם isVerified=false (מילה אישית) ו-isFavorite=true.
 *
 * מחלקות שמשתמשות בה:
 * ---------------------
 * MyWordsViewModel — לשמירת המילים
 * GraphicOverlay   — לציור מלבנים על התמונה
 */
public class ScanFragment extends Fragment {

    private static final String TAG = "ScanFragment";
    private static final Pattern HEBREW_PATTERN = Pattern.compile("[\u0590-\u05FF]");

    // ── Views ──────────────────────────────────────────────────────────────────
    private PreviewView viewFinder;
    private ImageView imageResult;
    private GraphicOverlay graphicOverlay;
    private View imageContainer;
    private TextView textTitle;
    private View captureButtons;
    private View reviewButtons;

    // ── Camera / OCR ──────────────────────────────────────────────────────────
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private TextRecognizer textRecognizer;

    // ── State ─────────────────────────────────────────────────────────────────
    private MyWordsViewModel viewModel;
    private List<Word> localWordsList = new ArrayList<>();
    private TextToSpeech tts;
    private File currentPhotoFile;
    private boolean isFreeTextMode = false;

    // ── Launchers ──────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startCamera();
            });

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null || !isAdded()) return;
                showReviewMode();
                imageResult.setImageURI(uri);

                final android.content.Context appCtx = requireContext().getApplicationContext();
                final File tempFile = new File(appCtx.getExternalCacheDir(),
                        "gallery_" + System.currentTimeMillis() + ".jpg");

                cameraExecutor.execute(() -> {
                    try {
                        try (InputStream is = appCtx.getContentResolver().openInputStream(uri);
                             OutputStream os = new FileOutputStream(tempFile)) {
                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = is.read(buf)) != -1) os.write(buf, 0, len);
                        }
                        if (!isAdded()) return;
                        currentPhotoFile = tempFile;
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            try {
                                recognizeText(InputImage.fromFilePath(requireContext(), Uri.fromFile(tempFile)));
                            } catch (IOException e) { Log.e(TAG, "Gallery OCR failed", e); }
                        });
                    } catch (IOException e) { Log.e(TAG, "Gallery copy failed", e); }
                });
            });

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_scan, container, false);

        viewFinder      = root.findViewById(R.id.viewFinder);
        imageResult     = root.findViewById(R.id.imageResult);
        graphicOverlay  = root.findViewById(R.id.graphicOverlay);
        imageContainer  = root.findViewById(R.id.imageContainer);
        textTitle       = root.findViewById(R.id.textTitle);
        captureButtons  = root.findViewById(R.id.captureButtons);
        reviewButtons   = root.findViewById(R.id.reviewButtons);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        cameraExecutor = Executors.newSingleThreadExecutor();

        tts = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US);
        });

        viewModel = new ViewModelProvider(this).get(MyWordsViewModel.class);
        viewModel.getAllWords().observe(getViewLifecycleOwner(), words -> {
            if (words != null) this.localWordsList = words;
        });

        root.findViewById(R.id.button_camera).setOnClickListener(v -> takePhoto());
        root.findViewById(R.id.button_gallery).setOnClickListener(v ->
                pickImageLauncher.launch("image/*"));
        root.findViewById(R.id.button_retake).setOnClickListener(v -> {
            showCaptureMode();
            startCamera();
        });

        MaterialButtonToggleGroup toggle = root.findViewById(R.id.toggleScanMode);
        toggle.check(R.id.btnBookScan);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) isFreeTextMode = (checkedId == R.id.btnFreeText);
        });

        setupOverlayClick();
        checkCameraPermission();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
        textRecognizer.close();
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
    }

    // ── Mode switching ─────────────────────────────────────────────────────────

    private void showCaptureMode() {
        viewFinder.setVisibility(View.VISIBLE);
        textTitle.setVisibility(View.VISIBLE);
        imageContainer.setVisibility(View.GONE);
        captureButtons.setVisibility(View.VISIBLE);
        reviewButtons.setVisibility(View.GONE);
        graphicOverlay.setWordBoxes(new ArrayList<>());
    }

    private void showReviewMode() {
        viewFinder.setVisibility(View.GONE);
        textTitle.setVisibility(View.GONE);
        imageContainer.setVisibility(View.VISIBLE);
        captureButtons.setVisibility(View.GONE);
        reviewButtons.setVisibility(View.VISIBLE);
    }

    // ── Touch ─────────────────────────────────────────────────────────────────

    private void setupOverlayClick() {
        graphicOverlay.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                float x = event.getX(), y = event.getY();
                for (GraphicOverlay.WordBox box : graphicOverlay.getWordBoxes()) {
                    if (graphicOverlay.getScaledRect(box.rect).contains(x, y)) {
                        handleWordClick(box);
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void handleWordClick(GraphicOverlay.WordBox box) {
        if (box.existsInDb) {
            tts.speak(box.text, TextToSpeech.QUEUE_FLUSH, null, null);
            // Find the word in the local list
            Word found = null;
            for (Word w : localWordsList) {
                if (box.text.equalsIgnoreCase(w.getEnglishWord())) { found = w; break; }
            }
            if (found == null) {
                Toast.makeText(getContext(), box.text + " קיים במאגר", Toast.LENGTH_SHORT).show();
                return;
            }
            if (found.isFavorite()) {
                Toast.makeText(getContext(), box.text + " כבר ברשימה האישית", Toast.LENGTH_SHORT).show();
                return;
            }
            final Word wordToAdd = found;
            new AlertDialog.Builder(requireContext())
                    .setTitle(box.text)
                    .setMessage("המילה קיימת במאגר. להוסיף לרשימה האישית שלך?")
                    .setPositiveButton("הוסף", (d, w) -> {
                        wordToAdd.setFavorite(true);
                        viewModel.update(wordToAdd);
                        Toast.makeText(getContext(), "נוסף לרשימה האישית!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("ביטול", null)
                    .show();
        } else {
            showAddDialogWithTranslation(box.text, box.pos);
        }
    }

    private void showAddDialogWithTranslation(String english, String detectedPos) {
        Translator translator = Translation.getClient(new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.HEBREW)
                .build());
        translator.downloadModelIfNeeded()
                .addOnSuccessListener(unused ->
                    translator.translate(english)
                        .addOnSuccessListener(hebrew -> {
                            if (isAdded()) showAddDialog(english, detectedPos, hebrew);
                            translator.close();
                        })
                        .addOnFailureListener(e -> {
                            if (isAdded()) showAddDialog(english, detectedPos, "");
                            translator.close();
                        })
                )
                .addOnFailureListener(e -> {
                    if (isAdded()) showAddDialog(english, detectedPos, "");
                    translator.close();
                });
    }

    private void showAddDialog(String english, String detectedPos, String suggestedHebrew) {
        View v = getLayoutInflater().inflate(R.layout.dialog_add_word, null);

        TextInputEditText etEnglish = v.findViewById(R.id.etEnglish);
        TextInputEditText etHeb     = v.findViewById(R.id.etHebrew);
        AutoCompleteTextView actvTag = v.findViewById(R.id.actvTag);
        AutoCompleteTextView actvPos = v.findViewById(R.id.actvPos);

        etEnglish.setText(english);
        if (suggestedHebrew != null && !suggestedHebrew.isEmpty()) etHeb.setText(suggestedHebrew);

        // Collect existing tags from word list
        Set<String> tagSet = new HashSet<>();
        for (Word w : localWordsList) {
            if (w.getTags() != null && !w.getTags().isEmpty()) tagSet.add(w.getTags());
        }
        ArrayAdapter<String> tagAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(tagSet));
        actvTag.setAdapter(tagAdapter);
        actvTag.setOnClickListener(vv -> actvTag.showDropDown());

        // POS options
        final String[] posDisplay = {
                "שם עצם (n)", "פועל (v)", "תואר (adj)", "תואר פועל (adv)",
                "מילת יחס (prep)", "כינוי (pron)", "אחר"
        };
        final String[] posCodes = {"n", "v", "adj", "adv", "prep", "pron", ""};
        ArrayAdapter<String> posAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, posDisplay);
        actvPos.setAdapter(posAdapter);
        actvPos.setOnClickListener(vv -> actvPos.showDropDown());

        // Pre-select detected POS
        if (detectedPos != null && !detectedPos.isEmpty()) {
            for (int i = 0; i < posCodes.length; i++) {
                if (posCodes[i].equals(detectedPos)) {
                    actvPos.setText(posDisplay[i], false);
                    break;
                }
            }
        }

        new AlertDialog.Builder(requireContext())
                .setView(v)
                .setPositiveButton("שמור", (d, w) -> {
                    String engText    = etEnglish.getText() != null ? etEnglish.getText().toString().trim() : english;
                    String hebrewText = etHeb.getText() != null ? etHeb.getText().toString() : "";
                    String tagText    = actvTag.getText() != null ? actvTag.getText().toString() : "";
                    String posText    = actvPos.getText() != null ? actvPos.getText().toString() : "";
                    String resolvedPos = (detectedPos != null && !detectedPos.isEmpty()) ? detectedPos : "n";
                    for (int i = 0; i < posDisplay.length; i++) {
                        if (posDisplay[i].equals(posText)) { resolvedPos = posCodes[i]; break; }
                    }
                    if (engText.isEmpty()) {
                        Toast.makeText(getContext(), "יש להזין מילה באנגלית", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Word newWord = new Word(engText, hebrewText, resolvedPos, "", "", 1,
                            System.currentTimeMillis());
                    newWord.setTags(tagText);
                    newWord.setFavorite(true);
                    newWord.setVerified(false); // always personal when added from scan
                    viewModel.insert(newWord);
                    Toast.makeText(getContext(), "נשמר ברשימה האישית!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)
            startCamera();
        else
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder().build();
                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
            } catch (Exception e) { Log.e(TAG, "Camera failed", e); }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) return;
        File file = new File(requireContext().getExternalCacheDir(),
                System.currentTimeMillis() + ".jpg");
        currentPhotoFile = file;
        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                        showReviewMode();
                        imageResult.setImageURI(Uri.fromFile(file));
                        try {
                            recognizeText(InputImage.fromFilePath(requireContext(), Uri.fromFile(file)));
                        } catch (IOException e) { Log.e(TAG, "Failed to load image", e); }
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        Log.e(TAG, "Photo capture failed", e);
                    }
                });
    }

    // ── OCR + smart crop ──────────────────────────────────────────────────────

    private void recognizeText(InputImage image) {
        final boolean freeText = isFreeTextMode;
        textRecognizer.process(image).addOnSuccessListener(result -> {
            if (!isAdded()) return;

            final List<GraphicOverlay.WordBox> rawBoxes;
            final Rect finalPageBounds;

            if (freeText) {
                rawBoxes = extractFreeTextBoxes(result);
                finalPageBounds = null;
            } else {
                // Book scan: compute pageBounds from bullet-containing blocks
                Rect pageBounds = null;
                for (Text.TextBlock block : result.getTextBlocks()) {
                    if (!blockHasBulletLine(block)) continue;
                    Rect bb = block.getBoundingBox();
                    if (bb != null) {
                        if (pageBounds == null) pageBounds = new Rect(bb);
                        else pageBounds.union(bb);
                    }
                }
                if (pageBounds == null) {
                    for (Text.TextBlock block : result.getTextBlocks()) {
                        Rect bb = block.getBoundingBox();
                        if (bb != null) {
                            if (pageBounds == null) pageBounds = new Rect(bb);
                            else pageBounds.union(bb);
                        }
                    }
                }
                rawBoxes = extractWordBoxes(result);
                finalPageBounds = pageBounds;
            }

            final File photoFile = currentPhotoFile;

            cameraExecutor.execute(() -> {
                Bitmap cropped = null;
                int cropLeft = 0, cropTop = 0;
                try {
                    Bitmap rotated = loadRotatedBitmap(photoFile);
                    if (rotated == null) return;

                    if (!freeText && finalPageBounds != null && !finalPageBounds.isEmpty()) {
                        int padX = (int)(finalPageBounds.width()  * 0.03f);
                        int padY = (int)(finalPageBounds.height() * 0.03f);
                        cropLeft = Math.max(0, finalPageBounds.left   - padX);
                        cropTop  = Math.max(0, finalPageBounds.top    - padY);
                        int cropRight  = Math.min(rotated.getWidth(),  finalPageBounds.right  + padX);
                        int cropBottom = Math.min(rotated.getHeight(), finalPageBounds.bottom + padY);
                        int cropW = cropRight - cropLeft;
                        int cropH = cropBottom - cropTop;
                        if (cropW > 0 && cropH > 0) {
                            cropped = Bitmap.createBitmap(rotated, cropLeft, cropTop, cropW, cropH);
                            rotated.recycle();
                        } else {
                            cropped = rotated;
                        }
                    } else {
                        cropped = rotated;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Bitmap crop failed", e);
                    return;
                }

                if (!isAdded()) { cropped.recycle(); return; }

                final int cl = cropLeft, ct = cropTop;
                List<GraphicOverlay.WordBox> adjustedBoxes = new ArrayList<>();
                for (GraphicOverlay.WordBox box : rawBoxes) {
                    if (box.rect == null) continue;
                    Rect adj = new Rect(box.rect);
                    adj.offset(-cl, -ct);
                    adjustedBoxes.add(new GraphicOverlay.WordBox(adj, box.existsInDb, box.text, box.pos));
                }

                final Bitmap finalCropped = cropped;
                final List<GraphicOverlay.WordBox> finalBoxes = adjustedBoxes;
                if (!isAdded()) { finalCropped.recycle(); return; }
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) { finalCropped.recycle(); return; }
                    imageResult.setImageBitmap(finalCropped);
                    graphicOverlay.setImageSourceInfo(finalCropped.getWidth(), finalCropped.getHeight());
                    graphicOverlay.setWordBoxes(finalBoxes);
                });
            });
        });
    }

    /** Returns true if the block contains at least one line with a bullet character. */
    private boolean blockHasBulletLine(Text.TextBlock block) {
        for (Text.Line line : block.getLines()) {
            String t = line.getText();
            if (t.contains("●") || t.contains("•")) return true;
        }
        return false;
    }

    /** Extracts WordBoxes from book-scan OCR result (bullet-pattern lines only). */
    private List<GraphicOverlay.WordBox> extractWordBoxes(Text result) {
        List<GraphicOverlay.WordBox> boxes = new ArrayList<>();
        for (Text.TextBlock block : result.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                String lineText = line.getText();

                if (HEBREW_PATTERN.matcher(lineText).find()) continue;

                boolean hasBullet = lineText.contains("●") || lineText.contains("•");
                if (!hasBullet) continue;

                int bulletIdx = lineText.indexOf('●');
                if (bulletIdx == -1) bulletIdx = lineText.indexOf('•');

                int parenIdx = lineText.indexOf('(', bulletIdx + 1);
                int colonIdx = lineText.indexOf(':', bulletIdx + 1);
                if (parenIdx == -1 && colonIdx == -1) continue;

                int endIdx;
                if (parenIdx == -1)      endIdx = colonIdx;
                else if (colonIdx == -1) endIdx = parenIdx;
                else                     endIdx = Math.min(parenIdx, colonIdx);

                String targetPhrase = lineText.substring(bulletIdx + 1, endIdx).trim();
                if (targetPhrase.isEmpty()) continue;

                String pos = "";
                if (parenIdx != -1 && (colonIdx == -1 || parenIdx < colonIdx)) {
                    int closeParen = lineText.indexOf(')', parenIdx + 1);
                    if (closeParen != -1) pos = lineText.substring(parenIdx + 1, closeParen).trim();
                }

                boolean exists = false;
                for (Word w : localWordsList) {
                    if (targetPhrase.equalsIgnoreCase(w.getEnglishWord())) { exists = true; break; }
                }

                Rect bounds = extractTargetBounds(line, targetPhrase);
                if (bounds == null) bounds = line.getBoundingBox();
                if (bounds != null) boxes.add(new GraphicOverlay.WordBox(bounds, exists, targetPhrase, pos));
            }
        }
        return boxes;
    }

    /** Extracts all English word-level boxes (free text mode). */
    private List<GraphicOverlay.WordBox> extractFreeTextBoxes(Text result) {
        List<GraphicOverlay.WordBox> boxes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Text.TextBlock block : result.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                if (HEBREW_PATTERN.matcher(line.getText()).find()) continue;
                for (Text.Element element : line.getElements()) {
                    String raw = element.getText().replaceAll("[^a-zA-Z]", "").trim();
                    if (raw.length() < 3) continue;
                    String lower = raw.toLowerCase(Locale.US);
                    if (!seen.add(lower)) continue;
                    boolean exists = false;
                    for (Word w : localWordsList) {
                        if (lower.equals(w.getEnglishWord().toLowerCase(Locale.US))) {
                            exists = true;
                            break;
                        }
                    }
                    Rect bounds = element.getBoundingBox();
                    if (bounds != null) {
                        boxes.add(new GraphicOverlay.WordBox(bounds, exists, raw, ""));
                    }
                }
            }
        }
        return boxes;
    }

    @Nullable
    private Bitmap loadRotatedBitmap(File file) throws IOException {
        if (file == null || !file.exists()) return null;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmap == null) return null;
        ExifInterface exif = new ExifInterface(file.getAbsolutePath());
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:  matrix.postRotate(90);  break;
            case ExifInterface.ORIENTATION_ROTATE_180: matrix.postRotate(180); break;
            case ExifInterface.ORIENTATION_ROTATE_270: matrix.postRotate(270); break;
            default: return bitmap;
        }
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        return rotated;
    }

    @Nullable
    private Rect extractTargetBounds(Text.Line line, String targetPhrase) {
        List<Text.Element> elements = line.getElements();
        if (elements == null || elements.isEmpty()) return null;

        Rect combined = null;
        boolean pastBullet = false;

        for (Text.Element element : elements) {
            String t = element.getText().trim();
            if (t.isEmpty()) continue;
            if (t.equals("●") || t.equals("•")) { pastBullet = true; continue; }
            if (t.startsWith("(") || t.startsWith(":") || t.endsWith(":")) break;
            if (pastBullet && element.getBoundingBox() != null) {
                if (combined == null) combined = new Rect(element.getBoundingBox());
                else combined.union(element.getBoundingBox());
            }
        }

        if (combined == null) {
            String lowerTarget = targetPhrase.toLowerCase(Locale.US);
            for (Text.Element element : elements) {
                String cleaned = element.getText()
                        .replaceAll("[^a-zA-Z\\s]", "").trim().toLowerCase(Locale.US);
                if (!cleaned.isEmpty() && lowerTarget.contains(cleaned)
                        && element.getBoundingBox() != null) {
                    if (combined == null) combined = new Rect(element.getBoundingBox());
                    else combined.union(element.getBoundingBox());
                }
            }
        }
        return combined;
    }
}
