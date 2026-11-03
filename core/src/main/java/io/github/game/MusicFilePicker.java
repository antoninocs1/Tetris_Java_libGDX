package io.github.game;

import java.io.File;
import java.util.function.Consumer;

/** Seletor de arquivo de música, implementado por cada plataforma. */
public interface MusicFilePicker {

    /**
     * Abre o seletor sem bloquear o jogo. O resultado chega em outra thread:
     * o arquivo escolhido, ou null se o jogador cancelar.
     */
    void pick(Consumer<File> onResult);
}
