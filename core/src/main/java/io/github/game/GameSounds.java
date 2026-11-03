package io.github.game;

import com.badlogic.gdx.audio.Sound;

import java.util.Random;

/** Sintetiza em código os efeitos sonoros das peças (dispensa arquivos .wav). */
public final class GameSounds {

    private static final int SAMPLE_RATE = 44100;

    private GameSounds() { }

    /** Aceleração de jato: ruído com filtro abrindo e ronco grave subindo. */
    public static Sound createJet() {
        Random random = new Random(11);
        int length = (int) (SAMPLE_RATE * 0.6f);
        float[] out = new float[length];
        float lowPass = 0f;
        double phase = 0;
        for (int i = 0; i < length; i++) {
            float t = i / (float) SAMPLE_RATE;
            float p = i / (float) length;

            float cutoff = 400f + 7000f * p * p;
            float alpha = 1f - (float) Math.exp(-2 * Math.PI * cutoff / SAMPLE_RATE);
            lowPass += alpha * (random.nextFloat() * 2f - 1f - lowPass);

            phase += 2 * Math.PI * (60f + 180f * p) / SAMPLE_RATE;
            float rumble = (float) (Math.sin(phase) + 0.5 * Math.sin(phase * 2));

            float env = Math.min(1f, t / 0.03f) * (float) Math.sin(Math.PI * Math.pow(p, 0.6));
            out[i] = (lowPass * 1.2f + rumble * 0.3f) * env;
        }
        return GlassSound.load("tetris_jet", GlassSound.toPcm(out, SAMPLE_RATE / 50));
    }

    /** Estalo curto do movimento lateral. */
    public static Sound createMove() {
        int length = (int) (SAMPLE_RATE * 0.04f);
        float[] out = new float[length];
        for (int i = 0; i < length; i++) {
            float t = i / (float) SAMPLE_RATE;
            out[i] = (float) (Math.sin(2 * Math.PI * 900 * t) * Math.exp(-t * 140));
        }
        return GlassSound.load("tetris_move", GlassSound.toPcm(out, SAMPLE_RATE / 500));
    }

    /** Varredura ascendente rápida da rotação. */
    public static Sound createRotate() {
        int length = (int) (SAMPLE_RATE * 0.09f);
        float[] out = new float[length];
        double phase = 0;
        for (int i = 0; i < length; i++) {
            float t = i / (float) SAMPLE_RATE;
            float p = i / (float) length;
            phase += 2 * Math.PI * (500f + 800f * p) / SAMPLE_RATE;
            out[i] = (float) ((Math.sin(phase) + 0.3 * Math.sin(phase * 2)) * Math.exp(-t * 28));
        }
        return GlassSound.load("tetris_rotate", GlassSound.toPcm(out, SAMPLE_RATE / 200));
    }

    /** Bipe eletrônico da peça transparente. */
    public static Sound createBeep() {
        int length = (int) (SAMPLE_RATE * 0.11f);
        float[] out = new float[length];
        for (int i = 0; i < length; i++) {
            float t = i / (float) SAMPLE_RATE;
            double ph = 2 * Math.PI * 1800 * t;
            float attack = Math.min(1f, t / 0.003f);
            out[i] = (float) ((Math.sin(ph) + 0.25 * Math.sin(ph * 3)) * attack);
        }
        return GlassSound.load("tetris_beep", GlassSound.toPcm(out, SAMPLE_RATE / 100));
    }

    /** Arpejo ascendente do início de partida. */
    public static Sound createStart() {
        return createJingle("tetris_start", new int[]{60, 64, 67, 72}, 0.09f, 0.35f);
    }

    /** Fanfarra curta e aguda ao passar de fase. */
    public static Sound createLevelUp() {
        return createJingle("tetris_levelup", new int[]{67, 72, 76, 79, 84}, 0.07f, 0.45f);
    }

    /** Descida lenta e grave do fim de jogo. */
    public static Sound createGameOver() {
        return createJingle("tetris_gameover", new int[]{69, 65, 62, 57, 52}, 0.22f, 0.7f);
    }

    /** Sequência de notas (MIDI) em timbre de fliperama; a última soa por {@code tail} segundos. */
    private static Sound createJingle(String name, int[] notes, float noteSeconds, float tail) {
        int length = (int) (SAMPLE_RATE * (noteSeconds * (notes.length - 1) + tail));
        float[] out = new float[length];
        for (int n = 0; n < notes.length; n++) {
            double freq = 440.0 * Math.pow(2, (notes[n] - 69) / 12.0);
            int start = (int) (SAMPLE_RATE * noteSeconds * n);
            float seconds = n == notes.length - 1 ? tail : noteSeconds * 2.5f;
            int duration = (int) (SAMPLE_RATE * seconds);
            for (int k = 0; k < duration && start + k < length; k++) {
                float t = k / (float) SAMPLE_RATE;
                double ph = 2 * Math.PI * freq * t;
                float attack = Math.min(1f, t / 0.004f);
                float env = attack * (float) Math.exp(-t * 3.5f / seconds);
                out[start + k] += (float) (Math.sin(ph) + Math.sin(ph * 3) / 3 + Math.sin(ph * 5) / 5) * env;
            }
        }
        return GlassSound.load(name, GlassSound.toPcm(out, SAMPLE_RATE / 50));
    }

    /** Pedra quebrando: baque grave, estalo seco e cascalho caindo. */
    public static Sound createStoneBreak() {
        Random random = new Random(23);
        int length = (int) (SAMPLE_RATE * 0.8f);
        float[] out = new float[length];

        for (int i = 0; i < length; i++) {
            float t = i / (float) SAMPLE_RATE;
            float thud = (float) (Math.sin(2 * Math.PI * (45 + 40 * Math.exp(-t * 30)) * t) * Math.exp(-t * 12));
            float crack = (random.nextFloat() * 2f - 1f) * (float) Math.exp(-t * 55);
            out[i] = thud * 0.9f + crack * 0.7f;
        }

        // Cascalho: pequenas rajadas de ruído abafado em instantes aleatórios
        for (int g = 0; g < 70; g++) {
            float start = 0.02f + (float) (-Math.log(1 - random.nextFloat() * 0.98f) * 0.14f);
            int s0 = (int) (start * SAMPLE_RATE);
            int dur = (int) (SAMPLE_RATE * (0.012f + random.nextFloat() * 0.03f));
            float amp = (0.15f + random.nextFloat() * 0.35f) * Math.max(0.1f, 1f - start);
            float alpha = 0.08f + random.nextFloat() * 0.3f;
            float lowPass = 0f;
            for (int k = 0; k < dur && s0 + k < length; k++) {
                lowPass += alpha * (random.nextFloat() * 2f - 1f - lowPass);
                out[s0 + k] += lowPass * amp * 2f * (1f - k / (float) dur);
            }
        }
        return GlassSound.load("tetris_stone", GlassSound.toPcm(out, SAMPLE_RATE / 10));
    }
}
