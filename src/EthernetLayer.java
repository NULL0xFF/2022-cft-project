/**
 * Ethernet Layer Class
 */
public class EthernetLayer extends BaseLayer {

    private EthernetHeader header;

    /**
     * Ethernet Layer Constructor
     */
    public EthernetLayer(String name) {
        super(name);
        resetHeader();
    }

    /**
     * Reset Ethernet Frame Header
     */
    public void resetHeader() {
        header = new EthernetHeader();
    }

    /**
     * Add Ethernet Frame Header to Data
     */
    public byte[] objectToByte(EthernetHeader header, byte[] dataArray, int arrayLength) {
        byte[] byteBuffer = new byte[arrayLength + 14];
        for (int i = 0; i < 6; i++) {
            byteBuffer[i] = header.dst.addr[i];
            byteBuffer[i + 6] = header.src.addr[i];
        }
        byteBuffer[12] = header.type[0];
        byteBuffer[13] = header.type[1];
        for (int i = 0; i < arrayLength; i++)
            byteBuffer[14 + i] = dataArray[i];

        return byteBuffer;
    }

    /**
     * Send Ethernet Frame
     */
    /* If the type is 0xff, the frame needs to be broadcasted */
    public boolean send(byte[] dataArray, int arrayLength) {
        if (dataArray == null && arrayLength == 0) 
            header.type = integerToByte2(0x2081); // Chat && ACK
        else
            header.type = integerToByte2(0x2080); // Chat && Normal

        byte[] bytes = this.objectToByte(header, dataArray, arrayLength);
        this.getUnderLayer().send(bytes, bytes.length);

        return true;
    }
    
    public boolean fileSend(byte[] dataArray, int arrayLength) {
    	if (dataArray == null && arrayLength == 0) 
    		header.type = integerToByte2(0x2091); // File && ACK
    	else 
    		header.type = integerToByte2(0x2090); // File && Normal
    	
    	byte[] bytes = this.objectToByte(header, dataArray, arrayLength);
        this.getUnderLayer().send(bytes, bytes.length);
        
    	return true;
    }

    /**
     * Remove Ethernet Frame Header
     */
    public byte[] removeHeader(byte[] dataArray, int arrayLength) {
        byte[] byteBuffer = new byte[arrayLength - 14];
        System.arraycopy(dataArray, 14, byteBuffer, 0, arrayLength - 14);
        return byteBuffer;
    }

    /**
     * Receive Ethernet Frame
     */
    public synchronized boolean receive(byte[] dataArray) {
        byte[] data;
        int dataType = byte2ToInteger(dataArray[12], dataArray[13]);

        if (dataType == 0x01) { // Normal
            if (!this.isMyPacket(dataArray) && !this.isBroadcast(dataArray) && this.isMine(dataArray)) {
                // Not My Packet & Not Broadcasted & Address pointing me!
                data = this.removeHeader(dataArray, dataArray.length);

                this.getUpperLayer(0).receive(data);
                this.send(null, 0); // ACK

                return true;
            }
        } 
        else if (dataType == 0x02) { // ACK
            this.getUpperLayer(0).receive(null);
        } 
        else if (dataType == 0x2090) {
        	
        } 
        else if {dataType == 0x2091) {
        	
        }
        	
        }
        else {
            // undefined type
        }
        return false;
    }

    /**
     * Convert A Integer to 2 Bytes
     */
    private byte[] integerToByte2(int value) {
        byte[] byteBuffer = new byte[2];
        byteBuffer[0] |= (byte) ((value & 0xFF00) >> 8);
        byteBuffer[1] |= (byte) (value & 0xFF);

        return byteBuffer;
    }

    /**
     * Convert 2 Bytes to A Integer
     */
    private int byte2ToInteger(byte value1, byte value2) {
        return (value1 << 8) | (value2);
    }

    /**
     * Check if the Ethernet Frame is Broadcast Frame
     */
    private boolean isBroadcast(byte[] dataArray) {
        for (int i = 0; i < 6; i++)
            if (dataArray[i] != (byte) 0xff)
                return false;
        return (dataArray[12] == (byte) 0xff && dataArray[13] == (byte) 0xff);
    }

    /**
     * Check if the Ethernet Frame is My Packet
     */
    private boolean isMyPacket(byte[] dataArray) {
        for (int i = 0; i < 6; i++)
            if (header.src.addr[i] != dataArray[6 + i])
                return false;
        return true;
    }

    /**
     * Check if the Ethernet Frame is for me
     */
    private boolean isMine(byte[] dataArray) {
        byte[] byteBuffer = header.src.addr;
        for (int i = 0; i < 6; i++)
            if (byteBuffer[i] != dataArray[i])
                return false;
        return true;
    }

    /**
     * Set Ethernet Source MAC Address
     */
    public void setSourceAddress(byte[] newSrcAddr) {
        header.src.addr = newSrcAddr;
    }

    /**
     * Set Ethernet Destination MAC Address
     */
    public void setDestinationAddress(byte[] newDstAddress) {
        header.dst.addr = newDstAddress;
    }

    /**
     * Ethernet Frame Sub-Class
     */
    private class EthernetHeader {
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
        private class EthernetAddress {
            /**
             * Ethernet MAC Address
             */
            private byte[] addr = new byte[6];

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
