/**
 * Base Layer Interface
 */
interface LayerInterface {

    /**
     * Get Layer Name
     */
    String getLayerName();

    /**
     * Get Under Layer
     */
    LayerInterface getUnderLayer();

    /**
     * Set Under Layer
     */
    void setUnderLayer(LayerInterface underLayer);

    /**
     * Get Upper Layer
     */
    LayerInterface getUpperLayer(int index);

    /**
     * Set Upper Layer
     */
    void setUpperLayer(LayerInterface upperLayer);

    /**
     * Set Under Upper Layer
     */
    default void setUnderUpperLayer(LayerInterface underUpperLayer) {
    }

    /**
     * Set Upper Under Layer
     */
    void setUpperUnderLayer(LayerInterface upperUnderLayer);

////////////////////////////////////////////////////////////////////////////
    /*
     * When declared with "default", it can be implemented in interface and
     * override-able.
     */

    /**
     * Send
     */
    default boolean send(byte[] input, int length) {
        return false;
    }

    /**
     * Send
     */
    default boolean send(String filename) {
        return false;
    }

    /**
     * Receive
     */
    default boolean receive(byte[] input) {
        return false;
    }

    /**
     * Receive
     */
    default boolean receive() {
        return false;
    }
////////////////////////////////////////////////////////////////////////////

}
