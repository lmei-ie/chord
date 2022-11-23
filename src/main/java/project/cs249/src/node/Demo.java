package project.cs249.src.node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.Collections;

import project.cs249.src.comm.SocketClient;
import project.cs249.src.util.Configs;
import project.cs249.src.util.Constants;

public class Demo {
    public static void main(String[] args){
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        int selection=-1;
        String menu="Operations:\n1. Print Chord ring.\n2. Print finger table for a particular node.\n3. Shutdown a node (simulate the node fails).\nPlease input: ";
        String str_superNodeAddr=Configs.ADDR_SUPERNODE;
        SuperNodeRMI superNodeRMI=null;
        try{
            superNodeRMI=(SuperNodeRMI) Naming.lookup("rmi://" + str_superNodeAddr+ "/SuperNodeRMI");
            
        }catch(Exception e){
            e.printStackTrace();
            System.exit(1);
        }
        Node node=null;
        SocketClient socketClient=null;
        while(true){
            System.out.println(menu);
            
            try {
                selection=Integer.parseInt(br.readLine());
                switch(selection){
                    case 1:
                        ArrayList<Integer> ids=superNodeRMI.getIdList();
                        Collections.sort(ids);
                        System.out.println("-----------------------------------------------");
                        System.out.println("Chord ring is: ");
                        StringBuilder sb=new StringBuilder();
                        for(int i=0;i<ids.size();i++){
                            sb.append(ids.get(i));
                            if(i!=ids.size()-1)sb.append(" -> ");
                        }
                        System.out.println(sb.toString());
                        System.out.println("-----------------------------------------------");
                    break;
                    case 2:
                        System.out.println("-----------------------------------------------");
                        System.out.println("Enter node id: ");
                        int id=Integer.parseInt(br.readLine());
                        node=superNodeRMI.getNode(id);
                        socketClient=new SocketClient(node.getIp(), node.getPort());
                        socketClient.sendCmd(Constants.DEMO_CMD_GETFT);
                        String str_ft=socketClient.readInfo();
                        System.out.println("Finger table for node "+id+" is: ");
                        System.out.print(str_ft);
                        System.out.println("-----------------------------------------------");
                    break;
                    case 3:
                        System.out.println("-----------------------------------------------");
                        System.out.println("Enter node id: ");
                        int id2=Integer.parseInt(br.readLine());
                        node=superNodeRMI.getNode(id2);
                        socketClient=new SocketClient(node.getIp(), node.getPort());
                        socketClient.sendCmd(Constants.DEMO_CMD_SHUTDOWN);
                        System.out.println("-----------------------------------------------");
                    break;
                }
            } catch (NumberFormatException | IOException e) {
                if(socketClient!=null) socketClient.shutdown();
                e.printStackTrace();
            }
        }
    }
}
