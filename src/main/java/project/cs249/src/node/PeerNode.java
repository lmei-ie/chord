package project.cs249.src.node;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import project.cs249.src.comm.SocketClient;
import project.cs249.src.comm.SocketServer;
import project.cs249.src.util.Configs;
import project.cs249.src.util.Constants;
import project.cs249.src.util.Logger;
import project.cs249.src.util.Utils;

public class PeerNode extends Node{
    private static final long serialVersionUID = 1190476516911661470L;
    private Node _predecessor; 
    //maximum number fo nodes allowed
    private int _m;
    //private Finger[] _fingerTable;
    private Node[] _fingerTable;
    private SuperNodeRMI _superNodeRMI;

    //for refreshing FT table entries.
    private volatile int _next;

    public PeerNode(String ip, String port, SuperNodeRMI superNodeRMI) {
        super(ip, port);
        //if conflict, the id should be rehash using the newest timestamp, need it for size of FT?
        this._predecessor=null; //synchronized on null object will cause nullPointerException
        this._fingerTable=null;
        this._m=0;
        this._next=0;
        this._superNodeRMI=superNodeRMI;
    }

    //PeerNode member functions
    private void initializeFT(){
        this._fingerTable=new Node[this._m];
        this._fingerTable[0]=this;
    }

    public void join() throws IOException {
        Node randomNode=this._superNodeRMI.getRamdonNode(this.getId());
        //if same, there is only one node in the ring.
        if(randomNode!=null && randomNode.getId()!=this.getId()){
            SocketClient socketClient=null;
            Node tempSuc = null;
            try{
                socketClient=new SocketClient(randomNode.getIp(),randomNode.getPort());
                //the successor's id must >= (pNode'id + 2^0)%2^m
                int key=(this.getId()+(int)Math.pow(2,0))%((int)Math.pow(2,_m));
                socketClient.sendNodeAndKey(Constants.P2P_CMD_FINDSUCCESSOR, (Node)this, key);

                //socket.readObject -> successor / null;
                //if null, this node dies? tell the supernode to remove it?
                tempSuc=socketClient.readReturnNode();
            }catch(IOException e){
                Logger.error(PeerNode.class, randomNode.toString()+" failed");
                if(socketClient!=null) socketClient.shutdown();
                try {
                    this._superNodeRMI.removeNode(randomNode.getId());
                } catch (RemoteException e1) {
                    Logger.error(PeerNode.class, "RMI removeNode error.");
                }
            }
            
            if(tempSuc!=null){
                Logger.info(PeerNode.class, this.toString()+" received successor "+tempSuc);
                this.setSuccessor(tempSuc);
                socketClient=new SocketClient(tempSuc.getIp(),tempSuc.getPort());
                //tell sucessor to update its predecessor
                socketClient.sendNode(Constants.P2P_CMD_RECEIVEPREDECESSOR, this);
                if(socketClient!=null) socketClient.shutdown();
                this._superNodeRMI.ackRegister(this);
            }
            else this.join();
        }
        else this._superNodeRMI.ackRegister(this);

	}

    public Node getSuccessor() {return this._fingerTable[0];}

    public void setSuccessor(Node successor) {
        synchronized(this){
            this._fingerTable[0]=successor;
        }
    }

    public void setPredecessor(Node predecessor) {
        synchronized(this){
            this._predecessor=predecessor;
        }
    }

    public Node getPredecessor() {return this._predecessor;}

    public void printFT(){
        Logger.info(PeerNode.class, this.toString()+" finger Table is");
        for(int i=0;i<this._m;i++){
            if(this._fingerTable[i]==null) System.out.println((i+1)+" "+(this.getId()+(int)Math.pow(2,i))%((int)Math.pow(2,_m))+" "+"NULL");
            else System.out.println((i+1)+" "+(this.getId()+(int)Math.pow(2,i))%((int)Math.pow(2,_m))+" "+this._fingerTable[i].toString());
        }
    }

    //RMI ops
    /**
     * get the node's chord info from superNode and set them.
     * @throws RemoteException
     */
    public void getNodeInfo() throws RemoteException{
        String rmiRes=this._superNodeRMI.getNodeInfo(this);
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

    public void ackRegister() throws RemoteException{
        this._superNodeRMI.ackRegister(this);
    }
    public void removeNode(int id){
	    try {
		this._superNodeRMI.removeNode(id);
	    } catch (RemoteException e) {
		Logger.error(PeerNode.class, "RMI removeNode error.");
	    }
    }

    //p2p ops
    /**
     * ask node n to find the successor of curNode
     * @param PeerNode need the address of curNode to send the successor back
     * @return PeerNode
     * @throws IOException
     */
    public Node find_successor(Node queryNode, int key){
        //Logger.info(PeerNode.class, pNode.toString()+" sucessor key is "+key);
        if(Utils.isInRange(key, this.getId(), this.getSuccessor().getId(), false, true)){
            //SocketClient socketClient=new SocketClient(pNode.getIp(),pNode.getPort());
            //socketClient.sendNode(Constants.P2P_CMD_RECEIVESUCCESSOR, this.getSuccessor());
            return this.getSuccessor();
        }
        else{
            Node hiPredecessor=this.closest_preceding_node(key);
            //Logger.info(PeerNode.class, "hiPredecessor to key "+key+" is "+hiPredecessor.toString());
            if(hiPredecessor.getId()==this.getId()){
                //SocketClient socketSender=new SocketClient(pNode.getIp(),pNode.getPort());
                //socketSender.sendNode(Constants.P2P_CMD_RECEIVESUCCESSOR, this);
                return this;
            }
            else{
                SocketClient socketClient=null;
                try{
                    socketClient=new SocketClient(hiPredecessor.getIp(),hiPredecessor.getPort());
                    //the successor's id must >= (curNode'id + 2^0)%2^m
                    socketClient.sendNodeAndKey(Constants.P2P_CMD_FINDSUCCESSOR, queryNode, key);
                    //use this socket to read?
                    Node tempSuc=socketClient.readReturnNode();
                    //the first null means hiPredecessor is down?
                    Logger.info(PeerNode.class, "received "+tempSuc.toString()+" from hiPredecessor "+hiPredecessor.toString());
                    return tempSuc;
                }catch(Exception e){
                    Logger.error(PeerNode.class, "failed to receive from hiPredecessor "+e.getMessage());
                    if(socketClient!=null) socketClient.shutdown();
                    try {
                        this._superNodeRMI.removeNode(hiPredecessor.getId());
                    } catch (RemoteException e1) {
                        Logger.error(PeerNode.class, "RMI removeNode error.");
                    }
                    return null;
                }
                
            }
        }
    }
    /**
     * search the loccal table for the highest predecessor of id
     * @return PeerNode
     */
    public Node closest_preceding_node(int key){
        for(int i=this._m-1;i>=0;i--){
            if(this._fingerTable[i]!=null){
                if(Utils.isInRange(this._fingerTable[i].getId(),this.getId(),key,false,false)==true){
                    return this._fingerTable[i];
                }
            }
        }
        return this;
    }

    public void stablize(){
        System.out.println("----------------------------stablize------------------------------");
        
        if(this.getSuccessor().getId()!=this.getId()){
            SocketClient socketClient=null;
            try{
                socketClient=new SocketClient(this.getSuccessor().getIp(), this.getSuccessor().getPort());
                socketClient.sendCmd(Constants.P2P_CMD_GETPREDECESSOR);
                Node x=socketClient.readReturnNode();
                if(socketClient!=null) socketClient.shutdown();
                //if(this.getSuccessor().getPredecessor()==null || this.getSuccessor().getPredecessor().getId()!=x.getId()){
                Logger.info(PeerNode.class, "Predecessor for "+this.getSuccessor().toString()+" is "+x.toString());
                //local p=copy of successor's precedessor needs to update too, which reducing socket connection for notifys
                //this.getSuccessor().setPredecessor(x);
                if(Utils.isInRange(x.getId(), this.getId(), this.getSuccessor().getId(), false, false)){
                    this.setSuccessor(x);
                }
                Logger.info(PeerNode.class, "Successor for "+this.getSuccessor().toString()+" is "+x.toString());
            }catch(IOException e){
                Logger.error(PeerNode.class, "stablize fail to connect to "+this.getSuccessor());
                this.setSuccessor(this);
                if(socketClient!=null) socketClient.shutdown();
                return;
            }
            try{
                socketClient=new SocketClient(this.getSuccessor().getIp(), this.getSuccessor().getPort());
                socketClient.sendNode(Constants.P2P_CMD_NOTIFY, this);
                
            }catch(IOException e){
                Logger.error(PeerNode.class, "stablize fail to connect to "+this.getSuccessor());
                this.setSuccessor(this);
            }finally{if(socketClient!=null) socketClient.shutdown();}

        }
        //the first joined node's successor is itself. and no change in sucessor should result in no need to stablize;
        else{
            //the first joined node, if its predecessor should be null, it does nothing until the predecessor is updated.
            //when the first node's predecessor got updated.
            if(this.getPredecessor()!=null){
                Node x=this.getPredecessor();
                Logger.info(PeerNode.class, "Predecessor for "+this.getSuccessor().toString()+" is "+x.toString());
                //updates its successor from itself to the correct one.
                if(Utils.isInRange(x.getId(), this.getId(), this.getSuccessor().getId(), false, false)){
                    this.setSuccessor(x);
                }
                SocketClient socketClient=null;
                try{
                    socketClient=new SocketClient(this.getSuccessor().getIp(), this.getSuccessor().getPort());
                    socketClient.sendNode(Constants.P2P_CMD_NOTIFY, this);
                }catch(IOException e){
                    Logger.error(PeerNode.class, "stablize fail to connect to"+this.getSuccessor());
                    this.setSuccessor(this);
                }finally{if(socketClient!=null) socketClient.shutdown();};
            }
        }
        

    }

    public void notifys(Node node){

        if(this.getPredecessor()==null || Utils.isInRange(node.getId(), this.getPredecessor().getId(), this.getId(), false, false)){
            this.setPredecessor(node);
            Logger.info(PeerNode.class, this.toString()+" is notified to update its predecessor to "+node.toString());
        }
        

    }

    public void fix_fingers(){
        System.out.println("----------------------------fix FT--------------------------------");

        synchronized (this){
            this._next++;
            if(this._next>=this._m) this._next=1;
        }
        
        Logger.info(PeerNode.class, this.toString()+" fixing fingerTable entry "+(this._next+1));
        SocketClient socketClient=null;
        try {
            Node randomNode=this._superNodeRMI.getRamdonNode(this.getId());
            //if == then there is only one node in the ring
            if(randomNode!=null && randomNode.getId()!=this.getId()){  
                socketClient = new SocketClient(randomNode.getIp(),randomNode.getPort());
                
                int key=(this.getId()+(int)Math.pow(2,this._next))%((int)Math.pow(2,_m));
                socketClient.sendNodeAndKey(Constants.P2P_CMD_FIXENTRY, this, key);
    
                //socket.readObject -> successor / null;
                //if null, this node dies? tell the supernode to remove it?
                Node tempSuc=socketClient.readReturnNode();
                this._fingerTable[this._next]=tempSuc;
            }
            else{
                if(this._fingerTable[this._next]!=null) this._fingerTable[this._next]=null;
            }
        }catch (IOException e) {
            this._fingerTable[this._next]=null;
            Logger.error(PeerNode.class, "fix FT "+e.getMessage());
        }finally{if(socketClient!=null) socketClient.shutdown();}
    }

    //heartbeat
    public void check_predecessor(){
        System.out.println("-------------------------check predecessor------------------------");
        if(this.getPredecessor()!=null){
            Logger.info(PeerNode.class,this.toString()+" sending heartbeat to predecessor "+this.getPredecessor().toString());
            SocketClient socketClient=null;
            boolean flag_removeNode=false;
            try {
                socketClient=new SocketClient(this.getPredecessor().getIp(),this.getPredecessor().getPort());
                //if no ack returned by the predecessor in 10s, we determin it dead.
                socketClient.sendCmd(Constants.P2P_CMD_HEARTBEAT);
                socketClient.setTimeout(10000);
                int resCode=socketClient.readCode();
                if(resCode!=Constants.P2P_CODE_ACK) throw new SocketTimeoutException("Predecessor does not ACK.");
                Logger.info(PeerNode.class,this.toString()+" received ACK from "+this.getPredecessor().toString());
            }
            catch (SocketTimeoutException | SocketException se) {
                Logger.error(PeerNode.class,this.getPredecessor().toString()+" failed. "+se.getMessage());
                flag_removeNode=true;
            }
            catch (Exception e){
                Logger.error(PeerNode.class, "check predescesor other exceptions. "+e.getMessage());
            }finally{
                if(socketClient!=null) socketClient.shutdown();
                if(flag_removeNode==true){
                    try {
                        this._superNodeRMI.removeNode(this.getPredecessor().getId());
                        this.setPredecessor(null);
                    } catch (RemoteException e1) {
                        Logger.error(PeerNode.class, "RMI removeNode error.");
                    }
                }
            }
        }
    }

    public String getStrFT() {
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<this._m;i++){
            if(this._fingerTable[i]==null) sb.append((i+1)+" "+(this.getId()+(int)Math.pow(2,i))%((int)Math.pow(2,_m))+" "+"NULL\n");
            else sb.append((i+1)+" "+(this.getId()+(int)Math.pow(2,i))%((int)Math.pow(2,_m))+" "+this._fingerTable[i].toString()+"\n");
        }
        return sb.toString();
    }

    public static void main(String[] args){
        
        // if(args.length<2){
        //     Logger.error(SuperNode.class,"Please input the peerNode's IP and port.");
        //     System.exit(1);
        // }
        //supernode address is well-known to peernodes
        String str_superNodeAddr=Configs.ADDR_SUPERNODE;

        /*TODO: Ask user to specify the ip address of the current machine*/
        String str_hostIp="localhost";
        //String str_hostIp=args[0];
        /*TODO: Ask user to specify the port number */
        String str_hostPort=Utils.getRandomPort(10000, 20000);
        //String str_hostPort=args[1];
        try {
            //RMI communicates with supernode to register
            SuperNodeRMI superNodeRMI=(SuperNodeRMI) Naming.lookup("rmi://" + str_superNodeAddr+ "/SuperNodeRMI");
            PeerNode curNode=new PeerNode(str_hostIp, str_hostPort, superNodeRMI);
            curNode.getNodeInfo();
            curNode.initializeFT();
            
            Thread t=new Thread(new Runnable() {
                public void run(){
                    SocketServer server=new SocketServer(Integer.parseInt(str_hostPort),curNode);
                    server.start();
                }
            },"Socket Server");
            t.start();
            
            //concurrent join will be an issue (no server to connect) b/c join randomly happnes before server starts.
            //(the first joined node does not connect to a socket server b/c it is the only one. )
            //a solution for above is to make main sleep and let thead starts first.
            Thread.sleep(1000);
            curNode.join();

            ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(3);
            Runnable stablization=()->{curNode.stablize();};

            Runnable fixFinger=()->{
                curNode.fix_fingers();
                curNode.printFT();
            };

            Runnable checkPredecessor=()->{
                curNode.check_predecessor();
            };
            List<ScheduledFuture<?>> futures=new ArrayList<>();
            futures.add(scheduledThreadPoolExecutor.scheduleAtFixedRate(checkPredecessor, 5, 10, TimeUnit.SECONDS));
            futures.add(scheduledThreadPoolExecutor.scheduleAtFixedRate(stablization, 10, 10, TimeUnit.SECONDS));
            futures.add(scheduledThreadPoolExecutor.scheduleAtFixedRate(fixFinger, 15, 10, TimeUnit.SECONDS));
            //ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(3);
            // Runnable runnable=()->{
            //     try {
            //         curNode.stablize();
            //         TimeUnit.SECONDS.sleep(2);
            //         curNode.fix_fingers();
            //         //TimeUnit.SECONDS.sleep(2);
            //         curNode.printFT();
            //         //TimeUnit.SECONDS.sleep(2);
            //         curNode.check_predecessor();
            //     } catch (InterruptedException e1) {
            //         Logger.error(TimeUnit.class, e1.getMessage());
            //     }
            // };
            // scheduledThreadPoolExecutor.scheduleWithFixedDelay(runnable, 5, 10, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            Logger.error(PeerNode.class,"Main "+e.getMessage());
            e.printStackTrace();
        }

    }
    
}
