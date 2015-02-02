package it.geosolutions.urltesting.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;


public class ContentTypeHelper
{

    private static Map<String, byte[]> magic_numbers;

    static
    {
        magic_numbers = new HashMap<String, byte[]>();
        magic_numbers.put("image/png", new byte[] { -119, 80, 78, 71 }); // \x89PNG
    }

    /**
     * Determine content type of data using some magic numbers
     * @param data
     * @return Content-Type value appropriate based on the magic number, if found, or null otherwise
     */
    public static String resolveContentType(byte[] data)
    {
        for (Map.Entry<String, byte[]> entry : magic_numbers.entrySet())
        {
            boolean match = false;
            if (data.length >= entry.getValue().length)
            {
                match = true;
                for (int i = 0; match && (i < entry.getValue().length); i++)
                {
                    if (entry.getValue()[i] != data[i])
                    {
                        match = false;
                    }
                }
            }
            if (match)
            {
                return entry.getKey();
            }
        }

        return null;
    }

    public static String getHexString(byte[] digest)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digest.length; i++)
        {
            sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    public static String getMessageDigest(File f) throws IOException, NoSuchAlgorithmException
    {
        DigestInputStream dis = null;
        try
        {
            dis = new DigestInputStream(new FileInputStream(f), MessageDigest.getInstance("SHA-1"));

            byte[] buf = new byte[4096];
            while (dis.read(buf) > -1)
            {
            }

            return getHexString(dis.getMessageDigest().digest());
        }
        finally
        {
            if (dis != null)
            {
                dis.close();
            }
        }
    }


}

