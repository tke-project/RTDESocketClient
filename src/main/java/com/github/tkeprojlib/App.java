package com.github.tkeprojlib;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        //IP Address
        //Frequency for receiving
        //Header string
        RTDESocketClient client= new RTDESocketClient("192.168.83.129", 20, "myApp") {

            @Override
            public void onReceive(Object[] values) throws IOException {
                double[] pos = (double[])values[0];

                System.out.println(pos[0]*180/Math.PI);
            }

            @Override
            public Object[] onSend() throws IOException {
                return null;
            }
            
        };

        client.addOutput(RTDEOutput.actual_q);
        client.start();
    }
}
