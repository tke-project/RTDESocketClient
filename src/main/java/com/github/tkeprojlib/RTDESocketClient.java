package com.github.tkeprojlib;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

public abstract class RTDESocketClient extends Thread {

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private String ipAddress;
    private RTDECommand cmd;

    public boolean isConnected = false;
    public boolean isRunning = true;

    private String outputKey = null;
    private String inputKey = null;
    private int freq;
    private byte inputRecipe;
    private byte outputRecipe;

    private LinkedHashMap<String, String> inputKeyTypeMap = new LinkedHashMap<String, String>();
    private LinkedHashMap<String, String> outputKeyTypeMap = new LinkedHashMap<String, String>();

    private int PORT = 30004;
    private String MESSAGE_HEADER = "Client to port30004:";

    public abstract void onReceive(Object[] values) throws IOException;

    public abstract Object[] onSend() throws IOException;

    public RTDESocketClient(String ipAddress, int frequency, String header) {

        this.ipAddress = ipAddress;
        this.freq = frequency;
        this.MESSAGE_HEADER += "(" + header + ")";
    }

    public void run() {

        while (isRunning) {

            try {

                Thread.sleep(1);

                socket = new Socket(ipAddress, PORT);
                socket.setSoTimeout(10000);
                inputStream = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream());

                System.out.println(MESSAGE_HEADER + "RTDE Client is connected");
                isConnected = true;

                if (!isProtocolVersion2()) {
                    throw new IOException("RTDE Protocol version is not ver.2");
                }

                if (outputKey != null)
                    outputRecipe = setupOutputs(freq);

                if (inputKey != null)
                    inputRecipe = setupInputs();

                if (!startReceiving()) {
                    throw new IOException("RTDE Starting is Failed");
                }

                System.out.println(MESSAGE_HEADER + "RTDE Data receiving is started");

                while (isRunning && isConnected) {

                    Thread.sleep(1);

                    if (outputKey != null)
                        recieveData();

                    if (inputKey != null)
                        sendData();

                }

            } catch (InterruptedException e) {
                isRunning = false;
                System.out.println(MESSAGE_HEADER + "Thread is interrpted");

            } catch (SocketException e) {
                isConnected = false;
                System.out.println(MESSAGE_HEADER + e.getMessage());

            } catch (IOException e) {
                isConnected = false;
                System.out.println(MESSAGE_HEADER + e.getMessage());
                System.out.println(MESSAGE_HEADER + "Socket is closed");
            } finally {

                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                    }
                }

            }

        }

        System.out.println(MESSAGE_HEADER + "Thread is finished");

    }

    private boolean isProtocolVersion2() throws IOException {
        cmd = RTDECommand.RTDE_REQUEST_PROTOCOL_VERSION;

        byte version = 2;
        byte[] payload = { 0, version };

        sendCommand(cmd, payload);
        byte[] inBytes = recieveBytes();

        if (inBytes != null) {
            return inBytes[3] == (byte) 1 ? true : false;
        } else
            return false;
    }

    /*
    private String getControlVersion() throws IOException {
        cmd = RTDECommand.RTDE_GET_URCONTROL_VERSION;

        sendCommand(cmd, null);

        byte[] inBytes = recieveBytes();

        byte[] buf = new byte[Integer.BYTES * 4];
        System.arraycopy(inBytes, 3, buf, 0, buf.length);

        int[] datas = Decorder.toInts(buf);

        return String.valueOf(datas[0]) + "." + String.valueOf(datas[1]) + "." + String.valueOf(datas[2]) + "."
                + String.valueOf(datas[3]);

    }
    */

    private byte setupOutputs(int freq) throws IOException {
        cmd = RTDECommand.RTDE_CONTROL_PACKAGE_SETUP_OUTPUTS;

        byte[] hd_freq = Encoder.fromDouble((double) freq);
        byte[] hd_key = outputKey.getBytes();
        byte[] payload = new byte[hd_freq.length + hd_key.length];

        System.arraycopy(hd_freq, 0, payload, 0, hd_freq.length);
        System.arraycopy(hd_key, 0, payload, hd_freq.length, hd_key.length);

        sendCommand(cmd, payload);

        byte[] inBytes = recieveBytes();

        if (inBytes == null)
            throw new IOException("Setup output error");

        byte[] typesBytes = new byte[inBytes.length - 4];
        System.arraycopy(inBytes, 4, typesBytes, 0, typesBytes.length);

        String[] outkeyArray = outputKey.split(",");
        String[] types = new String(typesBytes).split(",");

        outputKeyTypeMap.clear();
        for (int i = 0; i < outkeyArray.length; i++) {
            outputKeyTypeMap.put(outkeyArray[i], types[i]);
        }

        return inBytes[3];
    }

    private byte setupInputs() throws IOException {
        cmd = RTDECommand.RTDE_CONTROL_PACKAGE_SETUP_INPUTS;

        byte[] header_string = inputKey.getBytes();

        byte[] payload = new byte[header_string.length];
        System.arraycopy(header_string, 0, payload, 0, header_string.length);

        sendCommand(cmd, payload);

        byte[] inBytes = recieveBytes();

        if (inBytes == null)
            throw new IOException("Setup input error");

        byte[] typesBytes = new byte[inBytes.length - 4];
        System.arraycopy(inBytes, 4, typesBytes, 0, typesBytes.length);

        String bufstr = new String(typesBytes);

        String[] inkeyArray = inputKey.split(",");
        String[] types = bufstr.split(",");

        inputKeyTypeMap.clear();
        for (int i = 0; i < inkeyArray.length; i++) {
            inputKeyTypeMap.put(inkeyArray[i], types[i]);
        }

        return inBytes[3];
    }

    private boolean startReceiving() throws IOException {
        cmd = RTDECommand.RTDE_CONTROL_PACKAGE_START;

        sendCommand(cmd, null);

        byte[] inBytes = recieveBytes();

        if (inBytes != null)
            return inBytes[3] == (byte) 1 ? true : false;
        else
            return false;
    }

    private byte[] sendCommand(RTDECommand cmd, byte[] payload) throws IOException {
        int bufL = (payload != null ? payload.length : 0) + 3;

        // パッケージサイズを配列に格納
        ByteBuffer buffer = ByteBuffer.allocate(2);
        byte[] sizeinfo = buffer.putShort((short) bufL).array();

        byte[] outBytes = new byte[bufL];

        outBytes[0] = sizeinfo[0];
        outBytes[1] = sizeinfo[1];
        outBytes[2] = (byte) cmd.getType();

        if (payload != null)
            System.arraycopy(payload, 0, outBytes, 3, payload.length);

        outputStream.write(outBytes, 0, outBytes.length);
        outputStream.flush();

        return outBytes;

    }

    private byte[] recieveBytes() throws IOException {
        byte[] inBytes = new byte[1024];
        int size = 0;
        size = inputStream.read(inBytes);
        if (size > -1) {
            byte[] result = new byte[size];
            System.arraycopy(inBytes, 0, result, 0, size);
            return result;
        } else
            throw new IOException("Data recieve failed");
    }

    private void recieveData() throws IOException {
        byte[] inBytes = recieveBytes();

        if (inBytes[2] == RTDECommand.RTDE_DATA_PACKAGE.getType()) {

            byte[] databuf = new byte[inBytes.length - 4];
            System.arraycopy(inBytes, 4, databuf, 0, inBytes.length - 4);

            Object[] values = getValues(databuf);

            onReceive(values);

        } else if (inBytes[2] == RTDECommand.RTDE_TEXT_MESSAGE.getType()) {

            byte[] databuf = new byte[inBytes.length - 3];

            int size_m = (int) inBytes[3];
            int size_s = (int) inBytes[3 + size_m + 1];

            byte[] buf_m = new byte[size_m];
            byte[] buf_s = new byte[size_s];

            System.arraycopy(databuf, 4, buf_m, 0, size_m);
            System.arraycopy(databuf, 4 + size_m + 1, buf_s, 0, size_s);

            byte wlevel = databuf[databuf.length - 1];

            String message = new String(buf_m);
            String source = new String(buf_s);

            throw new IOException(message + "/" + source + "/Level:" + String.valueOf(wlevel));
        } else
            throw new IOException("Recieve Error");
    }

    private void sendData() throws IOException {

        Object[] values = onSend();

        byte[] buf = setBuffer(values);
        byte[] payload = new byte[buf.length + 1];

        payload[0] = inputRecipe;

        System.arraycopy(buf, 0, payload, 1, buf.length);

        sendCommand(RTDECommand.RTDE_DATA_PACKAGE, payload);
    }

    private Object[] getValues(byte[] databuf) {
        ByteBuffer buf = ByteBuffer.wrap(databuf);
        ArrayList<Object> values = new ArrayList<Object>();

        for (Iterator<Entry<String, String>> iterator = outputKeyTypeMap.entrySet().iterator(); iterator.hasNext();) {

            Object value = RTDEBuffer.decode(iterator.next(), buf);
            values.add(value);
        }

        return values.toArray(new Object[] {});
    }

    private byte[] setBuffer(Object[] values) {
        ArrayList<Byte> bytelist = new ArrayList<Byte>();
        int k = 0;

        for (Iterator<Entry<String, String>> iterator = inputKeyTypeMap.entrySet().iterator(); iterator.hasNext();) {
            Entry<String, String> entry = iterator.next();

            byte[] _buf = RTDEBuffer.encode(entry, values[k]);

            for (int i = 0; i < _buf.length; i++) {

                if (_buf != null)
                    bytelist.add(_buf[i]);
            }

            k++;
        }
        byte[] buf = new byte[bytelist.size()];
        for (int i = 0; i < bytelist.size(); i++) {
            buf[i] = bytelist.toArray(new Byte[] {})[i];
        }

        return buf;
    }

    public void addInput(RTDEInput input) {

        if (inputKey == null)
            inputKey = "";

        StringBuilder sb = new StringBuilder(inputKey);

        if (inputKey.equals(""))
            sb.append(input.getType());
        else {
            sb.append(",");
            sb.append(input.getType());

        }
        inputKey = sb.toString();

    }

    public void addOutput(RTDEOutput ouput) {

        if (outputKey == null)
            outputKey = "";

        StringBuilder sb = new StringBuilder(outputKey);

        if (outputKey.equals(""))
            sb.append(ouput.getType());
        else {
            sb.append(",");
            sb.append(ouput.getType());

        }

        outputKey = sb.toString();
    }
}