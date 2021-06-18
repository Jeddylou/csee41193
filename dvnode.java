import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

public class dvnode {
    private static int localport;
    private static HashMap<Integer, Integer> distanceMap = new HashMap<>();
    private static HashMap<Integer, Integer> nextHopMap = new HashMap<>();
    private static boolean flag = false;


    public static void main(String[] args) throws Exception{
        InetAddress address = InetAddress.getByName("localhost");
        Scanner sc = new Scanner(System.in);
        String param = sc.nextLine();
        String [] paramArray = param.split(" ");
        localport = Integer.valueOf(paramArray[0]);
        int length = (paramArray.length - 1) / 2;
        System.out.println("[" + new Date().getTime() + "] Node " + localport + " Routing Table");
        DatagramSocket socket = new DatagramSocket(localport);
        StringBuilder sb = new StringBuilder();
        sb.append(localport);
        sb.append(" ");
        distanceMap.put(localport, 0);
        nextHopMap.put(localport, localport);
        for(int i = 0; i < length; i ++) {
            int portnumber = Integer.valueOf(paramArray[i*2 + 1]);
            int distance = Integer.valueOf(paramArray[i*2 + 2].substring(1));
            sb.append(portnumber);
            sb.append(" ");
            sb.append(distance);
            sb.append(" ");
            distanceMap.put(portnumber, distance);
            nextHopMap.put(portnumber, localport);
            System.out.println("(." + distance + ") -> Node " + portnumber + " ;");
        }
        new Thread(() -> {
            while(true) {
                try {
                    byte[] data2 = new byte[1024];
                    DatagramPacket packet2 = new DatagramPacket(data2, data2.length);
                    socket.receive(packet2);
                    String info = new String(data2, 0, packet2.getLength());
                    String [] infoArray = info.split(" ");
                    int fromNode = Integer.valueOf(infoArray[0]);
                    //[<timestamp>] Message received at Node <port-vvvv> from Node <port-xxxx>
                    System.out.println("[" + new Date().getTime() + "] Message received at Node " + localport + "from Node" + fromNode);
                    flag = false;
                    BFAlgorithm(localport, distanceMap, nextHopMap, infoArray);
                    System.out.println("[" + new Date().getTime() + "] Node " + localport + " Routing Table");
                    if(!flag) {
                        for(int key : distanceMap.keySet()) {
                            if(key == localport) {
                                continue;
                            }
                            int distance = distanceMap.get(key);
                            int nextHop = nextHopMap.get(key);
                            if(nextHop == localport) {
                                System.out.println("(." + distance + ") -> Node " + key + " ;");
                            } else {
                                System.out.println("(." + distance + ") -> Node " + key + " ; Next hop -> Node " + nextHop);
                            }
                        }
                    } else{
                        //change happens
                        //send message to all other nodes
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append(localport);
                        sb2.append(" ");
                        for(int key : distanceMap.keySet()) {
                            if(key == localport) {
                                continue;
                            }
                            sb2.append(key);
                            sb2.append(" ");
                            sb2.append(distanceMap.get(key));
                            sb2.append(" ");
                            int distance = distanceMap.get(key);
                            int nextHop = nextHopMap.get(key);
                            if(nextHop == localport) {
                                System.out.println("(." + distance + ") -> Node " + key + " ;");
                            } else {
                                System.out.println("(." + distance + ") -> Node " + key + " ; Next hop -> Node " + nextHop);
                            }
                        }
                        for(int key : distanceMap.keySet()) {
                            if(key == localport) {
                                continue;
                            }
                            byte [] data3 = sb2.toString().trim().getBytes();
                            DatagramPacket packet = new DatagramPacket(data3, data3.length, address, key);
                            socket.send(packet);
                            //[<timestamp>] Message sent from Node <port-xxxx> to Node <port-vvvv>
                            System.out.println("[" + new Date().getTime() + "] Message sent from Node " + localport + " to Node " + key);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }).start();
        if(paramArray.length % 2 == 0) {
            //need to send message
            for(int key : distanceMap.keySet()) {
                if(key == localport) {
                    continue;
                }
                byte [] data = sb.toString().trim().getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, address, key);
                socket.send(packet);
                //[<timestamp>] Message sent from Node <port-xxxx> to Node <port-vvvv>
                System.out.println("[" + new Date().getTime() + "] Message sent from Node " + localport + " to Node " + key);
            }
        }
    }

    private static void BFAlgorithm(int localport, HashMap<Integer, Integer> distanceMap, HashMap<Integer, Integer> nextHopMap, String [] infoArray) {
        int inputEdge = (infoArray.length - 1) / 2;
        int midNode = Integer.valueOf(infoArray[0]);
        List<Integer> nodeList = new ArrayList<>();
        nodeList.addAll(distanceMap.keySet());
        for(int i = 0; i < inputEdge; i ++) {
            int newInputNode = Integer.valueOf(infoArray[2 * i + 1]);
            int newInputDistance = Integer.valueOf(infoArray[2 * i + 2]);
            if(!nodeList.contains(newInputNode)) {
                nodeList.add(newInputNode);
                distanceMap.put(newInputNode, newInputDistance + distanceMap.get(midNode));
                nextHopMap.put(newInputNode, midNode);
                flag = true;
            } else {
                int formalDis = distanceMap.get(newInputNode);
                int newDis = newInputDistance + distanceMap.get(midNode);
                if(newDis < formalDis) {
                    distanceMap.put(newInputNode, newDis);
                    nextHopMap.put(newInputDistance, midNode);
                    flag = true;
                }
            }
        }


    }
}

