package project.cs249.test.util;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import project.cs249.src.util.*;
/*
 * Log levels 
SEVERE	1000	Indicates some serious failure
WARNING	900	Potential Problem
INFO	800	General Info
CONFIG	700	Configuration Info
FINE	500	General developer info
FINER	400	Detailed developer info
FINEST	300	Specialized Developer Info
 */
public class UtilsTest {
    private final static Logger LOGGER=Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    @Test
    public void testHexToBytes(){
        String str_hex="2f4a33";
        byte[] bytes_tmp=Utils.hexToBytes(str_hex);

        LOGGER.log(Level.INFO, "hex String: "+str_hex+" bytes: ");
        for (int i = 0; i < bytes_tmp.length; i++) {
            System.out.print(bytes_tmp[i] + " ");
        }
    }

    @Test
    public void testBytesTohex(){
        byte[] bytes_tmp={47,74,51};
        String str_tmp=Utils.bytesToHex(bytes_tmp);
        LOGGER.log(Level.INFO,str_tmp);
    }

    @Test 
    public void testDateTimeToHex(){
        LOGGER.log(Level.INFO,Utils.dateTimeToHex());
    }

    @Test
    public void testHexToDateTime(){
        String str_tmp=Utils.dateTimeToHex();
        LocalDateTime ldt_tmp=Utils.hexToDateTime(str_tmp);
        LOGGER.log(Level.INFO, ""+ldt_tmp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
