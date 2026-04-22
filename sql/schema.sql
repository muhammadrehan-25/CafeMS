-- Cafe Management System - Database Schema
-- Works with both SQLite and MySQL

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role IN ('Admin', 'User')),
    full_name TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS menu_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    category_id INTEGER NOT NULL,
    price REAL NOT NULL CHECK(price >= 0),
    description TEXT,
    is_available INTEGER DEFAULT 1,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    table_no TEXT NOT NULL,
    user_id INTEGER NOT NULL,
    total_amount REAL NOT NULL,
    status TEXT DEFAULT 'Pending' CHECK(status IN ('Pending','Completed','Cancelled')),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS order_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    menu_item_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL CHECK(quantity > 0),
    unit_price REAL NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
);

-- Default Admin
INSERT OR IGNORE INTO users (username, password, role, full_name)
VALUES ('admin', 'admin123', 'Admin', 'Administrator');

INSERT OR IGNORE INTO users (username, password, role, full_name)
VALUES ('waiter1', 'waiter123', 'User', 'Ahmed Ali');

-- Categories
INSERT OR IGNORE INTO categories (name) VALUES ('Hot Drinks');
INSERT OR IGNORE INTO categories (name) VALUES ('Cold Drinks');
INSERT OR IGNORE INTO categories (name) VALUES ('Snacks');
INSERT OR IGNORE INTO categories (name) VALUES ('Main Course');
INSERT OR IGNORE INTO categories (name) VALUES ('Desserts');

-- Menu Items
INSERT OR IGNORE INTO menu_items (name, category_id, price, description) VALUES ('Chai (Tea)', 1, 50, 'Traditional desi chai');
INSERT OR IGNORE INTO menu_items (name, category_id, price, description) VALUES ('Cappuccino', 1, 250, 'Italian espresso with milk foam');
INSERT OR IGNORE INTO menu_items (name, category_id, price, description) VALUES ('Latte', 1, 280, 'Espresso with steamed milk');
INSERT OR IGNORE INTO menu_items (name, category_id, price, description) VALUES ('Cold Coffee', 2, 300, 'Blended iced coffee');
INSERT OR IGNORE INTO menu_items (name, category_id, price, description) VALUES ('Lemonade', 2, 150, 'Fresh lemon juice');
INSERT OR IGNORE INTO menu_items (name, category_id, price, description) VALUES ('Samosa (2pcs)', 3, 80, 'Crispy fried pastry');
INSERT OR IGNORE INTO menu_items (name, category_id, price, description) VALUES ('Club Sandwich', 3, 350, 'Triple-decker sandwich');
INSERT OR IGNORE INTO menu_items (name, category_id, price, description) VALUES ('Chicken Burger', 4, 450, 'Grilled chicken burger');
INSERT OR IGNORE INTO menu_items (name, category_id, price, description) VALUES ('Pasta', 4, 400, 'Creamy white sauce pasta');
INSERT OR IGNORE INTO menu_items (name, category_id, price, description) VALUES ('Chocolate Cake', 5, 200, 'Rich chocolate slice');
