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
            // Native Library Load
            String jNetPcapFile = null;
            System.out.println(System.getProperty("os.name"));

            if (isWindows(System.getProperty("os.name").toLowerCase()))
                jNetPcapFile = "jnetpcap.dll";
            else if (isUnix(System.getProperty("os.name").toLowerCase()))
                jNetPcapFile = "libjnetpcap.so";
            else
                throw new RuntimeException("unsupported operating system");

            System.load(new File(jNetPcapFile).getAbsolutePath());
            System.out.println(new File(jNetPcapFile).getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
            System.out.println("Native code library failed to load.\n" + e);
            System.exit(1);
        }
    }

    private final ArrayList<PcapIf> adapterList = new ArrayList<PcapIf>();
    private final StringBuilder errorBuffer = new StringBuilder();
    private Pcap adapterObject = null;
    private PcapIf adapterDevice = null;

    public NILayer(String name) {
        super(name);
        setAdapterList();
    }

    private static boolean isWindows(String osName) {
        return osName.indexOf("win") >= 0;
    }

    private static boolean isUnix(String osName) {
        return osName.indexOf("nix") >= 0 || osName.indexOf("nux") >= 0 || osName.indexOf("aix") >= 0;
    }

    public ArrayList<PcapIf> getAdapterList() {
        return this.adapterList;
    }

    private void setAdapterList() {
        // Bring All Network Adapter list of Host PC
        int r = Pcap.findAllDevs(adapterList, errorBuffer);
        System.out.println("Number of I/F : " + adapterList.size());
        // Error if there are no Network Adapter
        if (r == Pcap.NOT_OK || adapterList.isEmpty())
            System.out.println("[Error] Cannot read NIC. Error : " + errorBuffer.toString());
    }

    public void setAdapter(String adapterName) {
        adapterDevice = getAdapter(adapterName);
        packetStartDriver();
        receive();
    }

    public String getAdapterMACAddress(String adapterName) {
        String macAddress = "";
        try {
            macAddress = parseMACAddress(this.getAdapter(adapterName).getHardwareAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return macAddress;
    }

    public String parseMACAddress(byte[] byteMACAddress) {
        if (byteMACAddress == null) return "";
        String macAddress = "";
        for (int i = 0; i < 6; i++) {
            // 2자리 16진수를 대문자로, 그리고 1자리 16진수는 앞에 0을 붙임.
            macAddress += String.format("%02X%s", byteMACAddress[i], "");

            if (i != 5) {
                // 2자리 16진수 자리 단위 뒤에 "-"붙여주기
                macAddress += "-";
            }
        }
        System.out.println("mac_address:" + macAddress);
        return macAddress;
    }

    private void packetStartDriver() {
        int snaplen = 64 * 1024; // Capture all packets, no trucation
        int flags = Pcap.MODE_PROMISCUOUS; // capture all packets
        int timeout = 10 * 1000; // 10 seconds in millis
        adapterObject = Pcap.openLive(adapterDevice.getName(), snaplen, flags, timeout, errorBuffer);
    }

    private PcapIf getAdapter(String adapterName) {
        for (PcapIf adapter : adapterList)
            if (adapter.getName().equals(adapterName))
                return adapter;
        return null;
    }

    @Override
    public boolean send(byte[] input, int length) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(input);
        if (adapterObject.sendPacket(byteBuffer) != Pcap.OK) {
            System.err.println(adapterObject.getErr());
            return false;
        }
        return true;
    }

    @Override
    public boolean receive() {
        ReceiveThread receiveThread = new ReceiveThread(adapterObject, this.getUpperLayer(0));
        Thread thread = new Thread(receiveThread);
        thread.start();

        return false;
    }

    private class ReceiveThread implements Runnable {
        byte[] data;
        Pcap adapterObject;
        LayerInterface upperLayer;

        public ReceiveThread(Pcap adapObj, LayerInterface uLayer) {
            this.adapterObject = adapObj;
            this.upperLayer = uLayer;
        }

        @Override
        public void run() {
            while (true) {
                PcapPacketHandler<String> jPacketHandler = new PcapPacketHandler<String>() {
                    @Override
                    public void nextPacket(PcapPacket pcapPacket, String user) {
                        data = pcapPacket.getByteArray(0, pcapPacket.size());
                        upperLayer.receive(data);
                    }
                };
                adapterObject.loop(100000, jPacketHandler, "");
            }
        }
    }

}
