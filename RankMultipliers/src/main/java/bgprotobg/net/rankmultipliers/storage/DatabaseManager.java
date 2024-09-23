package bgprotobg.net.rankmultipliers.storage;

import bgprotobg.net.rankmultipliers.RankMultipliers;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final RankMultipliers plugin;
    private Connection connection;

    public DatabaseManager(RankMultipliers plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    private void initDatabase() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
                plugin.getLogger().info("Data folder created at " + dataFolder.getAbsolutePath());
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder + "/multipliers.db");
            plugin.getLogger().info("Database connected successfully.");

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS multipliers (" +
                        "player_name TEXT, " +
                        "currency TEXT, " +
                        "amount REAL, " +
                        "duration_left LONG)");
                plugin.getLogger().info("Table `multipliers` created or verified successfully.");
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Could not initialize SQLite database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            plugin.getLogger().warning("Database connection is closed. Attempting to reconnect...");
            initDatabase();
        }
    }

    public void saveMultiplier(MultiplierData data) {
        plugin.getLogger().info("Queueing save operation for player: " + data.getPlayerName());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                checkConnection();
                try (PreparedStatement statement = connection.prepareStatement(
                        "REPLACE INTO multipliers (player_name, currency, amount, duration_left) VALUES (?, ?, ?, ?)")) {
                    statement.setString(1, data.getPlayerName());
                    statement.setString(2, data.getCurrency());
                    statement.setDouble(3, data.getAmount());
                    statement.setLong(4, data.getDurationLeft());
                    statement.executeUpdate();
                    plugin.getLogger().info("Successfully saved multiplier data asynchronously.");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not save multiplier data asynchronously: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }



    public CompletableFuture<List<MultiplierData>> loadMultipliers(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            List<MultiplierData> multipliers = new ArrayList<>();
            try {
                checkConnection();
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT currency, amount, duration_left FROM multipliers WHERE player_name = ?")) {
                    statement.setString(1, playerName);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        long currentTime = System.currentTimeMillis();

                        while (resultSet.next()) {
                            String currency = resultSet.getString("currency");
                            double amount = resultSet.getDouble("amount");
                            long expiryTime = resultSet.getLong("duration_left");

                            long timeLeft = expiryTime - currentTime;

                            if (timeLeft > 0) {
                                multipliers.add(new MultiplierData(playerName, currency, amount, expiryTime));
                            } else {
                                removeMultiplier(playerName, currency);
                                plugin.getLogger().info("Removed expired multiplier: " + currency + " for player " + playerName);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not load multiplier data: " + e.getMessage());
                e.printStackTrace();
            }
            return multipliers;
        });
    }


    public void removeMultiplier(String playerName, String currency) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                checkConnection();
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM multipliers WHERE player_name = ? AND currency = ?")) {
                    statement.setString(1, playerName);
                    statement.setString(2, currency);
                    statement.executeUpdate();
                    plugin.getLogger().info("Successfully removed multiplier asynchronously: " + currency + " for player " + playerName);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not remove multiplier data asynchronously: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed successfully.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not close SQLite database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
