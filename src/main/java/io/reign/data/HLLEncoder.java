package io.reign.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;
import com.google.common.io.BaseEncoding;

public class HLLEncoder {

    private static final Logger logger = LoggerFactory.getLogger(HLLEncoder.class);

    /** Read the object from Base64 string. */
    public static HyperLogLogPlus fromString(String s) {
        try {
            byte[] data = BaseEncoding.base64().decode(s);
            ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream(data));
            Object o = ois.readObject();
            ois.close();
            return (HyperLogLogPlus) o;
        } catch (Exception e) {
            logger.debug("Exception while decoding HLL: {}", e);
            return null;
        }
    }

    /** Write the object to a Base64 string. */
    public static String toString(HyperLogLogPlus o) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            oos.close();
            return new String(BaseEncoding.base64().encode(baos.toByteArray()));
        } catch (Exception e) {
            logger.debug("Exception while encoding HLL: {}", e);
            return "";
        }
    }
}