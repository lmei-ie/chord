package project.cs249.src.node;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Map;

import project.cs249.src.comm.SocketSender;
import project.cs249.src.comm.SocketServer;
import project.cs249.src.util.Configs;
import project.cs249.src.util.ConstNames;
import project.cs249.src.util.Logger;
import project.cs249.src.util.Utils;

public class PeerNode extends Node implements Runnable{
    private static final long serialVersionUID = 1190476516911661470L;
    private int _preId;
    //maximum number fo nodes allowed
    private int _m;
    private SuperNodeRMI _superNodeRMI;


    public PeerNode(String ip, String port, SuperNodeRMI superNodeRMI) {
        super(ip, port);
        //if conflict, the id should be rehash using the newest timestamp, need it for size of FT?
        this._preId=-1;
        this._superNodeRMI=superNodeRMI;
        this._m=0;
    }

    /**
     * get the node's chord info from superNode and set them.
     * @throws RemoteException
     */
    public void getNodeInfo() throws RemoteException{
        String rmiRes=this._superNodeRMI.getNodeInfo((Node)this);
        Logger.info(PeerNode.class, rmiRes);
        Map<String,String> map_rmiRes=Utils.stringToMap(rmiRes);
        if(Integer.parseInt(map_rmiRes.get("status"))==ConstNames.RMI_CODE_HASH_SUCCESS){
            this.setId(Integer.parseInt(map_rmiRes.get("id")));
            this._m=Integer.parseInt(map_rmiRes.get("m"));
        }
        else if(Integer.parseInt(map_rmiRes.get("status"))==ConstNames.RMI_CODE_REHASH_SUCCESS){
            this.setId(Integer.parseInt(map_rmiRes.get("id")));
            this.setTimestamp(map_rmiRes.get("timestamp"));
            this._m=Integer.parseInt(map_rmiRes.get("m"));
        }
        else Logger.error(PeerNode.class, map_rmiRes.get("message"));
    }

    public void ackRegister() throws RemoteException{
        this._superNodeRMI.ackRegister(this.getId(),(Node)this);
    }

    @Override
    public void run() {
         //set up socket server for communication among peerNodes;
         SocketServer server=new SocketServer(Integer.parseInt(this.getPort()),this);
         server.start();
    }
    public static void main(String[] args){
        
        String str_superNodeAddr=Configs.ADDR_SUPERNODE;

        InetAddress inetAddress=null;
        try {
            inetAddress = Utils.getHostInetAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        
        String str_hostIp=inetAddress.getHostAddress();
        /*TODO: Ask user to specify the port number */
        String str_hostPort=Utils.getRandomPort(10000, 20000);
        
        try {
            //RMI communicates with supernode to register
            SuperNodeRMI superNodeRMI=(SuperNodeRMI) Naming.lookup("rmi://" + str_superNodeAddr+ "/SuperNodeRMI");
            PeerNode peerNode=new PeerNode(str_hostIp, str_hostPort,superNodeRMI);
            peerNode.getNodeInfo();
            peerNode.ackRegister();

            Node rmiRes=peerNode._superNodeRMI.getRamdonNode(peerNode.getId());
            
            Logger.info(PeerNode.class, "random node: "+rmiRes.toString());

            //create a thread for comm server
            Thread t=new Thread(peerNode);
            t.start();


            //not the same node as peerNode
            if(rmiRes.getId()!=peerNode.getId()){
                SocketSender socketSender = new SocketSender(rmiRes.getIp(), rmiRes.getPort());
                socketSender.sendData(1, peerNode);
            }
            
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
    
}
