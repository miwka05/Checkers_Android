package com.example.checkers;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Основной класс логики игры в шашки.
 */
public class CheckersGame {
    private static final String TAG = "CheckersGame";

    // --- Типы шашек и игроков ---
    public static final int EMPTY = 0;
    public static final int BLACK_PIECE = 1;
    public static final int WHITE_PIECE = 2;
    public static final int BLACK_KING = 3;
    public static final int WHITE_KING = 4;
    public static final int BLACK_PLAYER = 1;
    public static final int WHITE_PLAYER = 2;

    // --- Уровни сложности ИИ ---
    public static final int DIFFICULTY_EASY = 0;
    public static final int DIFFICULTY_MEDIUM = 1;
    public static final int DIFFICULTY_HARD = 2;

    // --- Состояние игры ---
    private int[][] board;
    private int currentPlayer;
    private boolean mustContinueJump;
    private int continueJumpRow = -1;
    private int continueJumpCol = -1;

    // --- Настройки ИИ ---
    private int aiPlayer = EMPTY;
    private int difficulty = DIFFICULTY_EASY;
    private Random random = new Random();

    // --- Обратный вызов для обновления UI из MainActivity ---
    public interface OnGameUpdateListener {
        void onAImoveCompleted();
    }
    private OnGameUpdateListener mainActivityCallback = null;

    /**
     * Конструктор игры с указанием сложности ИИ и цвета, за который он играет.
     */
    public CheckersGame(int difficulty, int aiPlayer) {
        this.difficulty = difficulty;
        this.aiPlayer = aiPlayer;
        Log.d(TAG, "Initializing new game with AI player: " + aiPlayer + " and difficulty: " + difficulty);
        initializeBoard();
        currentPlayer = WHITE_PLAYER; // Белые всегда ходят первыми
        mustContinueJump = false;
    }

    /**
     * Устаревший конструктор (по умолчанию: ИИ играет черными, легкий уровень).
     */
    public CheckersGame() {
        this(DIFFICULTY_EASY, BLACK_PLAYER);
    }

    /**
     * Устанавливает слушатель для уведомлений об окончании хода ИИ.
     */
    public void setOnGameUpdateListener(OnGameUpdateListener listener) {
        this.mainActivityCallback = listener;
    }

    /**
     * Инициализирует начальную расстановку шашек на доске.
     */
    private void initializeBoard() {
        board = new int[8][8];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 8; col++) {
                if ((row + col) % 2 == 1) {
                    board[row][col] = BLACK_PIECE;
                }
            }
        }
        for (int row = 5; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if ((row + col) % 2 == 1) {
                    board[row][col] = WHITE_PIECE;
                }
            }
        }
    }

    /**
     * Возвращает тип шашки по координатам (или EMPTY, если вышли за пределы доски).
     */
    public int getPieceAt(int row, int col) {
        if (row < 0 || row >= 8 || col < 0 || col >= 8) {
            return EMPTY;
        }
        return board[row][col];
    }

    /**
     * Возвращает текущего игрока (BLACK_PLAYER или WHITE_PLAYER).
     */
    public int getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * Проверяет, должен ли текущий игрок продолжить взятие той же шашкой.
     */
    public boolean mustContinueJump() {
        return mustContinueJump;
    }

    /**
     * Возвращает строку шашки, которая должна продолжить взятие.
     */
    public int getContinueJumpRow() {
        return continueJumpRow;
    }

    /**
     * Возвращает столбец шашки, которая должна продолжить взятие.
     */
    public int getContinueJumpCol() {
        return continueJumpCol;
    }

    /**
     * Проверяет, есть ли на доске обязательные взятия у текущего игрока.
     */
    public boolean hasForcedJumps() {
        if (mustContinueJump) {
            return false;
        }
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (isCurrentPlayerPiece(row, col) && canPieceJump(row, col)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Выводит текущее состояние доски в лог (для отладки).
     */
    private void logBoard() {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                sb.append(board[row][col]).append(" ");
            }
            sb.append("\n");
        }
        Log.d(TAG, "Current board:\n" + sb.toString());
    }

    /**
     * Определяет победителя игры или возвращает EMPTY, если игра продолжается.
     */
    public int getWinner() {
        logBoard();
        boolean blackExists = false;
        boolean whiteExists = false;
        boolean blackCanMove = false;
        boolean whiteCanMove = false;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if (piece == BLACK_PIECE || piece == BLACK_KING) {
                    blackExists = true;
                    if (!blackCanMove && hasAnyValidMoveForPiece(row, col)) {
                        blackCanMove = true;
                    }
                } else if (piece == WHITE_PIECE || piece == WHITE_KING) {
                    whiteExists = true;
                    if (!whiteCanMove && hasAnyValidMoveForPiece(row, col)) {
                        whiteCanMove = true;
                    }
                }
            }
        }

        if (!blackExists) return WHITE_PLAYER;
        if (!whiteExists) return BLACK_PLAYER;

        if (currentPlayer == BLACK_PLAYER && !hasAnyValidMovesForPlayer(BLACK_PLAYER)) {
            return WHITE_PLAYER;
        }
        if (currentPlayer == WHITE_PLAYER && !hasAnyValidMovesForPlayer(WHITE_PLAYER)) {
            return BLACK_PLAYER;
        }

        return EMPTY;
    }

    /**
     * Проверяет, есть ли у игрока хотя бы один допустимый ход.
     */
    private boolean hasAnyValidMovesForPlayer(int player) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (isPlayerPiece(row, col, player) && hasAnyValidMoveForPiece(row, col)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Проверяет, может ли конкретная шашка выполнить хотя бы один допустимый ход.
     */
    private boolean hasAnyValidMoveForPiece(int row, int col) {
        int piece = board[row][col];
        if (canPieceJump(row, col)) {
            return true;
        }
        if (isKing(piece)) {
            int[][] directions = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
            for (int[] dir : directions) {
                for (int distance = 1; distance < 8; distance++) {
                    int newRow = row + dir[0] * distance;
                    int newCol = col + dir[1] * distance;
                    if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                        if (board[newRow][newCol] == EMPTY && isValidKingMove(row, col, newRow, newCol)) {
                            return true;
                        }
                    } else {
                        break;
                    }
                }
            }
        } else {
            int[][] directions = (piece == BLACK_PIECE) ?
                    new int[][]{{1,1}, {1,-1}} : new int[][]{{-1,1}, {-1,-1}};
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8 &&
                        board[newRow][newCol] == EMPTY) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Проверяет, принадлежит ли шашка указанному игроку.
     */
    private boolean isPlayerPiece(int row, int col, int player) {
        int piece = board[row][col];
        if (player == BLACK_PLAYER) {
            return piece == BLACK_PIECE || piece == BLACK_KING;
        } else {
            return piece == WHITE_PIECE || piece == WHITE_KING;
        }
    }

    /**
     * Проверяет, можно ли выбрать указанную клетку для хода текущим игроком.
     */
    public boolean isValidSelection(int row, int col) {
        if (row < 0 || row >= 8 || col < 0 || col >= 8) return false;
        if (!isCurrentPlayerPiece(row, col)) return false;
        if (aiPlayer != EMPTY && currentPlayer == aiPlayer) {
            return false;
        }
        if (mustContinueJump) {
            return (row == continueJumpRow && col == continueJumpCol);
        }
        boolean canThisPieceJump = canPieceJump(row, col);
        boolean globalForcedJumps = hasForcedJumps();
        if (globalForcedJumps) {
            return canThisPieceJump;
        } else {
            return hasAnyValidMove(row, col);
        }
    }

    /**
     * Выполняет ход от имени ИИ с учётом уровня сложности.
     */
    public void makeAIMove() {
        if (aiPlayer == EMPTY || (currentPlayer != aiPlayer && !mustContinueJump)) {
            return;
        }

        if (mustContinueJump) {
            List<Move> possibleJumpsForContinuation = getPossibleJumpsForPiece(continueJumpRow, continueJumpCol);
            if (possibleJumpsForContinuation.isEmpty()) {
                Log.e(TAG, "AI must continue jump but has no possible jumps from (" + continueJumpRow + "," + continueJumpCol + ")");
                return;
            }

            Move chosenMove = selectBestMove(possibleJumpsForContinuation, true);
            if (chosenMove != null) {
                makeMove(chosenMove.fromRow, chosenMove.fromCol, chosenMove.toRow, chosenMove.toCol);
            }
            return;
        }

        List<Move> possibleMoves = getAllPossibleMoves(aiPlayer);
        if (possibleMoves.isEmpty()) {
            return;
        }

        Move chosenMove = selectBestMove(possibleMoves, false);
        if (chosenMove != null) {
            makeMove(chosenMove.fromRow, chosenMove.fromCol, chosenMove.toRow, chosenMove.toCol);
        }
    }

    /**
     * Вспомогательный метод для выбора лучшего хода ИИ в зависимости от сложности.
     */
    private Move selectBestMove(List<Move> moves, boolean isContinuation) {
        Move chosenMove = null;
        switch (difficulty) {
            case DIFFICULTY_EASY:
                chosenMove = moves.get(random.nextInt(moves.size()));
                break;
            case DIFFICULTY_MEDIUM:
                chosenMove = evaluateMoves(moves, 2);
                break;
            case DIFFICULTY_HARD:
                chosenMove = evaluateMoves(moves, 6);
                break;
        }
        if (chosenMove == null) {
            chosenMove = moves.get(random.nextInt(moves.size()));
        }
        return chosenMove;
    }

    /**
     * Оценивает возможные ходы с помощью MinMax и возвращает лучший.
     */
    private Move evaluateMoves(List<Move> moves, int depth) {
        double bestScore = Double.MIN_VALUE;
        List<Move> bestMoves = new ArrayList<>();
        for (Move move : moves) {
            int[][] tempBoard = copyBoard();
            simulateMove(move.fromRow, move.fromCol, move.toRow, move.toCol, tempBoard);
            int nextPlayer = (currentPlayer == BLACK_PLAYER) ? WHITE_PLAYER : BLACK_PLAYER;
            double score = minMax(tempBoard, depth, false, Double.MIN_VALUE, Double.MAX_VALUE, aiPlayer, nextPlayer);
            if (Double.isNaN(score) || Double.isInfinite(score)) continue;
            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(move);
            } else if (score == bestScore) {
                bestMoves.add(move);
            }
        }
        return bestMoves.isEmpty() ? null : bestMoves.get(random.nextInt(bestMoves.size()));
    }

    /**
     * Внутренний класс для представления хода.
     */
    private static class Move {
        int fromRow, fromCol, toRow, toCol;
        List<int[]> capturedPieces;
        Move(int fromRow, int fromCol, int toRow, int toCol) {
            this.fromRow = fromRow;
            this.fromCol = fromCol;
            this.toRow = toRow;
            this.toCol = toCol;
            this.capturedPieces = new ArrayList<>();
        }
    }

    /**
     * Возвращает все допустимые ходы для указанного игрока.
     */
    private List<Move> getAllPossibleMoves(int player) {
        List<Move> moves = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (isPlayerPiece(row, col, player)) {
                    moves.addAll(getPossibleJumpsForPiece(row, col));
                }
            }
        }
        if (!moves.isEmpty()) {
            return moves;
        }
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (isPlayerPiece(row, col, player)) {
                    moves.addAll(getPossibleRegularMovesForPiece(row, col));
                }
            }
        }
        return moves;
    }

    /**
     * Возвращает все возможные взятия для конкретной шашки.
     */
    private List<Move> getPossibleJumpsForPiece(int row, int col) {
        List<Move> jumps = new ArrayList<>();
        int piece = board[row][col];
        if (isKing(piece)) {
            int[][] directions = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
            for (int[] dir : directions) {
                for (int distance = 1; distance < 8; distance++) {
                    int newRow = row + dir[0] * distance;
                    int newCol = col + dir[1] * distance;
                    if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                        if (isValidKingJump(row, col, newRow, newCol)) {
                            Move move = new Move(row, col, newRow, newCol);
                            move.capturedPieces = getCapturedPiecesInMove(row, col, newRow, newCol);
                            jumps.add(move);
                        }
                    } else {
                        break;
                    }
                }
            }
        } else {
            int[][] directions = {{2,2}, {2,-2}, {-2,2}, {-2,-2}};
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                    if (isValidSimpleJump(row, col, newRow, newCol)) {
                        Move move = new Move(row, col, newRow, newCol);
                        int midRow = (row + newRow) / 2;
                        int midCol = (col + newCol) / 2;
                        move.capturedPieces.add(new int[]{midRow, midCol});
                        jumps.add(move);
                    }
                }
            }
        }
        return jumps;
    }

    /**
     * Возвращает все обычные (не взятия) ходы для конкретной шашки.
     */
    private List<Move> getPossibleRegularMovesForPiece(int row, int col) {
        List<Move> moves = new ArrayList<>();
        int piece = board[row][col];
        if (isKing(piece)) {
            int[][] directions = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
            for (int[] dir : directions) {
                for (int distance = 1; distance < 8; distance++) {
                    int newRow = row + dir[0] * distance;
                    int newCol = col + dir[1] * distance;
                    if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8 && board[newRow][newCol] == EMPTY) {
                        if (isValidKingMove(row, col, newRow, newCol)) {
                            moves.add(new Move(row, col, newRow, newCol));
                        }
                    } else {
                        break;
                    }
                }
            }
        } else {
            int[][] directions = (piece == BLACK_PIECE) ?
                    new int[][]{{1,1}, {1,-1}} : new int[][]{{-1,1}, {-1,-1}};
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8 && board[newRow][newCol] == EMPTY) {
                    moves.add(new Move(row, col, newRow, newCol));
                }
            }
        }
        return moves;
    }

    /**
     * Реализует алгоритм MinMax с альфа-бета отсечением для оценки позиции.
     */
    private double minMax(int[][] evalBoard, int depth, boolean isMaximizing, double alpha, double beta, int aiPlayer, int currentPlayer) {
        if (depth == 0) {
            return evaluatePosition(evalBoard, aiPlayer, currentPlayer);
        }
        if (!hasAnyValidMovesForPlayerOnBoard(evalBoard, currentPlayer)) {
            return (currentPlayer == aiPlayer) ? Double.MIN_VALUE / 2 : Double.MAX_VALUE / 2;
        }

        List<Move> possibleMoves = getAllPossibleMovesOnBoard(evalBoard, currentPlayer);
        int nextPlayer = (currentPlayer == BLACK_PLAYER) ? WHITE_PLAYER : BLACK_PLAYER;

        if (isMaximizing) {
            double maxEval = Double.MIN_VALUE;
            for (Move move : possibleMoves) {
                int[][] newBoard = copyBoard(evalBoard);
                simulateMove(move.fromRow, move.fromCol, move.toRow, move.toCol, newBoard);
                double eval = minMax(newBoard, depth - 1, false, alpha, beta, aiPlayer, nextPlayer);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            double minEval = Double.MAX_VALUE;
            for (Move move : possibleMoves) {
                int[][] newBoard = copyBoard(evalBoard);
                simulateMove(move.fromRow, move.fromCol, move.toRow, move.toCol, newBoard);
                double eval = minMax(newBoard, depth - 1, true, alpha, beta, aiPlayer, nextPlayer);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    // --- Вспомогательные методы для MinMax (работа с копией доски) ---

    private boolean hasAnyValidMovesForPlayerOnBoard(int[][] evalBoard, int player) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (isPlayerPieceOnBoard(evalBoard, row, col, player) && hasAnyValidMoveForPieceOnBoard(evalBoard, row, col, player)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Move> getAllPossibleMovesOnBoard(int[][] evalBoard, int player) {
        List<Move> moves = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (isPlayerPieceOnBoard(evalBoard, row, col, player)) {
                    moves.addAll(getPossibleJumpsForPieceOnBoard(evalBoard, row, col));
                }
            }
        }
        if (!moves.isEmpty()) {
            return moves;
        }
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (isPlayerPieceOnBoard(evalBoard, row, col, player)) {
                    moves.addAll(getPossibleRegularMovesForPieceOnBoard(evalBoard, row, col));
                }
            }
        }
        return moves;
    }

    private boolean isPlayerPieceOnBoard(int[][] evalBoard, int row, int col, int player) {
        int piece = evalBoard[row][col];
        if (player == BLACK_PLAYER) {
            return piece == BLACK_PIECE || piece == BLACK_KING;
        } else {
            return piece == WHITE_PIECE || piece == WHITE_KING;
        }
    }

    private boolean hasAnyValidMoveForPieceOnBoard(int[][] evalBoard, int row, int col, int player) {
        int piece = evalBoard[row][col];
        if (canPieceJumpOnBoard(evalBoard, row, col, player)) {
            return true;
        }
        if (isKing(piece)) {
            int[][] directions = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
            for (int[] dir : directions) {
                for (int distance = 1; distance < 8; distance++) {
                    int newRow = row + dir[0] * distance;
                    int newCol = col + dir[1] * distance;
                    if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                        if (evalBoard[newRow][newCol] == EMPTY && isValidKingMoveOnBoard(evalBoard, row, col, newRow, newCol)) {
                            return true;
                        }
                    } else {
                        break;
                    }
                }
            }
        } else {
            int[][] directions = (piece == BLACK_PIECE) ?
                    new int[][]{{1,1}, {1,-1}} : new int[][]{{-1,1}, {-1,-1}};
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8 && evalBoard[newRow][newCol] == EMPTY) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canPieceJumpOnBoard(int[][] evalBoard, int row, int col, int player) {
        int piece = evalBoard[row][col];
        if (isKing(piece)) {
            int[][] directions = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
            for (int[] dir : directions) {
                for (int distance = 1; distance < 8; distance++) {
                    int newRow = row + dir[0] * distance;
                    int newCol = col + dir[1] * distance;
                    if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                        if (isValidKingJumpOnBoard(evalBoard, row, col, newRow, newCol, player)) {
                            return true;
                        }
                    } else {
                        break;
                    }
                }
            }
            return false;
        } else {
            int[][] directions = {{2,2}, {2,-2}, {-2,2}, {-2,-2}};
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                    if (isValidSimpleJumpOnBoard(evalBoard, row, col, newRow, newCol, player)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private boolean isValidKingMoveOnBoard(int[][] evalBoard, int fromRow, int fromCol, int toRow, int toCol) {
        if (!isDiagonal(fromRow, fromCol, toRow, toCol)) return false;
        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);
        int currentRow = fromRow + rowStep;
        int currentCol = fromCol + colStep;
        while (currentRow != toRow && currentCol != toCol) {
            if (evalBoard[currentRow][currentCol] != EMPTY) {
                return false;
            }
            currentRow += rowStep;
            currentCol += colStep;
        }
        return true;
    }

    private boolean isValidKingJumpOnBoard(int[][] evalBoard, int fromRow, int fromCol, int toRow, int toCol, int player) {
        if (!isDiagonal(fromRow, fromCol, toRow, toCol)) return false;
        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);
        int currentRow = fromRow + rowStep;
        int currentCol = fromCol + colStep;
        boolean foundEnemy = false;
        while (currentRow != toRow && currentCol != toCol) {
            int currentPiece = evalBoard[currentRow][currentCol];
            if (currentPiece != EMPTY) {
                if (foundEnemy) return false;
                boolean isEnemy = (player == BLACK_PLAYER) ?
                        (currentPiece == WHITE_PIECE || currentPiece == WHITE_KING) :
                        (currentPiece == BLACK_PIECE || currentPiece == BLACK_KING);
                if (isEnemy) {
                    foundEnemy = true;
                } else {
                    return false;
                }
            }
            currentRow += rowStep;
            currentCol += colStep;
        }
        return foundEnemy && evalBoard[toRow][toCol] == EMPTY;
    }

    private boolean isValidSimpleJumpOnBoard(int[][] evalBoard, int fromRow, int fromCol, int toRow, int toCol, int player) {
        if (Math.abs(toRow - fromRow) != 2 || Math.abs(toCol - fromCol) != 2) return false;
        int middleRow = (fromRow + toRow) / 2;
        int middleCol = (fromCol + toCol) / 2;
        if (middleRow < 0 || middleRow >= 8 || middleCol < 0 || middleCol >= 8) return false;
        int middlePiece = evalBoard[middleRow][middleCol];
        boolean isEnemy = (player == BLACK_PLAYER) ?
                (middlePiece == WHITE_PIECE || middlePiece == WHITE_KING) :
                (middlePiece == BLACK_PIECE || middlePiece == BLACK_KING);
        boolean isDestinationEmpty = evalBoard[toRow][toCol] == EMPTY;
        return isEnemy && isDestinationEmpty;
    }

    private List<Move> getPossibleJumpsForPieceOnBoard(int[][] evalBoard, int row, int col) {
        List<Move> jumps = new ArrayList<>();
        int piece = evalBoard[row][col];
        int player = (piece == BLACK_PIECE || piece == BLACK_KING) ? BLACK_PLAYER : WHITE_PLAYER;
        if (isKing(piece)) {
            int[][] directions = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
            for (int[] dir : directions) {
                for (int distance = 1; distance < 8; distance++) {
                    int newRow = row + dir[0] * distance;
                    int newCol = col + dir[1] * distance;
                    if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                        if (isValidKingJumpOnBoard(evalBoard, row, col, newRow, newCol, player)) {
                            jumps.add(new Move(row, col, newRow, newCol));
                        }
                    } else {
                        break;
                    }
                }
            }
        } else {
            int[][] directions = {{2,2}, {2,-2}, {-2,2}, {-2,-2}};
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                    if (isValidSimpleJumpOnBoard(evalBoard, row, col, newRow, newCol, player)) {
                        jumps.add(new Move(row, col, newRow, newCol));
                    }
                }
            }
        }
        return jumps;
    }

    private List<Move> getPossibleRegularMovesForPieceOnBoard(int[][] evalBoard, int row, int col) {
        List<Move> moves = new ArrayList<>();
        int piece = evalBoard[row][col];
        if (isKing(piece)) {
            int[][] directions = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
            for (int[] dir : directions) {
                for (int distance = 1; distance < 8; distance++) {
                    int newRow = row + dir[0] * distance;
                    int newCol = col + dir[1] * distance;
                    if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8 && evalBoard[newRow][newCol] == EMPTY) {
                        if (isValidKingMoveOnBoard(evalBoard, row, col, newRow, newCol)) {
                            moves.add(new Move(row, col, newRow, newCol));
                        }
                    } else {
                        break;
                    }
                }
            }
        } else {
            int[][] directions = (piece == BLACK_PIECE) ?
                    new int[][]{{1,1}, {1,-1}} : new int[][]{{-1,1}, {-1,-1}};
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8 && evalBoard[newRow][newCol] == EMPTY) {
                    moves.add(new Move(row, col, newRow, newCol));
                }
            }
        }
        return moves;
    }

    /**
     * Применяет ход к указанной доске (копии) без изменения основной доски.
     */
    private void simulateMove(int fromRow, int fromCol, int toRow, int toCol, int[][] simBoard) {
        int piece = simBoard[fromRow][fromCol];
        boolean wasJump = Math.abs(toRow - fromRow) > 1;
        simBoard[toRow][toCol] = simBoard[fromRow][fromCol];
        simBoard[fromRow][fromCol] = EMPTY;
        if (wasJump) {
            if (isKing(piece)) {
                int rowStep = Integer.compare(toRow, fromRow);
                int colStep = Integer.compare(toCol, fromCol);
                int currentRow = fromRow + rowStep;
                int currentCol = fromCol + colStep;
                while (currentRow != toRow && currentCol != toCol) {
                    if (simBoard[currentRow][currentCol] != EMPTY) {
                        simBoard[currentRow][currentCol] = EMPTY;
                    }
                    currentRow += rowStep;
                    currentCol += colStep;
                }
            } else {
                int middleRow = (fromRow + toRow) / 2;
                int middleCol = (fromCol + toCol) / 2;
                simBoard[middleRow][middleCol] = EMPTY;
            }
        }
        if ((piece == BLACK_PIECE && toRow == 7) || (piece == WHITE_PIECE && toRow == 0)) {
            simBoard[toRow][toCol] = (piece == BLACK_PIECE) ? BLACK_KING : WHITE_KING;
        }
    }

    /**
     * Создаёт копию текущей доски.
     */
    private int[][] copyBoard() {
        int[][] newBoard = new int[8][8];
        for (int i = 0; i < 8; i++) {
            System.arraycopy(board[i], 0, newBoard[i], 0, 8);
        }
        return newBoard;
    }

    /**
     * Создаёт копию переданной доски.
     */
    private int[][] copyBoard(int[][] original) {
        int[][] newBoard = new int[8][8];
        for (int i = 0; i < 8; i++) {
            System.arraycopy(original[i], 0, newBoard[i], 0, 8);
        }
        return newBoard;
    }

    /**
     * Оценивает позицию на доске для ИИ (материал, мобильность, угрозы, центральность).
     */
    private double evaluatePosition(int[][] evalBoard, int aiPlayer, int currentPlayer) {
        double aiScore = 0;
        double opponentScore = 0;
        double aiMobility = 0;
        double opponentMobility = 0;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = evalBoard[row][col];
                if (piece == EMPTY) continue;
                boolean isAi = (aiPlayer == BLACK_PLAYER && (piece == BLACK_PIECE || piece == BLACK_KING)) ||
                        (aiPlayer == WHITE_PLAYER && (piece == WHITE_PIECE || piece == WHITE_KING));
                double value = isKing(piece) ? 3 : 1;

                if (!isKing(piece)) {
                    if (isAi) {
                        value += (aiPlayer == BLACK_PLAYER) ? row * 0.2 : (7 - row) * 0.2;
                    } else {
                        value += (aiPlayer == BLACK_PLAYER) ? (7 - row) * 0.2 : row * 0.2;
                    }
                }

                if (row >= 2 && row <= 5 && col >= 2 && col <= 5) {
                    value += isAi ? 0.3 : -0.3;
                }

                int oppPlayer = (aiPlayer == BLACK_PLAYER) ? WHITE_PLAYER : BLACK_PLAYER;
                int threatCount = getThreatCount(evalBoard, row, col, oppPlayer);
                if (threatCount > 0) {
                    if (isAi) {
                        value -= (isKing(piece) ? 6 : 3) * threatCount;
                    } else {
                        value += (isKing(piece) ? 6 : 3) * threatCount;
                    }
                }

                if (isAi) {
                    aiScore += value;
                } else {
                    opponentScore += value;
                }
            }
        }

        aiMobility = countMobility(evalBoard, aiPlayer);
        int oppPlayer = (aiPlayer == BLACK_PLAYER) ? WHITE_PLAYER : BLACK_PLAYER;
        opponentMobility = countMobility(evalBoard, oppPlayer);
        double mobilityBonus = aiMobility - opponentMobility;
        return ((aiScore - opponentScore) * 10 + mobilityBonus);
    }

    /**
     * Подсчитывает общую мобильность (количество возможных ходов) для игрока.
     */
    private int countMobility(int[][] evalBoard, int player) {
        int count = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (isPlayerPieceOnBoard(evalBoard, row, col, player)) {
                    if (isKing(evalBoard[row][col])) {
                        int[][] dirs = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
                        for (int[] d : dirs) {
                            for (int dist = 1; dist < 8; dist++) {
                                int nr = row + d[0] * dist;
                                int nc = col + d[1] * dist;
                                if (nr < 0 || nr >= 8 || nc < 0 || nc >= 8) break;
                                if (evalBoard[nr][nc] != EMPTY) break;
                                count++;
                            }
                        }
                    } else {
                        int[][] dirs = (player == BLACK_PLAYER) ?
                                new int[][]{{1,1},{1,-1}} :
                                new int[][]{{-1,1},{-1,-1}};
                        for (int[] d : dirs) {
                            int nr = row + d[0];
                            int nc = col + d[1];
                            if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8 && evalBoard[nr][nc] == EMPTY) {
                                count++;
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    /**
     * Подсчитывает количество угроз (возможных взятий) вражескими шашками данной клетке.
     */
    private int getThreatCount(int[][] evalBoard, int targetRow, int targetCol, int attackerPlayer) {
        int piece = evalBoard[targetRow][targetCol];
        if (piece == EMPTY) return 0;
        int threatCount = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int attackerPiece = evalBoard[r][c];
                if (!isPlayerPieceOnBoard(evalBoard, r, c, attackerPlayer)) continue;
                if (isKing(attackerPiece)) {
                    if (Math.abs(r - targetRow) == Math.abs(c - targetCol) &&
                            isValidKingJumpOnBoard(evalBoard, r, c, targetRow, targetCol, attackerPlayer)) {
                        threatCount++;
                    }
                } else {
                    if (Math.abs(r - targetRow) == 2 && Math.abs(c - targetCol) == 2 &&
                            isValidSimpleJumpOnBoard(evalBoard, r, c, targetRow, targetCol, attackerPlayer)) {
                        threatCount++;
                    }
                }
            }
        }
        return threatCount;
    }

    /**
     * Проверяет, находится ли шашка под угрозой взятия со стороны противника.
     */
    private boolean isPieceUnderAttack(int[][] evalBoard, int targetRow, int targetCol, int attackerPlayer) {
        int piece = evalBoard[targetRow][targetCol];
        if (piece == EMPTY) return false;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int attackerPiece = evalBoard[r][c];
                if (!isPlayerPieceOnBoard(evalBoard, r, c, attackerPlayer)) continue;
                if (isKing(attackerPiece)) {
                    if (Math.abs(r - targetRow) == Math.abs(c - targetCol) &&
                            isValidKingJumpOnBoard(evalBoard, r, c, targetRow, targetCol, attackerPlayer)) {
                        return true;
                    }
                } else {
                    if (Math.abs(r - targetRow) == 2 && Math.abs(c - targetCol) == 2 &&
                            isValidSimpleJumpOnBoard(evalBoard, r, c, targetRow, targetCol, attackerPlayer)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Выполняет ход на основной доске. Возвращает true при успехе.
     */
    public boolean makeMove(int fromRow, int fromCol, int toRow, int toCol) {
        Log.d(TAG, "makeMove from (" + fromRow + "," + fromCol + ") to (" + toRow + "," + toCol + ")");
        if (!isValidMove(fromRow, fromCol, toRow, toCol)) {
            Log.d(TAG, "Move invalid");
            return false;
        }

        int piece = board[fromRow][fromCol];
        List<int[]> capturedPieces = new ArrayList<>();
        boolean wasJump = false;

        if (isKing(piece)) {
            capturedPieces = getCapturedPiecesInMove(fromRow, fromCol, toRow, toCol);
            wasJump = !capturedPieces.isEmpty();
        } else {
            wasJump = Math.abs(toRow - fromRow) == 2;
            if (wasJump) {
                int middleRow = (fromRow + toRow) / 2;
                int middleCol = (fromCol + toCol) / 2;
                capturedPieces.add(new int[]{middleRow, middleCol});
            }
        }

        board[toRow][toCol] = board[fromRow][fromCol];
        board[fromRow][fromCol] = EMPTY;

        if (wasJump) {
            for (int[] captured : capturedPieces) {
                int capturedRow = captured[0];
                int capturedCol = captured[1];
                int capturedPiece = board[capturedRow][capturedCol];
                boolean isEnemy = (currentPlayer == BLACK_PLAYER) ?
                        (capturedPiece == WHITE_PIECE || capturedPiece == WHITE_KING) :
                        (capturedPiece == BLACK_PIECE || capturedPiece == BLACK_KING);
                if (isEnemy) {
                    board[capturedRow][capturedCol] = EMPTY;
                    Log.d(TAG, "Captured piece at (" + capturedRow + "," + capturedCol + ") type: " + capturedPiece);
                }
            }
        }

        checkForPromotion(toRow, toCol);

        if (wasJump) {
            boolean canContinue = canContinueJumping(toRow, toCol);
            if (canContinue) {
                mustContinueJump = true;
                continueJumpRow = toRow;
                continueJumpCol = toCol;
                if (aiPlayer != EMPTY && currentPlayer == aiPlayer) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        makeAIMove();
                    }, 500);
                }
                return true;
            }
        }

        int previousPlayer = currentPlayer;
        endTurn();

        boolean aiJustMoved = (aiPlayer != EMPTY && previousPlayer == aiPlayer);
        if (aiJustMoved) {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (mainActivityCallback != null) {
                    mainActivityCallback.onAImoveCompleted();
                }
            });
        }

        if (aiPlayer != EMPTY && currentPlayer == aiPlayer) {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                makeAIMove();
            }, 500);
        }

        return true;
    }

    /**
     * Завершает текущий ход: сбрасывает флаги продолжения и меняет игрока.
     */
    private void endTurn() {
        mustContinueJump = false;
        continueJumpRow = -1;
        continueJumpCol = -1;
        currentPlayer = (currentPlayer == BLACK_PLAYER) ? WHITE_PLAYER : BLACK_PLAYER;
        Log.d(TAG, "Turn ended. Now: " + (currentPlayer == BLACK_PLAYER ? "BLACK" : "WHITE"));
    }

    /**
     * Проверяет валидность хода (с учётом текущего состояния игры).
     */
    public boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (toRow < 0 || toRow >= 8 || toCol < 0 || toCol >= 8) return false;
        if (board[toRow][toCol] != EMPTY) return false;
        if (!isDiagonal(fromRow, fromCol, toRow, toCol)) return false;

        int piece = board[fromRow][fromCol];
        int distance = Math.abs(toRow - fromRow);

        if (mustContinueJump) {
            if (fromRow != continueJumpRow || fromCol != continueJumpCol) {
                return false;
            }
            return isValidJump(fromRow, fromCol, toRow, toCol);
        }

        boolean globalForcedJumps = hasForcedJumps();
        if (globalForcedJumps) {
            return isValidJump(fromRow, fromCol, toRow, toCol);
        }

        if (isKing(piece)) {
            return isValidKingMove(fromRow, fromCol, toRow, toCol);
        } else {
            if (distance != 1) return false;
            return isValidNormalMove(fromRow, fromCol, toRow, toCol);
        }
    }

    /**
     * Проверяет, расположены ли две клетки на одной диагонали.
     */
    private boolean isDiagonal(int fromRow, int fromCol, int toRow, int toCol) {
        return Math.abs(toRow - fromRow) == Math.abs(toCol - fromCol);
    }

    /**
     * Проверяет, является ли шашка дамкой.
     */
    private boolean isKing(int piece) {
        return piece == BLACK_KING || piece == WHITE_KING;
    }

    /**
     * Проверяет валидность обычного (не взятия) хода для простой шашки.
     */
    private boolean isValidNormalMove(int fromRow, int fromCol, int toRow, int toCol) {
        int piece = board[fromRow][fromCol];
        int rowDiff = toRow - fromRow;
        if (piece == BLACK_PIECE) {
            return rowDiff == 1;
        } else if (piece == WHITE_PIECE) {
            return rowDiff == -1;
        }
        return false;
    }

    /**
     * Проверяет возможность хода дамки без взятия.
     */
    private boolean isValidKingMove(int fromRow, int fromCol, int toRow, int toCol) {
        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);
        int currentRow = fromRow + rowStep;
        int currentCol = fromCol + colStep;
        while (currentRow != toRow && currentCol != toCol) {
            if (board[currentRow][currentCol] != EMPTY) {
                return false;
            }
            currentRow += rowStep;
            currentCol += colStep;
        }
        return true;
    }

    /**
     * Проверяет, является ли ход взятием (для любой шашки).
     */
    private boolean isValidJump(int fromRow, int fromCol, int toRow, int toCol) {
        int piece = board[fromRow][fromCol];
        if (isKing(piece)) {
            return isValidKingJump(fromRow, fromCol, toRow, toCol);
        } else {
            return isValidSimpleJump(fromRow, fromCol, toRow, toCol);
        }
    }

    /**
     * Проверяет валидность взятия простой шашкой.
     */
    private boolean isValidSimpleJump(int fromRow, int fromCol, int toRow, int toCol) {
        int rowDiff = toRow - fromRow;
        int colDiff = toCol - fromCol;
        if (Math.abs(rowDiff) != 2 || Math.abs(colDiff) != 2) {
            return false;
        }
        int middleRow = (fromRow + toRow) / 2;
        int middleCol = (fromCol + toCol) / 2;
        if (middleRow < 0 || middleRow >= 8 || middleCol < 0 || middleCol >= 8) {
            return false;
        }
        int middlePiece = board[middleRow][middleCol];
        boolean isEnemyPiece = (currentPlayer == BLACK_PLAYER) ?
                (middlePiece == WHITE_PIECE || middlePiece == WHITE_KING) :
                (middlePiece == BLACK_PIECE || middlePiece == BLACK_KING);
        boolean isDestinationEmpty = board[toRow][toCol] == EMPTY;
        return isEnemyPiece && isDestinationEmpty;
    }

    /**
     * Проверяет валидность взятия дамкой.
     */
    private boolean isValidKingJump(int fromRow, int fromCol, int toRow, int toCol) {
        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);
        int currentRow = fromRow + rowStep;
        int currentCol = fromCol + colStep;
        boolean foundEnemy = false;
        while (currentRow != toRow && currentCol != toCol) {
            int currentPiece = board[currentRow][currentCol];
            if (currentPiece != EMPTY) {
                if (foundEnemy) return false;
                boolean isEnemy = (currentPlayer == BLACK_PLAYER) ?
                        (currentPiece == WHITE_PIECE || currentPiece == WHITE_KING) :
                        (currentPiece == BLACK_PIECE || currentPiece == BLACK_KING);
                if (isEnemy) {
                    foundEnemy = true;
                } else {
                    return false;
                }
            }
            currentRow += rowStep;
            currentCol += colStep;
        }
        return foundEnemy && board[toRow][toCol] == EMPTY;
    }

    /**
     * Возвращает список координат сбитых шашек при ходе дамки.
     */
    private List<int[]> getCapturedPiecesInMove(int fromRow, int fromCol, int toRow, int toCol) {
        List<int[]> captured = new ArrayList<>();
        int piece = board[fromRow][fromCol];
        if (isKing(piece)) {
            int rowStep = Integer.compare(toRow, fromRow);
            int colStep = Integer.compare(toCol, fromCol);
            int currentRow = fromRow + rowStep;
            int currentCol = fromCol + colStep;
            while (currentRow != toRow && currentCol != toCol) {
                int currentPiece = board[currentRow][currentCol];
                if (currentPiece != EMPTY) {
                    boolean isEnemy = (currentPlayer == BLACK_PLAYER) ?
                            (currentPiece == WHITE_PIECE || currentPiece == WHITE_KING) :
                            (currentPiece == BLACK_PIECE || currentPiece == BLACK_KING);
                    if (isEnemy) {
                        captured.add(new int[]{currentRow, currentCol});
                    } else {
                        break;
                    }
                }
                currentRow += rowStep;
                currentCol += colStep;
            }
        }
        return captured;
    }

    /**
     * Проверяет, может ли шашка продолжить взятие после текущего хода.
     */
    private boolean canContinueJumping(int row, int col) {
        int piece = board[row][col];
        if (isKing(piece)) {
            int[][] directions = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
            for (int[] dir : directions) {
                for (int distance = 1; distance < 8; distance++) {
                    int newRow = row + dir[0] * distance;
                    int newCol = col + dir[1] * distance;
                    if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                        if (isValidKingJump(row, col, newRow, newCol)) {
                            return true;
                        }
                    } else {
                        break;
                    }
                }
            }
        } else {
            int[][] directions = {{2,2}, {2,-2}, {-2,2}, {-2,-2}};
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                    if (isValidSimpleJump(row, col, newRow, newCol)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Проверяет, может ли шашка выполнить хотя бы одно взятие.
     */
    private boolean canPieceJump(int row, int col) {
        int piece = board[row][col];
        if (isKing(piece)) {
            int[][] directions = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
            for (int[] dir : directions) {
                for (int distance = 1; distance < 8; distance++) {
                    int newRow = row + dir[0] * distance;
                    int newCol = col + dir[1] * distance;
                    if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                        if (isValidKingJump(row, col, newRow, newCol)) {
                            return true;
                        }
                    } else {
                        break;
                    }
                }
            }
            return false;
        } else {
            int[][] directions = {{2,2}, {2,-2}, {-2,2}, {-2,-2}};
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                    if (isValidSimpleJump(row, col, newRow, newCol)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Проверяет наличие хотя бы одного допустимого хода у шашки (включая простые ходы).
     */
    private boolean hasAnyValidMove(int row, int col) {
        int piece = board[row][col];
        if (isKing(piece)) {
            int[][] directions = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
            for (int[] dir : directions) {
                for (int distance = 1; distance < 8; distance++) {
                    int newRow = row + dir[0] * distance;
                    int newCol = col + dir[1] * distance;
                    if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                        if (board[newRow][newCol] == EMPTY && isValidKingMove(row, col, newRow, newCol)) {
                            return true;
                        }
                    } else {
                        break;
                    }
                }
            }
        } else {
            int[][] directions = (piece == BLACK_PIECE) ?
                    new int[][]{{1,1}, {1,-1}} : new int[][]{{-1,1}, {-1,-1}};
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8 &&
                        board[newRow][newCol] == EMPTY) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Проверяет, принадлежит ли шашка текущему игроку.
     */
    private boolean isCurrentPlayerPiece(int row, int col) {
        int piece = board[row][col];
        if (currentPlayer == BLACK_PLAYER) {
            return piece == BLACK_PIECE || piece == BLACK_KING;
        } else {
            return piece == WHITE_PIECE || piece == WHITE_KING;
        }
    }

    /**
     * Проверяет, нужно ли превратить шашку в дамку (достигла противоположного края).
     */
    private void checkForPromotion(int row, int col) {
        int piece = board[row][col];
        if (piece == BLACK_PIECE && row == 7) {
            board[row][col] = BLACK_KING;
        } else if (piece == WHITE_PIECE && row == 0) {
            board[row][col] = WHITE_KING;
        }
    }

    /**
     * Проверяет, завершена ли игра (есть победитель).
     */
    public boolean isGameOver() {
        int winner = getWinner();
        if (winner != EMPTY) {
            Log.d(TAG, "Game over! Winner: " + (winner == BLACK_PLAYER ? "BLACK" : "WHITE"));
            return true;
        }
        return false;
    }

    /**
     * Запускает игру, инициируя первый ход ИИ, если он играет белыми.
     */
    public void startGame() {
        if (aiPlayer == WHITE_PLAYER && currentPlayer == WHITE_PLAYER) {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                makeAIMove();
            }, 500);
        }
    }
}