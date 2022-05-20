import java.util.ArrayList;

public class ChatAppLayer extends BaseLayer {

    private static final int MTU = 1456;

    private final ArrayList<Boolean> ackCheckList = new ArrayList<>();
    private int fragCount = 0;
    private ChatAppHeader header;
    private byte[] fragBytes;

    public ChatAppLayer(String layerName) {
        super(layerName);
        resetHeader();
        ackCheckList.add(true);
    }

    private void resetHeader() {
        header = new ChatAppHeader();
    }

    private byte[] createFrame(byte[] dataArray, int dataLength) {
        byte[] frame = new byte[dataLength + 4];
        System.arraycopy(header.totalLength, 0, frame, 0, 2);
        frame[2] = header.type;
        frame[3] = header.unused;

        // Not ACK Frame
        if (dataArray != null && dataLength > 0) System.arraycopy(dataArray, 0, frame, 4, dataLength);

        return frame;
    }

    private byte[] removeHeader(byte[] frame, int frameLength) {
        print("remove header : " + String.format("%s, %d", frame.toString(), frameLength));
        printHex(frame, frameLength);

        byte[] dataArray = new byte[frameLength - 4]; // Remove ChatApp Header
        System.arraycopy(frame, 4, dataArray, 0, frameLength - 4);

        print("return " + dataArray.toString());
        printHex(dataArray, dataArray.length);

        return dataArray;
    }

    private byte[] integerToByte2(int value) {
        print("integer to byte[2] : " + String.format("0x%08X", value));

        byte[] byteBuffer = new byte[2];
        byteBuffer[0] |= (byte) ((value & 0xFF00) >> 8);
        byteBuffer[1] |= (byte) (value & 0xFF);

        print("return " + String.format("0x%02X%02X", byteBuffer[0], byteBuffer[1]));

        return byteBuffer;
    }

    private int byte2ToInteger(byte value1, byte value2) {
        print("byte[2] to integer : " + String.format("0x%02X%02X", value1, value2));
        print("return " + String.format("0x%08X", ((value1 & 0xFF) << 8) | (value2 & 0xFF)));
        return ((value1 & 0xFF) << 8) | (value2 & 0xFF);
    }

    /* Polling */
    private void waitACK() { // ACK Check
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

    private void fragmentedSend(byte[] dataArray, int dataLength) {
        byte[] frame = new byte[MTU];

        // First Send
        header.totalLength = integerToByte2(dataLength);
        header.type = (byte) (0x01);
        System.arraycopy(dataArray, 0, frame, 0, MTU);
        frame = createFrame(frame, MTU);
        getUnderLayer().send(frame, frame.length, getLayerName());

        int maxLength = dataLength / MTU;

        // Next Send
        header.totalLength = integerToByte2(MTU);
        header.type = (byte) (0x02);
        for (int index = 1; index < maxLength; index++) {
            this.waitACK(); // Wait for Previous Send
            if ((index + 1 == maxLength) && (dataLength % MTU == 0))
                header.type = (byte) (0x03);
            System.arraycopy(dataArray, MTU * index, frame, 0, MTU);
            frame = createFrame(frame, MTU);
            getUnderLayer().send(frame, frame.length, getLayerName());
        }

        // Last Send
        header.type = (byte) (0x03);
        if (dataLength % MTU != 0) {
            this.waitACK();

            frame = new byte[dataLength % MTU];
            header.totalLength = integerToByte2(dataLength % MTU);

            System.arraycopy(dataArray, dataLength - (dataLength % MTU), frame, 0, dataLength % MTU);
            frame = createFrame(frame, frame.length);
            getUnderLayer().send(frame, frame.length, getLayerName());
        }
    }

    @Override
    public boolean send(byte[] dataArray, int dataLength) {
        print("send : " + String.format("%s, %d", dataArray.toString(), dataLength));
        printHex(dataArray, dataLength);

        byte[] frame;
        header.totalLength = integerToByte2(dataLength);
        header.type = (byte) (0x00);

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
    public synchronized boolean receive(byte[] frame) {
        if (frame == null) {
            print("receive : ACK");

            ackCheckList.add(true);
            return true;
        }

        print("receive : " + frame.toString());
        printHex(frame, frame.length);

        byte[] dataArray;
        int dataType = (byte) (frame[2] & 0xFF);

        switch (dataType) {
            case 0x00:
                print("un-fragmented data");

                dataArray = removeHeader(frame, frame.length);
                getUpperLayer(0).receive(dataArray, "ChatApp");
                break;
            case 0x01:
                print("fragmented first data");

                fragBytes = new byte[byte2ToInteger(frame[0], frame[1])];
                fragCount = 1;
                dataArray = removeHeader(frame, frame.length);
                System.arraycopy(dataArray, 0, fragBytes, 0, MTU);
                break;
            case 0x02:
                print("fragmented next data");

                dataArray = removeHeader(frame, frame.length);
                System.arraycopy(dataArray, 0, fragBytes, fragCount * MTU, MTU);
                fragCount++;
                break;
            case 0x03:
                print("fragmented last data");

                dataArray = removeHeader(frame, frame.length);
                System.arraycopy(dataArray, 0, fragBytes, fragCount * MTU, byte2ToInteger(frame[0], frame[1]));
                fragCount++;
                getUpperLayer(0).receive(fragBytes, "ChatApp");
                break;
            default:
                printError("undefined type");
                return false;
        }

        getUnderLayer().send(null, 0, "ChatApp");
        return true;
    }

    private static class ChatAppHeader {

        byte[] totalLength;
        byte type;
        byte unused;
        byte[] data;

        public ChatAppHeader() {
            this.totalLength = new byte[2];
            this.type = 0x00;
            this.unused = 0x00;
            this.data = null;
        }

    }

}
