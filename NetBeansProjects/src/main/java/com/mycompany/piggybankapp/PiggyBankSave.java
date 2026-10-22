package com.mycompany.piggybankapp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Piggy Bank: keep a balance, keep a wish list of products, and watch the
 * progress bar fill up toward whichever product is selected.
 */
public class PiggyBankSave extends JFrame {

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(PiggyBankSave.class.getName());

    private static final Color CREAM = new Color(255, 255, 204);
    private static final Color MINT = new Color(153, 255, 153);
    private static final Color TABLE_GREEN = new Color(204, 255, 204);
    private static final String PESO = "\u20B1";
    private static final Path DATA_FILE =
            Paths.get(System.getProperty("user.home"), "piggybank-data.txt");

    // ---- state ----
    private double balance = 0.0;
    private double goal = 0.0;
    private String goalName = "";

    // ---- components ----
    private JLabel lblGoal;
    private JLabel lblBalance;
    private JProgressBar progressBar;
    private JTextField txtAmount;
    private JTextField txtProductName;
    private JTextField txtProductPrice;
    private JTable tblProducts;
    private DefaultTableModel model;

    public PiggyBankSave() {
        initUI();
        setLocationRelativeTo(null);
        loadDataFromFile();
        updateDisplay();
    }

    // =====================================================================
    // UI
    // =====================================================================
    private void initUI() {
        setTitle("Piggy Bank");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(CREAM);
        root.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        setContentPane(root);

        // --- top: title, goal, balance, progress ---
        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);

        JLabel lblTitle = new JLabel("My Savings Goal", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));

        lblGoal = new JLabel("Goal: none selected");
        lblGoal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblBalance = new JLabel("Current Balance: " + PESO + "0.00", SwingConstants.RIGHT);
        lblBalance.setFont(new Font("Segoe UI", Font.BOLD, 13));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(300, 24));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 10, 0);
        top.add(lblTitle, c);
        c.gridy = 1; c.gridwidth = 1; c.weightx = 1; c.insets = new Insets(0, 0, 6, 0);
        top.add(lblGoal, c);
        c.gridx = 1; c.weightx = 1;
        top.add(lblBalance, c);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.insets = new Insets(0, 0, 0, 0);
        top.add(progressBar, c);
        root.add(top, BorderLayout.NORTH);

        // --- middle: deposit/withdraw + product form ---
        JPanel middle = new JPanel(new GridBagLayout());
        middle.setOpaque(false);

        JPanel money = new JPanel(new GridBagLayout());
        money.setOpaque(false);
        money.setBorder(BorderFactory.createTitledBorder("Piggy Bank"));
        txtAmount = new JTextField(10);
        txtAmount.setToolTipText("Amount to deposit or withdraw");
        JButton btnDeposit = makeButton("Deposit");
        JButton btnWithdraw = makeButton("Withdraw");
        btnDeposit.addActionListener(e -> processTransaction(true));
        btnWithdraw.addActionListener(e -> processTransaction(false));
        txtAmount.addActionListener(e -> processTransaction(true));
        GridBagConstraints m = new GridBagConstraints();
        m.insets = new Insets(4, 6, 4, 6);
        m.gridx = 0; m.gridy = 0; m.gridwidth = 2; m.fill = GridBagConstraints.HORIZONTAL;
        money.add(new JLabel("Amount (" + PESO + ")"), m);
        m.gridy = 1;
        money.add(txtAmount, m);
        m.gridy = 2; m.gridwidth = 1; m.gridx = 0;
        money.add(btnDeposit, m);
        m.gridx = 1;
        money.add(btnWithdraw, m);

        JPanel products = new JPanel(new GridBagLayout());
        products.setOpaque(false);
        products.setBorder(BorderFactory.createTitledBorder("Products"));
        txtProductName = new JTextField(14);
        txtProductPrice = new JTextField(8);
        JButton btnAdd = makeButton("Add Product");
        JButton btnUpdate = makeButton("Update");
        JButton btnDelete = makeButton("Delete");
        JButton btnClear = makeButton("Clear");
        btnAdd.addActionListener(e -> addProduct());
        btnUpdate.addActionListener(e -> updateProduct());
        btnDelete.addActionListener(e -> deleteProduct());
        btnClear.addActionListener(e -> clearForm());
        txtProductPrice.addActionListener(e -> addProduct());
        GridBagConstraints p = new GridBagConstraints();
        p.insets = new Insets(4, 6, 4, 6);
        p.fill = GridBagConstraints.HORIZONTAL;
        p.gridx = 0; p.gridy = 0; p.weightx = 1;
        products.add(new JLabel("Product Name"), p);
        p.gridx = 1; p.weightx = 0;
        products.add(new JLabel("Target Price (" + PESO + ")"), p);
        p.gridx = 0; p.gridy = 1; p.weightx = 1;
        products.add(txtProductName, p);
        p.gridx = 1; p.weightx = 0;
        products.add(txtProductPrice, p);
        JPanel buttons = new JPanel(new java.awt.GridLayout(1, 4, 6, 0));
        buttons.setOpaque(false);
        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);
        p.gridx = 0; p.gridy = 2; p.gridwidth = 2; p.weightx = 1;
        products.add(buttons, p);

        GridBagConstraints mc = new GridBagConstraints();
        mc.gridy = 0; mc.fill = GridBagConstraints.BOTH; mc.weighty = 1;
        mc.gridx = 0; mc.weightx = 0; mc.insets = new Insets(0, 0, 0, 10);
        middle.add(money, mc);
        mc.gridx = 1; mc.weightx = 1; mc.insets = new Insets(0, 0, 0, 0);
        middle.add(products, mc);

        // --- bottom: table ---
        model = new DefaultTableModel(new Object[]{"Product Name", "Target Price"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblProducts = new JTable(model);
        tblProducts.setBackground(TABLE_GREEN);
        tblProducts.setRowHeight(24);
        tblProducts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblProducts.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onRowSelected();
            }
        });
        JScrollPane scroll = new JScrollPane(tblProducts);
        scroll.setPreferredSize(new Dimension(560, 180));

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(middle, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        pack();
        setMinimumSize(getSize());
    }

    private JButton makeButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(MINT);
        b.setForeground(new Color(51, 51, 51));
        return b;
    }

    // =====================================================================
    // Piggy bank: deposit / withdraw
    // =====================================================================
    private void processTransaction(boolean isDeposit) {
        double amount = parseMoney(txtAmount.getText(), "amount");
        if (Double.isNaN(amount)) {
            return;
        }
        if (!isDeposit && amount > balance) {
            JOptionPane.showMessageDialog(this, "Insufficient funds! You only have "
                    + PESO + String.format("%.2f", balance) + ".");
            return;
        }
        balance = round2(isDeposit ? balance + amount : balance - amount);
        txtAmount.setText("");
        saveDataToFile();
        updateDisplay();
        if (isDeposit && goal > 0 && balance >= goal) {
            JOptionPane.showMessageDialog(this, "You reached your goal for " + goalName + "!");
        }
    }

    // =====================================================================
    // Products: add / update / delete
    // =====================================================================
    private void addProduct() {
        String name = cleanName(txtProductName.getText());
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a product name.");
            return;
        }
        double price = parseMoney(txtProductPrice.getText(), "price");
        if (Double.isNaN(price)) {
            return;
        }
        if (indexOfProduct(name, -1) != -1) {
            JOptionPane.showMessageDialog(this, "\"" + name + "\" is already in the list.");
            return;
        }
        model.addRow(new Object[]{name, formatPrice(price)});
        int row = model.getRowCount() - 1;
        clearFormFields();
        tblProducts.setRowSelectionInterval(row, row); // makes it the current goal
        saveDataToFile();
    }

    private void updateProduct() {
        int row = tblProducts.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product from the list to update.");
            return;
        }
        String name = cleanName(txtProductName.getText());
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a product name.");
            return;
        }
        double price = parseMoney(txtProductPrice.getText(), "price");
        if (Double.isNaN(price)) {
            return;
        }
        if (indexOfProduct(name, row) != -1) {
            JOptionPane.showMessageDialog(this, "\"" + name + "\" is already in the list.");
            return;
        }
        model.setValueAt(name, row, 0);
        model.setValueAt(formatPrice(price), row, 1);
        goalName = name;
        goal = price;
        saveDataToFile();
        updateDisplay();
    }

    private void deleteProduct() {
        int row = tblProducts.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product from the list to delete.");
            return;
        }
        String name = String.valueOf(model.getValueAt(row, 0));
        int ok = JOptionPane.showConfirmDialog(this, "Delete \"" + name + "\"?",
                "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        model.removeRow(row); // selection clears, which resets the goal
        clearFormFields();
        saveDataToFile();
        updateDisplay();
    }

    private void clearForm() {
        tblProducts.clearSelection();
        clearFormFields();
    }

    private void clearFormFields() {
        txtProductName.setText("");
        txtProductPrice.setText("");
    }

    /** Selecting a row fills the form and makes that product the current goal. */
    private void onRowSelected() {
        int row = tblProducts.getSelectedRow();
        if (row == -1) {
            goal = 0.0;
            goalName = "";
        } else {
            goalName = String.valueOf(model.getValueAt(row, 0));
            goal = priceAt(row);
            txtProductName.setText(goalName);
            txtProductPrice.setText(String.format("%.2f", goal));
        }
        saveDataToFile();
        updateDisplay();
    }

    private int indexOfProduct(String name, int ignoreRow) {
        for (int i = 0; i < model.getRowCount(); i++) {
            if (i != ignoreRow && name.equalsIgnoreCase(String.valueOf(model.getValueAt(i, 0)))) {
                return i;
            }
        }
        return -1;
    }

    private double priceAt(int row) {
        String s = String.valueOf(model.getValueAt(row, 1)).replace(PESO, "").replace(",", "").trim();
        return Double.parseDouble(s);
    }

    // =====================================================================
    // Helpers
    // =====================================================================
    /** Returns the parsed positive amount, or NaN after showing an error dialog. */
    private double parseMoney(String text, String what) {
        String s = text.trim().replace(",", "").replace(PESO, "");
        if (s.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the " + what + ".");
            return Double.NaN;
        }
        try {
            double v = Double.parseDouble(s);
            if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0) {
                JOptionPane.showMessageDialog(this, "The " + what + " must be greater than 0.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return Double.NaN;
            }
            return round2(v);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid " + what + " (numbers only).",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return Double.NaN;
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String cleanName(String s) {
        return s.replaceAll("[\\t\\r\\n]+", " ").trim();
    }

    private static String formatPrice(double v) {
        return PESO + String.format("%,.2f", v);
    }

    private void updateDisplay() {
        lblBalance.setText("Current Balance: " + PESO + String.format("%,.2f", balance));
        if (goal > 0) {
            lblGoal.setText("Goal: " + goalName + " - " + PESO + String.format("%,.2f", goal));
            int pct = (int) Math.min(100, Math.floor(balance / goal * 100));
            progressBar.setValue(pct);
            progressBar.setString(balance >= goal ? "Goal reached!" : pct + "%");
        } else {
            lblGoal.setText("Goal: none selected");
            progressBar.setValue(0);
            progressBar.setString("Select a product to set your goal");
        }
    }

    // =====================================================================
    // Saving / loading (~/piggybank-data.txt)
    //   line 1: balance
    //   line 2: selected product name (may be empty)
    //   rest:   name <TAB> price
    // =====================================================================
    private void saveDataToFile() {
        List<String> lines = new ArrayList<>();
        lines.add(String.valueOf(balance));
        lines.add(goalName);
        for (int i = 0; i < model.getRowCount(); i++) {
            lines.add(model.getValueAt(i, 0) + "\t" + priceAt(i));
        }
        try {
            Files.write(DATA_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not save data", ex);
            JOptionPane.showMessageDialog(this, "Could not save your data:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDataFromFile() {
        if (!Files.exists(DATA_FILE)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(DATA_FILE, StandardCharsets.UTF_8);
            if (lines.size() < 2) {
                return;
            }
            balance = Double.parseDouble(lines.get(0).trim());
            String selected = lines.get(1).trim();
            for (int i = 2; i < lines.size(); i++) {
                String[] parts = lines.get(i).split("\t");
                if (parts.length == 2) {
                    model.addRow(new Object[]{parts[0], formatPrice(Double.parseDouble(parts[1]))});
                }
            }
            int row = selected.isEmpty() ? -1 : indexOfProduct(selected, -1);
            if (row != -1) {
                tblProducts.setRowSelectionInterval(row, row);
            }
        } catch (IOException | NumberFormatException ex) {
            logger.log(Level.WARNING, "Could not read saved data, starting fresh", ex);
        }
    }

    public static void main(String[] args) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> new PiggyBankSave().setVisible(true));
    }
}
