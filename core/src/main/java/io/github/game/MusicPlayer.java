package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/** Música de fundo sintetizada em código, em laço, com troca de faixa e volume. */
public class MusicPlayer {

    private static final int SAMPLE_RATE = 44100;
    private static final int VOLUME_STEP = 5;
    private static final String[] TRACK_NAMES = {"Korobeiniki", "Neon", "Arcade", "Espaço"};
    /** Tamanho máximo do nome exibido no painel para o arquivo escolhido pelo jogador. */
    private static final int MAX_NAME_LENGTH = 14;

    // Escolhas do jogador, salvas entre uma execução e outra
    private static final String PREFS_NAME = "tetris-music";
    private static final String KEY_TRACK = "track";
    private static final String KEY_CUSTOM_FILE = "customFile";
    private static final String KEY_VOLUME = "volume";
    private static final String KEY_ENABLED = "enabled";
    private final Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);

    /** Faixas já geradas: cada uma é sintetizada só na primeira vez em que é escolhida. */
    private final File[] trackFiles = new File[TRACK_NAMES.length];
    /** Arquivo escolhido pelo jogador; entra no ciclo de faixas logo depois das sintetizadas. */
    private File customFile;
    private Music music;
    private int track;
    private int volumePercent = 50;
    private boolean enabled = true;
    private boolean suspended = true;

    public MusicPlayer() {
        loadPreferences();
        loadTrack();
    }

    public void toggle() {
        enabled = !enabled;
        applyPlayState();
        savePreferences();
    }

    public void nextTrack() {
        int trackCount = TRACK_NAMES.length + (customFile != null ? 1 : 0);
        track = (track + 1) % trackCount;
        loadTrack();
        savePreferences();
    }

    /** Passa a tocar o arquivo escolhido; se ele não puder ser aberto, mantém a faixa atual e retorna false. */
    public boolean playFile(File file) {
        Music loaded;
        try {
            loaded = Gdx.audio.newMusic(Gdx.files.absolute(file.getAbsolutePath()));
        } catch (Exception e) {
            Gdx.app.error("MusicPlayer", "Nao foi possivel abrir " + file, e);
            return false;
        }
        dispose();
        customFile = file;
        track = TRACK_NAMES.length;
        startMusic(loaded);
        savePreferences();
        return true;
    }

    public void volumeUp() {
        setVolume(volumePercent + VOLUME_STEP);
        savePreferences();
    }

    public void volumeDown() {
        setVolume(volumePercent - VOLUME_STEP);
        savePreferences();
    }

    /** Silencia temporariamente (jogo sem partida em andamento) sem alterar a escolha do jogador. */
    public void setSuspended(boolean suspended) {
        if (this.suspended == suspended) return;
        this.suspended = suspended;
        applyPlayState();
    }

    public boolean isEnabled() { return enabled; }
    public int getVolumePercent() { return volumePercent; }
    public String getTrackName() {
        if (track < TRACK_NAMES.length) return TRACK_NAMES[track];
        String name = customFile.getName().replaceFirst("\\.[^.]*$", "");
        return name.length() <= MAX_NAME_LENGTH ? name : name.substring(0, MAX_NAME_LENGTH - 3) + "...";
    }

    public void dispose() {
        if (music != null) music.dispose();
        music = null;
    }

    private void setVolume(int percent) {
        volumePercent = Math.max(0, Math.min(100, percent));
        if (music != null) music.setVolume(volumePercent / 100f);
    }

    private void applyPlayState() {
        if (music == null) return;
        boolean shouldPlay = enabled && !suspended;
        if (shouldPlay && !music.isPlaying()) music.play();
        else if (!shouldPlay && music.isPlaying()) music.pause();
    }

    /** Recupera a última escolha; um MP3 que não existe mais é esquecido. */
    private void loadPreferences() {
        volumePercent = Math.max(0, Math.min(100, prefs.getInteger(KEY_VOLUME, volumePercent)));
        enabled = prefs.getBoolean(KEY_ENABLED, enabled);
        String path = prefs.getString(KEY_CUSTOM_FILE, "");
        if (!path.isEmpty() && new File(path).isFile()) customFile = new File(path);
        int trackCount = TRACK_NAMES.length + (customFile != null ? 1 : 0);
        track = prefs.getInteger(KEY_TRACK, 0);
        if (track < 0 || track >= trackCount) track = 0;
    }

    private void savePreferences() {
        prefs.putInteger(KEY_TRACK, track);
        prefs.putString(KEY_CUSTOM_FILE, customFile != null ? customFile.getAbsolutePath() : "");
        prefs.putInteger(KEY_VOLUME, volumePercent);
        prefs.putBoolean(KEY_ENABLED, enabled);
        prefs.flush();
    }

    private void loadTrack() {
        dispose();
        try {
            startMusic(Gdx.audio.newMusic(Gdx.files.absolute(trackFile().getAbsolutePath())));
        } catch (Exception e) {
            Gdx.app.error("MusicPlayer", "Nao foi possivel carregar a musica", e);
            if (track == TRACK_NAMES.length) {
                // MP3 salvo não abre mais: volta para a primeira faixa sintetizada
                customFile = null;
                track = 0;
                savePreferences();
                loadTrack();
            }
        }
    }

    private File trackFile() throws IOException {
        if (track == TRACK_NAMES.length) return customFile;
        if (trackFiles[track] == null) {
            trackFiles[track] = GlassSound.writeTempWav("tetris_music", GlassSound.toPcm(render(track), 0));
        }
        return trackFiles[track];
    }

    private void startMusic(Music loaded) {
        music = loaded;
        music.setLooping(true);
        setVolume(volumePercent);
        applyPlayState();
    }

    private static float[] render(int track) {
        switch (track) {
            case 0:
                return renderKorobeiniki();
            case 1:
                // Am F C G
                return renderGroove(100, new int[][]{{57, 60, 64}, {53, 57, 60}, {60, 64, 67}, {55, 59, 62}}, 4, 101L,
                    0.10f, 0.32f, 0.13f, 0.20f, 0.30f, 4, 0.5f);
            case 2:
                // C G Am F
                return renderGroove(150, new int[][]{{60, 64, 67}, {55, 59, 62}, {57, 60, 64}, {53, 57, 60}}, 4, 202L,
                    0f, 0.30f, 0.14f, 0.26f, 0.55f, 2, 0.4f);
            default:
                // Dm Bb F C
                return renderGroove(70, new int[][]{{50, 53, 57}, {46, 50, 53}, {53, 57, 60}, {48, 52, 55}}, 2, 303L,
                    0.18f, 0.20f, 0.06f, 0.20f, 0.18f, 2, 0f);
        }
    }

    // =====================================================================
    // COMPOSIÇÕES
    // =====================================================================

    /** "Korobeiniki", canção tradicional russa em domínio público: {nota MIDI, duração em colcheias}. */
    private static float[] renderKorobeiniki() {
        int[][] melody = {
            {76, 2}, {71, 1}, {72, 1}, {74, 2}, {72, 1}, {71, 1},
            {69, 2}, {69, 1}, {72, 1}, {76, 2}, {74, 1}, {72, 1},
            {71, 3}, {72, 1}, {74, 2}, {76, 2},
            {72, 2}, {69, 2}, {69, 4},
            {0, 1}, {74, 2}, {77, 1}, {81, 2}, {79, 1}, {77, 1},
            {76, 3}, {72, 1}, {76, 2}, {74, 1}, {72, 1},
            {71, 2}, {71, 1}, {72, 1}, {74, 2}, {76, 2},
            {72, 2}, {69, 2}, {69, 4},
            {76, 4}, {72, 4}, {74, 4}, {71, 4}, {72, 4}, {69, 4}, {68, 8},
            {76, 4}, {72, 4}, {74, 4}, {71, 4}, {72, 2}, {76, 2}, {81, 4}, {80, 8},
        };
        int[] bassRoots = {40, 45, 40, 45, 38, 36, 43, 45, 45, 40, 45, 40, 45, 40, 45, 40};

        Synth synth = new Synth(150, bassRoots.length * 4);
        int eighth = 0;
        for (int[] note : melody) {
            if (note[0] != 0) synth.note(eighth / 2.0, note[1] / 2.0, note[0], 0.34f, Synth.SQUARE, 2.5);
            eighth += note[1];
        }
        for (int bar = 0; bar < bassRoots.length; bar++) {
            for (int e = 0; e < 8; e++) {
                int midi = bassRoots[bar] + (e % 2 == 0 ? 0 : 12);
                synth.note(bar * 4 + e / 2.0, 0.45, midi, 0.32f, Synth.TRIANGLE, 3);
                synth.hat(bar * 4 + e / 2.0, e % 2 == 0 ? 0.05f : 0.03f);
            }
        }
        return synth.out;
    }

    /** Laço gerado a partir de uma progressão: um acorde por compasso, melodia fixa pela semente. */
    private static float[] renderGroove(int bpm, int[][] chords, int repeats, long seed, float padAmp, float bassAmp,
                                        float arpAmp, float leadAmp, float leadDensity, int arpPerBeat, float drumAmp) {
        Synth synth = new Synth(bpm, chords.length * repeats * 4);
        for (int rep = 0; rep < repeats; rep++) {
            // A mesma semente a cada repetição faz a melodia voltar, criando um refrão
            Random melody = new Random(seed);
            for (int i = 0; i < chords.length; i++) {
                int[] chord = chords[i];
                double bar = (rep * chords.length + i) * 4;

                if (padAmp > 0f) {
                    for (int midi : chord) synth.note(bar, 4, midi, padAmp, Synth.SOFT_SAW, 0.25);
                }
                for (int e = 0; e < 8; e++) {
                    synth.note(bar + e / 2.0, 0.45, chord[0] - 24, bassAmp, Synth.TRIANGLE, 2);
                }
                for (int s = 0; s < 4 * arpPerBeat; s++) {
                    int midi = chord[s % chord.length] + 12;
                    synth.note(bar + s / (double) arpPerBeat, 1.0 / arpPerBeat, midi, arpAmp, Synth.SQUARE, 8);
                }
                for (int e = 0; e < 8; e++) {
                    if (melody.nextFloat() >= leadDensity) continue;
                    int midi = chord[melody.nextInt(chord.length)] + 24;
                    synth.note(bar + e / 2.0, 1.0, midi, leadAmp, Synth.SOFT_SAW, 3);
                }
                if (drumAmp > 0f) {
                    for (int beat = 0; beat < 4; beat++) {
                        synth.kick(bar + beat, drumAmp);
                        if (beat % 2 == 1) synth.snare(bar + beat, drumAmp * 0.6f);
                        synth.hat(bar + beat + 0.5, drumAmp * 0.2f);
                    }
                }
            }
        }
        return synth.out;
    }

    // =====================================================================
    // SINTETIZADOR
    // =====================================================================

    /** Mixa notas e percussão; o que passa do fim volta ao início para o laço ficar sem emenda. */
    private static final class Synth {
        private static final int TABLE_SIZE = 4096;
        static final float[] SQUARE = table(new double[]{1, 0, 1 / 3.0, 0, 1 / 5.0, 0, 1 / 7.0});
        static final float[] TRIANGLE = table(new double[]{1, 0, 1 / 9.0, 0, 1 / 25.0});
        static final float[] SOFT_SAW = table(new double[]{1, 1 / 2.0, 1 / 3.0, 1 / 4.0});

        final float[] out;
        private final double secondsPerBeat;
        private final Random noise = new Random(1);

        Synth(int bpm, int beats) {
            secondsPerBeat = 60.0 / bpm;
            out = new float[(int) (beats * secondsPerBeat * SAMPLE_RATE)];
        }

        private static float[] table(double[] harmonics) {
            float[] table = new float[TABLE_SIZE];
            for (int i = 0; i < TABLE_SIZE; i++) {
                double ph = 2 * Math.PI * i / TABLE_SIZE;
                double v = 0;
                for (int h = 0; h < harmonics.length; h++) v += harmonics[h] * Math.sin(ph * (h + 1));
                table[i] = (float) v;
            }
            return table;
        }

        private int sampleAt(double beat) {
            return (int) (beat * secondsPerBeat * SAMPLE_RATE);
        }

        void note(double beat, double beats, int midi, float amp, float[] wave, double decay) {
            int start = sampleAt(beat);
            int length = sampleAt(beats);
            int release = Math.min(length / 2, SAMPLE_RATE / 50);
            int attack = Math.min(length / 2, SAMPLE_RATE / 200);
            double step = TABLE_SIZE * 440.0 * Math.pow(2, (midi - 69) / 12.0) / SAMPLE_RATE;
            double phase = 0;
            for (int k = 0; k < length; k++) {
                float env = amp * (float) Math.exp(-decay * k / SAMPLE_RATE);
                if (k < attack) env *= k / (float) attack;
                if (k > length - release) env *= (length - k) / (float) release;
                out[(start + k) % out.length] += wave[(int) phase] * env;
                phase += step;
                if (phase >= TABLE_SIZE) phase -= TABLE_SIZE;
            }
        }

        void kick(double beat, float amp) {
            int start = sampleAt(beat);
            int length = SAMPLE_RATE / 6;
            double phase = 0;
            for (int k = 0; k < length; k++) {
                float t = k / (float) SAMPLE_RATE;
                phase += 2 * Math.PI * (50 + 110 * Math.exp(-t * 40)) / SAMPLE_RATE;
                out[(start + k) % out.length] += (float) (Math.sin(phase) * Math.exp(-t * 18)) * amp;
            }
        }

        void snare(double beat, float amp) {
            burst(beat, SAMPLE_RATE / 7, 22, amp);
        }

        void hat(double beat, float amp) {
            burst(beat, SAMPLE_RATE / 25, 90, amp);
        }

        private void burst(double beat, int length, float decay, float amp) {
            int start = sampleAt(beat);
            float previous = 0f;
            for (int k = 0; k < length; k++) {
                float white = noise.nextFloat() * 2f - 1f;
                float env = (float) Math.exp(-decay * k / (float) SAMPLE_RATE);
                out[(start + k) % out.length] += (white - previous) * env * amp;
                previous = white;
            }
        }
    }
}
