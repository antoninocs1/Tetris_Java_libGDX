package io.github.game;

import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private final int rows = TetrisGame.BOARD_ROWS;
    private final int cols = TetrisGame.BOARD_COLS;
    private final Color[][] grid;
    /** Identifica a qual peça cada célula pertence (0 = vazia). */
    private final int[][] pieceIds;
    private int lastPieceId;

    public Board() {
        grid = new Color[rows][cols];
        pieceIds = new int[rows][cols];
    }

    public boolean isValidPosition(int[][] shape, int posX, int posY) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c] == 0) continue;

                int boardRow = posY + (shape.length - 1 - r);
                int boardCol = posX + c;

                if (boardRow < 0 || boardRow >= rows || boardCol < 0 || boardCol >= cols) return false;
                if (grid[boardRow][boardCol] != null) return false;
            }
        }
        return true;
    }

    public void placePiece(Tetromino piece) {
        int[][] shape = piece.getShape();
        int posX = piece.getX();
        int posY = piece.getY();
        Color color = piece.getColor();
        lastPieceId++;

        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[0].length; c++) {
                if (shape[r][c] == 0) continue;
                int boardRow = posY + (shape.length - 1 - r);
                int boardCol = posX + c;
                if (boardRow >= 0 && boardRow < rows && boardCol >= 0 && boardCol < cols) {
                    grid[boardRow][boardCol] = color;
                    pieceIds[boardRow][boardCol] = lastPieceId;
                }
            }
        }
    }

    /** Células {linha, coluna} da peça que ocupa a posição indicada. */
    public List<int[]> getPieceCells(int row, int col) {
        List<int[]> cells = new ArrayList<>();
        int id = pieceIds[row][col];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] != null && pieceIds[r][c] == id) cells.add(new int[]{r, c});
            }
        }
        return cells;
    }

    public void clearCell(int row, int col) {
        grid[row][col] = null;
        pieceIds[row][col] = 0;
    }

    /** Linha ocupada mais alta do tabuleiro, ou -1 se estiver vazio. */
    public int getHighestOccupiedRow() {
        for (int r = rows - 1; r >= 0; r--) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] != null) return r;
            }
        }
        return -1;
    }

    /**
     * Linha onde a peça transparente pousa na coluna: a célula vazia mais baixa, atravessando
     * todos os blocos acima dela. Retorna {@code rows} se a coluna estiver cheia até o topo.
     */
    public int findGhostRow(int col) {
        for (int r = 0; r < rows; r++) {
            if (grid[r][col] == null) return r;
        }
        return rows;
    }

    /** Linhas completas, em ordem crescente (de baixo para cima). */
    public List<Integer> getFullRows() {
        List<Integer> full = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            if (isRowFull(r)) full.add(r);
        }
        return full;
    }

    /** Remove as linhas indicadas (em ordem crescente), descendo as de cima. */
    public void removeRows(List<Integer> fullRows) {
        for (int i = fullRows.size() - 1; i >= 0; i--) {
            removeRow(fullRows.get(i));
        }
    }

    private boolean isRowFull(int row) {
        for (int c = 0; c < cols; c++) {
            if (grid[row][c] == null) return false;
        }
        return true;
    }

    private void removeRow(int row) {
        for (int r = row; r < rows - 1; r++) {
            System.arraycopy(grid[r + 1], 0, grid[r], 0, cols);
            System.arraycopy(pieceIds[r + 1], 0, pieceIds[r], 0, cols);
        }
        for (int c = 0; c < cols; c++) {
            clearCell(rows - 1, c);
        }
    }

    public Color getCell(int row, int col) { return grid[row][col]; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }

    public void reset() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                clearCell(r, c);
    }
}
