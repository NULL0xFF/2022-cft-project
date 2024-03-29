package datacomm;

public class EthernetLayer extends BaseLayer {

    private static final int MTU = 1500;

    private EthernetHeader header;

    public EthernetLayer(String layerName) {
        super(layerName);
        resetHeader();
    }

    private void resetHeader() {
        header = new EthernetHeader();
    }

    private byte[] createFrame(byte[] dataArray, int dataLength) {
        byte[] frame = (dataLength + 40 < 46) ? new byte[60] : new byte[dataLength + 54]; // Minimum Packet Size w/ IP & TCP Header
        System.arraycopy(header.dst.addr, 0, frame, 0, 6);
        System.arraycopy(header.src.addr, 0, frame, 6, 6);
        System.arraycopy(header.type, 0, frame, 12, 2);
        if (dataArray != null)
            System.arraycopy(dataArray, 0, frame, 54, dataLength); // Skip IP & TCP Header
        return frame;
    }

    private byte[] removeHeader(byte[] frame, int frameLength) {
        byte[] dataArray = new byte[frameLength - 54]; // Remove Ethernet & IP & TCP Header
        System.arraycopy(frame, 54, dataArray, 0, frameLength - 54);
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

    public void setDestinationAddress(byte[] destinationAddress) {
        header.dst.addr = destinationAddress;
    }

    public void setSourceAddress(byte[] sourceAddress) {
        header.src.addr = sourceAddress;
    }

    private boolean isMyPacket(byte[] frame) {
        for (int index = 0; index < 6; index++)
            if (header.src.addr[index] != frame[index + 6]) return false;
        return true;
    }

    private boolean isBroadcast(byte[] frame) {
        for (int index = 0; index < 6; index++)
            if (frame[index] != (byte) 0xFF) return false;
        return (frame[12] == (byte) 0xFF && frame[13] == (byte) 0xFF);
    }

    private boolean isMine(byte[] frame) {
        for (int index = 0; index < 6; index++)
            if (header.src.addr[index] != frame[index]) return false;
        return true;
    }

    @Override
    public boolean send(byte[] dataArray, int dataLength, String layerName) {
//        print("send : " + String.format("%s, %d, %s", dataArray == null ? "null" : dataArray.toString(), dataLength, layerName));
//        if (dataArray != null) printHex(dataArray, dataLength);

        if (layerName == null) {
            printError("layer name is null");
            return false;
        } else switch (layerName) {
            case "ChatApp":
                if (dataArray == null && dataLength == 0) {
                    // ChatApp ACK
                    // print("sending ChatApp ACK");
                    header.type = integerToByte2(0x2081);
                } else {
                    // ChatApp Data
                    // print("sending ChatApp Data");
                    header.type = integerToByte2(0x2080);
                }
                break;
            case "FileApp":
                if (dataArray == null && dataLength == 0) {
                    // FileApp ACK
                    // print("sending FileApp ACK");
                    header.type = integerToByte2(0x2091);
                } else {
                    // FileApp Data
                    // print("sending FileApp Data");
                    header.type = integerToByte2(0x2090);
                }
                break;
            default:
                printError("undefined layer " + layerName);
                return false;
        }

        byte[] frame = createFrame(dataArray, dataLength);

//        print("send frame to under layer");
//        if (layerName.equals("FileApp"))
//            printHex(frame, frame.length);

        getUnderLayer().send(frame, frame.length);
        return true;
    }

    @Override
    public boolean receive(byte[] frame) {

        byte[] dataArray;
        int dataType = byte2ToInteger(frame[12], frame[13]);

        if (!isMyPacket(frame) && !isBroadcast(frame) && isMine(frame)) {
            dataArray = removeHeader(frame, frame.length);
            switch (dataType) {
                case 0x2080:
                    getUpperLayer("ChatApp").receive(dataArray);
                    break;
                case 0x2081:
                    getUpperLayer("ChatApp").receive(null);
                    break;
                case 0x2090:
                    getUpperLayer("FileApp").receive(dataArray);
                    break;
                case 0x2091:
                    getUpperLayer("FileApp").receive(null);
                    break;
                default:
                    if (dataType != 0x0800) printError("undefined type " + String.format("%04X", dataType));
                    return false;
            }
            return true;
        }
        return false;
    }

    private static class EthernetHeader {

        EthernetAddress dst;
        EthernetAddress src;
        byte[] type;
        byte[] data;

        public EthernetHeader() {
            dst = new EthernetAddress();
            src = new EthernetAddress();
            type = new byte[2];
            data = null;
        }

        private static class EthernetAddress {

            byte[] addr = new byte[6];

            public EthernetAddress() {
                for (int index = 0; index < 6; index++)
                    addr[index] = (byte) 0x00;
            }

        }

    }

}
