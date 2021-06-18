import java.net.*;
import java.util.*;

public class SRNode {
    private static int localPort;
    private static int remotePort;
    private static InetAddress address;
    private static int windowSize;
    private static boolean mode;
    private static double p;
    private static int n;
    private static int index = 0;
    private static HashMap<Integer, Date> packetSentTimeMap = new HashMap<>();
    private static boolean [] hasReceive;
    private static boolean [] hasSent;
    private static boolean [] receiverHasReceive;
    private static boolean [] receiverHasDropped;

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        String param = sc.nextLine();
        String [] paramArray = param.split(" ");
        address = InetAddress.getByName("localhost");
        localPort = Integer.valueOf(paramArray[0]);
        remotePort = Integer.valueOf(paramArray[1]);
        windowSize = Integer.valueOf(paramArray[2]);
        String mode1 = paramArray[3];
        if("-d".equals(mode1)) {
            mode = true;
            n = Integer.valueOf(paramArray[4]);
        }else {
            mode = false;
            p = Double.valueOf(paramArray[4]);
        }
        String [] buffer = new String[1000];
        Arrays.fill(buffer, null);
        DatagramSocket socket = new DatagramSocket(localPort);
        new Thread(() -> {
            int recStart = 0;
            int recEnd = recStart + windowSize;
            int sendStart = 0;
            int sendEnd = sendStart + windowSize;
            int dataDropped = 0;
            int ACKDropped = 0;
            receiverHasReceive = new boolean[10000];
            receiverHasDropped = new boolean[10000];
            while(true) {
                try {
                    byte[] data = new byte[5];
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    socket.receive(packet);
                    String info = new String(data, 0, packet.getLength());
                    if(info.charAt(0) != 'A' && info.charAt(0) != 'B') {
                        //I am a receiver, I will send ACK to the sender
                        int realIndex = Integer.valueOf(info.substring(0, 4));
                        String recData = info.substring(4);
                        if(recData.equals("$")) {
                            byte[] BCKdata = ("BCK" + realIndex).getBytes();
                            DatagramPacket BCKpacket = new DatagramPacket(BCKdata, BCKdata.length, address, remotePort);
                            socket.send(BCKpacket);
                            System.out.println("[summary]" + dataDropped + "/" + realIndex + " packets dropped, loss rate = " + dataDropped * 1.0 / realIndex);
                            break;
                        }
                        boolean flag = changeFlagBymode(realIndex, mode, p, n);
                        if(flag) {
                            receiverHasReceive[realIndex] = true;
                            if(realIndex == recStart) {
                                while(receiverHasReceive[recStart]) {
                                    recStart ++;
                                    recEnd ++;
                                }
                                System.out.println("[" + new Date().getTime() + "] " + "packet" + realIndex + " " + recData + " received");
                                byte[] ACKdata = ("ACK" + realIndex).getBytes();
                                DatagramPacket ACKpacket = new DatagramPacket(ACKdata, ACKdata.length, address, remotePort);
                                socket.send(ACKpacket);
                                System.out.println("[" + new Date().getTime() + "] " + "ACK" + realIndex + " sent, window starts at " + recStart);
                            } else {
                                System.out.println("[" + new Date().getTime() + "] " + "packet" + realIndex + " " + recData + " received out of order, buffered");
                                byte[] ACKdata = ("ACK" + realIndex).getBytes();
                                DatagramPacket ACKpacket = new DatagramPacket(ACKdata, ACKdata.length, address, remotePort);
                                socket.send(ACKpacket);
                                System.out.println("[" + new Date().getTime() + "] " + "ACK" + realIndex + " sent, window starts at " + recStart);
                            }
                        } else {
                            receiverHasDropped[realIndex] = true;
                            dataDropped ++;
                            System.out.println("[" + new Date().getTime() + "] " + "packet" + realIndex + " " + recData + " dropped");
                        }
                    } else {
                        //I am a sender, I am waiting for the ACK
                        int sendindex = Integer.valueOf(info.substring(3));
                        if(info.charAt(0) == 'B') {
                            System.out.println("[summary]" + ACKDropped + "/" + sendindex + " ACKs dropped, loss rate = " + ACKDropped * 1.0/sendindex);
                            break;
                        }
                        boolean flag = changeFlagBymode(sendindex, mode, p, n);
                        if(flag) {
                            hasReceive[sendindex] = true;
                            if(sendindex == sendStart) {
                                while(hasReceive[sendStart]){
                                    sendStart ++;
                                    sendEnd ++;
                                }
                             }
                            System.out.println("[" + new Date().getTime() + "] " + "ACK" + sendindex + " received, window starts at " + sendStart);
                        } else {
				                            receiverHasDropped[sendindex] = true;
                            ACKDropped ++;
                            System.out.println("[" + new Date().getTime() + "] " + "ACK" + sendindex + " dropped");
                        }
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        while(true) {
            Scanner sc2 = new Scanner(System.in);
            String input = sc.nextLine();
            String [] inputArray = input.split(" ");
            if(!inputArray[0].equals("send")) {
                break;
            }
            String data = inputArray[1];
            int totalDatalength = data.length();
            while(index < totalDatalength) {
//                while(!emptyBuffer(buffer)) {
//                    System.out.println("the buffer is full, please wait");
//                    Thread.sleep(1000);
//                }
                if(index + 1000 > totalDatalength) {
                    for(int i = index; i < totalDatalength; i ++) {
                        buffer[i%1000] = build(i, data.charAt(i));
                    }
                    index = totalDatalength;
                } else {
                    for(int i = index; i < index + 1000; i ++) {
                        buffer[i%1000] = build(i, data.charAt(i));
                    }
                    index += 1000;
                }
            }
            hasReceive = new boolean[index + 1];
            hasSent = new boolean[index];
            for(int i = 0; i < index; i ++) {
                hasReceive[i] = false;
                hasSent[i] = false;
            }
            int left = 0;
            int right = windowSize;
            while(left < index) {
                for(int i = left; i < Math.min(right, index); i ++) {
                    if(!hasSent[i]) {
                        byte [] data1 = buffer[i].getBytes();
                        DatagramPacket packet = new DatagramPacket(data1, data1.length, address, remotePort);
                        socket.send(packet);
                        packetSentTimeMap.put(i, new Date());
                        hasSent[i] = true;
                        System.out.println("[" + new Date().getTime() + "] " + "packet" + i + " sent");
                    }
                }
                if(new Date().getTime() - packetSentTimeMap.get(left).getTime() >= 500) {
                    byte [] data2 = buffer[left].getBytes();
                    DatagramPacket packet = new DatagramPacket(data2, data2.length, address, remotePort);
                    socket.send(packet);
                    packetSentTimeMap.put(left, new Date());
                    System.out.println("[" + new Date().getTime() + "] " + "packet" + left + " timeout, resending");
                }
                if(hasReceive[left]) {
                    left ++;
                    right ++;
                }
            }
            //tell the receiver that all the data has been sent
            byte [] data3 = build(index, '$').getBytes();
            DatagramPacket packet = new DatagramPacket(data3, data3.length, address, remotePort);
            socket.send(packet);



        }
    }

    private static boolean changeFlagBymode(int index, boolean mode, double p, int n) {
        if(mode) {
            if(receiverHasDropped[index]) {
                return true;
            } else {
                return (index % n != 0);
            }
        } else {
            Random random = new Random();
            return random.nextDouble() > p;
        }
    }

    private static String build(int i, char data) {
        StringBuilder sb = new StringBuilder();
        int current = 1;
        int t = i;
        while(i / 10 > 0) {
            i /= 10;
            current ++;
        }
        if(current == 3) {
            sb.append("0");
            sb.append(t);
            sb.append(data);
        } else if(current == 2) {
            sb.append("00");
            sb.append(t);
            sb.append(data);
        } else {
            sb.append("000");
            sb.append(t);
            sb.append(data);
        }
        return sb.toString();
    }

    private static boolean emptyBuffer (String [] checkArray) {
        for(String i : checkArray) {
            if(i != null) {
                return false;
            }
        }
        return true;
    }
}

