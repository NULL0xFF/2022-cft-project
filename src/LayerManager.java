import java.util.ArrayList;
import java.util.StringTokenizer;

public class LayerManager {

    private final ArrayList<LayerInterface> layerStack = new ArrayList<>();
    private final ArrayList<LayerInterface> layerList = new ArrayList<>();
    private int topLayerNumber;
    private Node front;
    private Node rear;
    private int layerCount;

    public LayerManager() {
        front = rear = null;
        layerCount = 0;
        topLayerNumber = -1;
    }

    public void addLayer(LayerInterface layer) {
        layerList.add(layerCount, layer);
        layerCount++;
    }

    public LayerInterface getLayer(int index) {
        return layerList.get(index);
    }

    public LayerInterface getLayer(String name) {
        for (LayerInterface layer : layerList)
            if (layer.getLayerName().equals(name)) return layer;
        return null;
    }

    public void connectLayers(String layerListString) {
        makeList(layerListString);
        linkLayer(front);
    }

    private void makeList(String layerListString) {
        StringTokenizer tokenizer = new StringTokenizer(layerListString, " ");

        while (tokenizer.hasMoreTokens()) {
            Node node = new Node(tokenizer.nextToken());
            addNode(node);
        }
    }

    private void addNode(Node node) {
        if (front == null) {
            front = rear = node;
        } else {
            rear.next = node;
            rear = node;
        }
    }

    private void linkLayer(Node node) {
        LayerInterface layer = null;

        while (node != null) {
            if (layer == null) layer = getLayer(node.getToken());
            else {
                if (node.getToken().equals("(")) push(layer);
                else if (node.getToken().equals(")")) pop();
                else {
                    char mode = node.getToken().charAt(0);
                    String name = node.getToken().substring(1);

                    layer = getLayer(name);

                    switch (mode) {
                        case '*':
                            top().setUpperUnderLayer(layer);
                            break;
                        case '+':
                            top().setUpperLayer(layer);
                            break;
                        case '-':
                            top().setUnderLayer(layer);
                            break;
                    }

                }
            }

            node = node.getNext();
        }
    }

    private void push(LayerInterface layer) {
        topLayerNumber++;
        layerStack.add(topLayerNumber, layer);
    }

    private LayerInterface pop() {
        LayerInterface layer = layerStack.get(topLayerNumber);
        layerStack.remove(topLayerNumber);
        topLayerNumber--;
        return layer;
    }

    private LayerInterface top() {
        return layerStack.get(topLayerNumber);
    }

    private static class Node {
        private final String token;
        private Node next;

        public Node(String token) {
            this.token = token;
            this.next = null;
        }

        public String getToken() {
            return this.token;
        }

        public Node getNext() {
            return this.next;
        }

        public void setNext(Node nextNode) {
            this.next = nextNode;
        }
    }
}
