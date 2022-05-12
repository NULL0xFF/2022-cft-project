import java.util.ArrayList;

public class BaseLayer implements LayerInterface {
    /**
     * Layer Name
     */
    private String layerName = null;
    /**
     * Under Layer
     */
    private LayerInterface underLayer = null;
    /**
     * Upper Layer List
     */
    private final ArrayList<LayerInterface> upperLayerList = new ArrayList<LayerInterface>();

    public BaseLayer(String name) {
        this.layerName = name;
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
    public void setUnderLayer(LayerInterface uLayer) {
        this.underLayer = uLayer;
    }

    @Override
    public LayerInterface getUpperLayer(int index) {
        if (index < 0 || index > upperLayerList.size() || upperLayerList.size() < 0) return null;
        return upperLayerList.get(index);
    }

    @Override
    public void setUpperLayer(LayerInterface uLayer) {
        if (uLayer == null) return;
        this.upperLayerList.add(upperLayerList.size(), uLayer);
    }

    @Override
    public void setUpperUnderLayer(LayerInterface upperUnderLayer) {
        this.setUpperLayer(upperUnderLayer);
        upperUnderLayer.setUnderLayer(this);
    }
}
