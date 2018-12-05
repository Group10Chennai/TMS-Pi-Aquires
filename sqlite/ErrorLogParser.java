package sqlite;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import static sqlite.TMSDataUpdater.log;


/**
 *
 * @author group10
 */
// export CLASSPATH=/opt/Aquire/jars/sqlite-jdbc-3.19.3.jar:/opt/Aquire/jars/java-json.jar:/opt/Aquire/jars/log4j-1.2.17.jar:/opt/Aquire/;javac ErrorLogParser.java
// export CLASSPATH=/opt/Aquire/jars/sqlite-jdbc-3.19.3.jar:/opt/Aquire/jars/java-json.jar:/opt/Aquire/jars/log4j-1.2.17.jar:/opt/Aquire/;java sqlite.ErrorLogParser

public class ErrorLogParser{
    
// local credential 
//    static String HOST_URL = "http://172.16.0.229:8080/TMS/api/tms/pierrorlog";
//    static String ERROR_LOG_PATH = "/opt/properties/PiError/";
//    static String ERROR_LOG_FILE_NAME = "loggingRotatingFileExample.log";
//    static String ERROR_LOG_FILE_RENAME = "RenamedLogFile";
    
// server credential
    static String HOST_URL = "http://139.59.84.149:8080/TMS/api/tms/pierrorlog";
    static String ERROR_LOG_PATH = "/home/pi/Documents/TMS-Git/log";
    static String ERROR_LOG_FILE_NAME = "loggingRotatingFileExample.log";
    static String ERROR_LOG_FILE_RENAME = "RenamedLogFile";


    public static void main(String[] args) {

        File folder = new File(ERROR_LOG_PATH);
        ErrorLogParser listFiles = new ErrorLogParser();
        listFiles.listAllFiles(folder);
    }

    public void listAllFiles(File folder) {
        File[] fileNames = folder.listFiles();
        for (File file : fileNames) {
            try {
                if (file.isDirectory()) {
                    listAllFiles(file);
                } else {
                    System.out.println(file.getName());
                    processErrorLogFile(file);

                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void processErrorLogFile(File fileNames) {
        try {
            String file1 = fileNames.getPath();
            File errorLogFile = new File(file1);
            Map<String, JSONObject> haspMap = new HashMap<>();
            if (errorLogFile.exists()) {
                SimpleDateFormat sdtf = new SimpleDateFormat("dd-MM-yyyy_HHmmss");
                SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String renamedFileName = ERROR_LOG_FILE_RENAME + sdtf.format(new Date());
                File fileProcessing = new File(ERROR_LOG_PATH + renamedFileName + ".txt");
                errorLogFile.renameTo(fileProcessing);
                BufferedReader reader = new BufferedReader(new FileReader(ERROR_LOG_PATH + renamedFileName + ".txt"));
                String line = null;

                while ((line = reader.readLine()) != null) {
                    try {
                        String[] values = line.split("#");
                        JSONObject jsobj = new JSONObject();
                        if (haspMap.containsKey(String.valueOf(values[6]).replaceAll(",", "") + "_" + values[4])) {
                            // Already object exists
                            // Update the latest recored
                            jsobj = haspMap.get(String.valueOf(values[6]).replaceAll(",", "") + "_" + values[4]);
                            String existingDateTime = jsobj.getString("dateTime");
                            if ((new Date(sdt.parse(existingDateTime).getTime())).before((new Date(sdt.parse(values[0].split(",")[0]).getTime())))) {
                                jsobj.put("dateTime", values[0].split(",")[0]);
                                haspMap.put(String.valueOf(values[6]).replaceAll(",", "") + "_" + values[4], jsobj);
                            }
                        } else {
                            jsobj.put("dateTime", values[0].split(",")[0]);
                            jsobj.put("errorCode", values[4]);
                            jsobj.put("desc", values[5]);
                            jsobj.put("vehName", String.valueOf(values[6]).replaceAll(",", ""));
                            jsobj.put("id", values[7]);

                            haspMap.put(String.valueOf(values[6]).replaceAll(",", "") + "_" + values[4], jsobj);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    fileProcessing.delete();
                }
                // Creating a list of objects
                List<JSONObject> errorLogList = new ArrayList<>();
                for (Map.Entry<String, JSONObject> entry : haspMap.entrySet()) {
                    errorLogList.add(entry.getValue());
                }

                JSONObject jsonObj = new JSONObject();
                jsonObj.put("errorLog", errorLogList);

                JSONObject resp = callAPI(jsonObj);
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    private JSONObject callAPI(JSONObject urlParameters) {
        JSONObject json_resp = null;
        try {
            String url = HOST_URL;

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("POST");
            con.setRequestProperty("Content-type", "application/json; charset=utf-8");

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
}







///***************************************************************************************************************
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package sqlite;
//
//import java.io.BufferedReader;
//import java.io.DataOutputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.ConnectException;
//import java.net.HttpURLConnection;
//import java.net.SocketException;
//import java.net.URL;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Properties;
//import org.apache.log4j.Logger;
//import org.apache.log4j.PropertyConfigurator;
//import org.json.JSONObject;
//
///**
// *
// * @author group10
// */
//public class ErrorLogParser {
//    
//    static String HOST_URL = "http://localhost:8080/TMS/";
//    static String API_URL = "api/tms/pierrorlog";
//    static String ERROR_LOG_PATH = "/tmp/";
//    static String ERROR_LOG_FILE_NAME = "loggingRotatingFileExample.log";
//    static String ERROR_LOG_FILE_RENAME = "RenamedLogFile";
//    
//    static Logger log = Logger.getLogger(TMSDataUpdater.class.getName());
//    public final static void main(String args[]) {
//
//        try {
//            // Load properties
//            setProperties();
//
//            //Program started at
//            log.info("<<<<<<<<<<< ErrorLogParser - Boot Running Started on " + new Date());
//            System.out.println("ErrorLogParser -  Boot Running Started on " + new Date());
//            ErrorLogParser elp = new ErrorLogParser();
//            elp.processErrorLogFile();
//            
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
//    
//    private void processErrorLogFile(){
//        try {
//            File errorLogFile = new File(ERROR_LOG_PATH + ERROR_LOG_FILE_NAME);
//            Map<String, JSONObject> haspMap = new HashMap<>();
//            if(errorLogFile.exists())
//            {
//                SimpleDateFormat sdtf = new SimpleDateFormat("dd-MM-yyyy_HHmmss");
//                SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                String renamedFileName = ERROR_LOG_FILE_RENAME+sdtf.format(new Date());
//                File fileProcessing = new File(ERROR_LOG_PATH + renamedFileName+".txt");
//                errorLogFile.renameTo(fileProcessing);
//                BufferedReader reader = new BufferedReader(new FileReader(ERROR_LOG_PATH + renamedFileName+".txt"));
//                String line = null;
//                                
//                while ((line = reader.readLine()) != null) {
//                    try {
//                        System.out.println(line);
//                        String[] values = line.split("#");
//                        JSONObject jsobj = new JSONObject();
//                        if(haspMap.containsKey(String.valueOf(values[6]).replaceAll(",", "")+"_"+values[4])){
//                            // Already object exists
//                            // Update the latest recored
//                            jsobj = haspMap.get(String.valueOf(values[6]).replaceAll(",", "")+"_"+values[4]);
//                            String existingDateTime = jsobj.getString("dateTime");
//                            System.out.println("existing "+existingDateTime);
//                            System.out.println("new "+values[0].split(",")[0]);
//                            if((new Date(sdt.parse(existingDateTime).getTime())).before((new Date(sdt.parse(values[0].split(",")[0]).getTime())))){
//                                System.out.println("in if");
//                                jsobj.put("dateTime", values[0].split(",")[0]);
//                                haspMap.put(String.valueOf(values[6]).replaceAll(",", "")+"_"+values[4], jsobj);
//                            }
//                        } else {
//                            // New object
//                            jsobj.put("dateTime", values[0].split(",")[0]);
//                            jsobj.put("errorCode", values[4]);
//                            jsobj.put("desc", values[5]);
//                            jsobj.put("vehName", String.valueOf(values[6]).replaceAll(",", ""));
//                            jsobj.put("id", values[7]);
//
//                            haspMap.put(String.valueOf(values[6]).replaceAll(",", "")+"_"+values[4], jsobj);
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//                // Creating a list of objects
//                List<JSONObject> errorLogList = new ArrayList<>();
//                for (Map.Entry<String, JSONObject> entry : haspMap.entrySet()) {
//		    System.out.println(entry.getKey() + " = " + entry.getValue());
//                    errorLogList.add(entry.getValue());
//		}
//                
//                JSONObject jsonObj = new JSONObject();
//                jsonObj.put("errorLog", errorLogList);
//                
//                JSONObject resp = callAPI(API_URL, jsonObj);
//                System.out.println("resp: "+resp.toString());
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error(e.getMessage());
//        }
//    }
//    
//    private JSONObject callAPI(String API_URL, JSONObject urlParameters) {
//        JSONObject json_resp = null;
//        try {
//            String url = HOST_URL + API_URL;
//            
//            System.out.println("URL: "+ url);
//            URL obj = new URL(url);
//            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
//
//            //add reuqest header
//            con.setRequestMethod("POST");
//            con.setRequestProperty("Content-type", "application/json; charset=utf-8");
//
//            // Send post request
//            con.setDoOutput(true);
//            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
//            wr.writeBytes(urlParameters.toString());
//            wr.flush();
//            wr.close();
//
//            int responseCode = con.getResponseCode();
//
//            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
//            String inputLine;
//            StringBuffer response = new StringBuffer();
//
//            while ((inputLine = in.readLine()) != null) {
//                response.append(inputLine);
//            }
//            in.close();
//
//            json_resp = new JSONObject(response.toString());
//            json_resp.put("responseCode", responseCode);
//
//            con.disconnect();
//
//        } catch (ConnectException ce) {
//            // Unable to connect to server
//            try {
//                json_resp = new JSONObject();
//                json_resp.put("responseCode", 10000);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            ce.printStackTrace();
//            log.error(ce.getMessage());
//        } catch (SocketException se) {
//            //Socket exception - Connection is not established
//            try {
//                json_resp = new JSONObject();
//                json_resp.put("responseCode", 10001);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            se.printStackTrace();
//            log.error(se.getMessage());
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error(e.getMessage());
//        }
//        return json_resp;
//    }
//    
//    private static void setProperties() {
//
//        Properties prop = new Properties();
//        InputStream input = null;
//
//        try {
//            PropertyConfigurator.configure("/opt/Aquire/properties/log4j_ErrorLogParser.properties");
//            input = new FileInputStream("/opt/Aquire/properties/ErrorLogParser.properties");
//            // load a properties file
//            prop.load(input);
//
//            // get the property value and print it out
//            if (null != prop.getProperty("HOST_URL")) {
//                HOST_URL = prop.getProperty("HOST_URL");
//            }
//            if (null != prop.getProperty("ERROR_LOG_PATH")) {
//                ERROR_LOG_PATH = prop.getProperty("ERROR_LOG_PATH");
//            }
//
//        } catch (IOException ex) {
//            ex.printStackTrace();
//            log.error(ex.getMessage());
//        } finally {
//            if (input != null) {
//                try {
//                    input.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//}
