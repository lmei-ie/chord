package project.cs249.src.node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Map;

import project.cs249.src.comm.SocketSender;
import project.cs249.src.comm.SocketServer;
import project.cs249.src.util.Configs;
import project.cs249.src.util.Constants;
import project.cs249.src.util.Logger;
import project.cs249.src.util.Utils;

public class PeerNode extends Node implements Runnable{
    private static final long serialVersionUID = 1190476516911661470L;
    private PeerNode _predecessor;
    //maximum number fo nodes allowed
    private int _m;
    //private Finger[] _fingerTable;
    private PeerNode[] _fingerTable;

    public PeerNode(String ip, String port) {
        super(ip, port);
        //if conflict, the id should be rehash using the newest timestamp, need it for size of FT?
        this._predecessor=null;
        this._fingerTable=null;
        this._m=0;
    }

    //PeerNode member functions
    private void initializeFT(){
        //this._fingerTable=new Finger[_m];
        this._fingerTable=new PeerNode[this._m];
        // for(int i=0;i<_m;i++){
        //     this._fingerTable[i]=new Finger((this.getId()+(int)Math.pow(2,i))%((int)Math.pow(2,_m)));
        //     this._fingerTable[i].setSuccessor(this);
            
        // }
        this._fingerTable[0]=this;

        // Node randomNode=superNodeRMI.getRamdonNode(this.getId());
        // Logger.info(PeerNode.class, "random node: "+randomNode.toString());
        // //not the same node as peerNode
        // if(randomNode.getId()!=this.getId()){
        //     // SocketSender socketSender = new SocketSender(randomNode.getIp(), randomNode.getPort());
        //     // socketSender.sendData(1, this);
        // }
        // else{
        //     this._predecessor=this;
        //     for(int i=0;i<_m;i++) this._fingerTable[i].setSuccessor(this);
        // }
    }

    public void join(SuperNodeRMI superNodeRMI) throws IOException {
        PeerNode randomNode=superNodeRMI.getRamdonNode(this.getId());
        //if same, there is only one node in the ring.
        if(randomNode.getId()!=this.getId()){
            SocketSender socketSender=new SocketSender(randomNode.getIp(),randomNode.getPort());
            socketSender.sendNode(Constants.P2P_CMD_FINDSUCCESSOR, this);
        }

	}

    public PeerNode getSuccessor() {return this._fingerTable[0];}

    public synchronized void setSuccessor(PeerNode successor) {this._fingerTable[0]=successor;}

    public synchronized void setPredecessor(PeerNode predecessor) {this._predecessor=predecessor;}

    public PeerNode getPredecessor() {return this._predecessor;}

    public void printFT(){
        Logger.info(PeerNode.class, this.toString()+" finger Table is");
        for(int i=0;i<this._m;i++){
            if(this._fingerTable[i]==null) System.out.println((i+1)+" NULL");
            else System.out.println((i+1)+" "+this._fingerTable[i].toString());
        }
    }

    //RMI ops
    /**
     * get the node's chord info from superNode and set them.
     * @throws RemoteException
     */
    public void getNodeInfo(SuperNodeRMI superNodeRMI) throws RemoteException{
        String rmiRes=superNodeRMI.getNodeInfo(this);
        Logger.info(PeerNode.class, rmiRes);
        Map<String,String> map_rmiRes=Utils.stringToMap(rmiRes);
        if(Integer.parseInt(map_rmiRes.get("status"))==Constants.RMI_CODE_HASH_SUCCESS){
            this.setId(Integer.parseInt(map_rmiRes.get("id")));
            this._m=Integer.parseInt(map_rmiRes.get("m"));
        }
        else if(Integer.parseInt(map_rmiRes.get("status"))==Constants.RMI_CODE_REHASH_SUCCESS){
            this.setId(Integer.parseInt(map_rmiRes.get("id")));
            this.setTimestamp(map_rmiRes.get("timestamp"));
            this._m=Integer.parseInt(map_rmiRes.get("m"));
        }
        else Logger.error(PeerNode.class, map_rmiRes.get("message"));
    }

    public void ackRegister(SuperNodeRMI superNodeRMI) throws RemoteException{
        superNodeRMI.ackRegister(this);
    }

    //p2p ops
    /**
     * ask node n to find the successor of curNode
     * @param PeerNode need the address of curNode to send the successor back
     * @return PeerNode
     * @throws IOException
     */
    public void find_successor(PeerNode pNode) throws IOException{
        //the successor's id must >= (pNode'id + 2^0)%2^m
        int key=(pNode.getId()+(int)Math.pow(2,0))%((int)Math.pow(2,_m));
        if(Utils.isInRange(key, this.getId(), this.getSuccessor().getId(), true)){
            SocketSender socketSender=new SocketSender(pNode.getIp(),pNode.getPort());
            socketSender.sendNode(Constants.P2P_CMD_RECEIVESUCCESSOR, this.getSuccessor());
        }
        else{
            PeerNode hiPredecessor=this.closest_preceding_node(key);
            if(hiPredecessor.getId()==this.getId()){
                SocketSender socketSender=new SocketSender(pNode.getIp(),pNode.getPort());
                socketSender.sendNode(Constants.P2P_CMD_RECEIVESUCCESSOR, this);
            }
            else{
                SocketSender socketSender=new SocketSender(hiPredecessor.getIp(),hiPredecessor.getPort());
                //the successor's id must >= (curNode'id + 2^0)%2^m
                socketSender.sendNode(Constants.P2P_CMD_FINDSUCCESSOR, pNode);
            }
        }
    }
    /**
     * search the loccal table for the highest predecessor of id
     * @return PeerNode
     */
    public PeerNode closest_preceding_node(int key){
        for(int i=this._m-1;i>=0;i--){
            if(this._fingerTable[i]!=null){
                if(Utils.isInRange(this._fingerTable[i].getId(),this.getId(),key,false)==true){
                    return this._fingerTable[i];
                }
            }
        }
        return this;
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
            PeerNode curNode=new PeerNode(str_hostIp, str_hostPort);
            curNode.getNodeInfo(superNodeRMI);
            curNode.ackRegister(superNodeRMI);
            curNode.initializeFT();
            //create a thread for comm server
            Thread t=new Thread(curNode);
            t.start();

            curNode.join(superNodeRMI);
            curNode.printFT();
            
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
    
}
