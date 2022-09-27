package de.Linus122.TimeIsMoney.data;

import de.Linus122.TimeIsMoney.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.sql.Date;
import java.util.*;

public class MySQLPluginData extends PluginData{

    private Connection connection;

    public class PendingPayout {

    }

    /**
     * Loads data from file if {@link #dataFile} exists.
     */
    public MySQLPluginData(Main main, String host, int port, String username, String database, String password) {
        super(main);

        try {
            this.plugin.getLogger().info(String.format("Trying to connect to jdbc:mysql://%s:%d/%s", host, port, database));
            connection = DriverManager.getConnection(String.format("jdbc:mysql://%s:%d/%s", host, port, database), username, password);

            this.plugin.getLogger().info(String.format("MySQL connected!", host, port, database));

            // table for the player data
            String sqlCreate = "CREATE TABLE IF NOT EXISTS playerData ("
                    + "   uuid                     VARCHAR(36) PRIMARY KEY,"
                    + "   receivedToday            DOUBLE,"
                    + "   secondsSinceLastPayout   INTEGER,"
                    + "   lastPayoutDate           DATE)";

            Statement statement = connection.createStatement();
            statement.execute(sqlCreate);

            // table for tracking pending payouts for other servers
            String sqlCreate2 = "CREATE TABLE IF NOT EXISTS pendingPayouts ("
                    + "   id              INT NOT NULL AUTO_INCREMENT,"
                    + "   uuid            VARCHAR(36),"
                    + "   date            DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                    + "   PRIMARY KEY (id))";

            Statement statement2 = connection.createStatement();
            statement2.execute(sqlCreate2);

            // table for paid payouts
            String sqlCreate3 = "CREATE TABLE IF NOT EXISTS paidPayouts ("
                    + "   payout_id        INT NOT NULL,"
                    + "   server           VARCHAR(24),"
                    + "   FOREIGN KEY (payout_id) REFERENCES pendingPayouts(id) ON DELETE CASCADE)";

            Statement statement3 = connection.createStatement();
            statement3.execute(sqlCreate3);

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * Saves the data on disc to file {@link #dataFile}
     */
    public void saveData() {
        playerDataMap.forEach(this::savePlayerData);
    }

    public void savePlayerData(UUID uuid, PlayerData playerData) {
        try {
            PreparedStatement preparedStatement = connection
                    .prepareStatement("REPLACE INTO playerData (uuid, receivedToday, secondsSinceLastPayout, lastPayoutDate) VALUES (?, ?, ? ,?)");
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.setDouble(2, playerData.getReceivedToday());
            preparedStatement.setInt(3, playerData.getSecondsSinceLastPayout());
            preparedStatement.setDate(4, new java.sql.Date(playerData.getLastPayoutDate().getTime()));

            preparedStatement.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadData() {

    }

    private AbstractMap.Entry<UUID, PlayerData> readPlayerData(ResultSet result) throws SQLException {
        UUID uuid = UUID.fromString(result.getString("uuid"));
        double receivedToday = result.getDouble("receivedToday");
        int secondsOnline = result.getInt("secondsSinceLastPayout");
        Date date = new Date(result.getDate("lastPayoutDate").getTime());

        return new AbstractMap.SimpleEntry<>(uuid, new PlayerData(receivedToday, date, secondsOnline));
    }

    @Blocking
    public PlayerData getPlayerData(@NotNull Player player) {
        if(playerDataMap.containsKey(player.getUniqueId())) {
            return playerDataMap.get(player.getUniqueId());
        }
        try{
            // get data from DB
            ResultSet result = connection.prepareStatement("SELECT * FROM playerData WHERE uuid='" + player.getUniqueId() + "'").executeQuery();
            if(result.next()) {
                AbstractMap.Entry<UUID, PlayerData> data = this.readPlayerData(result);
                playerDataMap.put(data.getKey(), data.getValue());
            } else {
                // no entry, create new object
                playerDataMap.put(player.getUniqueId(), new PlayerData(0, new java.util.Date(), 0));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return playerDataMap.get(player.getUniqueId());
    }

    public void createPendingPayout(Player player) {
        try {
            PreparedStatement preparedStatement = connection
                    .prepareStatement("INSERT INTO pendingPayouts (uuid) VALUES (?)");
            preparedStatement.setString(1, player.getUniqueId().toString());

            preparedStatement.execute();


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getServerName() {
        return Bukkit.getServer().getWorlds().get(0).getUID().toString().substring(0,16) + ":" + Bukkit.getServer().getPort();
    }

    public List<Integer> getPendingPayouts(Date dateFrom, Player player) {
        List<Integer> pendingPayouts = new ArrayList<>();

        try{
            //ResultSet result = connection.prepareStatement("SELECT * FROM `pendingPayouts` WHERE NOT EXISTS (SELECT * FROM paidPayouts WHERE server='" + this.getServerName() + "' AND pendingPayouts.id = paidPayouts.payout_id) AND uuid='" + player.getUniqueId() + "';").executeQuery();
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `pendingPayouts` WHERE uuid=? AND date>?");
            preparedStatement.setString(1, player.getUniqueId().toString());
            preparedStatement.setTimestamp(2, new Timestamp(dateFrom.getTime()));

            ResultSet result = preparedStatement.executeQuery();

            while(result.next()) {
                int id = result.getInt("id");

                pendingPayouts.add(id);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return pendingPayouts;
    }

}
