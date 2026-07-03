package com.filato.campro;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

public class SpeechController {
    public interface Listener {
        void onState(String message);
        void onFinalText(String fullText, String lastPhrase);
        void onPartialText(String text);
        void onUnavailable(String message);
    }

    private final Context context;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private boolean active;
    private String accumulated = "";

    public SpeechController(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public boolean isAvailable() {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }

    public void start(String initialText) {
        if (!isAvailable()) {
            if (listener != null) listener.onUnavailable("Системное распознавание речи недоступно. Используй микрофон клавиатуры в текстовом поле.");
            return;
        }

        accumulated = initialText == null ? "" : initialText.trim();
        active = true;
        ensureRecognizer();
        if (listener != null) listener.onState("Старт диктовки. Говори весь заказ подряд, потом нажми Стоп и разобрать.");
        listenNow();
    }

    public String stop() {
        active = false;
        handler.removeCallbacksAndMessages(null);
        try {
            if (recognizer != null) {
                recognizer.stopListening();
                recognizer.cancel();
            }
        } catch (Throwable ignored) {}
        if (listener != null) listener.onState("Диктовка остановлена.");
        return accumulated == null ? "" : accumulated.trim();
    }

    public void destroy() {
        stop();
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
    }

    private void ensureRecognizer() {
        if (recognizer != null) return;

        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                if (listener != null) listener.onState("Слушаю. Пауза не страшна — распознавание перезапустится.");
            }

            @Override public void onBeginningOfSpeech() {
                if (listener != null) listener.onState("Идет речь...");
            }

            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}

            @Override public void onEndOfSpeech() {
                if (listener != null && active) listener.onState("Пауза. Жду продолжение...");
            }

            @Override public void onError(int error) {
                if (active) {
                    if (listener != null) listener.onState("Пауза / нет фразы. Продолжаю слушать...");
                    listenAgain(450);
                } else if (listener != null) {
                    listener.onState("Диктовка остановлена.");
                }
            }

            @Override public void onResults(Bundle results) {
                String phrase = bestText(results);
                if (phrase.length() > 0) {
                    accumulated = appendUnique(accumulated, phrase);
                    if (listener != null) listener.onFinalText(accumulated, phrase);
                }
                if (active) listenAgain(250);
            }

            @Override public void onPartialResults(Bundle partialResults) {
                String partial = bestText(partialResults);
                if (partial.length() > 0 && listener != null) listener.onPartialText(partial);
            }

            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void listenAgain(int delayMs) {
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (active) listenNow();
            }
        }, delayMs);
    }

    private void listenNow() {
        if (recognizer == null) return;
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 30000);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800);
            recognizer.cancel();
            recognizer.startListening(intent);
        } catch (Throwable throwable) {
            if (listener != null) listener.onState("Ошибка диктовки: " + throwable.getMessage());
        }
    }

    private static String bestText(Bundle bundle) {
        if (bundle == null) return "";
        ArrayList<String> list = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (list == null || list.isEmpty()) return "";
        String value = list.get(0);
        return value == null ? "" : value.trim();
    }

    private static String appendUnique(String base, String add) {
        base = base == null ? "" : base.trim();
        add = add == null ? "" : add.trim();
        if (add.length() == 0) return base;
        if (base.length() == 0) return add;
        if (base.endsWith(add)) return base;

        String lowerBase = base.toLowerCase();
        String lowerAdd = add.toLowerCase();
        if (lowerBase.endsWith(lowerAdd)) return base;

        return base + " " + add;
    }
}
