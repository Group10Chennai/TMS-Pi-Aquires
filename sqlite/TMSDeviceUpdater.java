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
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author group10
 */
// export CLASSPATH=/opt/Aquire/sqlite/DeviceUpdaterJars/sqlite-jdbc-3.19.3.jar:/opt/Aquire/sqlite/DeviceUpdaterJars/java-json.jar:/opt/Aquire/sqlite/DeviceUpdaterJars/log4j-1.2.17.jar:/opt/Aquire/;javac TMSDeviceUpdater.java
// export CLASSPATH=/opt/Aquire/sqlite/DeviceUpdaterJars/sqlite-jdbc-3.19.3.jar:/opt/Aquire/sqlite/DeviceUpdaterJars/java-json.jar:/opt/Aquire/sqlite/DeviceUpdaterJars/log4j-1.2.17.jar:/opt/Aquire/;java sqlite.TMSDeviceUpdater
public class TMSDeviceUpdater extends TimerTask {

    static Logger log = Logger.getLogger(TMSDeviceUpdater.class.getName());
    
    private long gloableLastUpdateDateTime = 1262284200000l;

    // Development Configurations
    static String HOST_URL = "https://qas.placer.in/TMS/";

    static int TIME_INTERVEL = 10 * 60 * 1000;

    static String SQLITE_DB_PATH = "jdbc:sqlite:/opt/Aquire/sqlite/TPMS.db";
    
    static int DROP_TABLE_EXCEPT_LIVE_TABLE_STATUS = 0;
    
    static int DROP_TABLE_LIVE_TABLE_STATUS = 0;

    public final static void main(String args[]) {
        System.out.println("com.sqlite.sample.TMLDeviceUpdater.main() " + new Date());

        TMSDeviceUpdater obj = new TMSDeviceUpdater();
        Connection conn = null;
        try {
            // Load the properties
            setProperties();
            
            // Create the connection for first time
            conn = obj.connectToSQLite();

            Statement stmt = conn.createStatement();
            String sqlCommand = "DROP TABLE IF EXISTS DeviceDetails ";
            if( DROP_TABLE_EXCEPT_LIVE_TABLE_STATUS == 1){
                System.out.println("Drop status : " + stmt.execute(sqlCommand));
            }
            
            // create Device details table
            String sql = "CREATE TABLE IF NOT EXISTS DeviceDetails(vehId INTEGER PRIMARY KEY, vehName text NOT NULL, "
                    + "BID integer, BUID text, RFID integer, RFUID text)";

            System.out.println("create q: " + sql);
            boolean createQStatus = stmt.execute(sql);
            if (createQStatus) {
                System.out.println("Table DeviceDetails is created successfully");
            } else {
                System.out.println("Table DeviceDetails is already exists");
            }

            // Create Tire details table
            if( DROP_TABLE_EXCEPT_LIVE_TABLE_STATUS == 1){
                sqlCommand = "DROP TABLE IF EXISTS TireDetails";
                System.out.println("Drop status : " + stmt.execute(sqlCommand));
            }
            
            String tire_creation_sql = "CREATE TABLE IF NOT EXISTS TireDetails(tireId INTEGER PRIMARY KEY,"
                    + " tireNumber text NOT NULL, sensorId Integer, sensorUID text, tirePosition String, vehId INTEGER)";

            System.out.println("create q: " + tire_creation_sql);
            createQStatus = stmt.execute(tire_creation_sql);
            if (createQStatus) {
                System.out.println("Table TireDetails is created successfully");
            } else {
                System.out.println("Table TireDetails is already exists");
            }

            // Create table for offline future
            // Two tables Report_data_master, Report_data_child
            if( DROP_TABLE_LIVE_TABLE_STATUS == 1){
                sqlCommand = "DROP TABLE IF EXISTS Report_data_master";
                System.out.println("Drop status : " + stmt.execute(sqlCommand));
            }
            
            String report_data_master_creation_sql = "CREATE TABLE IF NOT EXISTS"
                    + " Report_data_master(report_data_master_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " vehId INTEGER NOT NULL, device_date_time Long NOT NULL, count INTEGER)";

            System.out.println("create q: " + report_data_master_creation_sql);
            createQStatus = stmt.execute(report_data_master_creation_sql);
            if (createQStatus) {
                System.out.println("Table Report_data_master is created successfully");
            } else {
                System.out.println("Table Report_data_master is already exists");
            }

            if( DROP_TABLE_LIVE_TABLE_STATUS == 1){
                sqlCommand = "DROP TABLE IF EXISTS Report_data_child";
                System.out.println("Drop status : " + stmt.execute(sqlCommand));
            }
            
            String report_data_child_creation_sql = "CREATE TABLE IF NOT EXISTS"
                    + " Report_data_child(report_data_child_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " report_data_master_id INTEGER NOT NULL, vehId integer NOT NULL, tireId integer, tirePosition text NOT NULL,"
                    + " sensorUID text NOT NULL, pressure double NOT NULL, temp double NOT NULL, sensor_status text NULL)";

            System.out.println("create q: " + report_data_child_creation_sql);
            createQStatus = stmt.execute(report_data_child_creation_sql);
            if (createQStatus) {
                System.out.println("Table Report_data_child is created successfully");
            } else {
                System.out.println("Table Report_data_child is already exists");
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    System.out.println("DB connection closed");
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
        try {
            TimerTask timerTask = new TMSDeviceUpdater();
            //running timer task
            Timer timer = new Timer();
            //It is going to call the run method once in every 10 sec
            timer.scheduleAtFixedRate(timerTask, 0, TIME_INTERVEL);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
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

            if (null != prop.getProperty("DROP_TABLE_EXCEPT_LIVE_TABLE_STATUS")) {
                DROP_TABLE_EXCEPT_LIVE_TABLE_STATUS = Integer.valueOf(prop.getProperty("DROP_TABLE_EXCEPT_LIVE_TABLE_STATUS"));
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

    @Override
    public void run() {
        System.out.println("<<<<<<<<<<<<<<<<<<< beep " + new Date());
        Date currentDate = new Date();
        callDeviceDetailsAPI(gloableLastUpdateDateTime);
        callTireDetailsAPI(gloableLastUpdateDateTime);
        gloableLastUpdateDateTime = currentDate.getTime();
        //getDeviceDetails();
        //getTireDetails();
        log.info("last updated device date time is: "+new Date(gloableLastUpdateDateTime));
    }

    private void callTireDetailsAPI(long lastUpdateDateTime) {
        try {
            System.out.println("Call Tire Details API @ " + new Date(lastUpdateDateTime));
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
                processTireDetails(jsonResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void callDeviceDetailsAPI(long lastUpdateDateTime) {
        try {
            System.out.println("Call Device Details API @ " + new Date(lastUpdateDateTime));
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

            System.out.println("Connection to SQLite has been established.");

        } catch (Exception e) {
            System.out.println(e.getMessage());
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
                        System.out.println(vehDetails.getString("vehName") + " already exists");
                        updateDeviceDetails(conn, vehDetails.getInt("vehId"), vehDetails.getString("vehName"),
                                controllerID, controllerUID, rfid, rfidUID);
                    } else {
                        // New Vehicle details
                        System.out.println(vehDetails.getString("vehName") + " new vehicle");
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
                    System.out.println("DB connection closed");
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
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
            System.out.println(e.getMessage());
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
            System.out.println(e.getMessage());
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
                    System.out.println("tire position " + tirePosition);
                    System.out.println("vehId " + vehId);
                    if (rs.next()) {
                        // Vehicle details are already exist
                        System.out.println(tireDetails.getString("tireNumber") + " already exists");
                        updateTireDetails(conn, tireDetails.getInt("tireId"), tireDetails.getString("tireNumber"),
                                tireDetails.getInt("sensorId"), sensorUID, tirePosition, vehId);
                    } else {
                        // New Vehicle details
                        System.out.println(tireDetails.getString("tireNumber") + " new tire details");
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
                    System.out.println("DB connection closed");
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
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
            System.out.println(e.getMessage());
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
            System.out.println(e.getMessage());
        }
        return false;
    }

    public void getDeviceDetails() {
        System.out.println("<<< Show all device details >>>");
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
                System.out.println(rs.getInt(1) + "-" + rs.getString(2) + "-" + rs.getInt(3) + "-" + rs.getString(4)
                        + "-" + rs.getInt(5) + "-" + rs.getString(6));
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    System.out.println("DB connection closed");
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    public void getTireDetails() {
        System.out.println("<<< Show all Tire details >>>");
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
                System.out.println(rs.getInt(1) + "-" + rs.getString(2) + "-" + rs.getInt(3) + "-" + rs.getString(4)
                        + "-" + rs.getString(5) + "-" + rs.getInt(6));
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    System.out.println("DB connection closed");
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}
