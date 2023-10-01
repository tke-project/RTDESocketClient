package com.github.tkeprojlib;

public class BitConverter {
    public static boolean[] fromInt(int value) {
        boolean[] result = new boolean[Integer.SIZE];

        for (int i = 0; i < Integer.SIZE; i++) {
            result[i] = ((value & 1) == 1) ? true : false;
            value = value >> 1;
        }

        return result;
    }

}
