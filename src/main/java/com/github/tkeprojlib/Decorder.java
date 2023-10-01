package com.github.tkeprojlib;

import java.nio.ByteBuffer;

public class Decorder {
        public static double[] toDoubles(byte[] bytes) {

        ByteBuffer buf = ByteBuffer.wrap(bytes);
        double[] values = new double[bytes.length / (Double.SIZE / 8)];

        for (int i = 0; i < values.length; i++) {
            values[i] = buf.getDouble();
        }
        return values;
    }

    public static int[] toInts(byte[] bytes) {

        ByteBuffer buf = ByteBuffer.wrap(bytes);
        int[] values = new int[bytes.length / (Integer.SIZE / 8)];

        for (int i = 0; i < values.length; i++) {
            values[i] = buf.getInt();
        }
        return values;
    }

    public static short[] toShorts(byte[] bytes) {

        ByteBuffer buf = ByteBuffer.wrap(bytes);
        short[] values = new short[bytes.length / (Short.SIZE / 8)];

        for (int i = 0; i < values.length; i++) {
            values[i] = buf.getShort();
        }
        return values;
    }


}
