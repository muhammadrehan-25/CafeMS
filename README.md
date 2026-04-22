<div align="center">

<img src="src/cafe/logo.png" width="120" alt="AL Rehan Cafe Logo"/>

# AL Rehan Cafe Management System

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com)
[![SQLite](https://img.shields.io/badge/SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white)](https://www.sqlite.org)
[![Swing](https://img.shields.io/badge/Java%20Swing-GUI-6F4E37?style=for-the-badge&logo=java&logoColor=white)](https://docs.oracle.com/javase/tutorial/uiswing/)
[![Platform](https://img.shields.io/badge/Platform-Windows-0078D6?style=for-the-badge&logo=windows&logoColor=white)](https://www.microsoft.com/windows)
[![License](https://img.shields.io/badge/License-Academic-brightgreen?style=for-the-badge)](LICENSE)

**A Full-Featured Cafe Management Desktop Application**
*Built with Java Swing & SQLite for streamlined cafe operations*

[📥 Download Installer](#-installation) &nbsp;•&nbsp; [🚀 Run from Source](#-run-from-source-code) &nbsp;•&nbsp; [✨ Features](#-features) &nbsp;•&nbsp; [📄 Report](CafeMS_Project_Report.docx)

---

</div>

## 📌 Project Info

| | |
|:---|:---|
| 🏫 **University** | Mehran University of Engineering & Technology, Khairpur Mir's |
| 📚 **Course** | SW121 — Object Oriented Programming |
| 👨‍🏫 **Instructor** | Engr. Asmatullah Zubair |
| 🎓 **Batch** | K25SW |
| 👨‍💻 **Developer** | Muhammad Rehan — Roll No. K25SW003 |
| 📅 **Date** | April 2026 |

---


---

## ✨ Features

<table>
<tr>
<td width="50%">

### 🔐 Authentication
- Login with username & password
- **Admin** and **User** roles
- Role-based access control
- Shake animation on wrong password

### 📊 Dashboard
- Live revenue stats (Total, Today, Avg)
- 7-day revenue bar chart
- Recent orders table
- Auto-refresh every 30 seconds

### 🍽️ Menu Management
- Add / Edit / Delete menu items
- 5 categories (Hot Drinks, Cold Drinks, Snacks, Main Course, Desserts)
- Search by name, filter by category
- Toggle item availability

</td>
<td width="50%">

### 🛒 Order Management
- Browse menu with search & filter
- Cart with quantity, price & subtotal
- Place order → thermal receipt popup
- Filter orders by status

### 👥 User Management *(Admin only)*
- Add, Edit, Delete staff accounts
- Assign Admin / User roles
- Search users by name or username

### 📋 Reports & Export
- Thermal-style receipt on every order
- Export full orders list to CSV
- Dashboard revenue chart

</td>
</tr>
</table>

---

## 🏗️ Architecture

```
CafeMS/
├── 📁 src/cafe/
│   ├── 🖥️  ui/      →  LoginFrame, MainFrame, SplashScreen
│   │                    DashboardPanel, MenuPanel, OrderPanel
│   │                    OrdersListPanel, UserManagementPanel
│   ├── 🗄️  dao/     →  UserDAO, MenuDAO, OrderDAO
│   ├── 📦  models/  →  User, MenuItem, Order, OrderItem
│   └── ⚙️  utils/   →  DatabaseManager (Singleton), UIConstants
├── 📁 lib/           →  sqlite-jdbc-3.45.1.0.jar
├── 📁 dist/          →  Compiled JAR
└── 📄 README.md
```

### Design Patterns Used

| Pattern | Where Used |
|:---|:---|
| **Singleton** | `DatabaseManager` — one shared DB connection |
| **DAO Pattern** | `UserDAO`, `MenuDAO`, `OrderDAO` — DB logic separated from UI |
| **MVC-style** | Models hold data, DAOs handle DB, Panels display only |
| **Observer/Callback** | Dashboard auto-refreshes after every order placed |

---

## 🗄️ Database Design

```sql
users        →  id, username (UNIQUE), password, role CHECK('Admin','User'), full_name
categories   →  id, name (UNIQUE)
menu_items   →  id, name, category_id (FK), price CHECK(>=0), description, is_available
orders       →  id, table_no, user_id (FK), total_amount, status, created_at
order_items  →  id, order_id (FK), menu_item_id (FK), quantity, unit_price
```

> ✅ All tables are **auto-created on first launch** with seed data — no manual DB setup needed

---

## 💻 Installation

### System Requirements

| Requirement | Minimum |
|:---|:---|
| **OS** | Windows 10 / 11 (64-bit) |
| **RAM** | 512 MB |
| **Storage** | ~200 MB |
| **Java** | ❌ Not needed — bundled inside installer |

### Steps

```
1. Run  CafeMS-1.0.exe
2. If Windows blocks it → click "More info" → "Run anyway"
   (Safe — app is just not commercially signed)
3. Click Next → Choose install folder → Install → Finish
4. Launch from Start Menu → CafeMS  or desktop shortcut
```

### 🔑 Default Login Credentials

| Role | Username | Password |
|:---:|:---:|:---:|
| Administrator | `admin` | `admin123` |

---

## 🚀 Run from Source Code

```bash
# 1. Clone the repository
git clone https://github.com/muhammadrehan-25/CafeMS.git
cd CafeMS

# 2. Compile all Java files
javac -cp "lib/sqlite-jdbc-3.45.1.0.jar" -d out -sourcepath src $(find src -name "*.java")

# 3. Run the application
java -cp "out:lib/sqlite-jdbc-3.45.1.0.jar" cafe.ui.LoginFrame
```

> 💡 **Windows CMD** — replace `:` with `;` in classpath:
> ```cmd
> java -cp "out;lib/sqlite-jdbc-3.45.1.0.jar" cafe.ui.LoginFrame
> ```

---

## 🔗 Links

| | | 
|:---|:---|
| 🎬 **Demo Video** | *(Google Drive link — add after recording)* |
---

<div align="center">

*SW121 — Object Oriented Programming &nbsp;|&nbsp; Engr. Asmatullah Zubair &nbsp;|&nbsp; K25SW &nbsp;|&nbsp; MUET Khairpur Mir's &nbsp;|&nbsp; April 2026*

</div>
