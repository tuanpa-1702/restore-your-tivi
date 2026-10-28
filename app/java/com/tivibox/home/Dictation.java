package com.tivibox.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

/**
 * Voice typing for the keyboard through the TV's speech recognizer (Google on most boxes). The microphone is the
 * remote's (Bluetooth remotes with a mic button) or any USB/Bluetooth mic. Status lines are Vietnamese for the hint row.
 */
final class Dictation {
    interface Callback {
        /** listening = the mic key is lit; status = text for the hint line, or null to restore the normal hint. */
        void onState(boolean listening, String status);

        void onText(String text);

        void onNeedPermission();
    }

    private final Context context;
    private final Callback callback;
    private SpeechRecognizer recognizer;
    private boolean listening;

    Dictation(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;
    }

    boolean isListening() {
        return listening;
    }

    void start(String language) {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            callback.onState(false, "Cần cho phép dùng micro: chọn \"Cho phép\" trên màn hình vừa mở rồi bấm Nói lại");
            callback.onNeedPermission();
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback.onState(false, "TV không có dịch vụ nhận giọng nói (cần Google hoặc Google Speech Services)");
            return;
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context);
            recognizer.setRecognitionListener(listener);
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        listening = true;
        callback.onState(true, "Đang chuẩn bị micro…");
        recognizer.startListening(intent);
    }

    void stop() {
        if (recognizer != null && listening) recognizer.stopListening();
    }

    void cancel() {
        if (recognizer != null && listening) recognizer.cancel();
        finish(null);
    }

    void destroy() {
        if (recognizer != null) recognizer.destroy();
        recognizer = null;
        listening = false;
    }

    private void finish(String status) {
        listening = false;
        callback.onState(false, status);
    }

    private static String first(Bundle results) {
        ArrayList<String> list = results == null ? null : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        return list == null || list.isEmpty() ? "" : list.get(0).trim();
    }

    private final RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            callback.onState(true, "Đang nghe… nói vào micro trên remote · bấm Nói lần nữa để dừng");
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
            callback.onState(true, "Đang nhận dạng…");
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            String text = first(partialResults);
            if (!text.isEmpty()) callback.onState(true, "“" + text + "”");
        }

        @Override
        public void onResults(Bundle results) {
            String text = first(results);
            finish(text.isEmpty() ? "Không nghe rõ, bấm Nói để thử lại" : null);
            if (!text.isEmpty()) callback.onText(text);
        }

        @Override
        public void onError(int error) {
            finish(message(error));
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    };

    private static String message(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_NO_MATCH:
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Không nghe rõ, bấm Nói để thử lại";
            case SpeechRecognizer.ERROR_AUDIO:
                return "Không mở được micro (remote có micro và đang kết nối Bluetooth không?)";
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
            case SpeechRecognizer.ERROR_SERVER:
                return "Nhận giọng nói cần mạng, kiểm tra kết nối của TV";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Chưa có quyền dùng micro";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Micro đang bận, thử lại sau vài giây";
            case 12: // ERROR_LANGUAGE_NOT_SUPPORTED (API 31)
            case 13: // ERROR_LANGUAGE_UNAVAILABLE (API 31)
                return "Dịch vụ giọng nói của TV chưa có tiếng Việt";
            default:
                return "Lỗi nhận giọng nói (mã " + error + ")";
        }
    }
}
