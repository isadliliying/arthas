package com.wingli.arthas.recorder;

import org.apache.catalina.connector.CoyoteOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class CoyoteOutputStreamHelper {

    private static final Logger logger = LoggerFactory.getLogger(RequestRecordHelper.class);

    /**
     * 记录写数据 （插桩接口）
     */
    public static void recordWrite(Object outputStreamObj, Object[] args) {
        try {
            CoyoteOutputStream outputStream = (CoyoteOutputStream) outputStreamObj;
            if (!RequestRecordHelper.needToRecord(outputStream)){
                return;
            }
            if (args.length == 1 && args[0] instanceof Integer) {
                recordWrite1(outputStream, (Integer) args[0]);
            } else if (args.length == 1 && args[0] instanceof byte[]) {
                recordWrite2(outputStream, (byte[]) args[0]);
            } else if (args.length == 3 && args[0] instanceof byte[] && args[1] instanceof Integer && args[2] instanceof Integer) {
                recordWrite3(outputStream, (byte[]) args[0], (Integer) args[1], (Integer) args[2]);
            } else if (args.length == 1 && args[0] instanceof ByteBuffer) {
                recordWrite4(outputStream, (ByteBuffer) args[0]);
            }
        } catch (Throwable t) {
            logger.error("recordWrite err.", t);
        }
    }

    public static void recordWrite1(CoyoteOutputStream outputStream, int byteInt) {
        byte b = (byte) (byteInt & 0xff);
        byte[] bytes = new byte[]{b};
        RequestRecordHelper.addHttpResponseByte(outputStream, bytes);
    }

    public static void recordWrite2(CoyoteOutputStream outputStream, byte[] bytes) {
        int size = bytes.length;
        byte[] newBytes = new byte[size];
        System.arraycopy(bytes, 0, newBytes, 0, size);
        RequestRecordHelper.addHttpResponseByte(outputStream, newBytes);
    }

    public static void recordWrite3(CoyoteOutputStream outputStream, final byte[] b, final int off, final int len) {
        byte[] newBytes = new byte[len];
        System.arraycopy(b, off, newBytes, 0, len);
        RequestRecordHelper.addHttpResponseByte(outputStream, newBytes);
    }

    public static void recordWrite4(CoyoteOutputStream outputStream, ByteBuffer b) {
        byte[] newBytes = new byte[b.array().length];
        System.arraycopy(b.array(), 0, newBytes, 0, b.array().length);
        RequestRecordHelper.addHttpResponseByte(outputStream, newBytes);
    }

}
