import java.util.ArrayList;

public class BaseLayer implements LayerInterface {

    private final ArrayList<LayerInterface> upperLayerList = new ArrayList<>();

    private LayerInterface underLayer = null;
    private String layerName;

    public BaseLayer(String name) {
        this.layerName = name;
    }

    @Override
    public String getLayerName() {
        return this.layerName;
    }

    @Override
    public LayerInterface getUnderLayer() {
        return this.underLayer;
    }

    @Override
    public void setUnderLayer(LayerInterface newUnderLayer) {
        this.underLayer = newUnderLayer;
    }

    @Override
    public LayerInterface getUpperLayer(int index) {
        if (index < 0 || index > upperLayerList.size()) return null;
        return upperLayerList.get(index);
    }

    @Override
    public LayerInterface getUpperLayer(String layerName) {
        if (layerName == null || upperLayerList.isEmpty()) return null;
        for (LayerInterface layer : upperLayerList) {
            if (layer.getLayerName().equals(layerName)) return layer;
        }
        return null;
    }

    @Override
    public void setUpperLayer(LayerInterface newUpperLayer) {
        if (newUpperLayer == null) return;
        this.upperLayerList.add(upperLayerList.size(), newUpperLayer);
    }

    @Override
    public void setUnderUpperLayer(LayerInterface underUpperLayer) {
        this.setUnderLayer(underUpperLayer);
        underUpperLayer.setUpperLayer(this);
    }

    @Override
    public void setUpperUnderLayer(LayerInterface upperUnderLayer) {
        this.setUpperLayer(upperUnderLayer);
        upperUnderLayer.setUnderLayer(this);
    }

    @Override
    public boolean send(byte[] dataArray, int arrayLength) {
        return send(dataArray, arrayLength, null);
    }

    @Override
    public boolean send(byte[] dataArray, int arrayLength, String layerName) {
        return false;
    }

    @Override
    public boolean send(String fileName) {
        return false;
    }

    @Override
    public boolean receive(byte[] dataArray) {
        return false;
    }

    @Override
    public boolean receive() {
        return false;
    }
}