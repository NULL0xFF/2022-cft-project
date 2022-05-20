public class EthernetLayer extends BaseLayer {

    private EthernetHeader header;

    public EthernetLayer(String name) {
        super(name);
        resetHeader();
    }

    private static void print(String str) {
        System.out.println("[EthernetLayer] " + str);
    }

    private static void printError(String errStr) {
        System.err.println("[EthernetLayer] " + errStr);
    }

    private void resetHeader() {
        header = new EthernetHeader();
    }

    private byte[] createFrame(byte[] dataArray, int arrayLength) {
        byte[] byteBuffer = new byte[arrayLength + 14]; // 14-bytes-long Ethernet header
        for (int index = 0; index < 6; index++) {
            byteBuffer[index] = header.dst.addr[index];
            byteBuffer[index + 6] = header.src.addr[index];
        }
        byteBuffer[12] = header.type[0];
        byteBuffer[13] = header.type[1];

        System.arraycopy(dataArray, 0, byteBuffer, 14, arrayLength);
        return byteBuffer;
    }

    private byte[] removeHeader(byte[] dataArray, int arrayLength) {
        byte[] byteBuffer = new byte[arrayLength - 14]; // 14-bytes-long Ethernet Frame
        System.arraycopy(dataArray, 14, byteBuffer, 0, arrayLength - 14);
        return byteBuffer;
    }

    private byte[] integerToByte2(int value) {
        byte[] byteBuffer = new byte[2];
        byteBuffer[0] |= (byte) ((value & 0xFF00) >> 8);
        byteBuffer[1] |= (byte) (value & 0xFF);

        return byteBuffer;
    }

    private int byte2ToInteger(byte value1, byte value2) {
        print("byte[2] to integer -> " + String.format("value1 : 0x%02X, value2 : 0x%02X", value1, value2));
        int v1 = value1 & 0xFF;
        int v2 = value2 & 0xFF;
        return v1 << 8 | v2;
    }

    public void setSourceAddress(byte[] newSrcAddr) {
        header.src.addr = newSrcAddr;
    }

    public void setDestinationAddress(byte[] newDstAddress) {
        header.dst.addr = newDstAddress;
    }


    @Override
    public boolean send(byte[] dataArray, int arrayLength, String layerName) {
        print("layer name : " + layerName);
        if (layerName == null) {
            print("layer name is null");
            if (dataArray == null && arrayLength == 0)
                // Default ACK
                header.type = integerToByte2(0x2001);
            else
                // Default
                header.type = integerToByte2(0x2000);
        } else switch (layerName) {
            case "ChatApp":
                print("layer is ChatApp");
                if (dataArray == null && arrayLength == 0)
                    // ChatApp ACK
                    header.type = integerToByte2(0x2081);
                else
                    // ChatApp
                    header.type = integerToByte2(0x2080);
                break;
            case "FileApp":
                print("layer is FileApp");
                if (dataArray == null && arrayLength == 0)
                    // FileApp ACK
                    header.type = integerToByte2(0x2091);
                else
                    // FileApp
                    header.type = integerToByte2(0x2090);
                break;
            default:
                throw new RuntimeException("unsupported layer " + layerName);
        }

        print("creating frame");
        byte[] byteFrame = createFrame(dataArray, arrayLength);
        getUnderLayer().send(byteFrame, byteFrame.length);
        print("type " + String.format("0x%02X%02X", header.type[0], header.type[1]) + " sent");

        return true;
    }

    @Override
    public synchronized boolean receive(byte[] dataArray) {
        byte[] data;
        int dataType = byte2ToInteger(dataArray[12], dataArray[13]);
        print("data with type " + String.format("0x%04X", dataType) + " received");

        if (/* !this.isMyPacket(dataArray) && */ !this.isBroadcast(dataArray) && this.isMine(dataArray)) {
            data = removeHeader(dataArray, dataArray.length);
            switch (dataType) {
                case 0x2000:
                    // Default
                    break;
                case 0x2001:
                    // Default ACK
                    break;
                case 0x2080:
                    // ChatApp
                    print("ChatApp normal data");
                    getUpperLayer("ChatApp").receive(data);
//                    send(null, 0, "ChatApp"); // ACK
                    break;
                case 0x2081:
                    // ChatApp ACK
                    print("ChatApp ACK");
                    getUpperLayer("ChatApp").receive(null);
//                    send(null, 0, "ChatApp"); // ACK
                    break;
                case 0x2090:
                    // FileApp
                    getUpperLayer("FileApp").receive(data);
                    send(null, 0, "FileApp"); // ACK
                    break;
                case 0x2091:
                    // FileApp ACK
                    getUpperLayer("FileApp").receive(null);
                    send(null, 0, "FileApp"); // ACK
                    break;
                default:
                    printError(String.format("unsupported type 0x%04X", dataType));
            }
            return true;
        }
        return false;
    }

    /**
     * Check if the Ethernet Frame is Broadcast Frame
     */
    private boolean isBroadcast(byte[] dataArray) {
        for (int i = 0; i < 6; i++)
            if (dataArray[i] != (byte) 0xff) return false;
        return (dataArray[12] == (byte) 0xff && dataArray[13] == (byte) 0xff);
    }

    /**
     * Check if the Ethernet Frame is My Packet
     */
    private boolean isMyPacket(byte[] dataArray) {
        for (int i = 0; i < 6; i++)
            if (header.src.addr[i] != dataArray[6 + i]) return false;
        return true;
    }

    /**
     * Check if the Ethernet Frame is for me
     */
    private boolean isMine(byte[] dataArray) {
        byte[] byteBuffer = header.src.addr;
        for (int i = 0; i < 6; i++)
            if (byteBuffer[i] != dataArray[i]) return false;
        return true;
    }

    /**
     * Ethernet Frame Sub-Class
     */
    private static class EthernetHeader {
        /**
         * Destination MAC Address
         */
        EthernetAddress dst;
        /**
         * Source MAC Address
         */
        EthernetAddress src;
        /**
         * Ethernet Frame Type
         */
        byte[] type; // Type
        /**
         * Ethernet Frame Data
         */
        @SuppressWarnings("unused")
        byte[] data;

        /**
         * Ethernet Frame Sub-Class Constructor
         */
        public EthernetHeader() {
            this.dst = new EthernetAddress();
            this.src = new EthernetAddress();
            this.type = new byte[2];
            this.data = null;
        }

        /**
         * Ethernet Address Sub-Class
         */
        private static class EthernetAddress {
            /**
             * Ethernet MAC Address
             */
            byte[] addr = new byte[6];

            /**
             * Ethernet Address Sub-Class Constructor
             */
            public EthernetAddress() {
                this.addr[0] = (byte) 0x00;
                this.addr[1] = (byte) 0x00;
                this.addr[2] = (byte) 0x00;
                this.addr[3] = (byte) 0x00;
                this.addr[4] = (byte) 0x00;
                this.addr[5] = (byte) 0x00;
            }
        }
    }
}
