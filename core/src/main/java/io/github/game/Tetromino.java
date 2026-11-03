package io.github.game;

import com.badlogic.gdx.graphics.Color;

public class Tetromino {
    private final TetrisGame.TetrominoType type;
    private int[][] shape;
    private int x, y;
    private final Color color;

    public Tetromino(TetrisGame.TetrominoType type) {
        this.type = type;
        this.shape = type.copyShape();
        this.color = type.color;
        this.x = TetrisGame.BOARD_COLS / 2 - shape[0].length / 2;
        this.y = TetrisGame.BOARD_ROWS - shape.length;
    }

    public Tetromino(Tetromino other) {
        this.type = other.type;
        this.shape = new int[other.shape.length][];
        for (int i = 0; i < other.shape.length; i++) {
            this.shape[i] = other.shape[i].clone();
        }
        this.x = other.x;
        this.y = other.y;
        this.color = other.color;
    }

    /**
     * Indica se a peça pode ficar na posição. A peça transparente atravessa blocos, mas não pode
     * ficar abaixo da célula vazia mais baixa da coluna, garantindo que sempre pouse nela.
     */
    public boolean fits(Board board, int posX, int posY) {
        if (type != TetrisGame.TetrominoType.GHOST) return board.isValidPosition(shape, posX, posY);
        return posX >= 0 && posX < board.getCols() && posY >= board.findGhostRow(posX);
    }

    public boolean moveLeft(Board board) {
        if (fits(board, x - 1, y)) { x--; return true; }
        return false;
    }

    public boolean moveRight(Board board) {
        if (fits(board, x + 1, y)) { x++; return true; }
        return false;
    }

    public boolean moveDown(Board board) {
        if (fits(board, x, y - 1)) { y--; return true; }
        return false;
    }

    /** Deslocamentos {dx, dy} tentados no wall kick, em ordem; os de 2 colunas só servem à peça I. */
    private static final int[][] KICKS = {{0, 0}, {-1, 0}, {1, 0}, {-2, 0}, {2, 0}, {0, 1}};

    /**
     * Gira a peça no sentido horário. Com wallKick, se ela passar da borda superior desce o necessário,
     * e se ainda não couber tenta empurrá-la para os lados (ou uma linha acima) até achar uma posição livre.
     */
    public boolean rotate(Board board, boolean wallKick) {
        // Peças de um só quadrado (pedra e transparente) não giram
        if (shape.length == 1 && shape[0].length == 1) return false;
        int[][] rotated = rotateMatrix(shape);
        // Linhas que a peça girada ultrapassa no topo: o kick começa descendo esse tanto
        int topOverflow = wallKick ? Math.max(0, y + rotated.length - board.getRows()) : 0;
        int tries = wallKick ? KICKS.length : 1;
        for (int i = 0; i < tries; i++) {
            int[] kick = KICKS[i];
            if (Math.abs(kick[0]) == 2 && type != TetrisGame.TetrominoType.I) continue;
            int newX = x + kick[0];
            int newY = y - topOverflow + kick[1];
            if (board.isValidPosition(rotated, newX, newY)) {
                shape = rotated;
                x = newX;
                y = newY;
                return true;
            }
        }
        return false;
    }

    private int[][] rotateMatrix(int[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        int[][] rotated = new int[cols][rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                rotated[c][rows - 1 - r] = matrix[r][c];
            }
        }
        return rotated;
    }

    public int[][] getShape() { return shape; }
    public int getX() { return x; }
    public int getY() { return y; }
    public Color getColor() { return color; }
    public TetrisGame.TetrominoType getType() { return type; }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
