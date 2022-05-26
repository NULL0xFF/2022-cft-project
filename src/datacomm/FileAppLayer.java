package datacomm;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class FileAppLayer extends BaseLayer {

    private static final int MTU = 1448;

    private final ArrayList<Boolean> ackCheckList = new ArrayList<>();
    private final ArrayList<Boolean> responseList = new ArrayList<>();
    private final HashMap<Integer, byte[]> dataByteList = new HashMap<>();
    private FileAppHeader header;
    private byte[] fragBytes = null;
    private String filePath = null;

    public FileAppLayer(String layerName) {
        super(layerName);
        resetHeader();
        ackCheckList.add(true);
    }

    private void resetHeader() {
        header = new FileAppHeader();
    }

    private byte[] createFrame(byte[] dataArray, int dataLength) {
        byte[] frame = new byte[dataLength + 12];
        System.arraycopy(header.totalLength, 0, frame, 0, 4);
        System.arraycopy(header.fragType, 0, frame, 4, 2);
        frame[6] = header.messageType;
        frame[7] = header.unused;
        System.arraycopy(header.sequenceNumber, 0, frame, 8, 4);

        // Not ACK Frame
        if (dataArray != null && dataLength > 0) System.arraycopy(dataArray, 0, frame, 12, dataLength);

        return frame;
    }

    private byte[] removeHeader(byte[] frame, int frameLength) {
        byte[] dataArray = new byte[frameLength - 12]; // Remove ChatApp Header
        System.arraycopy(frame, 12, dataArray, 0, frameLength - 12);
        return dataArray;
    }

    private byte[] integerToByte2(int value) {
        byte[] byteBuffer = new byte[2];
        byteBuffer[0] |= (byte) ((value & 0xFF00) >> 8);
        byteBuffer[1] |= (byte) (value & 0xFF);
        return byteBuffer;
    }

    private int byte2ToInteger(byte value1, byte value2) {
        return ((value1 & 0xFF) << 8) | (value2 & 0xFF);
    }

    private byte[] integerToByte4(int value) {
        byte[] byteBuffer = new byte[4];
        for (int index = 0; index < 4; index++)
            byteBuffer[index] |= (byte) ((value & (0xFF << ((3 - index) * 8))) >> ((3 - index) * 8));
        return byteBuffer;
    }

    private int byte4ToInteger(byte value1, byte value2, byte value3, byte value4) {
        return ((value1 & 0xFF) << 24) | ((value2 & 0xFF) << 16) | ((value3 & 0xFF) << 8) | (value4 & 0xFF);
    }

    /* Polling */
    private void waitACK() {
        // ACK Check
        // TODO Change Polling to Event-driven
        while (ackCheckList.size() <= 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ackCheckList.remove(0);
    }

    private boolean waitResponse() {
        // Response Check
        // TODO Change Polling to Event-driven
        while (responseList.size() <= 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return responseList.remove(0);
    }

    private void fragmentedSend(byte[] dataArray, int dataLength) {
        byte[] frame = new byte[MTU];
        int seqNum = 1;

        // First Send
        print("send data #" + String.format("%04d", seqNum));
        header.totalLength = integerToByte4(dataLength);
        header.fragType = integerToByte2(0x01);
        header.sequenceNumber = integerToByte4(seqNum);
        seqNum++;
        System.arraycopy(dataArray, 0, frame, 0, MTU);
        frame = createFrame(frame, MTU);
        getUnderLayer().send(frame, frame.length, getLayerName());

        int maxLength = dataLength / MTU;
        ((UILayer) getUpperLayer(0)).updateProgress(seqNum / maxLength * 100);

        // Next Send
        header.totalLength = integerToByte4(MTU);
        header.fragType = integerToByte2(0x02);
        for (int index = 1; index < maxLength; index++) {
            this.waitACK(); // Wait for Previous Send
            print("send data #" + String.format("%04d", seqNum));

            header.sequenceNumber = integerToByte4(seqNum);
            seqNum++;

            if ((index + 1 == maxLength) && (dataLength % MTU == 0)) header.fragType = integerToByte2(0x02);

            System.arraycopy(dataArray, MTU * index, frame, 0, MTU);
            frame = createFrame(frame, MTU);

            getUnderLayer().send(frame, frame.length, getLayerName());
            ((UILayer) getUpperLayer(0)).updateProgress((int) (((float) seqNum / (float) maxLength) * 100));
        }

        // Last Send
        header.fragType = integerToByte2(0x03);
        if (dataLength % MTU != 0) {
            print("send data #" + String.format("%04d", seqNum));
            this.waitACK();

            header.sequenceNumber = integerToByte4(seqNum);

            frame = new byte[dataLength % MTU];
            header.totalLength = integerToByte4(dataLength % MTU);

            System.arraycopy(dataArray, dataLength - (dataLength % MTU), frame, 0, dataLength % MTU);
            frame = createFrame(frame, frame.length);
            getUnderLayer().send(frame, frame.length, getLayerName());
            ((UILayer) getUpperLayer(0)).updateProgress((seqNum / maxLength) * 100);
        }
    }

    private void sendResponse(byte[] dataArray) {
        JFileChooser fileChooser = ((UILayer) getUpperLayer(0)).openReceiveDialog(dataArray);

        filePath = fileChooser == null ? null : fileChooser.getSelectedFile().getAbsolutePath();

        header.totalLength = integerToByte4(0);
        header.fragType = integerToByte2(0);
        header.messageType = (byte) (fileChooser == null ? 0x03 : 0x02);
        header.sequenceNumber = integerToByte4(0);

        byte[] frame = createFrame(null, 0);
        getUnderLayer().send(frame, frame.length, "FileApp");
    }

    private void defragmentation() {
        try {
            int fragCount = dataByteList.size();
            for (int index = 1; index <= fragCount; index++) {
                byte[] buffer = dataByteList.get(index);
                System.arraycopy(buffer, 0, fragBytes, MTU * (index - 1), index == fragCount ? buffer.length : MTU);
            }
        } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
            e.printStackTrace();
        }
//        dataByteList.forEach((K, V) -> {
//        	System.arraycopy(V, 0, fragBytes, MTU * (K - 1), K == dataByteList.size() ? V.length : MTU);
//        });

        print("defragmented");
//        printHex(fragBytes, fragBytes.length);
    }

    private void sendACK() {
        getUnderLayer().send(null, 0, "FileApp");
    }

    private void saveFile(byte[] dataArray) {
        print("saving file");
        try {
            OutputStream outputStream = new FileOutputStream(new File(filePath));
            outputStream.write(dataArray);
            outputStream.close();
            print("file saved");
        } catch (IOException e) {
            printError("unable to save file to designated path");
        }
    }

    @Override
    public boolean send(byte[] dataArray, int dataLength) {
        byte[] frame;
        header.totalLength = integerToByte4(dataLength);
        header.fragType = integerToByte2(0x00);
        header.sequenceNumber = integerToByte4(1);

        this.waitACK(); // Wait for Previous Send
        if (dataLength > MTU) {
            print("fragmented send");
            fragmentedSend(dataArray, dataLength);
        } else {
            frame = createFrame(dataArray, dataLength);
            getUnderLayer().send(frame, frame.length, getLayerName());
        }

        return true;
    }

    @Override
    public boolean send(String filePath) {
        FileInputStream fileInputStream;
        File file;
        byte[] fileName;
        byte[] fileBuffer;

        file = new File(filePath);
        fileName = file.getName().getBytes();
        fileBuffer = new byte[(int) file.length()];

        try {
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(fileBuffer);
            fileInputStream.close();
        } catch (IOException e) {
            printError("failed to read file " + file.getName());
            e.printStackTrace();
            return false;
        }

        // Send File Name
        header.messageType = 0x00;
        send(fileName, fileName.length);

        if (this.waitResponse()) { // Wait for Acceptance
            // Send File
            header.messageType = 0x01;
            getUpperLayer(0).receive("Starting file transfer".getBytes(), "FileApp");
            print("starting file transfer");
            send(fileBuffer, fileBuffer.length);
            getUpperLayer(0).receive("File transfer done!".getBytes(), "FileApp");
            print("file transfer done!");
            return true;
        }

        // File Transfer Cancelled
        getUpperLayer(0).receive("File transfer cancelled.".getBytes(), "FileApp");
        printError("file transfer cancelled");
        // TODO Manipulate UI Layer As File Trasnfer Cancelled

        return false;
    }

    @Override
    public boolean receive(byte[] frame) {
        if (frame == null) {
            ackCheckList.add(true);
            return true;
        } else {
            sendACK();
        }

        byte[] dataArray;
        byte[] buffer;
        int messageType = (byte) (frame[6] & 0xFF);
        int fragType = byte2ToInteger(frame[4], frame[5]);
        int seqNum = byte4ToInteger(frame[8], frame[9], frame[10], frame[11]);

        switch (messageType) {
            case 0x00:
                // File Name
            case 0x01:
                // File Data
                dataArray = removeHeader(frame, frame.length);
                UILayer upperLayer = (UILayer) getUpperLayer(0);
                switch (fragType) {
                    case 0x00:
                        // Unfragmented Data
                        if (messageType == 0x00) {
                            // File Name
                            sendResponse(dataArray);
                        } else {
                            // File Data
                            upperLayer.updateProgress(100);
                            saveFile(dataArray);
                            upperLayer.receive("File Received".getBytes(), "FileApp");
                            upperLayer.unlockFileUI();
                        }
                        break;
                    case 0x01:
                        // First Data
                        fragBytes = new byte[byte4ToInteger(frame[0], frame[1], frame[2], frame[3])];
                        dataByteList.clear();
                    case 0x02:
                        // Next Data
                        print("received data #" + String.format("%04d", seqNum));
                        buffer = new byte[MTU];
                        System.arraycopy(dataArray, 0, buffer, 0, MTU);
                        dataByteList.put(seqNum, buffer);
                        print(String.valueOf((int) Math.ceil(seqNum / Math.ceil(fragBytes.length / (double) MTU) * 100)));
                        print(String.valueOf(fragBytes.length));
                        if (messageType == 0x01)
                            ((UILayer) getUpperLayer(0)).updateProgress((int) Math.ceil(seqNum / Math.ceil(fragBytes.length / (double) MTU) * 100));
                        break;
                    case 0x03:
                        // Last Data
                        buffer = new byte[frame.length - 12];
                        System.arraycopy(dataArray, 0, buffer, 0, frame.length - 12);
                        dataByteList.put(seqNum, buffer);
                        defragmentation();

                        if (messageType == 0x00) {
                            // File Name
                            sendResponse(fragBytes);
                        } else {
                            // File Data
                            upperLayer.updateProgress((int) Math.ceil(seqNum / Math.ceil(fragBytes.length / (double) MTU) * 100));
                            saveFile(fragBytes);
                            upperLayer.receive("File Received".getBytes(), "FileApp");
                            upperLayer.unlockFileUI();
                        }
                        break;
                    default:
                        printError("undefined frag type");
                        return false;
                }
                break;
            case 0x02:
                // Response : Accept
            case 0x03:
                // Response : Denial
                responseList.add(messageType == 0x02);
                break;
            default:
                printError("undefined message type");
                return false;
        }
        return true;
    }

    private static class FileAppHeader {

        byte[] totalLength;
        byte[] fragType;
        byte messageType;
        byte unused;
        byte[] sequenceNumber;
        byte[] data;

        public FileAppHeader() {
            totalLength = new byte[4];
            fragType = new byte[2];
            messageType = 0x00;
            unused = 0x00;
            sequenceNumber = new byte[4];
            data = null;
        }

    }

}
