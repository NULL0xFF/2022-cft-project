import java.util.ArrayList;

public abstract class BaseLayer implements LayerInterface {

    private final ArrayList<LayerInterface> upperLayerList = new ArrayList<>();
    private final String layerName;

    private LayerInterface underLayer = null;

    public BaseLayer(String layerName) {
        this.layerName = layerName;
    }

    public void print(String str) {
        System.out.printf("[%s] %s\n", layerName, str);
    }

    public void printError(String errStr) {
        System.err.printf("[%s] %s\n", layerName, errStr);
    }

    public void printHex(byte[] dataArray, int dataLength) {
        StringBuilder stringBuilder = new StringBuilder(String.format("[%s]", layerName));
        for (int index = 0; index < dataLength; index++) {
            stringBuilder.append(String.format(" %02X", dataArray[index]));
            if ((index + 1) % 8 == 0 && (index + 1) < dataLength)
                stringBuilder.append(String.format("\n[%s]", layerName));
        }
        System.out.println(stringBuilder);
    }

    @Override
    public String getLayerName() {
        return layerName;
    }

    @Override
    public LayerInterface getUnderLayer() {
        return underLayer;
    }

    @Override
    public void setUnderLayer(LayerInterface underLayer) {
        this.underLayer = underLayer;
    }

    @Override
    public LayerInterface getUpperLayer(int index) {
        if (index < 0 || index > upperLayerList.size()) return null;
        return upperLayerList.get(index);
    }

    @Override
    public LayerInterface getUpperLayer(String layerName) {
        if (layerName == null || upperLayerList.isEmpty()) return null;
        for (LayerInterface upperLayer : upperLayerList)
            if (upperLayer.getLayerName().equals(layerName))
                return upperLayer;
        return null;
    }

    @Override
    public void setUpperLayer(LayerInterface upperLayer) {
        if (upperLayer == null) return;
        upperLayerList.add(upperLayer);
    }

    @Override
    public void setUnderUpperLayer(LayerInterface underUpperLayer) {
        setUnderLayer(underUpperLayer);
        underUpperLayer.setUpperLayer(this);
    }

    @Override
    public void setUpperUnderLayer(LayerInterface upperUnderLayer) {
        setUpperLayer(upperUnderLayer);
        upperUnderLayer.setUnderLayer(this);
    }

    @Override
    public boolean send(byte[] dataArray, int dataLength) {
        return send(dataArray, dataLength, null);
    }

    @Override
    public boolean send(byte[] dataArray, int dataLength, String layerName) {
        return false;
    }

    @Override
    public boolean receive() {
        return false;
    }

    @Override
    public boolean receive(byte[] dataArray) {
        return receive(dataArray, null);
    }

    @Override
    public boolean receive(byte[] dataArray, String layerName) {
        return false;
    }
}
