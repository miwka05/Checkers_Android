package com.example.checkers;

import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "CheckersGame";

    // ----- Audio -----
    private SoundPool soundPool;
    private int clickSoundId;
    private float volume = 1.0f;
    private boolean soundEnabled = true;

    // ----- Game settings -----
    private int currentDifficulty = CheckersGame.DIFFICULTY_EASY;
    private int currentAIPlayer = CheckersGame.BLACK_PLAYER;
    private int currentTimeLimitMs = 5 * 60 * 1000;

    // ----- Game state -----
    private CheckersGame game;
    private GridLayout boardLayout;
    private ImageView[][] boardViews;
    private TextView statusText;
    private int selectedRow = -1;
    private int selectedCol = -1;

    // ----- Timers -----
    private long playerTimeLeft;
    private long aiTimeLeft;
    private CountDownTimer activeTimer = null;
    private TextView playerTimerText;
    private TextView aiTimerText;
    private boolean useTimers = true;

    // ----- Statistics -----
    private static final String PREFS_NAME = "CheckersPrefs";
    private static final String KEY_TOTAL_GAMES_EASY = "total_games_easy";
    private static final String KEY_PLAYER_WINS_EASY = "player_wins_easy";
    private static final String KEY_AI_WINS_EASY = "ai_wins_easy";
    private static final String KEY_TOTAL_GAMES_MEDIUM = "total_games_medium";
    private static final String KEY_PLAYER_WINS_MEDIUM = "player_wins_medium";
    private static final String KEY_AI_WINS_MEDIUM = "ai_wins_medium";
    private static final String KEY_TOTAL_GAMES_HARD = "total_games_hard";
    private static final String KEY_PLAYER_WINS_HARD = "player_wins_hard";
    private static final String KEY_AI_WINS_HARD = "ai_wins_hard";

    /**
     * Инициализация активности: настройка аудио и отображение главного меню.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initAudio();
        showMainMenu();
    }

    /**
     * Инициализирует SoundPool в зависимости от версии Android.
     */
    private void initAudio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder().setMaxStreams(4).build();
        } else {
            soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
        }
    }

    /**
     * Проигрывает звук клика, если звук включён.
     */
    private void playClickSound() {
        if (soundPool != null && clickSoundId != 0 && soundEnabled) {
            soundPool.play(clickSoundId, volume, volume, 1, 0, 1f);
        }
    }

    /**
     * Останавливает активный таймер (если он запущен).
     */
    private void stopActiveTimer() {
        if (activeTimer != null) {
            activeTimer.cancel();
            activeTimer = null;
        }
    }

    /**
     * Запускает обратный отсчёт для текущего игрока (человека или ИИ).
     */
    private void startTimerForCurrentPlayer() {
        if (!useTimers || game.isGameOver()) return;
        stopActiveTimer();
        boolean isPlayerTurn = (game.getCurrentPlayer() != currentAIPlayer);
        long timeLeft = isPlayerTurn ? playerTimeLeft : aiTimeLeft;
        if (timeLeft <= 0) {
            onTimeOut(isPlayerTurn ? game.getCurrentPlayer() : currentAIPlayer);
            return;
        }
        activeTimer = new CountDownTimer(timeLeft, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isPlayerTurn) {
                    playerTimeLeft = millisUntilFinished;
                } else {
                    aiTimeLeft = millisUntilFinished;
                }
                updateTimerDisplays();
            }

            @Override
            public void onFinish() {
                if (isPlayerTurn) {
                    playerTimeLeft = 0;
                } else {
                    aiTimeLeft = 0;
                }
                updateTimerDisplays();
                onTimeOut(isPlayerTurn ? game.getCurrentPlayer() : currentAIPlayer);
            }
        }.start();
    }

    /**
     * Обновляет отображение времени для игрока и ИИ на экране.
     */
    private void updateTimerDisplays() {
        if (!useTimers) {
            if (playerTimerText != null) playerTimerText.setText("Игрок: ∞");
            if (aiTimerText != null) aiTimerText.setText("ИИ:     ∞");
            return;
        }
        if (playerTimerText != null) {
            long min = playerTimeLeft / 60000;
            long sec = (playerTimeLeft % 60000) / 1000;
            playerTimerText.setText(String.format("Игрок: %02d:%02d", min, sec));
        }
        if (aiTimerText != null) {
            long min = aiTimeLeft / 60000;
            long sec = (aiTimeLeft % 60000) / 1000;
            aiTimerText.setText(String.format("ИИ:     %02d:%02d", min, sec));
        }
    }

    /**
     * Обработка истечения времени одного из игроков.
     */
    private void onTimeOut(int loser) {
        stopActiveTimer();
        int winner = (loser == CheckersGame.WHITE_PLAYER) ? CheckersGame.BLACK_PLAYER : CheckersGame.WHITE_PLAYER;
        String loserName = (loser == CheckersGame.BLACK_PLAYER) ? "Черные" : "Белые";
        incrementWin(winner, currentDifficulty);
        new AlertDialog.Builder(this)
                .setTitle("Время вышло!")
                .setMessage(loserName + " исчерпали лимит времени.\nПобедили " +
                        (winner == CheckersGame.BLACK_PLAYER ? "Черные" : "Белые") + "!")
                .setPositiveButton("Новая игра", (d, w) -> onRestartClick())
                .setNegativeButton("В меню", (d, w) -> showMainMenu())
                .setCancelable(false)
                .show();
    }

    /**
     * Увеличивает счётчик побед в зависимости от победителя и уровня сложности.
     */
    private void incrementWin(int winner, int difficulty) {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        String suffix = getKeySuffix(difficulty);
        int total = prefs.getInt("total_games" + suffix, 0) + 1;
        int playerWins = prefs.getInt("player_wins" + suffix, 0);
        int aiWins = prefs.getInt("ai_wins" + suffix, 0);
        if (winner != currentAIPlayer) {
            playerWins++;
        } else {
            aiWins++;
        }
        editor.putInt("total_games" + suffix, total);
        editor.putInt("player_wins" + suffix, playerWins);
        editor.putInt("ai_wins" + suffix, aiWins);
        editor.apply();
    }

    /**
     * Возвращает строку с полной статистикой по всем уровням сложности.
     */
    private String getStatisticsText() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        sb.append(" Уровень сложности:\n");
        appendStatsForDifficulty(sb, "Легкий", prefs,
                "total_games_easy", "player_wins_easy", "ai_wins_easy");
        appendStatsForDifficulty(sb, "Средний", prefs,
                "total_games_medium", "player_wins_medium", "ai_wins_medium");
        appendStatsForDifficulty(sb, "Сложный", prefs,
                "total_games_hard", "player_wins_hard", "ai_wins_hard");
        return sb.toString();
    }

    /**
     * Добавляет строку статистики для заданного уровня сложности.
     */
    private void appendStatsForDifficulty(StringBuilder sb, String name,
                                          android.content.SharedPreferences prefs,
                                          String totalKey, String playerKey, String aiKey) {
        int total = prefs.getInt(totalKey, 0);
        int player = prefs.getInt(playerKey, 0);
        int ai = prefs.getInt(aiKey, 0);
        sb.append("🔸 ").append(name).append(":\n");
        sb.append("   Всего игр: ").append(total).append("\n");
        sb.append("   Побед игрока: ").append(player).append("\n");
        sb.append("   Побед ИИ: ").append(ai).append("\n");
    }

    /**
     * Отображает экран статистики с кнопками возврата и сброса.
     */
    private void showStatistics() {
        setContentView(R.layout.activity_statistics);
        TextView statsTextView = findViewById(R.id.statisticsText);
        Button backButton = findViewById(R.id.backFromStatsButton);
        Button clearStatsButton = findViewById(R.id.clearStatsButton);
        statsTextView.setText(getStatisticsText());
        clearStatsButton.setOnClickListener(v -> {
            playClickSound();
            new AlertDialog.Builder(this)
                    .setTitle("Сброс статистики")
                    .setMessage("Вы уверены, что хотите удалить всю статистику?")
                    .setPositiveButton("Да", (d, w) -> clearStatistics())
                    .setNegativeButton("Нет", null)
                    .show();
        });
        backButton.setOnClickListener(v -> {
            playClickSound();
            showMainMenu();
        });
    }

    /**
     * Полностью удаляет сохранённую статистику из SharedPreferences.
     */
    private void clearStatistics() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.remove("total_games_easy");
        editor.remove("player_wins_easy");
        editor.remove("ai_wins_easy");
        editor.remove("total_games_medium");
        editor.remove("player_wins_medium");
        editor.remove("ai_wins_medium");
        editor.remove("total_games_hard");
        editor.remove("player_wins_hard");
        editor.remove("ai_wins_hard");
        editor.apply();
        Toast.makeText(this, "Статистика сброшена", Toast.LENGTH_SHORT).show();
    }

    /**
     * Возвращает суффикс ключа в зависимости от уровня сложности.
     */
    private String getKeySuffix(int difficulty) {
        switch (difficulty) {
            case CheckersGame.DIFFICULTY_EASY: return "_easy";
            case CheckersGame.DIFFICULTY_MEDIUM: return "_medium";
            case CheckersGame.DIFFICULTY_HARD: return "_hard";
            default: return "_easy";
        }
    }

    /**
     * Отображает главное меню игры.
     */
    private void showMainMenu() {
        setContentView(R.layout.main_menu);
        Button startGameBtn = findViewById(R.id.startGameButton);
        Button settingsBtn = findViewById(R.id.settingsButton);
        Button statisticsBtn = findViewById(R.id.statisticsButton);
        startGameBtn.setOnClickListener(v -> {
            playClickSound();
            showDifficultyMenu();
        });
        settingsBtn.setOnClickListener(v -> {
            playClickSound();
            showSettings();
        });
        statisticsBtn.setOnClickListener(v -> {
            playClickSound();
            showStatistics();
        });
    }

    /**
     * Отображает диалог выбора уровня сложности.
     */
    private void showDifficultyMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите сложность");
        String[] difficulties = {"Легкая", "Средняя", "Сложная"};
        builder.setItems(difficulties, (dialog, which) -> {
            switch (which) {
                case 0: currentDifficulty = CheckersGame.DIFFICULTY_EASY; break;
                case 1: currentDifficulty = CheckersGame.DIFFICULTY_MEDIUM; break;
                case 2: currentDifficulty = CheckersGame.DIFFICULTY_HARD; break;
            }
            showTimeLimitMenu();
        });
        builder.show();
    }

    /**
     * Отображает диалог выбора временного лимита.
     */
    private void showTimeLimitMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите лимит времени");
        String[] times = {"1 минута", "3 минуты", "5 минут", "10 минут", "Без ограничения"};
        builder.setItems(times, (dialog, which) -> {
            switch (which) {
                case 0: currentTimeLimitMs = 1 * 60 * 1000; break;
                case 1: currentTimeLimitMs = 3 * 60 * 1000; break;
                case 2: currentTimeLimitMs = 5 * 60 * 1000; break;
                case 3: currentTimeLimitMs = 10 * 60 * 1000; break;
                case 4: currentTimeLimitMs = -1; break;
            }
            showPlayerColorMenu();
        });
        builder.show();
    }

    /**
     * Отображает диалог выбора цвета игрока (белые — ходят первыми).
     */
    private void showPlayerColorMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите ваш цвет");
        String[] colorOptions = {"Белые (ходите первым)", "Чёрные (ходите вторым)"};
        builder.setItems(colorOptions, (dialog, which) -> {
            if (which == 0) {
                currentAIPlayer = CheckersGame.BLACK_PLAYER;
            } else {
                currentAIPlayer = CheckersGame.WHITE_PLAYER;
            }
            startGame();
        });
        builder.show();
    }

    /**
     * Отображает экран настроек: громкость и вкл/выкл звука.
     */
    private void showSettings() {
        setContentView(R.layout.settings);
        SeekBar volumeSeekBar = findViewById(R.id.volumeSeekBar);
        Switch soundSwitch = findViewById(R.id.soundSwitch);
        Button backButton = findViewById(R.id.backToMenuButton);
        volumeSeekBar.setProgress((int) (volume * 100));
        soundSwitch.setChecked(soundEnabled);
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volume = progress / 100.0f;
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> soundEnabled = isChecked);
        backButton.setOnClickListener(v -> {
            playClickSound();
            showMainMenu();
        });
    }

    /**
     * Запускает новую игру с текущими настройками.
     */
    private void startGame() {
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate started");
        try {
            useTimers = (currentTimeLimitMs > 0);
            if (useTimers) {
                playerTimeLeft = currentTimeLimitMs;
                aiTimeLeft = currentTimeLimitMs;
            }
            initializeGameWithSettings();
            game.setOnGameUpdateListener(this::onAImoveCompleted);
            setupBoard();
            playerTimerText = findViewById(R.id.playerTimerText);
            aiTimerText = findViewById(R.id.aiTimerText);
            updateTimerDisplays();
            Button restartButton = findViewById(R.id.restartButton);
            restartButton.setOnClickListener(v -> onRestartClick());
            Button surrenderButton = findViewById(R.id.surrenderButton);
            surrenderButton.setOnClickListener(v -> onSurrenderClick());
            statusText = findViewById(R.id.statusText);
            updateStatus();
            game.startGame();
            if (useTimers) {
                startTimerForCurrentPlayer();
            }
            Log.d(TAG, "onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
        }
    }

    /**
     * Коллбэк, вызываемый после завершения хода ИИ.
     */
    private void onAImoveCompleted() {
        Log.d(TAG, "onAImoveCompleted called in MainActivity");
        updateBoard();
        updateStatus();
        if (!game.isGameOver()) {
            if (useTimers) {
                startTimerForCurrentPlayer();
            }
        } else {
            stopActiveTimer();
            new android.os.Handler().postDelayed(this::showGameOverDialog, 400);
        }
    }

    /**
     * Создаёт новый объект CheckersGame с текущими настройками.
     */
    private void initializeGameWithSettings() {
        game = new CheckersGame(currentDifficulty, currentAIPlayer);
        boardLayout = findViewById(R.id.boardLayout);
        if (boardLayout == null) {
            throw new RuntimeException("boardLayout not found");
        }
        boardViews = new ImageView[8][8];
    }

    /**
     * Отображает диалог сдачи: записывает поражение и возвращает в меню.
     */
    public void onSurrenderClick() {
        new AlertDialog.Builder(this)
                .setTitle("Сдаться?")
                .setMessage("Вы уверены, что хотите сдаться? Поражение будет засчитано.")
                .setPositiveButton("Да", (dialog, which) -> {
                    int winner = currentAIPlayer;
                    int loser = (currentAIPlayer == CheckersGame.BLACK_PLAYER)
                            ? CheckersGame.WHITE_PLAYER
                            : CheckersGame.BLACK_PLAYER;
                    incrementWin(winner, currentDifficulty);
                    stopActiveTimer();
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Вы сдались")
                            .setMessage("Победили " +
                                    (winner == CheckersGame.BLACK_PLAYER ? "Черные" : "Белые") + "!")
                            .setPositiveButton("В меню", (d, w) -> showMainMenu())
                            .setCancelable(false)
                            .show();
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    /**
     * Запрашивает подтверждение и перезапускает текущую игру.
     */
    public void onRestartClick() {
        new AlertDialog.Builder(this)
                .setTitle("Новая игра")
                .setMessage("Вы уверены, что хотите начать новую игру? Текущая партия будет потеряна.")
                .setPositiveButton("Да", (dialog, which) -> {
                    stopActiveTimer();
                    if (useTimers) {
                        playerTimeLeft = currentTimeLimitMs;
                        aiTimeLeft = currentTimeLimitMs;
                    }
                    startGame();
                })
                .setNegativeButton("Нет", null)
                .setCancelable(true)
                .show();
    }

    /**
     * Инициализирует и отображает игровую доску (8x8).
     */
    private void setupBoard() {
        if (boardLayout == null) return;
        boardLayout.removeAllViews();
        boardLayout.setColumnCount(8);
        boardLayout.setRowCount(8);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ImageView cell = new ImageView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = 0;
                params.columnSpec = GridLayout.spec(col, 1f);
                params.rowSpec = GridLayout.spec(row, 1f);
                params.setMargins(1, 1, 1, 1);
                cell.setLayoutParams(params);
                setupCell(cell, row, col);
                boardViews[row][col] = cell;
                boardLayout.addView(cell);
            }
        }
        updateBoard();
    }

    /**
     * Настраивает внешний вид и обработчик клика для одной клетки доски.
     */
    private void setupCell(ImageView cell, final int row, final int col) {
        if ((row + col) % 2 == 0) {
            cell.setBackgroundColor(Color.parseColor("#F0D9B5"));
        } else {
            cell.setBackgroundColor(Color.parseColor("#B58863"));
        }
        cell.setOnClickListener(v -> handleCellClick(row, col));
    }

    /**
     * Обрабатывает клик по клетке: выбор фигуры или ход.
     */
    private void handleCellClick(int row, int col) {
        if (game.getCurrentPlayer() == currentAIPlayer) return;
        if (useTimers) stopActiveTimer();
        Log.d(TAG, "Cell clicked: " + row + ", " + col);
        try {
            if ((row + col) % 2 == 0) {
                if (!game.mustContinueJump()) {
                    resetSelection();
                }
                if (useTimers && !game.isGameOver()) {
                    startTimerForCurrentPlayer();
                }
                return;
            }
            if (selectedRow == -1 && selectedCol == -1) {
                if (game.isValidSelection(row, col)) {
                    selectedRow = row;
                    selectedCol = col;
                    highlightSelectedCell();
                    highlightAvailableMoves(row, col);
                    Log.d(TAG, "Piece selected: " + row + ", " + col);
                } else {
                    if (useTimers && !game.isGameOver()) {
                        startTimerForCurrentPlayer();
                    }
                }
            } else {
                Log.d(TAG, "Attempting move from (" + selectedRow + "," + selectedCol + ") to (" + row + "," + col + ")");
                if (isValidTarget(row, col)) {
                    if (game.makeMove(selectedRow, selectedCol, row, col)) {
                        updateBoard();
                        if (game.isGameOver()) {
                            stopActiveTimer();
                            showGameOverDialog();
                        } else if (game.mustContinueJump()) {
                            selectedRow = game.getContinueJumpRow();
                            selectedCol = game.getContinueJumpCol();
                            highlightSelectedCell();
                            highlightAvailableMoves(selectedRow, selectedCol);
                            Log.d(TAG, "Continue jumping with piece: " + selectedRow + ", " + selectedCol);
                        } else {
                            resetSelection();
                            updateStatus();
                        }
                    } else {
                        resetSelection();
                    }
                } else {
                    if (!game.mustContinueJump() && game.isValidSelection(row, col)) {
                        resetSelection();
                        selectedRow = row;
                        selectedCol = col;
                        highlightSelectedCell();
                        highlightAvailableMoves(row, col);
                    } else {
                        resetSelection();
                    }
                }
                if (!game.isGameOver() && !game.mustContinueJump()) {
                    if (useTimers) {
                        startTimerForCurrentPlayer();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in handleCellClick: " + e.getMessage(), e);
            resetSelection();
            if (useTimers && !game.isGameOver()) {
                startTimerForCurrentPlayer();
            }
        }
    }

    /**
     * Проверяет, является ли клетка допустимой целью для хода.
     */
    private boolean isValidTarget(int row, int col) {
        return game.isValidMove(selectedRow, selectedCol, row, col);
    }

    /**
     * Подсвечивает все допустимые ходы от указанной фигуры.
     */
    private void highlightAvailableMoves(int fromRow, int fromCol) {
        Log.d(TAG, "Highlighting moves from (" + fromRow + "," + fromCol + ")");
        resetAllHighlights();
        for (int toRow = 0; toRow < 8; toRow++) {
            for (int toCol = 0; toCol < 8; toCol++) {
                if ((toRow + toCol) % 2 == 1) {
                    if (game.isValidMove(fromRow, fromCol, toRow, toCol)) {
                        boardViews[toRow][toCol].setBackgroundColor(Color.GREEN);
                        Log.d(TAG, "Highlighting valid move to (" + toRow + "," + toCol + ")");
                    }
                }
            }
        }
        highlightSelectedCell();
    }

    /**
     * Подсвечивает выбранную фигуру красным цветом.
     */
    private void highlightSelectedCell() {
        if (selectedRow != -1 && selectedCol != -1) {
            boardViews[selectedRow][selectedCol].setBackgroundColor(Color.RED);
        }
    }

    /**
     * Сбрасывает все подсветки клеток на стандартные цвета доски.
     */
    private void resetAllHighlights() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if ((row + col) % 2 == 0) {
                    boardViews[row][col].setBackgroundColor(Color.parseColor("#F0D9B5"));
                } else {
                    boardViews[row][col].setBackgroundColor(Color.parseColor("#B58863"));
                }
            }
        }
    }

    /**
     * Сбрасывает выделение фигуры.
     */
    private void resetSelection() {
        selectedRow = -1;
        selectedCol = -1;
        resetAllHighlights();
    }

    /**
     * Обновляет отображение фигур на доске в соответствии с текущим состоянием игры.
     */
    private void updateBoard() {
        if (boardViews == null) return;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                try {
                    int piece = game.getPieceAt(row, col);
                    ImageView cell = boardViews[row][col];
                    if (cell != null) {
                        cell.setImageDrawable(null);
                        switch (piece) {
                            case CheckersGame.EMPTY: break;
                            case CheckersGame.BLACK_PIECE: cell.setImageResource(R.drawable.circle_black); break;
                            case CheckersGame.WHITE_PIECE: cell.setImageResource(R.drawable.circle_white); break;
                            case CheckersGame.BLACK_KING: cell.setImageResource(R.drawable.circle_black_king); break;
                            case CheckersGame.WHITE_KING: cell.setImageResource(R.drawable.circle_white_king); break;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating cell " + row + "," + col + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Обновляет текстовое поле статуса (кто ходит, обязательное взятие и т.д.).
     */
    private void updateStatus() {
        if (statusText == null) return;
        try {
            String player = game.getCurrentPlayer() == CheckersGame.BLACK_PLAYER ? "Черные" : "Белые";
            String status = "Ходят: " + player;
            if (game.mustContinueJump()) {
                status += " (Продолжайте брать!)";
            } else if (game.hasForcedJumps()) {
                status += " (Обязательное взятие!)";
            }
            statusText.setText(status);
        } catch (Exception e) {
            Log.e(TAG, "Error updating status: " + e.getMessage());
        }
    }

    /**
     * Показывает диалог окончания игры с результатом и кнопками действий.
     */
    private void showGameOverDialog() {
        try {
            int winner = game.getWinner();
            incrementWin(winner, currentDifficulty);
            String winnerName = winner == CheckersGame.BLACK_PLAYER ? "Черные" : "Белые";
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Игра окончена")
                    .setMessage("Победили " + winnerName + "!")
                    .setPositiveButton("Новая игра", (dialog, which) -> onRestartClick())
                    .setNegativeButton("В меню", (dialog, which) -> {
                        stopActiveTimer();
                        showMainMenu();
                    })
                    .setCancelable(false)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing game over dialog: " + e.getMessage());
        }
    }

    /**
     * Освобождает ресурсы при уничтожении активности: останавливает таймер и SoundPool.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopActiveTimer();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}