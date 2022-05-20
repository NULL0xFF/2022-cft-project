public interface LayerInterface {

    /**
     * Get Layer Name String
     *
     * @return layerName
     */
    String getLayerName();

    /**
     * Get Under Layer Object
     *
     * @return underLayer
     */
    LayerInterface getUnderLayer();

    /**
     * Set Under Layer Object
     *
     * @param underLayer LayerInterface Object
     */
    void setUnderLayer(LayerInterface underLayer);

    /**
     * Get Upper Layer Object
     *
     * @param index int
     * @return upperLayer
     */
    LayerInterface getUpperLayer(int index);

    /**
     * Get Upper Layer Object
     *
     * @param layerName String
     * @return upperLayer
     */
    LayerInterface getUpperLayer(String layerName);

    /**
     * Set Upper Layer Object
     *
     * @param upperLayer LayerInterface Object
     */
    void setUpperLayer(LayerInterface upperLayer);

    /**
     * Set Under Upper Layer Object
     *
     * @param underUpperLayer LayerInterface Object
     */
    void setUnderUpperLayer(LayerInterface underUpperLayer);

    /**
     * Set Upper Under Layer Object
     *
     * @param upperUnderLayer LayerInterface Object
     */
    void setUpperUnderLayer(LayerInterface upperUnderLayer);

    /**
     * Send Byte Data
     *
     * @param dataArray   byte[]
     * @param arrayLength int
     * @return boolean
     */
    boolean send(byte[] dataArray, int arrayLength);

    /**
     * Send Byte Data
     *
     * @param dataArray   byte[]
     * @param arrayLength int
     * @param layerName   String
     * @return boolean
     */
    boolean send(byte[] dataArray, int arrayLength, String layerName);

    /**
     * Send File Data
     *
     * @param fileName String
     * @return boolean
     */
    boolean send(String fileName);

    /**
     * Receive Data
     *
     * @param dataArray byte[]
     * @return boolean
     */
    boolean receive(byte[] dataArray);

    /**
     * Receive
     *
     * @return boolean
     */
    boolean receive();
}
