import java.util.ArrayList;

public class ChatAppLayer extends BaseLayer {

    private ChatAppHeader header;

    private byte[] fragBytes;
    private int fragCount = 0;
    private final ArrayList<Boolean> ackCheckList = new ArrayList<Boolean>();

    public ChatAppLayer(String name) {
        super(name);
        resetHeader();
        ackCheckList.add(true);
    }

    public void resetHeader() {
        header = new ChatAppHeader();
    }

    public byte[] objectToByte(ChatAppHeader header, byte[] dataArray, int arrayLength) {
        byte[] byteBuffer = new byte[arrayLength + 4];

        byteBuffer[0] = header.totalLength[0];
        byteBuffer[1] = header.totalLength[1];
        byteBuffer[2] = header.type;
        byteBuffer[3] = header.unused;

        if (arrayLength >= 0)
            System.arraycopy(dataArray, 0, byteBuffer, 4, arrayLength);

        return byteBuffer;
    }

    public byte[] removeHeader(byte[] dataArray, int arrayLength) {
        byte[] byteBuffer = new byte[arrayLength - 4];
        System.arraycopy(dataArray, 4, byteBuffer, 0, arrayLength - 4);
        return byteBuffer;
    }

    /* Polling */
    private void waitACK() { // ACK 泥댄겕
        while (ackCheckList.size() <= 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ackCheckList.remove(0);
    }

    private void fragSend(byte[] dataArray, int arrayLength) {
        byte[] byteBuffer = new byte[1500];
        int i = 0;
        header.totalLength = integerToByte2(arrayLength);
        header.type = (byte) (0x01);

        // First Send (type 0x01)
        //1500바이트 단위로 나뉨
        System.arraycopy(dataArray, 0, byteBuffer, 0, 1500);
        byteBuffer = objectToByte(header, byteBuffer, 1500);
        this.getUnderLayer().send(byteBuffer, byteBuffer.length);

        int maxLength = arrayLength / 1500;

        // Second Send (type 0x02)
        header.totalLength = integerToByte2(1500); // Every 1500 bytes
        header.type = (byte) (0x02); // Type 0x02
        for (i = 1; i < maxLength; i++) {
            this.waitACK(); // Wait for previous send ACK
            // Check if this iteration is final iteration
            if ((i + 1 == maxLength) && (arrayLength % 1500 == 0))
                header.type = (byte) (0x03); // This iteration is final so set type to 0x03
            System.arraycopy(dataArray, 1500 * i, byteBuffer, 0, 15000);
            byteBuffer = objectToByte(header, byteBuffer, 1500);
            this.getUnderLayer().send(byteBuffer, byteBuffer.length);
        }

        // Final Send (type 0x03)
        header.type = (byte) (0x03);
        // Leftover data which is not 10-byte-long
        if (arrayLength % 1500 != 0) {
            this.waitACK(); // Wait for previous send ACK

            byteBuffer = new byte[arrayLength % 1500]; // New byte object for leftover data
            header.totalLength = integerToByte2(arrayLength % 1500); // Set total length of leftover data to Frame Header

            System.arraycopy(dataArray, arrayLength - (arrayLength % 1500), byteBuffer, 0, arrayLength % 1500);
            byteBuffer = objectToByte(header, byteBuffer, byteBuffer.length);
            this.getUnderLayer().send(byteBuffer, byteBuffer.length);
        }
    }

    public boolean send(byte[] input, int length) {
        byte[] byteBuffer;
        header.totalLength = integerToByte2(length);
        header.type = (byte) (0x00); // Flag for unfragmented data

        this.waitACK(); // Wait for ACK
        if (length > 1500)
            this.fragSend(input, length); // Send fragmented data
        else {
            byteBuffer = this.objectToByte(header, input, input.length); // Send data
            this.getUnderLayer().send(byteBuffer, byteBuffer.length);
        }

        return true;
    }

    public synchronized boolean receive(byte[] input) {
        byte[] byteBuffer;
        int dataType = 0;

        if (input == null) { // ACK
            ackCheckList.add(true);
            return true;
        }

        dataType |= (byte) (input[2] & 0xFF);

        if (dataType == 0x00) {
            // Unfragmented Data Type
            byteBuffer = this.removeHeader(input, input.length);
            this.getUpperLayer(0).receive(byteBuffer);
        } else {
            // Fragmented Data Type
            if (dataType == 0x01) {
                // First Receive (type 0x01)
                int length = this.byte2ToInteger(input[0], input[1]); // First fragment has total length
                this.fragBytes = new byte[length];
                this.fragCount = 1;
                byteBuffer = this.removeHeader(input, input.length);
                System.arraycopy(byteBuffer, 0, this.fragBytes, 0, 10);
            } else if (dataType == 0x02) {
                // Next Receive (type 0x02)
                byteBuffer = this.removeHeader(input, input.length);
                System.arraycopy(byteBuffer, 0, this.fragBytes, this.fragCount * 1500, 1500);
                this.fragCount++;
            } else if (dataType == 0x03) {
                // Final Receive (type 0x03)
                byteBuffer = this.removeHeader(input, input.length);
                System.arraycopy(byteBuffer, 0, this.fragBytes, this.fragCount * 1500,
                        this.byte2ToInteger(input[0], input[1]));
                this.fragCount++;
                // Send Combined Data to Upper Layer
                this.getUpperLayer(0).receive(this.fragBytes);
            } else {
                throw new RuntimeException("unknown data type");
            }

        }
        this.getUnderLayer().send(null, 0); // Send ACK back
        return true;
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

    private class ChatAppHeader {
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