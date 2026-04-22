package cafe.models;

/**
 * MenuItem model representing a food/drink item on the menu.
 */
public class MenuItem {
    private int id;
    private String name;
    private int categoryId;
    private String categoryName;
    private double price;
    private String description;
    private boolean available;

    public MenuItem() {}

    public MenuItem(int id, String name, int categoryId, String categoryName, double price, String description, boolean available) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.price = price;
        this.description = description;
        this.available = available;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public String toString() { return name + " - Rs." + (int)price; }
}
