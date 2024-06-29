package org.Snail.Plus.utils;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class HWID {
    public static boolean CheckHWID() {
        String hwid = GetHWID();
        try {
            URL url = new URL("https://textsaver.flap.tv/lists/6lkt");
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(hwid)) {
                    return true;
                }
            }
        } catch (IOException e) {

        }
        return false;
    }

    public static String GetHWID() {
        return DigestUtils.sha3_256Hex(DigestUtils.md2Hex(DigestUtils.sha512Hex(DigestUtils.sha512Hex(System.getenv("os") + System.getProperty("os.name") + System.getProperty("os.arch") + System.getProperty("os.version") + System.getProperty("user.language") + System.getenv("SystemRoot") + System.getenv("HOMEDRIVE") + System.getenv("PROCESSOR_LEVEL") + System.getenv("PROCESSOR_REVISION") + System.getenv("PROCESSOR_IDENTIFIER") + System.getenv("PROCESSOR_ARCHITECTURE") + System.getenv("PROCESSOR_ARCHITEW6432") + System.getenv("NUMBER_OF_PROCESSORS")))));
    }
}