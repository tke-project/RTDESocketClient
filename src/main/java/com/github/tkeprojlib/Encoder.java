package com.github.tkeprojlib;

import java.nio.ByteBuffer;

public class Encoder {
        public static byte[] fromDouble(double value) {

        ByteBuffer buf = ByteBuffer.allocate(Double.SIZE / 8);
        return buf.putDouble(value).array();

    }

    public static byte[] fromShort(short value) {

        ByteBuffer buf = ByteBuffer.allocate(Short.SIZE / 8);
        return buf.putShort(value).array();

    }

    public static byte[] fromInt(int value) {

        ByteBuffer buf = ByteBuffer.allocate(Integer.SIZE / 8);
        return buf.putInt(value).array();

    }

    public static byte[] fromLong(long value) {

        ByteBuffer buf = ByteBuffer.allocate(Long.SIZE / 8);
        return buf.putLong(value).array();

    }


}
