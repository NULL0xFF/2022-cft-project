import java.util.ArrayList;

public class ChatAppLayer extends BaseLayer {

    private static final int MAXIMUM_DATA_LENGTH = 1456;

    private final ArrayList<Boolean> ackCheckList = new ArrayList<>();
    private ChatAppHeader header;
    private byte[] fragBytes;
    private int fragCount = 0;

    public ChatAppLayer(String name) {
        super(name);
        resetHeader();
        ackCheckList.add(true);
    }

    private void resetHeader() {
        header = new ChatAppHeader();
    }

    private byte[] createFrame(byte[] dataArray, int arrayLength) {
        byte[] byteBuffer = new byte[arrayLength + 4]; // 4-bytes-long Ethernet header

        byteBuffer[0] = header.totalLength[0];
        byteBuffer[1] = header.totalLength[1];
        byteBuffer[2] = header.type;
        byteBuffer[3] = header.unused;

        // Not ACK
        if (arrayLength >= 0) System.arraycopy(dataArray, 0, byteBuffer, 4, arrayLength);

        return byteBuffer;
    }

    private byte[] removeHeader(byte[] dataArray, int arrayLength) {
        byte[] byteBuffer = new byte[arrayLength - 4];
        System.arraycopy(dataArray, 4, byteBuffer, 0, arrayLength - 4);
        return byteBuffer;
    }

    private byte[] integerToByte2(int value) {
        byte[] byteBuffer = new byte[2];
        byteBuffer[0] |= (byte) ((value & 0xFF00) >> 8);
        byteBuffer[1] |= (byte) (value & 0xFF);

        return byteBuffer;
    }

    private int byte2ToInteger(byte value1, byte value2) {
        return (value1 << 8) | (value2);
    }

    /* Polling */
    private void waitACK() { // ACK Check
        while (ackCheckList.size() <= 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ackCheckList.remove(0);
    }

    private void fragmentedSend(byte[] dataArray, int arrayLength) {
        byte[] byteBuffer = new byte[MAXIMUM_DATA_LENGTH];

        header.totalLength = integerToByte2(arrayLength);
        header.type = (byte) (0x01);

        // First Send (type 0x01)
        System.arraycopy(dataArray, 0, byteBuffer, 0, MAXIMUM_DATA_LENGTH);
        byteBuffer = createFrame(byteBuffer, MAXIMUM_DATA_LENGTH);
        this.getUnderLayer().send(byteBuffer, byteBuffer.length, getLayerName());

        int maxLength = arrayLength / MAXIMUM_DATA_LENGTH;

        // Second Send (type 0x02)
        header.totalLength = integerToByte2(MAXIMUM_DATA_LENGTH); // Every maximum data bytes
        header.type = (byte) (0x02); // Type 0x02
        for (int index = 1; index < maxLength; index++) {
            this.waitACK(); // Wait for previous send ACK
            // Check if this iteration is final iteration
            if ((index + 1 == maxLength) && (arrayLength % MAXIMUM_DATA_LENGTH == 0))
                header.type = (byte) (0x03); // This iteration is final so set type to 0x03
            System.arraycopy(dataArray, MAXIMUM_DATA_LENGTH * index, byteBuffer, 0, MAXIMUM_DATA_LENGTH);
            byteBuffer = createFrame(byteBuffer, MAXIMUM_DATA_LENGTH);
            this.getUnderLayer().send(byteBuffer, byteBuffer.length, getLayerName());
        }

        // Final Send (type 0x03)
        header.type = (byte) (0x03);
        // Leftover data which is not maximum-data-byte-long
        if (arrayLength % MAXIMUM_DATA_LENGTH != 0) {
            this.waitACK(); // Wait for previous send ACK

            byteBuffer = new byte[arrayLength % MAXIMUM_DATA_LENGTH]; // New byte object for leftover data
            header.totalLength = integerToByte2(arrayLength % MAXIMUM_DATA_LENGTH); // Set total length of leftover data to Frame Header

            System.arraycopy(dataArray, arrayLength - (arrayLength % MAXIMUM_DATA_LENGTH), byteBuffer, 0, arrayLength % MAXIMUM_DATA_LENGTH);
            byteBuffer = createFrame(byteBuffer, byteBuffer.length);
            this.getUnderLayer().send(byteBuffer, byteBuffer.length, getLayerName());
        }
    }

    @Override
    public boolean send(byte[] dataArray, int arrayLength) {
        byte[] byteBuffer;
        header.totalLength = integerToByte2(arrayLength);
        header.type = (byte) (0x00);

        this.waitACK(); // Wait for ACK
        if (arrayLength > MAXIMUM_DATA_LENGTH) {
            this.fragmentedSend(dataArray, arrayLength);
        } else {
            byteBuffer = createFrame(dataArray, arrayLength);
            getUnderLayer().send(byteBuffer, byteBuffer.length, getLayerName());
        }

        return true;
    }

    @Override
    public synchronized boolean receive(byte[] dataArray) {
        byte[] byteBuffer;
        int dataType = 0;

        if (dataArray == null) { // ACK
            ackCheckList.add(true);
            return true;
        }

        dataType |= (byte) (dataArray[2] & 0xFF);

        if (dataType == 0x00) {
            // Unfragmented Data Type
            byteBuffer = this.removeHeader(dataArray, dataArray.length);
            this.getUpperLayer(0).receive(byteBuffer);
        } else {
            // Fragmented Data Type
            if (dataType == 0x01) {
                // First Receive (type 0x01)
                int length = this.byte2ToInteger(dataArray[0], dataArray[1]); // First fragment has total length
                this.fragBytes = new byte[length];
                this.fragCount = 1;
                byteBuffer = this.removeHeader(dataArray, dataArray.length);
                System.arraycopy(byteBuffer, 0, this.fragBytes, 0, MAXIMUM_DATA_LENGTH);
            } else if (dataType == 0x02) {
                // Next Receive (type 0x02)
                byteBuffer = this.removeHeader(dataArray, dataArray.length);
                System.arraycopy(byteBuffer, 0, this.fragBytes, this.fragCount * MAXIMUM_DATA_LENGTH, MAXIMUM_DATA_LENGTH);
                this.fragCount++;
            } else if (dataType == 0x03) {
                // Final Receive (type 0x03)
                byteBuffer = this.removeHeader(dataArray, dataArray.length);
                System.arraycopy(byteBuffer, 0, this.fragBytes, this.fragCount * MAXIMUM_DATA_LENGTH, this.byte2ToInteger(dataArray[0], dataArray[1]));
                this.fragCount++;
                // Send Combined Data to Upper Layer
                this.getUpperLayer(0).receive(this.fragBytes);
            } else {
                throw new RuntimeException("unknown data type");
            }

        }
        this.getUnderLayer().send(null, 0, "ChatApp"); // Send ACK back
        return true;
    }

    private static class ChatAppHeader {
        byte[] totalLength;
        byte type;
        byte unused;
        @SuppressWarnings("unused")
        byte[] data;

        public ChatAppHeader() {
            this.totalLength = new byte[2];
            this.type = 0x00;
            this.unused = 0x00;
            this.data = null;
        }
    }

}
