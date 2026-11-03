package io.github.game.lwjgl3;

import io.github.game.MusicFilePicker;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.function.Consumer;

/** Abre a janela de arquivos do sistema em outra thread, para o jogo continuar sendo desenhado. */
public class Lwjgl3MusicFilePicker implements MusicFilePicker {

    @Override
    public void pick(Consumer<File> onResult) {
        Thread thread = new Thread(() -> onResult.accept(showDialog()), "music-file-picker");
        thread.setDaemon(true);
        thread.start();
    }

    private static File showDialog() {
        try {
            FileDialog dialog = new FileDialog((Frame) null, "Escolher música MP3", FileDialog.LOAD);
            dialog.setFile("*.mp3");
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);
            String name = dialog.getFile();
            String directory = dialog.getDirectory();
            dialog.dispose();
            return name == null ? null : new File(directory, name);
        } catch (Exception e) {
            System.err.println("Nao foi possivel abrir o seletor de arquivos: " + e);
            return null;
        }
    }
}
