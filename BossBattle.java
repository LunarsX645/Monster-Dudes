import java.awt.*;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class BossBattle extends JDialog {

    private Game game;
    private List<Monster> playerMonsters;
    private Monster bossMonster;
    private Monster activePlayerMonster;
    private boolean isBattleOver = false;

    // UI Components for Boss
    private JLabel bossImageLabel;
    private JLabel bossNameLabel;
    private JLabel bossHpLabel;
    private JProgressBar bossHpBar;

    // UI Components for Player
    private JPanel playerMonstersPanel;
    private JTextArea messageArea;
    private JButton attackButton;
    private JButton runButton;
    private JButton closeButton;

    // Boss Stage Data
    private int currentBossStage = 0;
    // HP % thresholds for stages (e.g., 80% HP remaining for stage 1)
    // The last threshold (0) means the boss is defeated
    private final int[] stageThresholds = { 80, 60, 40, 20, 0 };
    private final String[] bossSpritePaths = {
        "/Game Items/Foster_0.png", // Stage 0
        "/Game Items/Foster_1.png", // Stage 1
        "/Game Items/Foster_2.png", // Stage 2
        "/Game Items/Foster_3.png", // Stage 3
        "/Game Items/Foster_4.png"  // Stage 4 (final)
    };
    private int baseBossAttack = 10; // Initial boss attack
    // ⭐ Removed bossHealAmount and bossDamageIncreasePerStage for simplification ⭐
    private int bossDamageIncreasePerStage = 10; // Amount boss damage increases per stage

    public BossBattle(JFrame owner, Game game, List<Monster> playerMonsters) {
        super(owner, "💥 Boss Battle: Mr. Foster!", true);
        this.game = game;
        this.playerMonsters = playerMonsters;

        // Mr. Foster's initial stats.
        // Base attack will be modified by the 50% reduction during his turn.
        this.bossMonster = new Monster("Mr. Foster", 200, baseBossAttack);

        this.activePlayerMonster = getFirstAvailablePlayerMonster();
        if (activePlayerMonster == null) {
            JOptionPane.showMessageDialog(this, "All your monsters have fainted! You cannot fight Mr. Foster.", "No Monsters Available", JOptionPane.ERROR_MESSAGE);
            isBattleOver = true;
            dispose();
            return;
        }

        setupUI();
        updateBossDisplay();
        updatePlayerMonstersDisplay();
        updateStatus("Mr. Foster has appeared! Prepare for battle!");
        
        pack();
        setSize(800, 600);
        setLocationRelativeTo(owner);
        setResizable(false);
        setVisible(true);
    }

    private Monster getFirstAvailablePlayerMonster() {
        for (Monster m : playerMonsters) {
            if (!m.isFainted()) {
                return m;
            }
        }
        return null;
    }

    private void setupUI() {
        setLayout(new BorderLayout(15, 15));
        
        // --- North Panel: Boss Info ---
        JPanel bossInfoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bossInfoPanel.setBorder(BorderFactory.createTitledBorder("Enemy: Mr. Foster"));

        bossImageLabel = new JLabel();
        bossInfoPanel.add(bossImageLabel);

        JPanel bossStatsPanel = new JPanel(new GridLayout(2, 1));
        bossStatsPanel.setOpaque(false);
        bossNameLabel = createLabel(bossMonster.getName() + " (Stage " + (currentBossStage + 1) + ")");
        bossStatsPanel.add(bossNameLabel);

        JPanel bossHpRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        bossHpRow.setOpaque(false);
        bossHpLabel = createLabel("HP: " + bossMonster.getHp() + "/" + bossMonster.getMaxHealth());
        bossHpBar = new JProgressBar(0, bossMonster.getMaxHealth());
        bossHpBar.setValue(bossMonster.getHp());
        bossHpBar.setPreferredSize(new Dimension(250, 25));
        bossHpBar.setForeground(Color.RED);
        bossHpBar.setBackground(Color.LIGHT_GRAY);
        bossHpRow.add(bossHpLabel);
        bossHpRow.add(bossHpBar);
        bossStatsPanel.add(bossHpRow);
        
        bossInfoPanel.add(bossStatsPanel);
        add(bossInfoPanel, BorderLayout.NORTH);

        // --- Center Panel: Battle Log ---
        messageArea = new JTextArea(8, 40);
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, 18));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Battle Log"));
        add(scrollPane, BorderLayout.CENTER);

        // --- South Panel: Player Monsters & Actions ---
        JPanel southPanel = new JPanel(new BorderLayout(10, 10));
        southPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        playerMonstersPanel = new JPanel();
        playerMonstersPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        playerMonstersPanel.setBorder(BorderFactory.createTitledBorder("Your Monsters"));
        southPanel.add(playerMonstersPanel, BorderLayout.NORTH);

        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        attackButton = new JButton("Attack!");
        runButton = new JButton("Run Away");
        closeButton = new JButton("Close Battle");

        Font buttonFont = new Font("Arial", Font.BOLD, 24);
        attackButton.setFont(buttonFont);
        runButton.setFont(buttonFont);
        closeButton.setFont(buttonFont);

        actionButtonPanel.add(attackButton);
        actionButtonPanel.add(runButton);
        actionButtonPanel.add(closeButton);
        southPanel.add(actionButtonPanel, BorderLayout.SOUTH);

        add(southPanel, BorderLayout.SOUTH);

        // Action Listeners
        attackButton.addActionListener(e -> performTurn());
        runButton.addActionListener(e -> {
            updateStatus("You fled the battle! Mr. Foster remains undefeated.");
            endBattle(false);
        });
        closeButton.addActionListener(e -> dispose());
        closeButton.setVisible(false);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 22));
        return label;
    }

    private void updateStatus(String message) {
        if (!message.isEmpty()) {
            messageArea.append(message + "\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        }
        updateBossDisplay();
        updatePlayerMonstersDisplay();
    }

    private void updateBossDisplay() {
        bossNameLabel.setText(bossMonster.getName() + " (Stage " + (currentBossStage + 1) + ")");
        bossHpLabel.setText("HP: " + bossMonster.getHp() + "/" + bossMonster.getMaxHealth());
        animateBar(bossHpBar, bossMonster.getHp());

        try {
            ImageIcon icon = new ImageIcon(ImageIO.read(getClass().getResource(bossSpritePaths[currentBossStage])));
            Image scaled = icon.getImage().getScaledInstance(128, 128, Image.SCALE_SMOOTH);
            bossImageLabel.setIcon(new ImageIcon(scaled));
        } catch (IOException e) {
            System.err.println("Error loading boss image for stage " + currentBossStage + ": " + e.getMessage());
            bossImageLabel.setText("[IMG ERR]");
        }
    }

    private void updatePlayerMonstersDisplay() {
        playerMonstersPanel.removeAll();

        ButtonGroup group = new ButtonGroup();
        boolean anyMonsterAvailable = false;

        for (Monster m : playerMonsters) {
            JRadioButton monsterButton = new JRadioButton(m.getName() + " HP: " + m.getHp() + "/" + m.getMaxHealth());
            monsterButton.setFont(new Font("SansSerif", Font.PLAIN, 18));
            monsterButton.setActionCommand(m.getName());

            if (m.isFainted()) {
                monsterButton.setEnabled(false);
                monsterButton.setForeground(Color.GRAY);
            } else {
                anyMonsterAvailable = true;
                monsterButton.addActionListener(e -> activePlayerMonster = m);
            }

            group.add(monsterButton);
            playerMonstersPanel.add(monsterButton);

            if (m == activePlayerMonster && !m.isFainted()) {
                monsterButton.setSelected(true);
            } else if (m == activePlayerMonster && m.isFainted()) {
                activePlayerMonster = getNextAvailablePlayerMonster();
                if (activePlayerMonster != null) {
                    updatePlayerMonstersDisplay();
                    return;
                }
            }
        }

        if (!anyMonsterAvailable && !isBattleOver) {
            updateStatus("All your monsters have fainted!");
            endBattle(false);
        }
        playerMonstersPanel.revalidate();
        playerMonstersPanel.repaint();
    }


    private void animateBar(JProgressBar bar, int targetValue) {
        Timer timer = new Timer(10, null);
        timer.addActionListener(e -> {
            int current = bar.getValue();
            if (current < targetValue) {
                bar.setValue(Math.min(current + 1, targetValue));
            } else if (current > targetValue) {
                bar.setValue(Math.max(current - 1, targetValue));
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start();
    }

    private void performTurn() {
        if (isBattleOver || activePlayerMonster == null || activePlayerMonster.isFainted()) {
            if (activePlayerMonster == null || activePlayerMonster.isFainted()) {
                updateStatus("You must select an unfainted monster to attack!");
            }
            return;
        }

        // --- Player's Turn ---
        updateStatus("👉 " + activePlayerMonster.getName() + " attacks Mr. Foster!");
        bossMonster.takeDamage(activePlayerMonster.getAttack());
        updateStatus("You dealt " + activePlayerMonster.getAttack() + " damage to Mr. Foster!");
        updateBossDisplay();

        // Check for boss stage transition or defeat
        checkBossStage();

        if (bossMonster.isFainted()) {
            updateStatus("✅ Mr. Foster has been defeated!");
            endBattle(true);
            return;
        }

        // --- Mr. Foster's Turn ---
        updateStatus("⚡ Mr. Foster attacks " + activePlayerMonster.getName() + "!");
        // ⭐ Reduce Mr. Foster's damage by 50% ⭐
        int bossDamage = bossMonster.getAttack();
        int reducedBossDamage = Math.max(1, (int) (bossDamage * 0.5)); // Ensure damage is at least 1
        activePlayerMonster.takeDamage(reducedBossDamage);
        updateStatus("You took " + reducedBossDamage + " damage from Mr. Foster!"); // Report reduced damage
        updatePlayerMonstersDisplay();

        if (activePlayerMonster.isFainted()) {
            updateStatus("💀 " + activePlayerMonster.getName() + " fainted!");
            activePlayerMonster = getNextAvailablePlayerMonster();
            if (activePlayerMonster != null) {
                updateStatus("Go! " + activePlayerMonster.getName() + "!");
                updatePlayerMonstersDisplay();
            } else {
                updateStatus("All your monsters have fainted! You blacked out against Mr. Foster.");
                endBattle(false);
            }
        }
    }

    private void checkBossStage() {
        int currentBossHpPercent = (int) ((double) bossMonster.getHp() / bossMonster.getMaxHealth() * 100);

        for (int i = currentBossStage; i < stageThresholds.length; i++) {
            if (currentBossHpPercent <= stageThresholds[i] && i < stageThresholds.length -1) {
                currentBossStage++;
                // ⭐ Removed boss healing here ⭐
                // Increase boss damage
                bossMonster.setAttack(bossMonster.getAttack() + bossDamageIncreasePerStage);
                updateStatus("Mr. Foster powers up! His attack increased to " + bossMonster.getAttack() + "!"); // Message updated
                updateStatus("Mr. Foster is now in Stage " + (currentBossStage + 1) + "!");
                updateBossDisplay();
                break;
            }
            if (currentBossHpPercent <= stageThresholds[stageThresholds.length - 1]) {
                 currentBossStage = stageThresholds.length - 1;
                 break;
            }
        }
    }

    private Monster getNextAvailablePlayerMonster() {
        for (Monster m : playerMonsters) {
            if (!m.isFainted() && m != activePlayerMonster) {
                return m;
            }
        }
        return null;
    }

    private void endBattle(boolean playerWon) {
        isBattleOver = true;
        attackButton.setEnabled(false);
        runButton.setEnabled(false);
        closeButton.setVisible(true);

        if (playerWon) {
            updateStatus("\n🎉 CONGRATULATIONS! You defeated Mr. Foster!");
            game.addCoins(999999);
            updateStatus("💰 You received 999,999 coins!");
            JOptionPane.showMessageDialog(this, "You have defeated Mr. Foster!\nVictory is yours, along with a massive reward!", "Victory!", JOptionPane.INFORMATION_MESSAGE);
        } else {
            updateStatus("\nGame Over! Mr. Foster remains dominant.");
            JOptionPane.showMessageDialog(this, "Your monsters have been defeated. Mr. Foster has won this time.", "Defeat!", JOptionPane.ERROR_MESSAGE);
        }
    }
}