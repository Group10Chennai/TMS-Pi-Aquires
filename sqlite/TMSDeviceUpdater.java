/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqlite;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author group10
 */
// export CLASSPATH=/opt/Aquire/jars/sqlite-jdbc-3.19.3.jar:/opt/Aquire/jars/java-json.jar:/opt/Aquire/jars/log4j-1.2.17.jar:/opt/Aquire/;javac TMSDeviceUpdater.java
// export CLASSPATH=/opt/Aquire/jars/sqlite-jdbc-3.19.3.jar:/opt/Aquire/jars/java-json.jar:/opt/Aquire/jars/log4j-1.2.17.jar:/opt/Aquire/;java sqlite.TMSDeviceUpdater
public class TMSDeviceUpdater {

    static Logger log = Logger.getLogger(TMSDeviceUpdater.class.getName());

    // Development Configurations
    static String HOST_URL = "https://tpms-api.placer.in/TMS/";

    static int TIME_INTERVEL = 10 * 60 * 1000;

    static String SQLITE_DB_PATH = "jdbc:sqlite:/opt/Aquire/sqlite/TPMS.db";

    static int DROP_TABLE_LIVE_TABLE_STATUS = 0;

    static int DROP_TABLE_DEVICE_TABLE_STATUS = 0;

    public final static void main(String args[]) {

        System.out.println("Boot Running Started on " + new Date());

        TMSDeviceUpdater obj = new TMSDeviceUpdater();
        Connection conn = null;
        try {
            // Load the properties
            BasicConfigurator.configure();
            setProperties();

            log.info("<<<<<<<<<<<< Boot Running Started on " + new Date());
            // Create the connection for first time
            conn = obj.connectToSQLite();

            Statement stmt = conn.createStatement();

            String last_updated_creation_sql = "CREATE TABLE IF NOT EXISTS Last_Updated_On(pId INTEGER PRIMARY KEY, dateInLong Number)";

            boolean createQStatus = stmt.execute(last_updated_creation_sql);
            if (createQStatus) {
                log.info("Last_Updated_On Table is created successfully");
            } else {
                log.info("Table TireDetails is already exists");
            }
            //sri
//            obj.updateTheLastUpdateDateTime(stmt, gloableLastUpdateDateTime);

            String sqlCommand = "DROP TABLE IF EXISTS DeviceDetails ";
            if (DROP_TABLE_DEVICE_TABLE_STATUS == 1) {
                log.info("DeviceDetails Table dropped: " + stmt.execute(sqlCommand));
            }

            // create Device details table
            String sql = "CREATE TABLE IF NOT EXISTS DeviceDetails(vehId INTEGER PRIMARY KEY, vehName text NOT NULL, "
                    + "BID integer, BUID text, RFID integer, RFUID text)";

            createQStatus = stmt.execute(sql);
            if (createQStatus) {
                log.info("Table DeviceDetails is created successfully");
            } else {
                log.info("Table DeviceDetails is already exists");
            }

            // Create Tire details table
            if (DROP_TABLE_DEVICE_TABLE_STATUS == 1) {
                sqlCommand = "DROP TABLE IF EXISTS TireDetails";
                log.info("TireDetails Table dropped: " + stmt.execute(sqlCommand));
            }

            String tire_creation_sql = "CREATE TABLE IF NOT EXISTS TireDetails(tireId INTEGER PRIMARY KEY,"
                    + " tireNumber text NOT NULL, sensorId Integer, sensorUID text, tirePosition String, vehId INTEGER)";

            createQStatus = stmt.execute(tire_creation_sql);
            if (createQStatus) {
                log.info("Table TireDetails is created successfully");
            } else {
                log.info("Table TireDetails is already exists");
            }

            // Create table for offline future
            // Two tables Report_data_master, Report_data_child
            if (DROP_TABLE_LIVE_TABLE_STATUS == 1) {
                sqlCommand = "DROP TABLE IF EXISTS Report_data_master";
                log.info("Report_data_master Table dropped: " + stmt.execute(sqlCommand));
            }

            String report_data_master_creation_sql = "CREATE TABLE IF NOT EXISTS"
                    + " Report_data_master(report_data_master_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " vehId INTEGER NOT NULL, device_date_time Long NOT NULL, count INTEGER)";

            createQStatus = stmt.execute(report_data_master_creation_sql);
            if (createQStatus) {
                log.info("Table Report_data_master is created successfully");
            } else {
                log.info("Table Report_data_master is already exists");
            }

            if (DROP_TABLE_LIVE_TABLE_STATUS == 1) {
                sqlCommand = "DROP TABLE IF EXISTS Report_data_child";
                log.info("Report_data_child Table dropped: " + stmt.execute(sqlCommand));
            }

            String report_data_child_creation_sql = "CREATE TABLE IF NOT EXISTS"
                    + " Report_data_child(report_data_child_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " report_data_master_id INTEGER NOT NULL, vehId integer NOT NULL, tireId integer, tirePosition text NOT NULL,"
                    + " sensorUID text NOT NULL, pressure double NOT NULL, temp double NOT NULL, sensor_status text NULL)";

            createQStatus = stmt.execute(report_data_child_creation_sql);
            if (createQStatus) {
                log.info("Table Report_data_child is created successfully");
            } else {
                log.info("Table Report_data_child is already exists");
            }

            obj.startRunning();

        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    log.info("DB connection closed");
                }
            } catch (SQLException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    private static void setProperties() {

        Properties prop = new Properties();
        InputStream input = null;

        try {
            PropertyConfigurator.configure("/opt/Aquire/properties/log4j_TMSDeviceUpdater.properties");
            input = new FileInputStream("/opt/Aquire/properties/TMSDeviceUpdater.properties");
            // load a properties file
            prop.load(input);

            // get the property value and print it out
            if (null != prop.getProperty("HOST_URL")) {
                HOST_URL = prop.getProperty("HOST_URL");
            }
            if (null != prop.getProperty("TIME_INTERVEL")) {
                TIME_INTERVEL = Integer.valueOf(prop.getProperty("TIME_INTERVEL"));
            }
            if (null != prop.getProperty("SQLITE_DB_PATH")) {
                SQLITE_DB_PATH = prop.getProperty("SQLITE_DB_PATH");
            }

            if (null != prop.getProperty("DROP_TABLE_DEVICE_TABLE_STATUS")) {
                DROP_TABLE_DEVICE_TABLE_STATUS = Integer.valueOf(prop.getProperty("DROP_TABLE_DEVICE_TABLE_STATUS"));
            }

            if (null != prop.getProperty("DROP_TABLE_LIVE_TABLE_STATUS")) {
                DROP_TABLE_LIVE_TABLE_STATUS = Integer.valueOf(prop.getProperty("DROP_TABLE_LIVE_TABLE_STATUS"));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            log.error(ex.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private long getLastUpdatedDateTime() {
        long gloableLastUpdateDateTime = 1262284200000l; // 01-Jan-2010
        Connection conn = null;
        try {
            conn = connectToSQLite();
            Statement stmt = conn.createStatement();
            String sql = "SELECT * FROM Last_Updated_On where pId = 1";

            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                return rs.getLong("dateInLong");
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                log.error(ex.getMessage());
            }
        }
        return gloableLastUpdateDateTime;
    }

    private boolean updateTheLastUpdateDateTime(long dateTime) {
        Connection conn = null;
        try {
            conn = connectToSQLite();
            Statement stmt = conn.createStatement();

            String sql = "SELECT * FROM Last_Updated_On where pId = 1";

            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                // Updated the date time
                sql = "update Last_Updated_On set dateInLong = " + dateTime + " where pId = 1";
            } else {
                // Insert the date time
                sql = "INSERT INTO Last_Updated_On(pId, dateInLong) VALUES(1, " + dateTime + ")";
            }
            stmt.executeUpdate(sql);
            
            rs.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                log.error(ex.getMessage());
            }
        }

        return false;
    }

    public void startRunning() {
        Date currentDate = new Date();

        //Get last updated date time
        long lastUpdatedDateTime = getLastUpdatedDateTime();

        callDeviceDetailsAPI(lastUpdatedDateTime);
        callTireDetailsAPI(lastUpdatedDateTime);

        // Assign new date time
        lastUpdatedDateTime = currentDate.getTime();
        updateTheLastUpdateDateTime(lastUpdatedDateTime);

        //getDeviceDetails();
        //getTireDetails();
        log.info("last updated device date time is: " + new Date(lastUpdatedDateTime));
    }

    private void callTireDetailsAPI(long lastUpdateDateTime) {
        try {
            log.info("Call Tire Details API @ " + new Date(lastUpdateDateTime));
            String URI = HOST_URL + "api/tms/getModifiedTiresList?lastUpdateDateTime=" + lastUpdateDateTime;

            URI = URI.replace(" ", "%20");

            URL u = new URL(URI);
            URLConnection uc = u.openConnection();
            InputStream raw = uc.getInputStream();
            InputStream buffer = new BufferedInputStream(raw);
            Reader r = new InputStreamReader(buffer);
            int c;
            StringBuffer status_code = new StringBuffer();
            while ((c = r.read()) != -1) {
                status_code.append((char) c);
            }

            String processStatus = status_code + "";
            JSONArray jsonResponse = new JSONArray(processStatus);
            if (jsonResponse.length() > 0) {
                log.info("Tire detials are updated- Size: " + jsonResponse.length());
                processTireDetails(jsonResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void callDeviceDetailsAPI(long lastUpdateDateTime) {
        try {
            log.info("Call Device Details API @ " + new Date(lastUpdateDateTime));
            String URI = HOST_URL + "api/tms/getModifiedVehList?lastUpdateDateTime=" + lastUpdateDateTime;

            URI = URI.replace(" ", "%20");

            URL u = new URL(URI);
            URLConnection uc = u.openConnection();
            InputStream raw = uc.getInputStream();
            InputStream buffer = new BufferedInputStream(raw);
            Reader r = new InputStreamReader(buffer);
            int c;
            StringBuffer status_code = new StringBuffer();
            while ((c = r.read()) != -1) {
                status_code.append((char) c);
            }

            String processStatus = status_code + "";
            JSONArray jsonResponse = new JSONArray(processStatus);
            if (jsonResponse.length() > 0) {
                log.info("Vehicle details are updated - Size: " + jsonResponse.length());
                processDeviceDetails(jsonResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Connection connectToSQLite() {
        Connection conn = null;
        try {
            // db parameters
            Class.forName("org.sqlite.JDBC");

            // create a connection to the database
            conn = DriverManager.getConnection(SQLITE_DB_PATH);

            log.info("Connection to SQLite has been established.");

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return conn;
    }

    private boolean processDeviceDetails(JSONArray jsonResponse) {
        // Create a DB connection
        TMSDeviceUpdater obj = new TMSDeviceUpdater();
        Connection conn = obj.connectToSQLite();
        try {
            for (int i = 0; i < jsonResponse.length(); i++) {
                JSONObject vehDetails = jsonResponse.getJSONObject(i);

                if (null != vehDetails) {
                    // Check whether vehicle exists in our database or not
                    Statement stmt = conn.createStatement();
                    String sql = "SELECT * FROM DeviceDetails where vehId =" + vehDetails.getInt("vehId");
                    ResultSet rs = stmt.executeQuery(sql);
                    // Check null values
                    int controllerID = 0;
                    String controllerUID = "";
                    if (!vehDetails.isNull("controllerID")) {
                        controllerID = vehDetails.getInt("controllerID");
                    }
                    if (!vehDetails.isNull("controllerUID")) {
                        controllerUID = vehDetails.getString("controllerUID");
                    }
                    int rfid = 0;
                    String rfidUID = "";
                    if (!vehDetails.isNull("rfid")) {
                        rfid = vehDetails.getInt("rfid");
                    }
                    if (!vehDetails.isNull("rfiduid")) {
                        rfidUID = vehDetails.getString("rfiduid");
                    }

                    if (rs.next()) {
                        // Vehicle details are already exist
                        updateDeviceDetails(conn, vehDetails.getInt("vehId"), vehDetails.getString("vehName"),
                                controllerID, controllerUID, rfid, rfidUID);
                    } else {
                        // New Vehicle details
                        insertDeviceDetails(conn, vehDetails.getInt("vehId"), vehDetails.getString("vehName"),
                                controllerID, controllerUID, rfid, rfidUID);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    log.info("DB connection closed");
                }
            } catch (SQLException ex) {
                log.error(ex.getMessage());
            }
        }
        return false;
    }

    private boolean insertDeviceDetails(Connection conn, int vehId, String vehName, int bluetoothId,
            String BluetoothUID, int RFID, String RFUID) {
        String sql = "INSERT INTO DeviceDetails(vehId, vehName, BID, BUID, RFID, RFUID) VALUES(?,?,?,?,?,?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, vehId);
            pstmt.setString(2, vehName);
            pstmt.setInt(3, bluetoothId);
            pstmt.setString(4, BluetoothUID);
            pstmt.setInt(5, RFID);
            pstmt.setString(6, RFUID);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return false;
    }

    private boolean updateDeviceDetails(Connection conn, int vehId, String vehName, int bluetoothId,
            String BluetoothUID, int RFID, String RFUID) {
        String sql = "update DeviceDetails set vehName=?, BID=?, BUID=?, RFID=?, RFUID=? where vehId = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // set the corresponding param
            pstmt.setString(1, vehName);
            pstmt.setInt(2, bluetoothId);
            pstmt.setString(3, BluetoothUID);
            pstmt.setInt(4, RFID);
            pstmt.setString(5, RFUID);

            pstmt.setInt(6, vehId);
            // update 
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return false;
    }

    private boolean processTireDetails(JSONArray jsonResponse) {
        // Create a DB connection
        TMSDeviceUpdater obj = new TMSDeviceUpdater();
        Connection conn = obj.connectToSQLite();
        try {
            for (int i = 0; i < jsonResponse.length(); i++) {
                JSONObject tireDetails = jsonResponse.getJSONObject(i);

                if (null != tireDetails) {
                    // Check whether vehicle exists in our database or not
                    Statement stmt = conn.createStatement();
                    String sql = "SELECT * FROM TireDetails where tireId =" + tireDetails.getInt("tireId");
                    ResultSet rs = stmt.executeQuery(sql);
                    String tirePosition = "";
                    if (!tireDetails.isNull("tirePosition")) {
                        tirePosition = tireDetails.get("tirePosition") + "";
                    }
                    String sensorUID = "";
                    if (!tireDetails.isNull("sensorUID")) {
                        sensorUID = tireDetails.getString("sensorUID");
                    }

                    int vehId = 0;
                    if (tireDetails.has("vehId") && tireDetails.isNull("vehId")) {
                        vehId = 0;
                    } else {
                        vehId = (Integer) tireDetails.get("vehId");
                    }
                    if (rs.next()) {
                        // Vehicle details are already exist
                        updateTireDetails(conn, tireDetails.getInt("tireId"), tireDetails.getString("tireNumber"),
                                tireDetails.getInt("sensorId"), sensorUID, tirePosition, vehId);
                    } else {
                        // New Vehicle details
                        insertTireDetails(conn, tireDetails.getInt("tireId"), tireDetails.getString("tireNumber"),
                                tireDetails.getInt("sensorId"), sensorUID, tirePosition, vehId);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    log.info("DB connection closed");
                }
            } catch (SQLException ex) {
                log.error(ex.getMessage());
            }
        }
        return false;
    }

    private boolean insertTireDetails(Connection conn, int tireId, String tireNumber, int sensorId,
            String sensorUID, String tirePosition, int vehId) {
        String sql = "INSERT INTO TireDetails(tireId, tireNumber, sensorId, sensorUID, tirePosition, vehId) VALUES(?,?,?,?,?,?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tireId);
            pstmt.setString(2, tireNumber);
            pstmt.setInt(3, sensorId);
            pstmt.setString(4, sensorUID);
            pstmt.setString(5, tirePosition);
            pstmt.setInt(6, vehId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return false;
    }

    private boolean updateTireDetails(Connection conn, int tireId, String tireNumber, int sensorId,
            String sensorUID, String tirePosition, int vehId) {
        String sql = "update TireDetails set tireNumber=?, sensorId=?, sensorUID=?, tirePosition=?, vehId=? where tireId = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // set the corresponding param

            pstmt.setString(1, tireNumber);
            pstmt.setInt(2, sensorId);
            pstmt.setString(3, sensorUID);
            pstmt.setString(4, tirePosition);
            pstmt.setInt(5, vehId);

            pstmt.setInt(6, tireId);
            // update 
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return false;
    }

    private void getDeviceDetails() {
        log.info("<<< Show all device details >>>");
        Connection conn = null;
        try {
            TMSDeviceUpdater obj = new TMSDeviceUpdater();
            conn = obj.connectToSQLite();

            Statement stmt = conn.createStatement();
            String sql = "SELECT * FROM DeviceDetails";
//            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery(sql);

            // loop through the result set
            while (rs.next()) {
//                System.out.println(rs.getInt(1) + "-" + rs.getString(2) + "-" + rs.getInt(3) + "-" + rs.getString(4)
//                        + "-" + rs.getInt(5) + "-" + rs.getString(6));
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    log.info("DB connection closed");
                }
            } catch (SQLException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    private void getTireDetails() {
        log.info("<<< Show all Tire details >>>");
        Connection conn = null;
        try {
            TMSDeviceUpdater obj = new TMSDeviceUpdater();
            conn = obj.connectToSQLite();

            Statement stmt = conn.createStatement();
            String sql = "SELECT * FROM TireDetails";
//            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery(sql);

            // loop through the result set
            while (rs.next()) {
//                System.out.println(rs.getInt(1) + "-" + rs.getString(2) + "-" + rs.getInt(3) + "-" + rs.getString(4)
//                        + "-" + rs.getString(5) + "-" + rs.getInt(6));
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    log.info("DB connection closed");
                }
            } catch (SQLException ex) {
                log.error(ex.getMessage());
            }
        }
    }
}
