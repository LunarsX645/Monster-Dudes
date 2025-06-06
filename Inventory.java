import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class Inventory extends JPanel {
    private List<Monster> monsters;
    private Game game;

    public Inventory(Game game, List<Monster> monsters) {
        this.game = game;
        this.monsters = monsters;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(0, 0, 0, 200));

        JLabel title = new JLabel("Your Monsters");
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        add(Box.createVerticalStrut(10));
        add(title);

        for (Monster m : monsters) {
            add(Box.createVerticalStrut(10));
            add(createMonsterPanel(m));
        }
    }

    private JPanel createMonsterPanel(Monster m) {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.setOpaque(false);

        JLabel nameLabel = new JLabel(m.getName() + " - HP: " + m.getHp() + "/" + m.getMaxHealth());
        nameLabel.setForeground(Color.WHITE);

        try {
            String imagePath = "Game Items/" + m.getName().toLowerCase() + ".png";
            System.out.println("Loading image from: " + imagePath);
            Image img = ImageIO.read(new File(imagePath));
            JLabel iconLabel = new JLabel(new ImageIcon(img.getScaledInstance(50, 50, Image.SCALE_SMOOTH)));
            panel.add(iconLabel);
        } catch (IOException e) {
            panel.add(new JLabel("[No image]"));
        }

        panel.add(nameLabel);

        JButton healButton = new JButton("Heal");
        panel.add(healButton);

        healButton.addActionListener(e -> {
            if (m.getHp() >= m.getMaxHealth()) {
                JOptionPane.showMessageDialog(this, m.getName() + " is already at full health.");
                return;
            }

            String input = JOptionPane.showInputDialog(this, "How much HP to heal?");
            if (input == null) return;

            try {
                int hpToHeal = Integer.parseInt(input);
                if (hpToHeal <= 0) {
                    JOptionPane.showMessageDialog(this, "Enter a positive number.");
                    return;
                }

                if (m.getHp() + hpToHeal > m.getMaxHealth()) {
                    JOptionPane.showMessageDialog(this, "Error: beyond max health.");
                    return;
                }

                int cost = hpToHeal / 5; // 1 coin = 5 HP, rounded down
                if (cost < 1) {
                    JOptionPane.showMessageDialog(this, "You must heal at least 5 HP (costs 1 coin).");
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(this,
                        "Cost: " + cost + " coins\nPay?", "Confirm Heal", JOptionPane.OK_CANCEL_OPTION);

                if (confirm == JOptionPane.OK_OPTION) {
                    if (game.spendCoins(cost)) {
                        m.heal(hpToHeal);
                        JOptionPane.showMessageDialog(this, "Healed " + hpToHeal + " HP!");
                        nameLabel.setText(m.getName() + " - HP: " + m.getHp() + "/" + m.getMaxHealth());
                        revalidate();
                        repaint();
                    } else {
                        JOptionPane.showMessageDialog(this, "Broke! Not enough coins.");
                    }
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid input. Please enter a number.");
            }
        });

        return panel;
    }
}
