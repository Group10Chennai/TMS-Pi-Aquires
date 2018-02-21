/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sqlite;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
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
//import javax.mail.Authenticator;
//import javax.mail.Message;
//import javax.mail.PasswordAuthentication;
//import javax.mail.Session;
//import javax.mail.Transport;
//import javax.mail.internet.InternetAddress;
//import javax.mail.internet.MimeMessage;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author group10
 */
// export CLASSPATH=/opt/Aquire/jars/sqlite-jdbc-3.19.3.jar:/opt/Aquire/jars/java-json.jar:/opt/Aquire/jars/log4j-1.2.17.jar:/opt/Aquire/;javac TMSDataUpdater.java
// export CLASSPATH=/opt/Aquire/jars/sqlite-jdbc-3.19.3.jar:/opt/Aquire/jars/java-json.jar:/opt/Aquire/jars/log4j-1.2.17.jar:/opt/Aquire/;java sqlite.TMSDataUpdater
public class TMSDataUpdater extends TimerTask {

    static String HOST_URL = "https://tpms-api.placer.in/TMS/";

    static int TIME_INTERVEL = 10 * 60 * 1000;

    static String SQLITE_DB_PATH = "jdbc:sqlite:/opt/Aquire/sqlite/TPMS.db";

    static String MAIL_ADDRESS = "noreply@groupten.com";
    static String MAIL_PASSWORD = "Group10@123";
    static String TO_MAIL_ADDRESSES = "ganta@groupten.com, avineshwaran@groupten.com";

    static String API_URL = "api/tms/saveTPMSLatestData";
    static Logger log = Logger.getLogger(TMSDataUpdater.class.getName());

    public final static void main(String args[]) {

        try {
            // Load properties
            setProperties();

            //Program started at
            log.info("Boot Running Started on " + new Date());

            TMSDataUpdater obj = new TMSDataUpdater();
            JSONObject requestParam = new JSONObject();
            requestParam.put("device_date_time", (new Date()).getTime());
            requestParam.put("vehId", 7);
            requestParam.put("count", 0);
            int master_id = 0;
            //master_id = obj.addDummyData_master(requestParam);

            if (master_id > 0) {

                requestParam.put("report_data_master_id", master_id);

                // Individual tire details
                requestParam.put("tirePosition", "01");
                requestParam.put("sensorUID", "32a7c5");
                requestParam.put("tireId", 1);
                requestParam.put("pressure", 1);
                requestParam.put("temp", 1);
                requestParam.put("sensor_status", "000100");
                obj.addDummyData_child(requestParam);

                requestParam.put("tirePosition", "02");
                requestParam.put("sensorUID", "32a7c5");
                requestParam.put("tireId", 2);
                requestParam.put("pressure", 1);
                requestParam.put("temp", 1);
                requestParam.put("sensor_status", "000100");
                obj.addDummyData_child(requestParam);

                requestParam.put("tirePosition", "03");
                requestParam.put("sensorUID", "32a7c5");
                requestParam.put("tireId", 3);
                requestParam.put("pressure", 1);
                requestParam.put("temp", 1);
                requestParam.put("sensor_status", "000100");
                obj.addDummyData_child(requestParam);

                requestParam.put("tirePosition", "04");
                requestParam.put("sensorUID", "32a7c5");
                requestParam.put("tireId", 4);
                requestParam.put("pressure", 1);
                requestParam.put("temp", 1);
                requestParam.put("sensor_status", "000100");
                obj.addDummyData_child(requestParam);

                requestParam.put("tirePosition", "05");
                requestParam.put("sensorUID", "32a7c5");
                requestParam.put("tireId", 5);
                requestParam.put("pressure", 1);
                requestParam.put("temp", 1);
                requestParam.put("sensor_status", "000100");
                obj.addDummyData_child(requestParam);

                requestParam.put("tirePosition", "06");
                requestParam.put("sensorUID", "32a7c5");
                requestParam.put("tireId", 6);
                requestParam.put("pressure", 1);
                requestParam.put("temp", 1);
                requestParam.put("sensor_status", "000100");

                obj.addDummyData_child(requestParam);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            TimerTask timerTask = new TMSDataUpdater();
            //running timer task
            Timer timer = new Timer();
            //It is going to call the run method once in every 10 sec
            timer.scheduleAtFixedRate(timerTask, 0, TIME_INTERVEL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setProperties() {

        Properties prop = new Properties();
        InputStream input = null;

        try {
            PropertyConfigurator.configure("/opt/Aquire/properties/log4j_TMSDataUpdater.properties");
            input = new FileInputStream("/opt/Aquire/properties/TMSDataUpdater.properties");
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
        log.info("<<<<<<<<<<<<<<<<<<< beep " + new Date());
        checkAndSendDataToServer();
    }

    private void checkAndSendDataToServer() {
        Connection conn = null;
        int rsLength = 0;
        try {
            conn = connectToSQLite();

            Statement stmt = conn.createStatement();
            String sql = "SELECT * FROM Report_data_master where count <= 5;";
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                try {
                    rsLength++;
                    // Send this data to server and then update the record from the database depends on API response
                    JSONObject requestParam = new JSONObject();
                    requestParam.put("vehId", rs.getInt("vehId"));
                    requestParam.put("device_date_time", rs.getLong("device_date_time"));

                    JSONArray tyres = new JSONArray();
                    //Get master table key
                    int master_id = rs.getInt("report_data_master_id");
                    int count = rs.getInt("count");
                    String sql1 = "SELECT * FROM Report_data_child where report_data_master_id = " + master_id;
                    Statement stmt1 = conn.createStatement();
                    ResultSet rs1 = stmt1.executeQuery(sql1);
                    while (rs1.next()) {
                        JSONObject tyre = prepareTyreObj(rs1.getString("tirePosition"), rs1.getString("sensorUID"),
                                rs1.getDouble("pressure"), rs1.getDouble("temp"), rs1.getString("sensor_status"));
                        tyres.put(tyre);
                    }
                    requestParam.put("tyres", tyres);

                    // Calling API                    
                    JSONObject resp = callAPI(API_URL, requestParam);
                    if (null != resp) {
                        if (resp.getInt("responseCode") == 10000) {
                            // There is no internet connection
                            log.warn("Unable to connect to Server");
                        } else if (resp.getInt("responseCode") == 10001) {
                            // There is no internet connection
                            log.warn("No internet connection");
                        } else if (resp.getBoolean("status")) {
                            // Sent data to server successfully
                            // Delete the records from child
                            String deleteQuery = "delete from Report_data_child where report_data_master_id = ?";
                            PreparedStatement preparedStmt = conn.prepareStatement(deleteQuery);
                            preparedStmt.setLong(1, master_id);

                            // execute the java preparedstatement
                            preparedStmt.executeUpdate();

                            // Delete the record from master
                            deleteQuery = "delete from Report_data_master where report_data_master_id = ?";
                            preparedStmt = conn.prepareStatement(deleteQuery);
                            preparedStmt.setLong(1, master_id);

                            // execute the java preparedstatement
                            preparedStmt.executeUpdate();
                        } else {
                            if (count == 5) {
                                // Send a mail with this obj and update the count
                                log.info("send a mail");
//                                sendErrorMail("TMS Data is not updating", requestParam.toString(), TO_MAIL_ADDRESSES);
                            }
                            count++;
                            String query = "update Report_data_master set count = ? where report_data_master_id = ?";
                            PreparedStatement preparedStmt = conn.prepareStatement(query);
                            preparedStmt.setInt(1, count);
                            preparedStmt.setInt(2, master_id);

                            // execute the java preparedstatement
                            preparedStmt.executeUpdate();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(e.getMessage());
                }
            }
            log.info("call api and update Size: " + rsLength);
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
                log.info(ex.getMessage());
            }
        }
    }

    private JSONObject prepareTyreObj(String position, String sensorUID, double pressure, double temp, String sensor_status) {
        JSONObject tyre = new JSONObject();
        try {
            if (position.equalsIgnoreCase("01")) {
                tyre.put("position", "FL");
            } else if (position.equalsIgnoreCase("02")) {
                tyre.put("position", "FR");
            } else if (position.equalsIgnoreCase("03")) {
                tyre.put("position", "RLO");
            } else if (position.equalsIgnoreCase("04")) {
                tyre.put("position", "RLI");
            } else if (position.equalsIgnoreCase("05")) {
                tyre.put("position", "RRI");
            } else if (position.equalsIgnoreCase("06")) {
                tyre.put("position", "RRO");
            }

            tyre.put("sensorUID", sensorUID);
            tyre.put("pressure", pressure);
            tyre.put("temp", temp);
            tyre.put("sensor_status", sensor_status);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
        return tyre;
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
            log.info(e.getMessage());
        }
        return conn;
    }

    private JSONObject callAPI(String API_URL, JSONObject urlParameters) {
        JSONObject json_resp = null;
        try {
            String url = HOST_URL + API_URL;
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            //add reuqest header
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-type", "application/json; charset=utf-8");

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters.toString());
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            json_resp = new JSONObject(response.toString());
            json_resp.put("responseCode", responseCode);

            con.disconnect();

        } catch (ConnectException ce) {
            // Unable to connect to server
            try {
                json_resp = new JSONObject();
                json_resp.put("responseCode", 10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ce.printStackTrace();
            log.error(ce.getMessage());
        } catch (SocketException se) {
            //Socket exception - Connection is not established
            try {
                json_resp = new JSONObject();
                json_resp.put("responseCode", 10001);
            } catch (Exception e) {
                e.printStackTrace();
            }
            se.printStackTrace();
            log.error(se.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
        return json_resp;
    }

    private int addDummyData_master(JSONObject urlParameters) {
        Connection conn = null;
        try {
            conn = connectToSQLite();
            String sql = "INSERT INTO Report_data_master(vehId, device_date_time, count) VALUES(?, ?, ?)";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, urlParameters.getInt("vehId"));
            pstmt.setLong(2, urlParameters.getLong("device_date_time"));
            pstmt.setInt(3, urlParameters.getInt("count"));

            return pstmt.executeUpdate();

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

        return 0;
    }

    private void addDummyData_child(JSONObject urlParameters) {
        Connection conn = null;
        try {
            conn = connectToSQLite();
            String sql = "INSERT INTO Report_data_child(report_data_master_id, vehId, tireId, tirePosition, sensorUID, pressure, temp, "
                    + "sensor_status) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, urlParameters.getInt("report_data_master_id"));
            pstmt.setInt(2, urlParameters.getInt("vehId"));
            pstmt.setInt(3, urlParameters.getInt("tireId"));
            pstmt.setString(4, urlParameters.getString("tirePosition"));
            pstmt.setString(5, urlParameters.getString("sensorUID"));
            pstmt.setDouble(6, urlParameters.getDouble("pressure"));
            pstmt.setDouble(7, urlParameters.getDouble("temp"));
            pstmt.setString(8, urlParameters.getString("sensor_status"));

            pstmt.executeUpdate();

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
    /*
    public void sendErrorMail(String subject, String body, String address)
    {
    	System.out.println("in sendErrorMail "+new Date());
        // Get system properties
        Properties properties = System.getProperties();

        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host","m.outlook.com");// "smtp.gmail.com");
        properties.put("mail.smtp.user", MAIL_ADDRESS); // User name
        properties.put("mail.smtp.password", MAIL_PASSWORD); // password
//        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.store.protocol", "pop3");

        try {
            Session session = Session.getDefaultInstance(properties,
                    new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(MAIL_ADDRESS, MAIL_PASSWORD);
                }
            });

            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(MAIL_ADDRESS));
            
            InternetAddress[] iAdressArray = InternetAddress.parse(address);
            msg.setRecipients(Message.RecipientType.TO, iAdressArray);

            msg.setSubject(subject);
            msg.setText(body);
            msg.setSentDate(new Date());

            Transport.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
     */
}
