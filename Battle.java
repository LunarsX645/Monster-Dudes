import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;

public class Battle extends JDialog {

    private Game game;
    private Monster playerMonster;
    private Monster wildMonster;
    private boolean isBattleOver = false;

    private JLabel playerHpLabel;
    private JLabel wildHpLabel;
    private JLabel coinsLabel;
    private JTextArea messageArea;
    private JButton attackButton;
    private JButton runButton;
    private JButton closeButton;

    private List<Monster> inventory;

    // Added for health bars
    private JProgressBar playerHpBar;
    private JProgressBar wildHpBar;

    public Battle(JFrame owner, Game game, Monster playerMonster, List<Monster> inventory) {
        super(owner, "⚔️ Battle Begins!", true);
        this.game = game;
        this.playerMonster = playerMonster;
        this.inventory = inventory;
        this.wildMonster = generateRandomWildMonster();
        setupUI();
        updateStatus("A wild " + wildMonster.getName() + " appeared!");
        pack();
        setSize(800, 600);
        setLocationRelativeTo(owner);
        setResizable(false);
        setVisible(true);
    }

    // This constructor seems unused if all battles go through Game
    // But kept for completeness or if testing allows it
    public Battle(JFrame owner, Monster playerMonster) {
        super(owner, "⚔️ Battle Begins!", true);
        this.playerMonster = playerMonster;
        this.wildMonster = generateRandomWildMonster();
        setupUI();
        updateStatus("A wild " + wildMonster.getName() + " appeared!");
        pack();
        setSize(800, 600);
        setLocationRelativeTo(owner);
        setResizable(false);
        setVisible(true);
    }

    public Monster generateRandomWildMonster() {
        Random rand = new Random();
        String[] names = { "Snorb", "Fluffin", "Dirtbeast", "Barkachu" };
        int index = rand.nextInt(names.length);
        int hp = rand.nextInt(10) + 20; // 20-29 HP
        int attack = rand.nextInt(5) + 3; // 3-7 Attack
        return new Monster(names[index], hp, attack);
    }

    private void setupUI() {
        setLayout(new BorderLayout(20, 20));

        JPanel statsPanel = new JPanel(new GridLayout(3, 2, 5, 20));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        statsPanel.add(createMonsterPanel(playerMonster, true));
        statsPanel.add(createMonsterPanel(wildMonster, false));

        JPanel coinsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        coinsPanel.setOpaque(false);
        coinsPanel.add(createLabel("Your Coins:"));
        coinsLabel = createLabel(game != null ? String.valueOf(game.getCoins()) : "0");
        coinsPanel.add(coinsLabel);
        statsPanel.add(coinsPanel);

        add(statsPanel, BorderLayout.NORTH);

        messageArea = new JTextArea(6, 40);
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, 24));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Battle Log"));
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        attackButton = new JButton("Attack");
        runButton = new JButton("Run");
        closeButton = new JButton("Close");

        Font buttonFont = new Font("Arial", Font.BOLD, 28);
        attackButton.setFont(buttonFont);
        runButton.setFont(buttonFont);
        closeButton.setFont(buttonFont);

        buttonPanel.add(attackButton);
        buttonPanel.add(runButton);
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        attackButton.addActionListener(e -> performTurn());
        runButton.addActionListener(e -> {
            updateStatus("You ran away safely!");
            endBattle();
        });
        closeButton.addActionListener((ActionEvent e) -> dispose());
        closeButton.setVisible(false);
    }

    private JPanel createMonsterPanel(Monster monster, boolean isPlayer) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        try {
            String path = "Game Items/" + monster.getName().toLowerCase() + ".png";
            // Use getClass().getResource() for loading from classpath in JAR
            ImageIcon icon = new ImageIcon(ImageIO.read(getClass().getResource(path)));
            Image scaled = icon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
            JLabel imageLabel = new JLabel(new ImageIcon(scaled));
            panel.add(imageLabel);
        } catch (IOException e) {
            System.err.println("Error loading image for " + monster.getName() + ": " + e.getMessage());
            panel.add(new JLabel("[Missing Image]"));
        }

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(createLabel(monster.getName()));

        // HP row: label + health bar side-by-side
        JPanel hpRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        hpRow.setOpaque(false);

        JLabel hpLabel = createLabel("HP: " + monster.getHp() + "/" + monster.getMaxHealth());

        JProgressBar hpBar = new JProgressBar(0, monster.getMaxHealth());
        hpBar.setValue(monster.getHp());
        hpBar.setPreferredSize(new Dimension(150, 20));
        hpBar.setForeground(Color.RED);
        hpBar.setBackground(Color.LIGHT_GRAY);
        hpBar.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        hpRow.add(hpLabel);
        hpRow.add(hpBar);

        textPanel.add(hpRow);

        panel.add(textPanel);

        if (isPlayer) {
            playerHpLabel = hpLabel;
            playerHpBar = hpBar;
        } else {
            wildHpLabel = hpLabel;
            wildHpBar = hpBar;
        }

        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 28));
        return label;
    }

    private void updateStatus(String message) {
        if (!message.isEmpty()) {
            messageArea.append(message + "\n");
        }

        playerHpLabel.setText("HP: " + playerMonster.getHp() + "/" + playerMonster.getMaxHealth());
        wildHpLabel.setText("HP: " + wildMonster.getHp() + "/" + wildMonster.getMaxHealth());

        animateBar(playerHpBar, playerMonster.getHp());
        animateBar(wildHpBar, wildMonster.getHp());

        if (game != null) {
             coinsLabel.setText(String.valueOf(game.getCoins()));
        }
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
        if (isBattleOver) return;

        updateStatus("👉 " + playerMonster.getName() + " attacks!");
        wildMonster.takeDamage(playerMonster.getAttack());
        updateStatus("You dealt " + playerMonster.getAttack() + " damage!");

        if (wildMonster.getHp() <= 0) {
            updateStatus("✅ Wild " + wildMonster.getName() + " was defeated!");

            if (game != null) {
                game.addCoins(5);
                game.incrementBattlesWon(); // Increment counter for boss battle
                updateStatus("💰 You earned 5 coins!");
            }

            int choice = JOptionPane.showConfirmDialog(this,
                            "Do you want to try to catch " + wildMonster.getName() + "?",
                            "Catch Monster",
                            JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                double chance = Math.random();
                if (chance < 0.75) { // 75% chance to catch
                    // Create a new instance of the monster to avoid issues with it being already "fainted"
                    // Or, reset the wildMonster's HP and add it. For now, let's just create a new one.
                    // This assumes the wildMonster data is only for battle and not intended to be "caught" as is.
                    // Let's go with creating a new monster to be added
                    Monster caughtMonster = new Monster(wildMonster.getName(), wildMonster.getElement(),
                                                        wildMonster.getAttack(), wildMonster.getAge(),
                                                        wildMonster.getWeight(), wildMonster.getMaxHealth());
                    caughtMonster.setHp(caughtMonster.getMaxHealth()); // Caught monsters start full health
                    inventory.add(caughtMonster);
                    updateStatus("🎉 " + caughtMonster.getName() + " was caught!");
                } else {
                    updateStatus("💨 " + wildMonster.getName() + " escaped!");
                }
            } else {
                updateStatus("You chose not to catch " + wildMonster.getName() + ".");
            }

            endBattle();
            return;
        }

        // Wild monster's turn if it's still alive
        updateStatus("⚡ Wild " + wildMonster.getName() + " attacks!");
        playerMonster.takeDamage(wildMonster.getAttack());
        updateStatus("You took " + wildMonster.getAttack() + " damage!");

        if (playerMonster.isFainted()) {
            updateStatus("💀 Your " + playerMonster.getName() + " fainted!");
            // Check if other monsters are available
            Monster nextMonster = getNextAvailableMonster();
            if (nextMonster != null) {
                playerMonster = nextMonster; // Switch to the next monster
                updateStatus("Go! " + playerMonster.getName() + "!");
                // Re-create the player monster panel to update image/labels
                // This is a bit clunky, but works. A better solution might be
                // to have the createMonsterPanel return a JPanel and then just update its components.
                // For simplicity here, just update labels/bars.
                playerHpLabel.setText("HP: " + playerMonster.getHp() + "/" + playerMonster.getMaxHealth());
                playerHpBar.setMaximum(playerMonster.getMaxHealth());
                animateBar(playerHpBar, playerMonster.getHp());
                // Also update the image if you want to switch sprite (requires more work here)
            } else {
                updateStatus("All your monsters have fainted! You blacked out.");
                endBattle();
            }
        }
    }

    private Monster getNextAvailableMonster() {
        for (Monster m : inventory) {
            if (!m.isFainted() && m != playerMonster) {
                return m;
            }
        }
        return null; // No other non-fainted monsters
    }

    private void endBattle() {
        isBattleOver = true;
        attackButton.setEnabled(false);
        runButton.setEnabled(false);
        closeButton.setVisible(true);
    }
}