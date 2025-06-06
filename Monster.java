
public class Monster {
    private String name;
    private String element;
    private int attack;
    private int age;
    private int weight;
    private int hp;
    private int maxHp; // Max HP added for healing

    // Constructors
    public Monster() {
        // Default monster (e.g., Doggin, a common starter)
        this("Doggin", "Normal", 10, 1, 50, 100);
    }

    public Monster(String name, String element) {
        this(name, element, 10, 1, 50, 100);
    }

    public Monster(String name, int hp, int attack) {
        this(name, "Normal", attack, 1, 50, hp);
    }

    public Monster(String name, String element, int attack, int age, int weight, int hp) {
        this.name = name;
        this.element = element;
        this.attack = attack;
        this.age = age;
        this.weight = weight;
        this.hp = hp;
        this.maxHp = hp; // Initialize maxHp to starting hp
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getElement() {
        return element;
    }

    public int getAttack() {
        return attack;
    }

    public int getAge() {
        return age;
    }

    public int getWeight() {
        return weight;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHealth() { // Changed from getMaxHp to getMaxHealth for consistency
        return maxHp;
    }

    // Setters (Adding setAttack)
    public void setName(String name) {
        this.name = name;
    }

    public void setHp(int hp) {
        this.hp = Math.max(0, Math.min(hp, maxHp)); // Ensure HP stays within bounds
    }

    public void setMaxHp(int maxHp) { // Added if you ever need to change max HP
        this.maxHp = maxHp;
        setHp(this.hp); // Adjust current HP if new maxHp is smaller
    }

    // ⭐ ADD THIS METHOD ⭐
    public void setAttack(int attack) {
        this.attack = attack;
    }


    // Battle actions
    public boolean isFainted() {
        return hp <= 0;
    }

    public void attack(Monster target) {
        target.takeDamage(this.attack);
    }

    public void takeDamage(int damage) {
        this.hp -= damage;
        if (this.hp < 0) {
            this.hp = 0;
        }
    }

    // Healing methods
    public void healToFull() {
        this.hp = this.maxHp;
    }

    /**
     * Heals the monster by a specified amount, not exceeding max HP.
     * @param amount The amount of HP to heal.
     * @return The actual amount of HP healed.
     */
    public int heal(int amount) {
        int oldHp = this.hp;
        this.hp += amount;
        if (this.hp > this.maxHp) {
            this.hp = this.maxHp;
        }
        return this.hp - oldHp; // Return actual amount healed
    }

    /**
     * Calculates the coin cost to heal a given amount of HP.
     * @param healAmount The amount of HP to heal.
     * @return The cost in coins (1 coin per 2 HP, rounded up).
     */
    public static int calculateHealCost(int healAmount) {
        // 1 coin per 2 HP, rounded up
        return (healAmount + 1) / 2;
    }
}