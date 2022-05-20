import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacketHandler;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class NILayer extends BaseLayer {

    static {
        try {
            String jNetPcap;
            String osName = System.getProperty("os.name").toLowerCase();

            System.out.println("[NILayer] operating system : " + osName);

            if (osName.contains("win")) jNetPcap = "jnetpcap.dll";
            else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix"))
                jNetPcap = "libjnetpcap.so";
            else throw new RuntimeException("unsupported operating system");

            // Native Library Load
            File jNetPcapFile = new File(jNetPcap);
            System.load(jNetPcapFile.getAbsolutePath());

            System.out.println("[NILayer] file loaded : " + jNetPcapFile.getName());
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private final ArrayList<PcapIf> pcapInterfaceList = new ArrayList<>();
    private final StringBuilder errorStringBuilder = new StringBuilder();
    private Pcap pcapObject = null;
    private PcapIf pcapInterface = null;

    public NILayer(String layerName) {
        super(layerName);
        setInterfaceList();
    }

    public ArrayList<PcapIf> getInterfaceList() {
        return pcapInterfaceList;
    }

    private PcapIf getInterface(String interfaceName) {
        for (PcapIf pcapIf : pcapInterfaceList)
            if (pcapIf.getName().equals(interfaceName)) return pcapIf;
        return null;
    }

    public String getInterfaceMACAddress(String interfaceName) {
        String macAddress = "";
        try {
            macAddress = parseMACAddress(getInterface(interfaceName).getHardwareAddress());
        } catch (IOException e) {
            printError("failed to get MAC address from interface " + interfaceName);
        }
        return macAddress;
    }

    private void setInterfaceList() {
        // Bring All Network Adapter list of Host PC
        int result = Pcap.findAllDevs(pcapInterfaceList, errorStringBuilder);

        print("number of interface : " + pcapInterfaceList.size());
        for (PcapIf pcapIf : pcapInterfaceList)
            print("interface found : " + pcapIf.getDescription());

        // Error if there are no Network Adapter
        if (result == Pcap.NOT_OK || pcapInterfaceList.isEmpty())
            printError("cannot read network interface card\n" + errorStringBuilder);
    }


    public void setInterface(String interfaceName) {
        pcapInterface = getInterface(interfaceName);
        capturePacket();
        receive();
    }

    private String parseMACAddress(byte[] byteMACAddress) {
        if (byteMACAddress == null) return "";
        StringBuilder stringBuilder = new StringBuilder();
        for (int index = 0; index < 6; index++) {
            // byte to HEX
            stringBuilder.append(String.format("%02X", byteMACAddress[index]));
            if (index != 5) stringBuilder.append("-");
        }
        print("parsed MAC address : " + stringBuilder);
        return stringBuilder.toString();

    }

    private void capturePacket() {
        int snaplen = 64 * 1024; // Capture all packets, no trucation
        int flags = Pcap.MODE_PROMISCUOUS; // capture all packets
        int timeout = 10 * 1000; // 10 seconds in millis
        pcapObject = Pcap.openLive(pcapInterface.getName(), snaplen, flags, timeout, errorStringBuilder);
    }

    @Override
    public boolean send(byte[] dataArray, int dataLength) {
        print("send : " + String.format("%s, %d", dataArray.toString(), dataLength));
        printHex(dataArray, dataLength);

        print("send data");

        ByteBuffer byteBuffer = ByteBuffer.wrap(dataArray);
        if (pcapObject.sendPacket(byteBuffer) != Pcap.OK) {
            printError(pcapObject.getErr());
            return false;
        }

        print("data sent with size " + dataLength);

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
                PcapPacketHandler<String> jPacketHandler = (packet, user) -> {
                    data = packet.getByteArray(0, packet.size());

//                        print("received packet");
//                        printHex(data, packet.size());

                    upperLayer.receive(data);
                };
                pcapObject.loop(100000, jPacketHandler, "");
            }
        }

    }
}