import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class NILayer extends BaseLayer {

    static {
        try {
            String jNetPcap = null;
            String osName = System.getProperty("os.name");
            print("operating system : " + osName);

            if (isWindows(System.getProperty("os.name").toLowerCase())) jNetPcap = "jnetpcap.dll";
            else if (isUnix(System.getProperty("os.name").toLowerCase())) jNetPcap = "libjnetpcap.so";
            else throwRuntimeException("unsupported operating system");

            // Native Library Load
            File jNetPcapFile = new File(jNetPcap);
            System.load(jNetPcapFile.getAbsolutePath());
            print("file loaded : " + jNetPcapFile.getName());
        } catch (UnsatisfiedLinkError e) {
            printError("native code library failed to load\n" + e);
            System.exit(1);
        }
    }

    private final ArrayList<PcapIf> pcapInterfaceList = new ArrayList<>();
    private final StringBuilder errorStringBuilder = new StringBuilder();
    private Pcap pcapObject = null;
    private PcapIf pcapInterface = null;

    public NILayer(String name) {
        super(name);
        setInterfaceList();
    }

    private static boolean isWindows(String osName) {
        return osName.contains("win");
    }

    private static boolean isUnix(String osName) {
        return osName.contains("nix") || osName.contains("nux") || osName.contains("aix");
    }

    private static void throwRuntimeException(String errStr) {
        throw new RuntimeException("[NILayer] " + errStr);
    }

    private static void print(String str) {
        System.out.println("[NILayer] " + str);
    }

    private static void printError(String errStr) {
        System.err.println("[NILayer] " + errStr);
    }

    public ArrayList<PcapIf> getInterfaceList() {
        return pcapInterfaceList;
    }

    private void setInterfaceList() {
        // Bring All Network Adapter list of Host PC
        int result = Pcap.findAllDevs(pcapInterfaceList, errorStringBuilder);
        print("number of interface : " + pcapInterfaceList.size());
        // Error if there are no Network Adapter
        if (result == Pcap.NOT_OK || pcapInterfaceList.isEmpty())
            printError("cannot read network interface card\n" + errorStringBuilder);
    }

    public void setInterfaceByName(String interfaceName) {
        pcapInterface = getInterface(interfaceName);
        capturePacket();
        receive();
    }

    public String getInterfaceMACAddess(String interfaceName) {
        String macAddress = "";
        try {
            macAddress = parseMACAddress(getInterface(interfaceName).getHardwareAddress());
        } catch (IOException e) {
            printError("faild to get MAC address from interface " + interfaceName + "\n" + e);
        }
        return macAddress;
    }

    private String parseMACAddress(byte[] byteMACAddress) {
        if (byteMACAddress == null) return "";
        String macAddress = "";
        for (int index = 0; index < 6; index++) {
            // byte to HEX
            macAddress += String.format("%02X%s", byteMACAddress[index], "");
            if (index != 5) {
                macAddress += "-";
            }
        }
        print("parsed MAC address : " + macAddress);
        return macAddress;
    }

    public PcapIf getInterface(String interfaceName) {
        for (PcapIf pI : pcapInterfaceList)
            if (pI.getName().equals(interfaceName)) return pI;
        return null;
    }


    private void capturePacket() {
        int snaplen = 64 * 1024; // Capture all packets, no trucation
        int flags = Pcap.MODE_PROMISCUOUS; // capture all packets
        int timeout = 10 * 1000; // 10 seconds in millis
        pcapObject = Pcap.openLive(pcapInterface.getName(), snaplen, flags, timeout, errorStringBuilder);
    }

    @Override
    public boolean send(byte[] dataArray, int arrayLength) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int index = 0; index < arrayLength; index++) {
            stringBuilder.append(String.format("%02X ", dataArray[index]));
            if ((index + 1) % 8 == 0 && (index + 1) != arrayLength) {
                print(stringBuilder.toString());
                stringBuilder.setLength(0);
            }
        }
        print(stringBuilder.toString());
        ByteBuffer byteBuffer = ByteBuffer.wrap(dataArray);
        if (pcapObject.sendPacket(byteBuffer) != Pcap.OK) {
            printError(pcapObject.getErr());
            return false;
        }
        print("data sent with size " + arrayLength);
        print(dataArray.toString());
        return true;
    }

    @Override
    public boolean receive() {
        new Thread(new ReceiveThread(pcapObject, getUpperLayer(0))).start();
        return false;
    }


    private class ReceiveThread implements Runnable {

        private final Pcap pcapObject;
        private final LayerInterface upperLayer;
        private byte[] data;

        public ReceiveThread(Pcap pcapObject, LayerInterface layer) {
            this.pcapObject = pcapObject;
            this.upperLayer = layer;
        }

        @Override
        public void run() {
            while (true) {
                PcapPacketHandler<String> jPacketHandler = new PcapPacketHandler<String>() {
                    @Override
                    public void nextPacket(PcapPacket packet, String user) {
                        data = packet.getByteArray(0, packet.size());
                        print("data received with size " + packet.size());
                        upperLayer.receive(data);
                    }
                };
                pcapObject.loop(100000, jPacketHandler, "");
            }
        }

    }
}
