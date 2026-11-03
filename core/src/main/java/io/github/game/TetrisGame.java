package io.github.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TetrisGame extends ApplicationAdapter {

    // ---------- Constantes ----------
    public static final int BOARD_COLS = 10;
    public static final int BOARD_ROWS = 20;
    public static final int BLOCK_SIZE = 60;

    // Layout em unidades de mundo; a câmera (FitViewport) ajusta tudo ao tamanho da janela.
    private static final int BOARD_X = 60;
    private static final int BOARD_Y = 60;
    private static final int BOARD_W = BOARD_COLS * BLOCK_SIZE;  // 600
    private static final int BOARD_H = BOARD_ROWS * BLOCK_SIZE;  // 1200
    private static final int PANEL_X = BOARD_X + BOARD_W + 50;   // 710
    private static final int PANEL_W = 520;

    public static final int WORLD_WIDTH = PANEL_X + PANEL_W + 60;  // 1290
    public static final int WORLD_HEIGHT = BOARD_H + 2 * BOARD_Y;  // 1320

    public static final float BASE_DROP_INTERVAL = 1.0f;
    public static final float DROP_SPEED_FACTOR = 0.9f;

    /** Tempo em que as linhas completas ficam rachadas antes de estilhaçar. */
    private static final float CLEAR_DELAY = 0.18f;

    /** Pedra e peça transparente só surgem com a pilha chegando à 5ª linha (índice 4). */
    private static final int SPECIAL_MIN_ROW = 4;
    /** Chance de cada peça especial; cada peça comum tem cerca de 12%. */
    private static final float SPECIAL_PIECE_CHANCE = 0.07f;
    /** Intervalo do bipe enquanto a peça transparente está em jogo. */
    private static final float GHOST_BEEP_INTERVAL = 0.6f;
    /** Quantas peças aparecem na fila de próximas. */
    private static final int NEXT_COUNT = 3;

    // Opções visuais, salvas entre uma execução e outra
    private static final String SETTINGS_NAME = "tetris-settings";
    private static final String KEY_LANDING_PREVIEW = "landingPreview";
    private static final String KEY_FULL_QUEUE = "fullQueue";
    private static final String KEY_WALL_KICK = "wallKick";
    // Recorde: maior pontuação e o nome de quem a fez
    private static final String KEY_HIGH_SCORE = "highScore";
    private static final String KEY_HIGH_SCORE_NAME = "highScoreName";
    private static final int MAX_NAME_LENGTH = 12;

    /** Fim de jogo: intervalo entre uma linha e a seguinte ficar cinza, de baixo para cima. */
    private static final float GAME_OVER_ROW_DELAY = 0.06f;
    /** Tempo que cada linha leva para perder a cor por completo. */
    private static final float GAME_OVER_FADE = 0.25f;
    /** Tempo de surgimento do painel "FIM DO JOGO" depois que o tabuleiro apaga. */
    private static final float GAME_OVER_PANEL_FADE = 0.4f;

    // ---------- Tipos de peça ----------
    public enum TetrominoType {
        I(new int[][]{{1, 1, 1, 1}}, new Color(0.20f, 0.95f, 1.00f, 1f)),
        O(new int[][]{{1, 1}, {1, 1}}, new Color(1.00f, 0.90f, 0.20f, 1f)),
        T(new int[][]{{0, 1, 0}, {1, 1, 1}}, new Color(0.75f, 0.35f, 1.00f, 1f)),
        S(new int[][]{{0, 1, 1}, {1, 1, 0}}, new Color(0.30f, 1.00f, 0.45f, 1f)),
        Z(new int[][]{{1, 1, 0}, {0, 1, 1}}, new Color(1.00f, 0.30f, 0.35f, 1f)),
        J(new int[][]{{1, 0, 0}, {1, 1, 1}}, new Color(0.30f, 0.50f, 1.00f, 1f)),
        L(new int[][]{{0, 0, 1}, {1, 1, 1}}, new Color(1.00f, 0.60f, 0.15f, 1f)),
        /** Pedra: ao pousar, destrói a peça em que bateu e se desfaz junto. */
        STONE(new int[][]{{1}}, new Color(0.56f, 0.46f, 0.36f, 1f)),
        /** Transparente: atravessa os blocos e se encaixa no primeiro buraco da coluna. */
        GHOST(new int[][]{{1}}, new Color(0.80f, 0.95f, 1.00f, 0.45f));

        public final int[][] shape;
        public final Color color;

        TetrominoType(int[][] shape, Color color) {
            this.shape = shape;
            this.color = color;
        }

        public int[][] copyShape() {
            int[][] copy = new int[shape.length][];
            for (int i = 0; i < shape.length; i++) {
                copy[i] = shape[i].clone();
            }
            return copy;
        }
    }

    private static final TetrominoType[] CLASSIC_TYPES = {
        TetrominoType.I, TetrominoType.O, TetrominoType.T, TetrominoType.S,
        TetrominoType.Z, TetrominoType.J, TetrominoType.L
    };

    /** Tom suave do fundo a cada nível (volta ao início depois do último). */
    private static final Color[] LEVEL_TINTS = {
        new Color(0.25f, 0.85f, 1.00f, 1f),  // ciano (o neon original)
        new Color(0.30f, 0.85f, 0.75f, 1f),  // verde-água
        new Color(0.55f, 0.45f, 1.00f, 1f),  // violeta
        new Color(1.00f, 0.50f, 0.75f, 1f),  // rosa
        new Color(1.00f, 0.70f, 0.35f, 1f),  // âmbar
        new Color(0.45f, 0.90f, 0.40f, 1f),  // verde
        new Color(0.95f, 0.45f, 0.40f, 1f),  // coral
    };

    // ---------- Câmera ----------
    private OrthographicCamera camera;
    private FitViewport viewport;
    private float shake;
    /** Cor atual do fundo; caminha suavemente até o tom do nível. */
    private final Color backgroundTint = new Color(LEVEL_TINTS[0]);

    // ---------- Recursos gráficos ----------
    private SpriteBatch batch;
    private BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private Texture pixel;
    private Texture blockBody;
    private Texture blockGloss;
    private Texture glow;
    private Texture ring;
    private Texture arrow;
    private Texture backgroundTexture;
    private Texture[] shardTextures;
    private Texture[] crackTextures;
    private Texture stoneTexture;
    private Sound shatterSound;
    private Sound jetSound;
    private Sound moveSound;
    private Sound rotateSound;
    private Sound beepSound;
    private Sound stoneBreakSound;
    private Sound startSound;
    private Sound levelUpSound;
    private Sound gameOverSound;
    private MusicPlayer musicPlayer;
    private Effects effects;

    private final MusicFilePicker musicFilePicker;
    /** Seletor de arquivo aberto: o teclado do jogo fica ignorado até ele fechar. */
    private boolean pickingMusic;
    private boolean musicFileInvalid;

    // ---------- Estado do jogo ----------
    private Board board;
    private Tetromino currentPiece;
    /** Próximas peças; a primeira é a que entra em seguida. */
    private final List<Tetromino> nextPieces = new ArrayList<>();

    private float dropTimer;
    private float dropInterval = BASE_DROP_INTERVAL;
    private int score;
    private float displayScore;
    private int lines;
    private int level = 1;
    /** Tempo de partida em segundos (para durante a pausa). */
    private float playTime;
    /** Falso na tela inicial, até o jogador pressionar ENTER. */
    private boolean started;
    private boolean gameOver;
    private boolean paused;
    private boolean soundOn = true;
    private Preferences settings;
    /** Peça fantasma (onde a peça vai pousar); tecla G. */
    private boolean landingPreviewOn = true;
    /** Fila mostra as próximas NEXT_COUNT peças (ligado) ou só a próxima; tecla C. */
    private boolean fullQueueOn = false;
    /** Ao girar junto à parede ou a blocos, empurra a peça para o lado (ou 1 acima) se couber; tecla K. */
    private boolean wallKickOn = false;

    private int highScore;
    private String highScoreName = "";
    /** Fim de jogo com novo recorde: o teclado digita o nome até o ENTER. */
    private boolean enteringName;
    private final StringBuilder nameInput = new StringBuilder();

    /** Tempo desde o fim do jogo, para apagar o tabuleiro linha a linha. */
    private float gameOverTime;
    /** Linhas que já ficaram cinza (cada uma solta um flash ao apagar). */
    private int gameOverRowsDone;
    /** Momento em que o tabuleiro termina de apagar e o painel começa a surgir. */
    private float gameOverAnimEnd;
    private final Color grayColor = new Color();
    private float time;

    /** Deslocamento visual (desce suave entre células). */
    private float pieceOffsetY;

    private float ghostBeepTimer;

    private boolean clearing;
    private float clearTimer;
    private final List<Integer> clearingRows = new ArrayList<>();

    private final Random random = new Random();

    // ---------- DAS / ARR (Delayed Auto Shift) ----------
    // Tempo que a seta precisa ficar pressionada antes de começar a repetir
    private static final float MOVE_DAS_DELAY = 0.20f;
    // Intervalo entre cada movimento repetido após o DAS
    private static final float MOVE_ARR_DELAY = 0.08f;
    // Intervalo entre cada queda no soft-drop (seta para baixo segurada)
    private static final float SOFT_DROP_INTERVAL = 0.05f;

    private boolean leftHeld = false;
    private float leftTimer = 0f;

    private boolean rightHeld = false;
    private float rightTimer = 0f;

    private boolean downHeld = false;
    private float downTimer = 0f;

    public TetrisGame(MusicFilePicker musicFilePicker) {
        this.musicFilePicker = musicFilePicker;
    }

    // =====================================================================
    // CICLO DE VIDA
    // =====================================================================

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply(true);

        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        font.setUseIntegerPositions(false);

        Random texRandom = new Random(42);
        pixel = GlassTextures.createPixel();
        blockBody = GlassTextures.createBlockBody(BLOCK_SIZE);
        blockGloss = GlassTextures.createBlockGloss(BLOCK_SIZE);
        glow = GlassTextures.createGlow(64);
        ring = GlassTextures.createRing(256);
        arrow = GlassTextures.createArrow(32);
        backgroundTexture = GlassTextures.createBackground(WORLD_WIDTH, WORLD_HEIGHT, texRandom);
        shardTextures = GlassTextures.createShards(8, 24, texRandom);
        crackTextures = GlassTextures.createCracks(4, BLOCK_SIZE, texRandom);
        stoneTexture = GlassTextures.createStone(BLOCK_SIZE, texRandom);
        shatterSound = GlassSound.createShatter();
        jetSound = GameSounds.createJet();
        moveSound = GameSounds.createMove();
        rotateSound = GameSounds.createRotate();
        beepSound = GameSounds.createBeep();
        stoneBreakSound = GameSounds.createStoneBreak();
        startSound = GameSounds.createStart();
        levelUpSound = GameSounds.createLevelUp();
        gameOverSound = GameSounds.createGameOver();
        musicPlayer = new MusicPlayer();
        effects = new Effects(shardTextures, glow, ring, random);

        settings = Gdx.app.getPreferences(SETTINGS_NAME);
        landingPreviewOn = settings.getBoolean(KEY_LANDING_PREVIEW, true);
        fullQueueOn = settings.getBoolean(KEY_FULL_QUEUE, false);
        wallKickOn = settings.getBoolean(KEY_WALL_KICK, false);
        highScore = settings.getInteger(KEY_HIGH_SCORE, 0);
        highScoreName = settings.getString(KEY_HIGH_SCORE_NAME, "");

        // Digitação do nome do recorde (letras chegam por keyTyped; ENTER é tratado em handleInput)
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyTyped(char c) {
                if (!enteringName || gameOverAnimating()) return false;
                if (c == '\b') {
                    if (nameInput.length() > 0) nameInput.setLength(nameInput.length() - 1);
                } else if (c >= ' ' && nameInput.length() < MAX_NAME_LENGTH && font.getData().hasGlyph(c)
                    && !(c == ' ' && nameInput.length() == 0)) {
                    nameInput.append(c);
                }
                return true;
            }
        });

        board = new Board();
        resetGame();
    }

    private void resetGame() {
        board.reset();
        effects.clear();
        nextPieces.clear();
        for (int i = 0; i < NEXT_COUNT; i++) nextPieces.add(new Tetromino(getRandomType()));
        spawnNewPiece();
        gameOver = false;
        paused = false;
        clearing = false;
        clearingRows.clear();
        score = 0;
        displayScore = 0;
        lines = 0;
        level = 1;
        playTime = 0;
        dropInterval = BASE_DROP_INTERVAL;
        dropTimer = 0;

        // reset de estado do DAS/ARR
        leftHeld = false;  leftTimer = 0;
        rightHeld = false; rightTimer = 0;
        downHeld = false;  downTimer = 0;
    }

    private void startGame() {
        resetGame();
        started = true;
        musicFileInvalid = false;
        playSound(startSound, 0.8f, 1f);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (pixel != null) pixel.dispose();
        if (blockBody != null) blockBody.dispose();
        if (blockGloss != null) blockGloss.dispose();
        if (glow != null) glow.dispose();
        if (ring != null) ring.dispose();
        if (arrow != null) arrow.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (shardTextures != null) for (Texture t : shardTextures) t.dispose();
        if (crackTextures != null) for (Texture t : crackTextures) t.dispose();
        if (stoneTexture != null) stoneTexture.dispose();
        if (shatterSound != null) shatterSound.dispose();
        if (jetSound != null) jetSound.dispose();
        if (moveSound != null) moveSound.dispose();
        if (rotateSound != null) rotateSound.dispose();
        if (beepSound != null) beepSound.dispose();
        if (stoneBreakSound != null) stoneBreakSound.dispose();
        if (startSound != null) startSound.dispose();
        if (levelUpSound != null) levelUpSound.dispose();
        if (gameOverSound != null) gameOverSound.dispose();
        if (musicPlayer != null) musicPlayer.dispose();
    }

    // =====================================================================
    // SPAWN DE PEÇAS
    // =====================================================================

    private void spawnNewPiece() {
        currentPiece = nextPieces.remove(0);
        nextPieces.add(new Tetromino(getRandomType()));
        pieceOffsetY = 0f;

        if (!currentPiece.fits(board, currentPiece.getX(), currentPiece.getY())) {
            gameOver = true;
            gameOverTime = 0f;
            gameOverRowsDone = 0;
            gameOverAnimEnd = (board.getHighestOccupiedRow() + 1) * GAME_OVER_ROW_DELAY + GAME_OVER_FADE;
            shake = 18f;
            playSound(gameOverSound, 0.9f, 1f);
            if (score > highScore) {
                enteringName = true;
                nameInput.setLength(0);
                nameInput.append(highScoreName);
            }
        } else if (currentPiece.getType() == TetrominoType.GHOST) {
            playSound(beepSound, 0.5f, 1f);
            ghostBeepTimer = GHOST_BEEP_INTERVAL;
        }
    }

    private TetrominoType getRandomType() {
        if (board.getHighestOccupiedRow() >= SPECIAL_MIN_ROW) {
            float roll = random.nextFloat();
            if (roll < SPECIAL_PIECE_CHANCE) return TetrominoType.STONE;
            if (roll < SPECIAL_PIECE_CHANCE * 2) return TetrominoType.GHOST;
        }
        return CLASSIC_TYPES[random.nextInt(CLASSIC_TYPES.length)];
    }

    private void playSound(Sound sound, float volume, float pitch) {
        if (soundOn && sound != null) sound.play(volume, pitch, 0f);
    }

    // =====================================================================
    // RENDER
    // =====================================================================

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 1f / 20f);
        time += delta;
        handleInput(delta);
        musicPlayer.setSuspended(!started || gameOver || paused);

        if (started && !gameOver && !paused) {
            update(delta);
        }
        effects.update(delta);
        pieceOffsetY -= pieceOffsetY * Math.min(1f, delta * 22f);
        displayScore += (score - displayScore) * Math.min(1f, delta * 8f);
        if (score - displayScore < 0.5f) displayScore = score;
        backgroundTint.lerp(LEVEL_TINTS[(level - 1) % LEVEL_TINTS.length], Math.min(1f, delta * 1.2f));
        if (gameOver) updateGameOver(delta);

        // Tremor de câmera ao estilhaçar linhas
        shake = Math.max(0f, shake - delta * 60f);
        camera.position.set(WORLD_WIDTH / 2f + (random.nextFloat() - 0.5f) * shake,
            WORLD_HEIGHT / 2f + (random.nextFloat() - 0.5f) * shake, 0);
        camera.update();

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        drawBackground();
        drawBoard();
        if (started && !gameOver && !clearing && landingPreviewOn) drawLandingPreview();
        effects.drawTrail(batch);
        if (started && !gameOver && !clearing) drawCurrentPiece();
        effects.drawFront(batch);
        drawHud();
        drawClock();

        if (!started) drawStartScreen();
        else if (gameOver) drawGameOver();
        else if (paused) drawPaused();

        batch.end();
    }

    private void drawBackground() {
        Color tint = backgroundTint;
        batch.setColor(Color.WHITE);
        batch.draw(backgroundTexture, -40, -40, WORLD_WIDTH + 80, WORLD_HEIGHT + 80);

        // Véu leve com o tom do nível; fraco o bastante para não esconder a grade
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(tint.r, tint.g, tint.b, 0.13f);
        batch.draw(pixel, -40, -40, WORLD_WIDTH + 80, WORLD_HEIGHT + 80);

        // Orbes de luz flutuando lentamente, alternando o tom do nível e um tom mais claro dele
        for (int i = 0; i < 5; i++) {
            float phase = i * 1.7f;
            float ox = WORLD_WIDTH * (0.1f + 0.2f * i) + (float) Math.sin(time * 0.15f + phase) * 90f;
            float oy = WORLD_HEIGHT * (0.25f + 0.13f * ((i * 3) % 5)) + (float) Math.cos(time * 0.11f + phase) * 70f;
            float size = 420f + 120f * (i % 3);
            if (i % 2 == 0) batch.setColor(tint.r, tint.g, tint.b, 0.14f);
            else batch.setColor(0.5f + tint.r * 0.5f, 0.3f + tint.g * 0.4f, 0.5f + tint.b * 0.5f, 0.10f);
            batch.draw(glow, ox - size / 2, oy - size / 2, size, size);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawBoard() {
        // Fundo de vidro escuro com grade sutil
        // O tom do nível tinge só de leve o vidro escuro, para a grade continuar visível
        Color tint = backgroundTint;
        drawRect(BOARD_X, BOARD_Y, BOARD_W, BOARD_H,
            0.02f + tint.r * 0.10f, 0.02f + tint.g * 0.10f, 0.04f + tint.b * 0.10f, 0.78f);
        batch.setColor(0.3f + tint.r * 0.4f, 0.3f + tint.g * 0.4f, 0.3f + tint.b * 0.4f, 0.10f);
        for (int c = 1; c < BOARD_COLS; c++) batch.draw(pixel, BOARD_X + c * BLOCK_SIZE - 1, BOARD_Y, 2, BOARD_H);
        for (int r = 1; r < BOARD_ROWS; r++) batch.draw(pixel, BOARD_X, BOARD_Y + r * BLOCK_SIZE - 1, BOARD_W, 2);
        drawNeonFrame(BOARD_X, BOARD_Y, BOARD_W, BOARD_H, -1, -1, tint.r, tint.g, tint.b);

        // Sombras primeiro, depois os blocos (dá profundidade ao vidro)
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Color color = board.getCell(r, c);
                if (color == null || color.a < 1f) continue;
                drawBlockShadow(BOARD_X + c * BLOCK_SIZE, BOARD_Y + r * BLOCK_SIZE, BLOCK_SIZE);
            }
        }
        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Color color = board.getCell(r, c);
                if (color == null) continue;
                Color shown = gameOver ? fadeToGray(color, r) : color;
                drawBlock(BOARD_X + c * BLOCK_SIZE, BOARD_Y + r * BLOCK_SIZE, BLOCK_SIZE, shown, color.a);
            }
        }

        // Linhas completas rachando antes de estilhaçar
        if (clearing) {
            float progress = 1f - clearTimer / CLEAR_DELAY;
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            for (int row : clearingRows) {
                for (int c = 0; c < BOARD_COLS; c++) {
                    float x = BOARD_X + c * BLOCK_SIZE;
                    float y = BOARD_Y + row * BLOCK_SIZE;
                    batch.setColor(1f, 1f, 1f, 0.25f + 0.6f * progress);
                    batch.draw(crackTextures[(row * 7 + c) % crackTextures.length], x, y, BLOCK_SIZE, BLOCK_SIZE);
                    batch.setColor(1f, 1f, 1f, 0.35f * progress);
                    batch.draw(pixel, x, y, BLOCK_SIZE, BLOCK_SIZE);
                }
            }
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    /** Cor do bloco na linha dada durante o fim de jogo: passa ao cinza quando chega a vez da linha. */
    private Color fadeToGray(Color color, int row) {
        if (color == TetrominoType.STONE.color) return color;
        float t = Math.max(0f, Math.min(1f, (gameOverTime - row * GAME_OVER_ROW_DELAY) / GAME_OVER_FADE));
        float gray = 0.12f + 0.4f * (0.3f * color.r + 0.59f * color.g + 0.11f * color.b);
        return grayColor.set(color.r + (gray - color.r) * t, color.g + (gray - color.g) * t,
            color.b + (gray - color.b) * t, color.a);
    }

    /** Peça fantasma: contorno translúcido mostrando onde a peça atual vai pousar. */
    private void drawLandingPreview() {
        Tetromino landing = new Tetromino(currentPiece);
        while (landing.moveDown(board)) { /* desce até pousar */ }
        if (landing.getY() == currentPiece.getY()) return;

        Color color = currentPiece.getColor();
        int[][] shape = landing.getShape();
        float pulse = 0.85f + 0.15f * (float) Math.sin(time * 4f);
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c] == 0) continue;
                float x = BOARD_X + (landing.getX() + c) * BLOCK_SIZE;
                float y = BOARD_Y + (landing.getY() + shape.length - 1 - r) * BLOCK_SIZE;
                drawRect(x + 3, y + 3, BLOCK_SIZE - 6, BLOCK_SIZE - 6, color.r, color.g, color.b, 0.10f);
                drawOutline(x + 3, y + 3, BLOCK_SIZE - 6, BLOCK_SIZE - 6, 3f, -1, -1,
                    color.r, color.g, color.b, 0.55f * pulse);
            }
        }
    }

    private void drawCurrentPiece() {
        float originX = BOARD_X + currentPiece.getX() * BLOCK_SIZE;
        float originY = BOARD_Y + currentPiece.getY() * BLOCK_SIZE + pieceOffsetY;
        if (currentPiece.getType() == TetrominoType.GHOST) {
            // Halo pulsante: mantém a peça visível enquanto atravessa os outros blocos
            float halo = BLOCK_SIZE * 2f;
            float inset = (halo - BLOCK_SIZE) / 2f;
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.setColor(0.7f, 0.95f, 1f, 0.45f + 0.25f * (float) Math.sin(time * 8f));
            batch.draw(glow, originX - inset, originY - inset, halo, halo);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
        drawShape(currentPiece.getShape(), originX, originY, BLOCK_SIZE, currentPiece.getColor());
    }

    /** Desenha uma forma cuja célula inferior-esquerda fica em (originX, originY). */
    private void drawShape(int[][] shape, float originX, float originY, float size, Color color) {
        for (int pass = 0; pass < 2; pass++) {
            for (int r = 0; r < shape.length; r++) {
                for (int c = 0; c < shape[0].length; c++) {
                    if (shape[r][c] == 0) continue;
                    float x = originX + c * size;
                    float y = originY + (shape.length - 1 - r) * size;
                    if (pass == 1) drawBlock(x, y, size, color, color.a);
                    else if (color.a >= 1f) drawBlockShadow(x, y, size);
                }
            }
        }
    }

    private void drawBlockShadow(float x, float y, float size) {
        batch.setColor(0f, 0f, 0f, 0.35f);
        batch.draw(pixel, x + size * 0.1f, y - size * 0.1f, size, size);
    }

    /** Bloco de vidro 3D: corpo tingido semitransparente + reflexo branco. */
    private void drawBlock(float x, float y, float size, Color color, float alpha) {
        if (color == TetrominoType.STONE.color) {
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(stoneTexture, x, y, size, size);
            return;
        }
        batch.setColor(color.r, color.g, color.b, 0.88f * alpha);
        batch.draw(blockBody, x, y, size, size);
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(blockGloss, x, y, size, size);
    }

    // ---------- HUD ----------

    private void drawHud() {
        int top = BOARD_Y + BOARD_H;
        int small = 95, gap = 22, nextH = 250;
        float centerX = PANEL_X + PANEL_W / 2f;

        int scoreY = top - small;
        drawGroupBox(PANEL_X, scoreY, PANEL_W, small, "Pontuação");
        drawValue(String.valueOf((int) displayScore), centerX, scoreY + small / 2f - 8);

        // Linhas e Nível lado a lado, para sobrar espaço para a fila de peças
        int statsY = scoreY - gap - small;
        float halfW = (PANEL_W - gap) / 2f;
        drawGroupBox(PANEL_X, statsY, halfW, small, "Linhas");
        drawValue(String.valueOf(lines), PANEL_X + halfW / 2f, statsY + small / 2f - 8);
        drawGroupBox(PANEL_X + halfW + gap, statsY, halfW, small, "Nível");
        drawValue(String.valueOf(level), PANEL_X + halfW * 1.5f + gap, statsY + small / 2f - 8);

        int nextY = statsY - gap - nextH;
        float bob = (float) Math.sin(time * 2f) * 5f;
        if (fullQueueOn) {
            drawGroupBox(PANEL_X, nextY, PANEL_W, nextH, "Próximas peças");
            // A primeira, maior e flutuando, em cima; as seguintes menores, lado a lado embaixo
            drawNextPiece(nextPieces.get(0), centerX, nextY + nextH - 82 + bob, 48f);
            for (int i = 1; i < nextPieces.size(); i++) {
                float slotW = PANEL_W / (float) (nextPieces.size() - 1);
                drawNextPiece(nextPieces.get(i), PANEL_X + slotW * (i - 0.5f), nextY + 58, 30f);
            }
        } else {
            drawGroupBox(PANEL_X, nextY, PANEL_W, nextH, "Próxima peça");
            drawNextPiece(nextPieces.get(0), centerX, nextY + (nextH - 20) / 2f + bob, BLOCK_SIZE);
        }

        int helpY = BOARD_Y;
        int helpH = nextY - gap - BOARD_Y;
        drawGroupBox(PANEL_X, helpY, PANEL_W, helpH, "Controles");
        drawControls(helpY, helpH);
    }

    /** Relógio da partida, centralizado no espaço acima do tabuleiro. */
    private void drawClock() {
        float cy = BOARD_Y + BOARD_H + (WORLD_HEIGHT - BOARD_Y - BOARD_H) / 2f + 4f;
        Color color = paused ? new Color(0.85f, 0.97f, 1f, 0.55f + 0.45f * (float) Math.sin(time * 4f))
            : new Color(0.85f, 0.97f, 1f, 1f);
        drawTextCentered("Tempo  " + formatTime(playTime), 2.1f, color, BOARD_X + BOARD_W / 2f, cy, true);
    }

    /** mm:ss, ou h:mm:ss a partir de uma hora. */
    private static String formatTime(float seconds) {
        int total = (int) seconds;
        int h = total / 3600, m = (total / 60) % 60, s = total % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }

    /** Peça da fila centralizada em (cx, cy), com blocos do tamanho dado. */
    private void drawNextPiece(Tetromino piece, float cx, float cy, float size) {
        int[][] shape = piece.getShape();
        float w = shape[0].length * size, h = shape.length * size;
        drawShape(shape, cx - w / 2f, cy - h / 2f, size, piece.getColor());
    }

    private void drawValue(String text, float centerX, float centerY) {
        drawTextCentered(text, 3.4f, new Color(0.85f, 0.97f, 1f, 1f), centerX, centerY, true);
    }

    private void drawControls(int boxY, int boxH) {
        float rowH = 50f;
        float y = boxY + boxH - 62f;
        float keyX = PANEL_X + 28f;
        float textX = PANEL_X + 190f;
        Color text = new Color(0.8f, 0.9f, 1f, 1f);

        // Setas: rotação da textura (aponta para cima por padrão)
        drawKeycap(keyX, y, 48, null, 90f);
        drawKeycap(keyX + 58, y, 48, null, 270f);
        drawText("Mover", 1.8f, text, textX, y + 32);
        y -= rowH;
        drawKeycap(keyX, y, 48, null, 0f);
        drawText("Girar", 1.8f, text, textX, y + 32);
        y -= rowH;
        drawKeycap(keyX, y, 48, null, 180f);
        drawText("Descer", 1.8f, text, textX, y + 32);
        y -= rowH;
        drawKeycap(keyX, y, 140, "ESPAÇO", 0f);
        drawText("Queda livre", 1.8f, text, textX, y + 32);
        y -= rowH;
        drawKeycap(keyX, y, 48, "P", 0f);
        drawText("Pausar", 1.8f, text, textX, y + 32);
        y -= rowH;
        drawKeycap(keyX, y, 48, "M", 0f);
        drawText(soundOn ? "Som: ligado" : "Som: desligado", 1.8f, text, textX, y + 32);
        y -= rowH;
        drawKeycap(keyX, y, 48, "N", 0f);
        drawText(musicPlayer.isEnabled() ? "Música: ligada" : "Música: desligada", 1.8f, text, textX, y + 32);
        y -= rowH;
        drawKeycap(keyX, y, 48, "T", 0f);
        drawText("Faixa: " + musicPlayer.getTrackName(), 1.55f, text, textX, y + 30);
        y -= rowH;
        drawKeycap(keyX, y, 48, "-", 0f);
        drawKeycap(keyX + 58, y, 48, "+", 0f);
        drawText("Volume: " + musicPlayer.getVolumePercent() + "%", 1.8f, text, textX, y + 32);
        y -= rowH;
        drawKeycap(keyX, y, 48, "G", 0f);
        drawText(landingPreviewOn ? "Fantasma: ligado" : "Fantasma: desligado", 1.8f, text, textX, y + 32);
        y -= rowH;
        drawKeycap(keyX, y, 48, "C", 0f);
        drawText(fullQueueOn ? "Fila: " + NEXT_COUNT + " peças" : "Fila: 1 peça", 1.8f, text, textX, y + 32);
        y -= rowH;
        drawKeycap(keyX, y, 48, "K", 0f);
        drawText(wallKickOn ? "Wall kick: ligado" : "Wall kick: desligado", 1.8f, text, textX, y + 32);
        y -= rowH;
        drawKeycap(keyX, y, 120, "ENTER", 0f);
        drawText("Iniciar", 1.8f, text, textX, y + 32);
    }

    /** Tecla estilizada; se label for null desenha uma seta com a rotação dada. */
    private void drawKeycap(float x, float y, float w, String label, float arrowRotation) {
        float h = 44f;
        drawRect(x, y, w, h, 0.08f, 0.14f, 0.26f, 0.95f);
        drawRect(x, y + h - 4, w, 4, 0.4f, 0.8f, 1f, 0.35f);
        drawOutline(x, y, w, h, 2f, -1, -1, 0.35f, 0.8f, 1f, 0.8f);
        batch.setColor(0.85f, 0.95f, 1f, 1f);
        if (label == null) {
            float s = 28f;
            batch.draw(arrow, x + (w - s) / 2f, y + (h - s) / 2f, s / 2f, s / 2f, s, s, 1f, 1f,
                arrowRotation, 0, 0, arrow.getWidth(), arrow.getHeight(), false, false);
        } else {
            drawTextCentered(label, 1.5f, new Color(0.85f, 0.95f, 1f, 1f), x + w / 2f, y + h / 2f, false);
        }
    }

    /** GroupBox: painel de vidro com moldura neon e título "embutido" na borda superior. */
    private void drawGroupBox(float x, float y, float w, float h, String title) {
        drawRect(x, y, w, h, 0.03f, 0.05f, 0.13f, 0.72f);
        font.getData().setScale(1.7f);
        layout.setText(font, title);
        float gapStart = x + 22f;
        float gapEnd = gapStart + layout.width + 20f;
        drawNeonFrame(x, y, w, h, gapStart, gapEnd, 0.25f, 0.85f, 1f);
        drawTextCentered(title, 1.7f, new Color(0.45f, 0.9f, 1f, 1f), (gapStart + gapEnd) / 2f, y + h, false);
    }

    /** Moldura neon com halo; se gapStart >= 0 deixa uma abertura na borda superior. */
    private void drawNeonFrame(float x, float y, float w, float h, float gapStart, float gapEnd,
                               float r, float g, float b) {
        drawNeonFrame(x, y, w, h, gapStart, gapEnd, r, g, b, 1f);
    }

    private void drawNeonFrame(float x, float y, float w, float h, float gapStart, float gapEnd,
                               float r, float g, float b, float alpha) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int i = 3; i >= 1; i--) {
            float k = i * 3f;
            drawOutline(x - k, y - k, w + 2 * k, h + 2 * k, 3f + k, gapStart, gapEnd, r, g, b, 0.06f * alpha);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        drawOutline(x, y, w, h, 3f, gapStart, gapEnd, r, g, b, 0.95f * alpha);
    }

    private void drawOutline(float x, float y, float w, float h, float t, float gapStart, float gapEnd,
                             float r, float g, float b, float a) {
        batch.setColor(r, g, b, a);
        batch.draw(pixel, x, y, w, t);                 // baixo
        batch.draw(pixel, x, y, t, h);                 // esquerda
        batch.draw(pixel, x + w - t, y, t, h);      // direita
        if (gapStart < 0) {
            batch.draw(pixel, x, y + h - t, w, t);  // topo
        } else {
            batch.draw(pixel, x, y + h - t, gapStart - x, t);
            batch.draw(pixel, gapEnd, y + h - t, x + w - gapEnd, t);
        }
    }

    private void drawRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        batch.setColor(r, g, b, a);
        batch.draw(pixel, x, y, w, h);
    }

    private void drawText(String text, float scale, Color color, float x, float y) {
        font.getData().setScale(scale);
        font.setColor(color);
        font.draw(batch, text, x, y);
    }

    private void drawTextCentered(String text, float scale, Color color, float cx, float cy, boolean shadow) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        float x = cx - layout.width / 2f;
        float y = cy + layout.height / 2f;
        if (shadow) {
            font.setColor(0f, 0f, 0f, 0.6f);
            font.draw(batch, text, x + 3, y - 3);
        }
        font.setColor(color);
        font.draw(batch, text, x, y);
    }

    // ---------- Overlays ----------

    private void drawStartScreen() {
        drawRect(BOARD_X, BOARD_Y, BOARD_W, BOARD_H, 0f, 0f, 0f, 0.55f);
        float cx = BOARD_X + BOARD_W / 2f;
        float cy = BOARD_Y + BOARD_H / 2f;
        drawTextCentered("TETRIS", 5f, new Color(0.45f, 0.9f, 1f, 1f), cx, cy + 70, true);
        float blink = 0.55f + 0.45f * (float) Math.sin(time * 4f);
        drawTextCentered("Pressione ENTER para começar", 2f, new Color(1f, 1f, 1f, blink), cx, cy - 20, false);
        drawTextCentered(musicFileInvalid ? "MP3 inválido. Pressione A para escolher outro"
                : "Pressione A para escolher uma música MP3", 1.5f,
            new Color(0.8f, 0.9f, 1f, 1f), cx, cy - 75, false);
        if (highScore > 0) {
            drawTextCentered("Recorde: " + highScore + "  -  " + highScoreName, 1.8f,
                new Color(1f, 0.85f, 0.3f, 1f), cx, cy - 150, true);
        }
    }

    /** Avança a animação do fim de jogo: cada linha com blocos solta um flash ao ficar cinza. */
    private void updateGameOver(float delta) {
        gameOverTime += delta;
        while (gameOverRowsDone < BOARD_ROWS && gameOverTime >= gameOverRowsDone * GAME_OVER_ROW_DELAY) {
            int row = gameOverRowsDone++;
            for (int c = 0; c < BOARD_COLS; c++) {
                if (board.getCell(row, c) == null) continue;
                effects.lineFlash(BOARD_X, BOARD_Y + row * BLOCK_SIZE, BOARD_W, BLOCK_SIZE);
                break;
            }
        }
    }

    private boolean gameOverAnimating() {
        return gameOver && gameOverTime < gameOverAnimEnd;
    }

    private void drawGameOver() {
        // O painel só surge (e cresce de leve) depois que o tabuleiro termina de apagar
        float a = Math.max(0f, Math.min(1f, (gameOverTime - gameOverAnimEnd) / GAME_OVER_PANEL_FADE));
        if (a <= 0f) return;
        float grow = 0.9f + 0.1f * a;

        drawRect(0, 0, WORLD_WIDTH, WORLD_HEIGHT, 0f, 0f, 0f, 0.6f * a);
        float cx = BOARD_X + BOARD_W / 2f;
        float cy = BOARD_Y + BOARD_H / 2f;
        float w = 580 * grow, h = 400 * grow;
        drawRect(cx - w / 2, cy - h / 2, w, h, 0.04f, 0.03f, 0.12f, 0.92f * a);
        drawNeonFrame(cx - w / 2, cy - h / 2, w, h, -1, -1, 1f, 0.3f, 0.5f, a);

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        float pulse = 0.25f + 0.1f * (float) Math.sin(time * 3f);
        batch.setColor(1f, 0.25f, 0.4f, pulse * a);
        batch.draw(glow, cx - 300, cy + 20, 600, 180);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        drawTextCentered("FIM DO JOGO", 4.2f * grow, new Color(1f, 0.35f, 0.45f, a), cx, cy + 130 * grow, true);
        drawTextCentered("Pontuação: " + score, 2.2f, new Color(1f, 1f, 1f, a), cx, cy + 40, true);
        drawTextCentered("Linhas: " + lines + "   Nível: " + level + "   Tempo: " + formatTime(playTime), 1.8f,
            new Color(0.8f, 0.9f, 1f, a), cx, cy - 15, false);
        float blink = 0.55f + 0.45f * (float) Math.sin(time * 4f);
        Color gold = new Color(1f, 0.85f, 0.3f, a);
        if (enteringName) {
            drawTextCentered("NOVO RECORDE! Digite seu nome:", 1.8f, gold, cx, cy - 65, true);
            String cursor = blink > 0.55f ? "_" : " ";
            drawTextCentered(nameInput + cursor, 2.4f, new Color(1f, 1f, 1f, a), cx, cy - 115, true);
            drawTextCentered("Pressione ENTER para salvar", 1.6f,
                new Color(0.45f, 0.9f, 1f, blink * a), cx, cy - 165, false);
            return;
        }
        if (highScore > 0) {
            drawTextCentered("Recorde: " + highScore + "  -  " + highScoreName, 1.8f, gold, cx, cy - 65, true);
        }
        drawTextCentered("Pressione ENTER para jogar novamente", 1.6f,
            new Color(0.45f, 0.9f, 1f, blink * a), cx, cy - 120, false);
        drawTextCentered(musicFileInvalid ? "MP3 inválido. Pressione A para escolher outro"
                : "Pressione A para escolher outra música MP3", 1.5f,
            new Color(0.8f, 0.9f, 1f, a), cx, cy - 160, false);
    }

    private void drawPaused() {
        drawRect(BOARD_X, BOARD_Y, BOARD_W, BOARD_H, 0f, 0f, 0f, 0.55f);
        float cx = BOARD_X + BOARD_W / 2f;
        float cy = BOARD_Y + BOARD_H / 2f;
        drawTextCentered("PAUSADO", 4f, new Color(0.45f, 0.9f, 1f, 1f), cx, cy + 30, true);
        drawTextCentered("Pressione P para continuar", 1.7f, Color.WHITE, cx, cy - 40, false);
        drawTextCentered(musicFileInvalid ? "MP3 inválido. Pressione A para escolher outro"
                : "Pressione A para escolher outra música MP3", 1.5f,
            new Color(0.8f, 0.9f, 1f, 1f), cx, cy - 90, false);
    }

    // =====================================================================
    // INPUT (com DAS/ARR)
    // =====================================================================

    private void handleInput(float delta) {
        if (pickingMusic) return;
        if (enteringName) {
            // As letras viram o nome; os atalhos ficam desligados até confirmar
            if (!gameOverAnimating() && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER))) {
                saveHighScore();
            }
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) soundOn = !soundOn;
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            landingPreviewOn = !landingPreviewOn;
            settings.putBoolean(KEY_LANDING_PREVIEW, landingPreviewOn).flush();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            fullQueueOn = !fullQueueOn;
            settings.putBoolean(KEY_FULL_QUEUE, fullQueueOn).flush();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            wallKickOn = !wallKickOn;
            settings.putBoolean(KEY_WALL_KICK, wallKickOn).flush();
        }
        handleMusicInput();

        if (!started || gameOver) {
            // Durante a animação do fim de jogo, ENTER e A esperam o painel surgir
            if (gameOverAnimating()) return;
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ENTER)) {
                startGame();
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
                chooseMusicFile();
            }
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) paused = !paused;
        if (paused) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.A)) chooseMusicFile();
            return;
        }
        if (clearing) return;

        // Rotação (instantânea — a cada toque)
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            if (currentPiece.rotate(board, wallKickOn)) playSound(rotateSound, 0.55f, 1f);
        }

        // Hard drop (queda instantânea — a cada toque)
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            playSound(jetSound, 0.8f, 1f);
            int startY = currentPiece.getY();
            while (movePieceDown()) { /* desce ao fundo */ }
            if (currentPiece.getY() < startY) hardDropEffect(startY);
            lockPiece();
            dropTimer = 0f;
            return;
        }

        // Movimento horizontal com DAS/ARR
        handleHorizontal(delta);

        // Soft drop com repetição controlada
        handleSoftDrop(delta);
    }

    private void saveHighScore() {
        String name = nameInput.toString().trim();
        highScore = score;
        highScoreName = name.isEmpty() ? "Anônimo" : name;
        settings.putInteger(KEY_HIGH_SCORE, highScore).putString(KEY_HIGH_SCORE_NAME, highScoreName).flush();
        enteringName = false;
    }

    /** Música: tocar/parar, trocar de faixa e volume (funcionam também com o jogo pausado). */
    private void handleMusicInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) musicPlayer.toggle();
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) musicPlayer.nextTrack();
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)
            || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_SUBTRACT)) {
            musicPlayer.volumeDown();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)
            || Gdx.input.isKeyJustPressed(Input.Keys.PLUS)
            || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ADD)) {
            musicPlayer.volumeUp();
        }
    }

    /** Abre o seletor de MP3; a música escolhida começa a tocar na próxima partida. */
    private void chooseMusicFile() {
        pickingMusic = true;
        musicFileInvalid = false;
        musicFilePicker.pick(file -> Gdx.app.postRunnable(() -> {
            pickingMusic = false;
            if (file != null) musicFileInvalid = !musicPlayer.playFile(file);
        }));
    }

    private void movePieceLeft() {
        if (currentPiece.moveLeft(board)) playSound(moveSound, 0.4f, 1f);
    }

    private void movePieceRight() {
        if (currentPiece.moveRight(board)) playSound(moveSound, 0.4f, 1f);
    }

    /** Trata ← e → com DAS (delay inicial) e ARR (repetição contínua). */
    private void handleHorizontal(float delta) {
        boolean leftPressed  = Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean rightPressed = Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        // Se ambas as setas estão pressionadas ao mesmo tempo, ignora as duas
        // (evita "vibração" da peça).
        if (leftPressed && rightPressed) {
            leftHeld = false;  leftTimer = 0;
            rightHeld = false; rightTimer = 0;
            return;
        }

        // ---------- ESQUERDA ----------
        if (leftPressed) {
            if (!leftHeld) {
                // Primeiro frame: move 1 célula imediatamente
                movePieceLeft();
                leftHeld = true;
                leftTimer = 0f;
            } else {
                leftTimer += delta;
                if (leftTimer >= MOVE_DAS_DELAY
                    && leftTimer - MOVE_DAS_DELAY >= MOVE_ARR_DELAY) {
                    movePieceLeft();
                    // Reancora o timer para o próximo repeat vir após ARR
                    leftTimer = MOVE_DAS_DELAY;
                }
            }
        } else {
            leftHeld = false;
            leftTimer = 0f;
        }

        // ---------- DIREITA ----------
        if (rightPressed) {
            if (!rightHeld) {
                movePieceRight();
                rightHeld = true;
                rightTimer = 0f;
            } else {
                rightTimer += delta;
                if (rightTimer >= MOVE_DAS_DELAY
                    && rightTimer - MOVE_DAS_DELAY >= MOVE_ARR_DELAY) {
                    movePieceRight();
                    rightTimer = MOVE_DAS_DELAY;
                }
            }
        } else {
            rightHeld = false;
            rightTimer = 0f;
        }
    }

    /** Soft drop (seta ↓) com repetição controlada em SOFT_DROP_INTERVAL. */
    private void handleSoftDrop(float delta) {
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            if (!downHeld) {
                // Primeiro frame: desce 1 célula
                downHeld = true;
                downTimer = 0f;
                if (movePieceDown()) dropTimer = 0f;
            } else {
                downTimer += delta;
                if (downTimer >= SOFT_DROP_INTERVAL) {
                    downTimer -= SOFT_DROP_INTERVAL;
                    if (movePieceDown()) dropTimer = 0f;
                }
            }
        } else {
            downHeld = false;
            downTimer = 0f;
        }
    }

    /** Queda rápida: faixa de luz por coluna, de onde a peça estava até onde pousou, e impacto no chão. */
    private void hardDropEffect(int startY) {
        int[][] shape = currentPiece.getShape();
        int endY = currentPiece.getY();
        Color color = currentPiece.getColor();
        for (int c = 0; c < shape[0].length; c++) {
            int topRow = -1, bottomRow = -1;
            for (int r = 0; r < shape.length; r++) {
                if (shape[r][c] == 0) continue;
                if (topRow < 0) topRow = r;
                bottomRow = r;
            }
            if (topRow < 0) continue;
            float x = BOARD_X + (currentPiece.getX() + c) * BLOCK_SIZE + BLOCK_SIZE / 2f;
            float yBottom = BOARD_Y + (endY + shape.length - 1 - bottomRow) * BLOCK_SIZE;
            float yTop = Math.min(BOARD_Y + BOARD_H, BOARD_Y + (startY + shape.length - topRow) * BLOCK_SIZE);
            effects.dropStreak(x, yBottom, yTop, BLOCK_SIZE, color);
            effects.impact(x, yBottom, BLOCK_SIZE, color);
        }
        shake = Math.max(shake, 6f + Math.min(startY - endY, 20) * 0.5f);
    }

    /** Desce a peça uma célula, deixando o rastro de jato e suavizando o movimento. */
    private boolean movePieceDown() {
        if (!currentPiece.moveDown(board)) return false;
        pieceOffsetY = Math.min(pieceOffsetY + BLOCK_SIZE, BLOCK_SIZE * 1.5f);

        int[][] shape = currentPiece.getShape();
        for (int c = 0; c < shape[0].length; c++) {
            for (int r = 0; r < shape.length; r++) {
                if (shape[r][c] == 0) continue;
                // primeiro bloco (de cima para baixo) da coluna: o rastro sai da célula acima dele
                int vacatedRow = currentPiece.getY() + (shape.length - 1 - r) + 1;
                if (vacatedRow < BOARD_ROWS) {
                    float x = BOARD_X + (currentPiece.getX() + c) * BLOCK_SIZE + BLOCK_SIZE / 2f;
                    float y = BOARD_Y + vacatedRow * BLOCK_SIZE;
                    effects.trail(x, y, BLOCK_SIZE, currentPiece.getColor());
                }
                break;
            }
        }
        return true;
    }

    // =====================================================================
    // UPDATE (gravidade automática)
    // =====================================================================

    private void update(float delta) {
        playTime += delta;
        if (clearing) {
            clearTimer -= delta;
            if (clearTimer <= 0f) finishClear();
            return;
        }

        if (currentPiece.getType() == TetrominoType.GHOST) {
            ghostBeepTimer -= delta;
            if (ghostBeepTimer <= 0f) {
                playSound(beepSound, 0.5f, 1f);
                ghostBeepTimer = GHOST_BEEP_INTERVAL;
            }
        }

        dropTimer += delta;
        if (dropTimer >= dropInterval) {
            dropTimer = 0;
            if (!movePieceDown()) {
                lockPiece();
            }
        }
    }

    private void lockPiece() {
        if (currentPiece.getType() == TetrominoType.STONE) {
            smashWithStone();
            return;
        }
        if (currentPiece.getType() == TetrominoType.GHOST) {
            playSound(beepSound, 0.7f, 1.5f);
        }
        board.placePiece(currentPiece);

        List<Integer> full = board.getFullRows();
        if (full.isEmpty()) {
            spawnNewPiece();
            return;
        }

        // Linhas racham por um instante e depois estilhaçam (finishClear)
        clearing = true;
        clearTimer = CLEAR_DELAY;
        clearingRows.clear();
        clearingRows.addAll(full);
        int n = full.size();
        playSound(shatterSound, Math.min(1f, 0.6f + 0.1f * n), 1.1f - 0.07f * n);
    }

    /** A pedra se desfaz ao pousar, levando junto a peça em que bateu (se houver). */
    private void smashWithStone() {
        int col = currentPiece.getX();
        int row = currentPiece.getY();
        float stoneX = BOARD_X + col * BLOCK_SIZE;
        float centerX = stoneX + BLOCK_SIZE / 2f;
        effects.shatterBlock(stoneX, BOARD_Y + row * BLOCK_SIZE, BLOCK_SIZE, currentPiece.getColor(), centerX);

        boolean hitPiece = row > 0 && board.getCell(row - 1, col) != null;
        if (hitPiece) {
            for (int[] cell : board.getPieceCells(row - 1, col)) {
                float x = BOARD_X + cell[1] * BLOCK_SIZE;
                float y = BOARD_Y + cell[0] * BLOCK_SIZE;
                effects.shatterBlock(x, y, BLOCK_SIZE, board.getCell(cell[0], cell[1]), centerX);
                effects.lineFlash(x, y, BLOCK_SIZE, BLOCK_SIZE);
                board.clearCell(cell[0], cell[1]);
            }
        }
        shake = hitPiece ? 20f : 8f;
        playSound(stoneBreakSound, hitPiece ? 1f : 0.6f, 1f);
        spawnNewPiece();
    }

    private void finishClear() {
        float centerX = BOARD_X + BOARD_W / 2f;
        for (int row : clearingRows) {
            float y = BOARD_Y + row * BLOCK_SIZE;
            for (int c = 0; c < BOARD_COLS; c++) {
                Color color = board.getCell(row, c);
                if (color != null) {
                    effects.shatterBlock(BOARD_X + c * BLOCK_SIZE, y, BLOCK_SIZE, color, centerX);
                }
            }
            effects.lineFlash(BOARD_X, y, BOARD_W, BLOCK_SIZE);
        }
        board.removeRows(clearingRows);

        int cleared = clearingRows.size();
        shake = 8f + 6f * cleared;
        if (cleared == 4) {
            // Tetris: onda de choque a partir do meio das linhas destruídas
            float midRow = (clearingRows.get(0) + clearingRows.get(cleared - 1)) / 2f;
            effects.shockwave(centerX, BOARD_Y + (midRow + 0.5f) * BLOCK_SIZE, backgroundTint);
            shake = 40f;
        }
        lines += cleared;
        switch (cleared) {
            case 1:
                score += 100;
                break;
            case 2:
                score += 300;
                break;
            case 3:
                score += 500;
                break;
            case 4:
                score += 800;
                break;
        }
        int previousLevel = level;
        level = 1 + lines / 10;
        if (level > previousLevel) playSound(levelUpSound, 0.9f, 1f);
        dropInterval = BASE_DROP_INTERVAL *
            (float) Math.pow(DROP_SPEED_FACTOR, level - 1);

        clearing = false;
        clearingRows.clear();
        spawnNewPiece();
    }
}
