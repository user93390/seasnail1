package org.Snail.Plus.utils;

import org.apache.commons.codec.digest.DigestUtils;

public class HWID {
    public static String GetHWID() {
        return DigestUtils.sha3_256Hex(DigestUtils.md2Hex(DigestUtils.sha512Hex(DigestUtils.sha512Hex(String.valueOf(System.getenv("os")) + System.getProperty("os.name") + System.getProperty("os.arch") + System.getProperty("os.version") + System.getProperty("user.language") + System.getenv("SystemRoot") + System.getenv("HOMEDRIVE") + System.getenv("PROCESSOR_LEVEL") + System.getenv("PROCESSOR_REVISION") + System.getenv("PROCESSOR_IDENTIFIER") + System.getenv("PROCESSOR_ARCHITECTURE") + System.getenv("PROCESSOR_ARCHITEW6432") + System.getenv("NUMBER_OF_PROCESSORS")))));
    }
}