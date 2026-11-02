package com.smartdining.bugs;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.smartdining.utils.ConfigUtil;

/**
 * Doc va don du lieu truc tiep trong SQL Server de kiem chung ket qua.
 * Dung chung cau hinh db.url / db.username / db.password voi ung dung (META-INF/config.properties).
 */
final class TestDatabase {
    /** Moi ban ghi do bo test nay tao ra deu bat dau bang tien to nay (cung quy uoc "QA " cua cleanup-test-data.sql). */
    static final String PREFIX = "QA BUG ";

    private TestDatabase() {
    }

    static Connection connect() throws SQLException {
        String url = ConfigUtil.get("db.url");
        if (url == null) {
            throw new IllegalStateException("Thieu db.url trong META-INF/config.properties");
        }
        return DriverManager.getConnection(url, ConfigUtil.get("db.username"), ConfigUtil.get("db.password"));
    }

    static long count(String sql, Object... params) {
        Object value = scalar(sql, params);
        return value == null ? 0 : ((Number) value).longValue();
    }

    static BigDecimal decimal(String sql, Object... params) {
        return (BigDecimal) scalar(sql, params);
    }

    static Object scalar(String sql, Object... params) {
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getObject(1) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Loi truy van: " + sql, ex);
        }
    }

    /** Xoa ban test (ten bat dau bang PREFIX) cung hoa don, chi tiet, thanh toan, yeu cau cua ban do. */
    static void cleanup() {
        String tables = "SELECT ID FROM [Tables] WHERE TableName LIKE '" + PREFIX + "%'";
        String orders = "SELECT ID FROM [Orders] WHERE Table_ID IN (" + tables + ")";
        String[] statements = {
            "DELETE FROM [Payments] WHERE Order_ID IN (" + orders + ")",
            "DELETE FROM [Order_Details] WHERE Order_ID IN (" + orders + ")",
            "DELETE FROM [Table_Requests] WHERE Table_ID IN (" + tables + ")",
            "DELETE FROM [Orders] WHERE Table_ID IN (" + tables + ")",
            "DELETE FROM [Tables] WHERE TableName LIKE '" + PREFIX + "%'"
        };
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            for (String sql : statements) {
                statement.executeUpdate(sql);
            }
            connection.commit();
        } catch (SQLException ex) {
            throw new IllegalStateException("Khong don duoc du lieu test", ex);
        }
    }
}
