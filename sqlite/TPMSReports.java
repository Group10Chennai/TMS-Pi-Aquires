package sqlite;

import java.io.BufferedInputStream;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;


public class TPMSReports {
    
    static String HOST_URL = "https://139.59.84.149:8080/TMS/api/tms/TPMSDailyReport";  //live-server
    static String HOST_URL1 = "https://172.16.0.229:8080/TMS/api/tms/TPMSDailyReport";    //localhost
    public static void main(String[] args) {
        
        for(int i = 0; i < 1500; i++)
        {
            
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
            System.out.println("online time "+now);
            ZonedDateTime nextRun = now.withHour(15).withMinute(50).withSecond(0);
            System.out.println("manul time "+nextRun);
            System.out.println(i);
            if(now.equals(nextRun)){
                System.out.println("inside time match");
                try {
                    String URI = HOST_URL;
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
                    System.out.println("status_code - " + status_code);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                i = 0;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(TPMSReports.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
