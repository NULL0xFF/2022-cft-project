package datacomm;

public interface LayerInterface {

    String getLayerName();

    LayerInterface getUnderLayer();

    void setUnderLayer(LayerInterface underLayer);

    LayerInterface getUpperLayer(int index);

    LayerInterface getUpperLayer(String layerName);

    void setUpperLayer(LayerInterface upperLayer);

    void setUnderUpperLayer(LayerInterface underUpperLayer);

    void setUpperUnderLayer(LayerInterface upperUnderLayer);

    boolean send(byte[] dataArray, int dataLength);

    boolean send(byte[] dataArray, int dataLength, String layerName);

    boolean send(String filePath);

    boolean receive();

    boolean receive(byte[] dataArray);

    boolean receive(byte[] dataArray, String layerName);

}
