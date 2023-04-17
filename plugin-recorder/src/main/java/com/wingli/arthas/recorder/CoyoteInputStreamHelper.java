package com.wingli.arthas.recorder;

import org.apache.catalina.connector.CoyoteInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class CoyoteInputStreamHelper {

    private static final Logger logger = LoggerFactory.getLogger(RequestRecordHelper.class);

    /**
     * 记录读数据 （插桩接口）
     */
    public static void recordRead(Object inputStreamObj, Object[] args, Object returnObj) {
        try {
            CoyoteInputStream inputStream = (CoyoteInputStream) inputStreamObj;
            if (!RequestRecordHelper.needToRecord(inputStream)) {
                return;
            }
            int size = (Integer) returnObj;
            if (args.length == 0) {
                recordRead1(inputStream, size);
            } else if (args.length == 1 && args[0] instanceof byte[]) {
                recordRead2(inputStream, size, (byte[]) args[0]);
            } else if (args.length == 3 && args[0] instanceof byte[] && args[1] instanceof Integer && args[2] instanceof Integer) {
                recordRead3(inputStream, size, (byte[]) args[0], (Integer) args[1], (Integer) args[2]);
            } else if (args.length == 1 && args[0] instanceof ByteBuffer) {
                recordRead4(inputStream, size, (ByteBuffer) args[0]);
            }
        } catch (Throwable t) {
            logger.error("recordRead err.", t);
        }
    }

    /**
     * 记录读数据 （插桩接口）
     */
    public static void recordReadLine(Object inputStreamObj, Object[] args, Object returnObj) {
        try {
            CoyoteInputStream inputStream = (CoyoteInputStream) inputStreamObj;
            if (!RequestRecordHelper.needToRecord(inputStream)) {
                return;
            }
            int size = (Integer) returnObj;
            if (args.length == 3 && args[0] instanceof byte[] && args[1] instanceof Integer && args[2] instanceof Integer) {
                recordRead5(inputStream, size, (byte[]) args[0], (Integer) args[1], (Integer) args[2]);
            }
        } catch (Throwable t) {
            logger.error("recordReadLine err.", t);
        }
    }

    public static void recordRead1(CoyoteInputStream inputStream, int byteInt) {
        if (byteInt >= 0) {
            byte b = (byte) (byteInt & 0xff);
            byte[] bytes = new byte[]{b};
            RequestRecordHelper.addHttpRequestByte(inputStream, bytes);
        }

    }

    public static void recordRead2(CoyoteInputStream inputStream, int size, byte[] bytes) {
        if (size >= 0) {
            byte[] newBytes = new byte[size];
            System.arraycopy(bytes, 0, newBytes, 0, size);
            RequestRecordHelper.addHttpRequestByte(inputStream, newBytes);
        }
    }

    public static void recordRead3(CoyoteInputStream inputStream, int size, final byte[] b, final int off, final int len) {
        if (size >= 0) {
            byte[] newBytes = new byte[size];
            System.arraycopy(b, off, newBytes, 0, size);
            RequestRecordHelper.addHttpRequestByte(inputStream, newBytes);
        }
    }

    public static void recordRead4(CoyoteInputStream inputStream, int size, ByteBuffer b) {
        if (size >= 0) {
            byte[] newBytes = new byte[size];
            System.arraycopy(b.array(), 0, newBytes, 0, size);
            RequestRecordHelper.addHttpRequestByte(inputStream, newBytes);
        }
    }

    public static void recordRead5(CoyoteInputStream inputStream, int size, byte[] b, final int off, final int len) {
        if (size >= 0) {
            byte[] newBytes = new byte[len];
            System.arraycopy(b, off, newBytes, 0, len);
            RequestRecordHelper.addHttpRequestByte(inputStream, newBytes);
        }
    }

}
