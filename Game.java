import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel; // Import JLabel
import javax.swing.JOptionPane; // Import JOptionPane
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;

@FunctionalInterface
interface RunnableWithMonster {
    void run(Monster chosen);
}

public class Game extends JPanel implements KeyListener {

    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 600;

    private int avatarX;
    private int avatarY;

    private Image avatarFront;
    private Image avatarBack;
    private Image avatarLeft;
    private Image avatarRight;
    private Image currentAvatar;

    private JFrame frame;

    private boolean inBattle = false;
    private boolean waitingForMoveAfterBattle = false;

    private Monster playerMonster; // This will be the first monster in collectedMonsters

    private boolean showInventory = false;
    private JDialog inventoryDialog;
    private List<Monster> collectedMonsters = new ArrayList<>();

    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean shiftPressed = false;
    private boolean ePressed = false;
    private boolean xPressed = false; // Added for boss battle trigger

    private double scaleFactor;

    private boolean inventoryOpen = false;

    private int coins;

    private List<Rectangle> grassCollisionAreas = new ArrayList<>();
    private List<Rectangle> roadRects = new ArrayList<>();

    private static final int TILE_SIZE = 32;
    private static final int ROAD_WIDTH = 48; // approx 1.5x avatar width (32)

    // Boss Battle specific variables
    private int battlesWon = 9;
    private static final int BOSS_THRESHOLD = 10;
    private boolean bossBattleAvailable = false;
    private JDialog bossPromptDialog; // To keep track of the boss prompt dialog

    public Game() {
        try {
            avatarFront = ImageIO.read(getClass().getResource("/Game Items/avatar_front.png"));
            avatarBack = ImageIO.read(getClass().getResource("/Game Items/avatar_back.png"));
            avatarLeft = ImageIO.read(getClass().getResource("/Game Items/avatar_left.png"));
            avatarRight = ImageIO.read(getClass().getResource("/Game Items/avatar_right.png"));
            currentAvatar = avatarFront;
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Generate the procedural map with road and grass collision
        generateMap();

        frame = new JFrame("Monster APSCA-A FINAL PROJECT");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.addKeyListener(this);
        frame.setSize(GAME_WIDTH, GAME_HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        avatarX = (GAME_WIDTH - 64) / 2;
        avatarY = (GAME_HEIGHT - 64) / 2;

        playerMonster = new Monster(); // Initial monster
        collectedMonsters.add(playerMonster); // Add initial monster to collection

        updateScaleFactor();

        Timer timer = new Timer(16, e -> {
            // If in battle (regular or boss), just repaint
            if (inBattle) {
                repaint();
                return;
            }
            // If waiting for move after battle and no movement keys are pressed, just repaint
            if (waitingForMoveAfterBattle && !upPressed && !downPressed && !leftPressed && !rightPressed) {
                repaint();
                return;
            }
            // If waitingForMoveAfterBattle is true and a movement key is pressed, reset it
            if (waitingForMoveAfterBattle) {
                waitingForMoveAfterBattle = false;
            }

            int baseSpeed = 1;
            int speed = shiftPressed ? baseSpeed * 4 : baseSpeed * 2;

            boolean moved = false;
            int prevX = avatarX;
            int prevY = avatarY;

            if (upPressed) {
                avatarY -= speed;
                currentAvatar = avatarFront;
                moved = true;
            }
            if (downPressed) {
                avatarY += speed;
                currentAvatar = avatarBack;
                moved = true;
            }
            if (leftPressed) {
                avatarX -= speed;
                currentAvatar = avatarLeft;
                moved = true;
            }
            if (rightPressed) {
                avatarX += speed;
                currentAvatar = avatarRight;
                moved = true;
            }

            // Clamp avatar inside window boundaries
            avatarX = Math.max(0, Math.min(avatarX, GAME_WIDTH - 32));
            avatarY = Math.max(0, Math.min(avatarY, GAME_HEIGHT - 32));

            // Collision check: avatar cannot walk on grass collision areas
            Rectangle avatarRect = new Rectangle(avatarX, avatarY, 32, 32);

            boolean collided = false;
            for (Rectangle grass : grassCollisionAreas) {
                if (avatarRect.intersects(grass)) {
                    collided = true;
                    break;
                }
            }

            if (collided) {
                avatarX = prevX;
                avatarY = prevY;
            }

            if (moved && !collided) {
                pixelsSinceLastBattle += speed;
                if (pixelsSinceLastBattle >= PIXEL_THRESHOLD && Math.random() < 0.01) {
                    inBattle = true;
                    pixelsSinceLastBattle = 0;

                    // Check if boss battle is available but not triggered yet
                    if (battlesWon >= BOSS_THRESHOLD && !bossBattleAvailable && bossPromptDialog == null) {
                        bossBattleAvailable = true;
                        showBossPrompt();
                        // If boss prompt is shown, don't start regular battle immediately
                        inBattle = false; // Stay in exploration mode while prompt is up
                    } else if (!bossBattleAvailable) { // Only trigger regular battle if boss battle isn't pending
                        if (collectedMonsters.size() > 1) {
                            selectMonsterForBattle(chosenMonster -> {
                                startBattle(chosenMonster);
                            });
                        } else {
                            startBattle(playerMonster);
                        }
                    }
                }
            }

            repaint();
        });
        timer.start();

        frame.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                updateScaleFactor();
            }
        });
    }

    private void generateMap() {
        grassCollisionAreas.clear();
        roadRects.clear();

        int verticalTiles = GAME_HEIGHT / TILE_SIZE;

        int roadX = (GAME_WIDTH / 2) - (ROAD_WIDTH / 2);
        Rectangle mainRoad = new Rectangle(roadX, 0, ROAD_WIDTH, GAME_HEIGHT);
        roadRects.add(mainRoad);

        for (int y = 0; y < verticalTiles; y++) {
            int tileY = y * TILE_SIZE;
            for (int x = 0; x < GAME_WIDTH; x += TILE_SIZE) {
                Rectangle tileRect = new Rectangle(x, tileY, TILE_SIZE, TILE_SIZE);

                boolean isOnRoad = false;
                for (Rectangle road : roadRects) {
                    if (road.intersects(tileRect)) {
                        isOnRoad = true;
                        break;
                    }
                }
                if (!isOnRoad) {
                    grassCollisionAreas.add(tileRect);
                }
            }
        }

        int branchSpacingTiles = 5; // Increased spacing (e.g., every 5 tiles instead of 2)
        int branchStartTile = 2;

        // Left branches: start at x = 1 tile away, width shrunk by 1 tile
        for (int i = 0; i < 3; i++) {
            int branchY = (branchStartTile + i * branchSpacingTiles) * TILE_SIZE;
            Rectangle branch = new Rectangle(TILE_SIZE, branchY, roadRects.get(0).x - TILE_SIZE, ROAD_WIDTH);
            roadRects.add(branch);
            grassCollisionAreas.removeIf(grassTile -> grassTile.intersects(branch));
        }

        // Right branches: width reduced by 1 tile from the right edge
        for (int i = 0; i < 2; i++) {
            int branchY = (branchStartTile + i * branchSpacingTiles) * TILE_SIZE;
            int branchX = roadRects.get(0).x + ROAD_WIDTH;
            int branchWidth = GAME_WIDTH - branchX - TILE_SIZE; // subtract 1 tile from right edge
            Rectangle branch = new Rectangle(branchX, branchY, branchWidth, ROAD_WIDTH);
            roadRects.add(branch);
            grassCollisionAreas.removeIf(grassTile -> grassTile.intersects(branch));
        }

        // Just to be sure, remove grass collision on all road areas
        for (Rectangle road : roadRects) {
            grassCollisionAreas.removeIf(grassTile -> grassTile.intersects(road));
        }
    }

    private int pixelsSinceLastBattle = 0;
    private static final int PIXEL_THRESHOLD = 500;

    public void addCoins(int amount) {
        coins += amount;
    }

    public boolean spendCoins(int amount) {
        if (coins >= amount) {
            coins -= amount;
            return true;
        }
        return false;
    }

    public int getCoins() {
        return coins;
    }

    public void incrementBattlesWon() {
        battlesWon++;
        // If the threshold is met, make boss battle available
        if (battlesWon >= BOSS_THRESHOLD && !bossBattleAvailable) {
            bossBattleAvailable = true;
            showBossPrompt();
        }
    }

    private void showBossPrompt() {
        if (bossPromptDialog != null && bossPromptDialog.isVisible()) {
            return; // Don't show again if already visible
        }

        bossPromptDialog = new JDialog(frame, "Boss Battle Approaching!", true);
        bossPromptDialog.setLayout(new BoxLayout(bossPromptDialog.getContentPane(), BoxLayout.Y_AXIS));
        bossPromptDialog.setLocationRelativeTo(frame);
        bossPromptDialog.setSize(300, 150);

        JLabel message = new JLabel("You have defeated " + BOSS_THRESHOLD + " monsters!");
        JLabel question = new JLabel("Fight Mr. Foster, the ultimate boss?");
        message.setAlignmentX(CENTER_ALIGNMENT);
        question.setAlignmentX(CENTER_ALIGNMENT);
        message.setFont(new Font("Arial", Font.BOLD, 14));
        question.setFont(new Font("Arial", Font.BOLD, 14));

        JPanel buttonPanel = new JPanel();
        JButton yesButton = new JButton("Yes!");
        JButton noButton = new JButton("No, later");

        yesButton.addActionListener(e -> {
            bossPromptDialog.dispose();
            bossPromptDialog = null;
            startBossBattle();
        });

        noButton.addActionListener(e -> {
            bossPromptDialog.dispose();
            bossPromptDialog = null;
            // Boss battle remains available, can be triggered by 'X'
        });

        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);

        bossPromptDialog.add(Box.createVerticalGlue());
        bossPromptDialog.add(message);
        bossPromptDialog.add(question);
        bossPromptDialog.add(Box.createVerticalStrut(10));
        bossPromptDialog.add(buttonPanel);
        bossPromptDialog.add(Box.createVerticalGlue());

        bossPromptDialog.setVisible(true);
    }

    private void healAllMonsters() {
        for (Monster m : collectedMonsters) {
            m.healToFull();
        }
        System.out.println("All monsters healed to full before boss battle!");
    }

    private void startBossBattle() {
        if (inBattle) return; // Prevent starting if already in any battle
        
        healAllMonsters(); // Heal all monsters before the boss fight
        inBattle = true; // Set battle flag for the main game loop

        // Create and show the BossBattle dialog
        new BossBattle(frame, this, collectedMonsters) {
            @Override
            public void dispose() {
                super.dispose();
                inBattle = false; // Battle ended, allow movement
                waitingForMoveAfterBattle = true; // Prevent immediate movement
                bossBattleAvailable = false; // Reset boss battle availability after it starts
                battlesWon = 0; // Reset regular battle count after boss fight
                // Clear any pending movement keys
                upPressed = false;
                downPressed = false;
                leftPressed = false;
                rightPressed = false;
            }
        };
    }

    private void toggleInventory() {
        if (inventoryOpen) {
            if (inventoryDialog != null) {
                inventoryDialog.dispose();
                inventoryDialog = null;
            }
            inventoryOpen = false;
        } else {
            inventoryDialog = new JDialog(frame, "Inventory", false);
            inventoryDialog.setSize(300, 400);
            inventoryDialog.setLocationRelativeTo(frame);
            Inventory inventoryPanel = new Inventory(this, collectedMonsters);
            inventoryDialog.add(new JScrollPane(inventoryPanel));
            inventoryDialog.setVisible(true);
            inventoryOpen = true;
        }
    }

    private void updateScaleFactor() {
        int windowWidth = getWidth();
        int windowHeight = getHeight();
        if (windowWidth == 0 || windowHeight == 0) {
            scaleFactor = 1.0;
            return;
        }
        double xScale = (double) windowWidth / GAME_WIDTH;
        double yScale = (double) windowHeight / GAME_HEIGHT;
        scaleFactor = Math.min(xScale, yScale);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(java.awt.Color.BLACK);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        int scaledWidth = (int) (GAME_WIDTH * scaleFactor);
        int scaledHeight = (int) (GAME_HEIGHT * scaleFactor);
        int xOffset = (getWidth() - scaledWidth) / 2;
        int yOffset = (getHeight() - scaledHeight) / 2;
        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(xOffset, yOffset);
        g2d.scale(scaleFactor, scaleFactor);

        // Draw green grass background (fill entire area first)
        g2d.setColor(new Color(34, 139, 34)); // forest green grass
        g2d.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        // Draw dirt road (brown rectangles)
        g2d.setColor(new Color(139, 69, 19)); // brown dirt color
        for (Rectangle road : roadRects) {
            g2d.fillRect(road.x, road.y, road.width, road.height);
        }

        // Draw avatar
        if (currentAvatar != null) {
            g2d.drawImage(currentAvatar, avatarX, avatarY, 32, 32, this);
        }

        g2d.setColor(java.awt.Color.WHITE);
        g2d.drawString("Pixels since last battle: " + pixelsSinceLastBattle, 10, 20);
        g2d.drawString("Coins: " + coins, 10, 40);
        g2d.drawString("Battles: " + battlesWon + "/" + BOSS_THRESHOLD, 10, 60); // New counter display

        g2d.setTransform(oldTransform);

        if (showInventory) {
            g2d.setColor(java.awt.Color.RED);
            g2d.fillRect(100, 100, 200, 100);
            g2d.setColor(java.awt.Color.WHITE);
            g2d.drawString("Inventory Open", 110, 130);
        }
    }

    private void selectMonsterForBattle(RunnableWithMonster onMonsterSelected) {
        JDialog selectionDialog = new JDialog(frame, "Choose Your Monster", true);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Filter for non-fainted monsters
        List<Monster> availableMonsters = new ArrayList<>();
        for (Monster m : collectedMonsters) {
            if (!m.isFainted()) {
                availableMonsters.add(m);
            }
        }

        if (availableMonsters.isEmpty()) {
            JOptionPane.showMessageDialog(selectionDialog, "All your monsters have fainted! You ran away.", "No Monsters Available", JOptionPane.ERROR_MESSAGE);
            selectionDialog.dispose();
            inBattle = false; // End the battle attempt
            waitingForMoveAfterBattle = true;
            return;
        }


        for (Monster m : availableMonsters) {
            JButton button = new JButton(m.getName() + " - HP: " + m.getHp() + "/" + m.getMaxHealth());
            button.addActionListener(e -> {
                selectionDialog.dispose();
                onMonsterSelected.run(m);
            });
            panel.add(button);
        }

        selectionDialog.add(panel);
        selectionDialog.pack();
        selectionDialog.setLocationRelativeTo(frame);
        selectionDialog.setVisible(true);
    }
    
    private void startBattle(Monster chosenMonster) {
        new Battle(frame, this, chosenMonster, collectedMonsters) {
            @Override
            public void dispose() {
                super.dispose();
                inBattle = false;
                waitingForMoveAfterBattle = true;
                // Clear any pending movement keys
                upPressed = false;
                downPressed = false;
                leftPressed = false;
                rightPressed = false;
            }
        };
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W) {
            upPressed = true;
            currentAvatar = avatarFront;
        } else if (code == KeyEvent.VK_S) {
            downPressed = true;
            currentAvatar = avatarBack;
        } else if (code == KeyEvent.VK_A) {
            leftPressed = true;
            currentAvatar = avatarLeft;
        } else if (code == KeyEvent.VK_D) {
            rightPressed = true;
            currentAvatar = avatarRight;
        } else if (code == KeyEvent.VK_SHIFT) {
            shiftPressed = true;
        } else if (code == KeyEvent.VK_E) {
            if (!ePressed) { // Only trigger once per press
                ePressed = true;
                toggleInventory();
                System.out.println("e pressed - toggle inventory");
            }
        } else if (code == KeyEvent.VK_X) { // New: 'X' key for boss battle
            if (!xPressed) { // Only trigger once per press
                xPressed = true;
                if (bossBattleAvailable && !inBattle && bossPromptDialog == null) {
                    showBossPrompt();
                    System.out.println("x pressed - show boss prompt");
                } else if (inBattle) {
                    System.out.println("Cannot trigger boss battle: currently in another battle.");
                } else if (!bossBattleAvailable) {
                    System.out.println("Boss battle not available yet (defeat " + BOSS_THRESHOLD + " monsters).");
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W) {
            upPressed = false;
        } else if (code == KeyEvent.VK_S) {
            downPressed = false;
        } else if (code == KeyEvent.VK_A) {
            leftPressed = false;
        } else if (code == KeyEvent.VK_D) {
            rightPressed = false;
        } else if (code == KeyEvent.VK_SHIFT) {
            shiftPressed = false;
        } else if (code == KeyEvent.VK_E) {
            ePressed = false;
        } else if (code == KeyEvent.VK_X) {
            xPressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        new Game();
    }
}