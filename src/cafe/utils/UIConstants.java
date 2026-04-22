package cafe.utils;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;


/**
 * UIConstants - Centralized styling constants for the Cafe Management System.
 * Ensures a consistent, attractive UI across all panels.
 */
public class UIConstants {

    // Color Palette - Warm Cafe Theme
    public static final Color PRIMARY      = new Color(0x6F4E37);   // Coffee Brown
    public static final Color PRIMARY_DARK = new Color(0x4A3123);   // Dark Espresso
    public static final Color ACCENT       = new Color(0xD4A96A);   // Caramel Gold
    public static final Color SUCCESS      = new Color(0x4CAF50);   // Green
    public static final Color DANGER       = new Color(0xE53935);   // Red
    public static final Color WARNING      = new Color(0xFF9800);   // Orange
    public static final Color BG_LIGHT     = new Color(0xFDF6EC);   // Cream White
    public static final Color BG_PANEL     = new Color(0xFAF0E6);   // Linen
    public static final Color TEXT_DARK    = new Color(0x2C1810);   // Dark Brown
    public static final Color TEXT_GRAY    = new Color(0x757575);   // Gray
    public static final Color SIDEBAR_BG   = new Color(0x3E2723);   // Deep Espresso
    public static final Color TABLE_HEADER = new Color(0x6F4E37);
    public static final Color TABLE_ROW1   = new Color(0xFFF8F0);
    public static final Color TABLE_ROW2   = new Color(0xFFEDD8);
    public static final Color BORDER_COLOR = new Color(0xD7CCC8);

    // Gradient Colors - Sidebar & Buttons (warm espresso gradient)
    public static final Color GRAD_TOP    = new Color(0x5D3A1A);   // Warm Espresso top
    public static final Color GRAD_BOTTOM = new Color(0x2C1A0E);   // Deep Roast bottom

    // Button gradient colors
    public static final Color BTN_PRIMARY_TOP    = new Color(0x8D6244);
    public static final Color BTN_PRIMARY_BOT    = new Color(0x5C3D24);
    public static final Color BTN_SUCCESS_TOP    = new Color(0x66BB6A);
    public static final Color BTN_SUCCESS_BOT    = new Color(0x388E3C);
    public static final Color BTN_DANGER_TOP     = new Color(0xEF5350);
    public static final Color BTN_DANGER_BOT     = new Color(0xC62828);
    public static final Color BTN_GRAY_TOP       = new Color(0x9E9E9E);
    public static final Color BTN_GRAY_BOT       = new Color(0x616161);
    public static final Color BTN_ACCENT_TOP     = new Color(0xE8BC7A);
    public static final Color BTN_ACCENT_BOT     = new Color(0xB8893A);

    // Fonts
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BOLD    = new Font("Segoe UI", Font.BOLD, 14);

    // Sizes
    public static final int SIDEBAR_WIDTH  = 220;
    public static final int BUTTON_HEIGHT  = 40;
    public static final Dimension FIELD_SIZE = new Dimension(250, 36);

    private UIConstants() {}

    /**
     * Creates a JLabel that displays a live clock (HH:mm:ss — dd MMM yyyy),
     * updated every second via a Swing Timer.
     */
    public static JLabel createLiveClock() {
        JLabel clock = new JLabel();
        clock.setFont(new Font("Segoe UI", Font.BOLD, 13));
        clock.setForeground(TEXT_GRAY);

        // Helper to update text
        Runnable tick = () -> clock.setText(
                java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm:ss a  •  dd MMM yyyy")));
        tick.run(); // set initial value

        Timer timer = new Timer(1000, e -> tick.run());
        timer.setRepeats(true);
        timer.start();

        return clock;
    }

    /**
     * Applies a gradient-painted header renderer to every column of the given table.
     * Uses the same GRAD_TOP → GRAD_BOTTOM palette as the sidebar.
     */
    public static void applyGradientHeader(JTable table) {
        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                // Use a custom panel as the cell
                JPanel cell = new JPanel(new BorderLayout()) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                        GradientPaint gp = new GradientPaint(
                                0, 0, GRAD_TOP,
                                0, getHeight(), GRAD_BOTTOM);
                        g2.setPaint(gp);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        // Right-side separator line
                        g2.setColor(new Color(255, 255, 255, 30));
                        g2.drawLine(getWidth() - 1, 4, getWidth() - 1, getHeight() - 4);
                        g2.dispose();
                    }
                };
                cell.setOpaque(false);
                cell.setBorder(new EmptyBorder(0, 10, 0, 10));

                JLabel lbl = new JLabel(value == null ? "" : value.toString());
                lbl.setFont(FONT_BOLD);
                lbl.setForeground(Color.WHITE);
                cell.add(lbl, BorderLayout.CENTER);
                return cell;
            }
        });
        header.setPreferredSize(new Dimension(0, 32));
        header.setOpaque(false);
        header.setBorder(null);
    }
}
