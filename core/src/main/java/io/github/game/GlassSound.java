package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/** Sintetiza em código o som de vidro temperado estilhaçando (dispensa arquivos .wav). */
public final class GlassSound {

    private static final int SAMPLE_RATE = 44100;

    private GlassSound() { }

    /** Gera o som, grava num .wav temporário e o carrega; retorna null se não houver áudio. */
    public static Sound createShatter() {
        return load("tetris_glass", synthesize(new Random(7)));
    }

    /** Carrega amostras PCM como Sound; retorna null se não houver áudio. */
    static Sound load(String name, short[] samples) {
        try {
            File file = writeTempWav(name, samples);
            return Gdx.audio.newSound(Gdx.files.absolute(file.getAbsolutePath()));
        } catch (Exception e) {
            Gdx.app.error("GlassSound", "Nao foi possivel criar o som", e);
            return null;
        }
    }

    static File writeTempWav(String name, short[] samples) throws IOException {
        File file = File.createTempFile(name, ".wav");
        file.deleteOnExit();
        writeWav(file, samples);
        return file;
    }

    private static short[] synthesize(Random random) {
        int length = (int) (SAMPLE_RATE * 1.3f);
        float[] out = new float[length];

        // 1) Estalo inicial + chiado de vidro (ruído com passa-altas simples)
        float prev = 0f;
        for (int i = 0; i < length; i++) {
            float t = i / (float) SAMPLE_RATE;
            float noise = random.nextFloat() * 2f - 1f;
            float high = noise - prev;
            prev = noise;
            float env = (float) (0.9 * Math.exp(-t * 90) + 0.25 * Math.exp(-t * 7));
            out[i] += high * env * 0.5f;
        }

        // 2) Baque grave do impacto
        for (int i = 0; i < length; i++) {
            float t = i / (float) SAMPLE_RATE;
            out[i] += (float) (Math.sin(2 * Math.PI * 70 * t) * Math.exp(-t * 28) * 0.35);
        }

        // 3) Tilintar dos pedacinhos caindo (muitos "pings" agudos e curtos)
        int pings = 140;
        for (int p = 0; p < pings; p++) {
            float start = 0.01f + (float) (-Math.log(1 - random.nextFloat() * 0.98f) * 0.22f);
            if (start > 1.1f) continue;
            float freq = 2200f + random.nextFloat() * 7000f;
            float decay = 30f + random.nextFloat() * 50f;
            float amp = (0.08f + random.nextFloat() * 0.22f) * (1.2f - start);
            int s0 = (int) (start * SAMPLE_RATE);
            int dur = (int) (SAMPLE_RATE * 0.12f);
            for (int k = 0; k < dur && s0 + k < length; k++) {
                float t = k / (float) SAMPLE_RATE;
                float env = (float) Math.exp(-t * decay);
                double ph = 2 * Math.PI * freq * t;
                out[s0 + k] += amp * env * (float) (Math.sin(ph) + 0.5 * Math.sin(ph * 2.76));
            }
        }

        return toPcm(out, SAMPLE_RATE / 10);
    }

    /** Normaliza as amostras e aplica fade-out nas últimas {@code fade} amostras. */
    static short[] toPcm(float[] out, int fade) {
        int length = out.length;
        float peak = 0f;
        for (float v : out) peak = Math.max(peak, Math.abs(v));
        float gain = peak > 0 ? 0.9f / peak : 1f;
        short[] pcm = new short[length];
        for (int i = 0; i < length; i++) {
            float v = out[i] * gain;
            if (i > length - fade) v *= (length - i) / (float) fade;
            pcm[i] = (short) (Math.max(-1f, Math.min(1f, v)) * 32767);
        }
        return pcm;
    }

    private static void writeWav(File file, short[] samples) throws IOException {
        int dataSize = samples.length * 2;
        byte[] bytes = new byte[44 + dataSize];
        writeAscii(bytes, 0, "RIFF");
        writeInt(bytes, 4, 36 + dataSize);
        writeAscii(bytes, 8, "WAVE");
        writeAscii(bytes, 12, "fmt ");
        writeInt(bytes, 16, 16);
        writeShort(bytes, 20, 1);                // PCM
        writeShort(bytes, 22, 1);                // mono
        writeInt(bytes, 24, SAMPLE_RATE);
        writeInt(bytes, 28, SAMPLE_RATE * 2);    // byte rate
        writeShort(bytes, 32, 2);                // block align
        writeShort(bytes, 34, 16);               // bits por amostra
        writeAscii(bytes, 36, "data");
        writeInt(bytes, 40, dataSize);
        for (int i = 0; i < samples.length; i++) writeShort(bytes, 44 + i * 2, samples[i]);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
        }
    }

    private static void writeAscii(byte[] b, int off, String s) {
        for (int i = 0; i < s.length(); i++) b[off + i] = (byte) s.charAt(i);
    }

    private static void writeInt(byte[] b, int off, int v) {
        b[off] = (byte) v;
        b[off + 1] = (byte) (v >> 8);
        b[off + 2] = (byte) (v >> 16);
        b[off + 3] = (byte) (v >> 24);
    }

    private static void writeShort(byte[] b, int off, int v) {
        b[off] = (byte) v;
        b[off + 1] = (byte) (v >> 8);
    }
}
