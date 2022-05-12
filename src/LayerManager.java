import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Layer Manager Class
 */
/* Connects Layers */
public class LayerManager {

    Node mp_sListHead;
    Node mp_sListTail;
    private int m_nTop;
    private int m_nLayerCount;
    private final ArrayList<LayerInterface> mp_Stack = new ArrayList<LayerInterface>();
    private final ArrayList<LayerInterface> mp_aLayers = new ArrayList<LayerInterface>();// chatapp,socket,IPCDIG들을 저장

    public LayerManager() {
        m_nLayerCount = 0;
        mp_sListHead = null;
        mp_sListTail = null;
        m_nTop = -1;
    }

    public void addLayer(LayerInterface pLayer) {
        mp_aLayers.add(m_nLayerCount++, pLayer);
        // m_nLayerCount++;
    }

    public LayerInterface getLayer(int nindex) {
        return mp_aLayers.get(nindex);
    }

    public LayerInterface getLayer(String pName) {
        for (int i = 0; i < m_nLayerCount; i++) {
            if (pName.compareTo(mp_aLayers.get(i).getLayerName()) == 0) return mp_aLayers.get(i);
        }
        return null;
    }

    public void connectLayers(String pcList) {
        makeList(pcList);
        linkLayer(mp_sListHead); // mPList에 넣은 값들과 연결
    }

    private void makeList(String pcList) { // 들어오는 Layer 이름을 token으로 잘름
        StringTokenizer tokens = new StringTokenizer(pcList, " ");

        for (; tokens.hasMoreElements(); ) {
            Node pNode = allocNode(tokens.nextToken()); // 각 토큰을 노드로 할당
            addNode(pNode);// 해당 토큰의 노드를 mp_list로 연결해줌

        }
    }

    private Node allocNode(String pcName) { // 각 토큰의 노드들을 연결해줌
        Node node = new Node(pcName);

        return node;
    }

    private void addNode(Node pNode) {
        if (mp_sListHead == null) {
            mp_sListHead = mp_sListTail = pNode;
        } else {
            mp_sListTail.next = pNode;
            mp_sListTail = pNode;
        }
    }

    private void push(LayerInterface pLayer) {
        mp_Stack.add(++m_nTop, pLayer);
        // mp_Stack.add(pLayer);
        // m_nTop++;
    }

    private LayerInterface pop() {
        LayerInterface pLayer = mp_Stack.get(m_nTop);
        mp_Stack.remove(m_nTop);
        m_nTop--;

        return pLayer;
    }

    private LayerInterface top() {
        return mp_Stack.get(m_nTop);
    }

    private void linkLayer(Node pNode) { // 계층 간 연결해줌
        LayerInterface pLayer = null;

        while (pNode != null) {
            if (pLayer == null) pLayer = getLayer(pNode.token);
            else {
                if (pNode.token.equals("(")) push(pLayer);
                else if (pNode.token.equals(")")) pop();
                else {
                    char cMode = pNode.token.charAt(0);
                    String pcName = pNode.token.substring(1);

                    pLayer = getLayer(pcName);

                    switch (cMode) {
                        case '*':
                            top().setUpperUnderLayer(pLayer); // 양방향연결
                            break;
                        case '+':
                            top().setUpperLayer(pLayer); // 윗방향 연결
                            break;
                        case '-':
                            top().setUnderLayer(pLayer); // 아래방향 연결
                            break;
                    }
                }
            }

            pNode = pNode.next;

        }
    }

    private class Node {
        private final String token;
        private Node next;

        public Node(String input) {
            this.token = input;
            this.next = null;
        }
    }

}
