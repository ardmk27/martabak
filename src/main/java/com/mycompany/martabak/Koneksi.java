package com.mycompany.martabak;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Koneksi {
    private static final String URL = "jdbc:mysql://localhost:3306/martabak";
    private static final String USER = "root";
    private static final String PASS = "";

    public static Connection getKoneksi() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // tes cepat: klik kanan file ini > Run File
    public static void main(String[] args) {
        try (Connection c = getKoneksi()) {
            System.out.println("Koneksi berhasil!");
        } catch (SQLException e) {
            System.out.println("Gagal: " + e.getMessage());
        }
    }
}