/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.martabak;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author User
 */
public class menu extends javax.swing.JFrame {
        private String idKolom(String tabelDb) {
        return tabelDb.equals("menu_base") ? "id_base" : "id_topping";
    }
   

    private void loadMenu(javax.swing.JTable tabel, String tabelDb, String kategori, String judulKolom) {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", judulKolom, "Harga"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col != 0;
            }
        };

        String sql = "SELECT " + idKolom(tabelDb) + " AS id, nama, harga FROM " + tabelDb + " WHERE kategori = ?";
        try (Connection c = Koneksi.getKoneksi();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, kategori);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt("id"), rs.getString("nama"), rs.getInt("harga")});
                }
            }
        } catch (SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Gagal ambil data: " + e.getMessage());
        }

        model.addTableModelListener(ev -> {
            if (ev.getType() == javax.swing.event.TableModelEvent.UPDATE && ev.getFirstRow() >= 0) {
                simpanEdit(model, ev.getFirstRow(), tabelDb);
            }
        });

        tabel.setModel(model);
        tabel.removeColumn(tabel.getColumnModel().getColumn(0));
    }

    private void simpanEdit(DefaultTableModel model, int baris, String tabelDb) {
        int id = (int) model.getValueAt(baris, 0);
        String nama = String.valueOf(model.getValueAt(baris, 1)).trim();
        String hargaTeks = String.valueOf(model.getValueAt(baris, 2)).trim();

        int harga;
        try {
            harga = Integer.parseInt(hargaTeks);
            if (nama.isEmpty() || harga <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Nama tidak boleh kosong dan harga harus angka lebih dari 0.");
            javax.swing.SwingUtilities.invokeLater(this::refreshMenu);
            return;
        }

        String sql = "UPDATE " + tabelDb + " SET nama = ?, harga = ? WHERE " + idKolom(tabelDb) + " = ?";
        try (Connection c = Koneksi.getKoneksi();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nama);
            ps.setInt(2, harga);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Gagal menyimpan: " + e.getMessage());
        }
        javax.swing.SwingUtilities.invokeLater(this::refreshMenu);
    }

    private void hapusBaris(javax.swing.JTable tabel, String tabelDb) {
        int baris = tabel.getSelectedRow();
        if (baris < 0) return;
        int b = tabel.convertRowIndexToModel(baris);
        DefaultTableModel m = (DefaultTableModel) tabel.getModel();
        int id = (int) m.getValueAt(b, 0);
        String nama = String.valueOf(m.getValueAt(b, 1));

        int jawab = javax.swing.JOptionPane.showConfirmDialog(this,
                "Hapus \"" + nama + "\" dari menu?", "Konfirmasi", javax.swing.JOptionPane.YES_NO_OPTION);
        if (jawab != javax.swing.JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM " + tabelDb + " WHERE " + idKolom(tabelDb) + " = ?";
        try (Connection c = Koneksi.getKoneksi();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) {
                javax.swing.JOptionPane.showMessageDialog(this,
                    "Item ini tidak bisa dihapus karena sudah dipakai di pesanan.");
            } else {
                javax.swing.JOptionPane.showMessageDialog(this, "Gagal menghapus: " + e.getMessage());
            }
        }
        refreshMenu();
    }

    private void pasangPopup(javax.swing.JTable tabel, String tabelDb) {
        javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem itemHapus = new javax.swing.JMenuItem("Hapus");
        popup.add(itemHapus);
        itemHapus.addActionListener(e -> hapusBaris(tabel, tabelDb));

        tabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { tampil(e); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { tampil(e); }
            private void tampil(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int baris = tabel.rowAtPoint(e.getPoint());
                    if (baris >= 0) {
                        tabel.setRowSelectionInterval(baris, baris);
                        popup.show(tabel, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private void pasangEditLangsung(javax.swing.JTable tabel, String tabelDb) {
        javax.swing.DefaultCellEditor editor = (javax.swing.DefaultCellEditor) tabel.getDefaultEditor(Object.class);
        editor.setClickCountToStart(1);

        tabel.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).put(
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0), "hapusBaris");
        tabel.getActionMap().put("hapusBaris", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                hapusBaris(tabel, tabelDb);
            }
        });
    }
    

    private void bukaTambah(String tabelDb, String kategori) {
        tambahMenu dialog = new tambahMenu(this, true);
        dialog.setTarget(tabelDb, kategori);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        refreshMenu();
    }

    private void refreshMenu() {
        loadMenu(jTable1, "menu_base",    "manis", "Base");
        loadMenu(jTable3, "menu_topping", "manis", "Topping");
        loadMenu(jTable2, "menu_base",    "telor", "Base");
        loadMenu(jTable5, "menu_topping", "telor", "Topping");
    }
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(menu.class.getName());

    /**
     * Creates new form menu
     */
    public menu() {
        initComponents();
        pasangPopup(jTable1, "menu_base");
        pasangPopup(jTable3, "menu_topping");
        pasangPopup(jTable2, "menu_base");
        pasangPopup(jTable5, "menu_topping");
        pasangEditLangsung(jTable1, "menu_base");
        pasangEditLangsung(jTable3, "menu_topping");
        pasangEditLangsung(jTable2, "menu_base");
        pasangEditLangsung(jTable5, "menu_topping");
        refreshMenu();
   
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLayeredPane1 = new javax.swing.JLayeredPane();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable3 = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        tambahBaseTelor = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTable5 = new javax.swing.JTable();
        tambahBaseManis = new javax.swing.JButton();
        tambahToppingManis = new javax.swing.JButton();
        tambahToppingTelor = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        pesanan = new javax.swing.JMenu();
        menu = new javax.swing.JMenu();
        riwayat = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        jLayeredPane1.setPreferredSize(new java.awt.Dimension(800, 377));

        jLabel1.setFont(new java.awt.Font("Bakso Sapi", 0, 18)); // NOI18N
        jLabel1.setText("Daftar Menu");

        jLabel2.setText("Martabak Manis");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Original", "20000"},
                {"Black", "21000"},
                {"Pandan", "21000"},
                {"Red Velvet", "22000"}
            },
            new String [] {
                "Base", "Harga"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        jTable3.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"coklat", "5000"},
                {"keju", "5000"},
                {"kacang", "3000"},
                {"wijen", "2000"},
                {"ketan", "5000"},
                {"duren", "7000"},
                {"matcha", "5000"},
                {"pisang", "4000"}
            },
            new String [] {
                "Topping", "Harga"
            }
        ));
        jScrollPane3.setViewportView(jTable3);

        jLabel3.setText("Martabak Telor");

        jScrollPane2.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane2.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"2 Telor Ayam", "10000"},
                {"3 Telor Ayam", "12000"},
                {"4 Telor Ayam", "15000"},
                {"2 Telor Bebek", "15000"},
                {"3 Telor Beber", "17000"},
                {"4 Telor Bebek", "20000"}
            },
            new String [] {
                "Base", "Harga"
            }
        ));
        jScrollPane2.setViewportView(jTable2);

        tambahBaseTelor.setText("+ Tambah Menu");
        tambahBaseTelor.addActionListener(this::tambahBaseTelorActionPerformed);

        jScrollPane5.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane5.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        jTable5.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Daging Ayam", "7000"},
                {"Daging Sapi", "10000"}
            },
            new String [] {
                "Topping", "Harga"
            }
        ));
        jScrollPane5.setViewportView(jTable5);

        tambahBaseManis.setText("+ Tambah Menu");
        tambahBaseManis.addActionListener(this::tambahBaseManisActionPerformed);

        tambahToppingManis.setText("+ Tambah Menu");
        tambahToppingManis.addActionListener(this::tambahToppingManisActionPerformed);

        tambahToppingTelor.setText("+ Tambah Menu");
        tambahToppingTelor.addActionListener(this::tambahToppingTelorActionPerformed);

        jLayeredPane1.setLayer(jLabel1, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(jLabel2, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(jScrollPane1, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(jScrollPane3, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(jLabel3, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(jScrollPane2, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(tambahBaseTelor, javax.swing.JLayeredPane.PALETTE_LAYER);
        jLayeredPane1.setLayer(jScrollPane5, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(tambahBaseManis, javax.swing.JLayeredPane.PALETTE_LAYER);
        jLayeredPane1.setLayer(tambahToppingManis, javax.swing.JLayeredPane.PALETTE_LAYER);
        jLayeredPane1.setLayer(tambahToppingTelor, javax.swing.JLayeredPane.PALETTE_LAYER);

        javax.swing.GroupLayout jLayeredPane1Layout = new javax.swing.GroupLayout(jLayeredPane1);
        jLayeredPane1.setLayout(jLayeredPane1Layout);
        jLayeredPane1Layout.setHorizontalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jLayeredPane1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jLayeredPane1Layout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 373, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 373, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jLayeredPane1Layout.createSequentialGroup()
                            .addComponent(jLabel2)
                            .addGap(306, 306, 306)
                            .addComponent(jLabel3))
                        .addGroup(jLayeredPane1Layout.createSequentialGroup()
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 373, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 373, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jLayeredPane1Layout.createSequentialGroup()
                            .addGap(321, 321, 321)
                            .addComponent(jLabel1)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jLayeredPane1Layout.createSequentialGroup()
                        .addComponent(tambahBaseManis, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(tambahBaseTelor, javax.swing.GroupLayout.PREFERRED_SIZE, 373, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(59, 59, 59))
            .addGroup(jLayeredPane1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tambahToppingManis, javax.swing.GroupLayout.PREFERRED_SIZE, 373, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(tambahToppingTelor, javax.swing.GroupLayout.PREFERRED_SIZE, 373, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jLayeredPane1Layout.setVerticalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jLayeredPane1Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tambahBaseTelor, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tambahBaseManis, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 125, Short.MAX_VALUE)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tambahToppingManis, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tambahToppingTelor, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(19, 19, 19))
        );

        pesanan.setText("Pesanan");
        pesanan.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                pesananMenuSelected(evt);
            }
        });
        jMenuBar1.add(pesanan);

        menu.setForeground(new java.awt.Color(0, 0, 153));
        menu.setText("Menu");
        jMenuBar1.add(menu);

        riwayat.setText("Riwayat");
        riwayat.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                riwayatMenuSelected(evt);
            }
        });
        jMenuBar1.add(riwayat);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 380, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void pesananMenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_pesananMenuSelected
        pesanan framePesanan = new pesanan();

        framePesanan.setLocationRelativeTo(null);
        framePesanan.setVisible(true);

        this.dispose();
    }//GEN-LAST:event_pesananMenuSelected

    private void tambahBaseTelorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tambahBaseTelorActionPerformed
      bukaTambah("menu_topping", "manis");
    }//GEN-LAST:event_tambahBaseTelorActionPerformed

    private void tambahBaseManisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tambahBaseManisActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tambahBaseManisActionPerformed

    private void tambahToppingManisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tambahToppingManisActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tambahToppingManisActionPerformed

    private void tambahToppingTelorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tambahToppingTelorActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tambahToppingTelorActionPerformed

    private void riwayatMenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_riwayatMenuSelected
        com.mycompany.martabak.riwayat frameMenu = new com.mycompany.martabak.riwayat();

        frameMenu.setLocationRelativeTo(null);
        frameMenu.setVisible(true);

        this.dispose();
    }//GEN-LAST:event_riwayatMenuSelected

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new menu().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTable jTable3;
    private javax.swing.JTable jTable5;
    private javax.swing.JMenu menu;
    private javax.swing.JMenu pesanan;
    private javax.swing.JMenu riwayat;
    private javax.swing.JButton tambahBaseManis;
    private javax.swing.JButton tambahBaseTelor;
    private javax.swing.JButton tambahToppingManis;
    private javax.swing.JButton tambahToppingTelor;
    // End of variables declaration//GEN-END:variables

    
}
