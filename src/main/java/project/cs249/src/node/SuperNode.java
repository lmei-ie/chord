package project.cs249.src.node;

import java.math.BigInteger;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import project.cs249.src.comm.SocketClient;
import project.cs249.src.util.Constants;
import project.cs249.src.util.Logger;
import project.cs249.src.util.Utils;

public class SuperNode extends UnicastRemoteObject implements SuperNodeRMI{
    private static final long serialVersionUID = 1L;

     //how many bits is the identifier. should allow users to input;
     private static int _m;
     //maximum number of nodes in DHT, donated by 2^m;
     private static int _maxNumNodes;

     private int _numNodes;
     private Node[] _nodeRing;
     private volatile boolean _busy;
     private List<Integer> _idList;

     private Deque<Node> _wakeupQ;
 
    public SuperNode(int m) throws RemoteException{
        super();
        _m = m;
        _maxNumNodes = (int) Math.pow(2,m);
        _numNodes = 0;
        _nodeRing = new PeerNode[_maxNumNodes];
        _busy=false;
        _idList=new ArrayList<>();
        _wakeupQ=new LinkedList<>();
    }

    private int hashFunc(String timestamp, String ip, String port) throws NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.reset();
        String hashString = timestamp+ip+port;
        md.update(hashString.getBytes());
        byte[] hashBytes = md.digest();
        BigInteger hashNum = new BigInteger(1,hashBytes);

        return Math.abs(hashNum.intValue()) % _maxNumNodes;  
    }

    /**
     * params are from peerNode.
     * RMI server will spawn a thread to handle every request, should avoid concurrent issue
     * @param timestamp
     * @param ip
     * @param port
     * @return status_id_predecessor (success) / status_newId_newTimestamo (failure) 
     * @throws RemoteException
     */
    @Override
    public String getNodeInfo(Node node) throws RemoteException{
        Map<String,Object> map_res=new HashMap<>();
        Logger.info(SuperNode.class,node.toString()+" invoked getLiveInfo");
        if(!_busy){
            //concurrent joing control
            synchronized(this){
                //_busy should set to false after the node has ack joining.
                _busy=true;
                int hashIdx=-1;
                map_res.put("status",Constants.RMI_CODE_HASH_ERROR);
                if(_numNodes+1>_maxNumNodes){
                    map_res.put("message","Number of nodes exceed maximum allowance.");
                    return Utils.mapToString(map_res);
                }
                try{
                    hashIdx=hashFunc(node.getTimestamp(), node.getIp(), node.getPort());
                    Logger.info(SuperNode.class,node.getIp()+":"+node.getPort()+" "+node.getTimestamp()+" has hash value: "+hashIdx);
                    if(_nodeRing[hashIdx]!=null){
                        String newTimeStamp=null;
                        while(_nodeRing[hashIdx]!=null){
                            newTimeStamp=Utils.dateTimeToHex();
                            hashIdx=hashFunc(newTimeStamp, node.getIp(), node.getPort());
                        }
                        map_res.put("status",Constants.RMI_CODE_REHASH_SUCCESS);
                        map_res.put("timestamp",newTimeStamp);
                    }else{
                        map_res.put("status",Constants.RMI_CODE_HASH_SUCCESS);
                    }
                    map_res.put("id",hashIdx);
                    map_res.put("m",_m);
                }
                catch(NoSuchAlgorithmException e){
                    Logger.error(SuperNode.class, e.getMessage());
                    map_res.put("message","RMI error");
                }
            }
        }
        else {
            map_res.put("status", Constants.RMI_CODE_SNODE_BUSY);
            map_res.put("message","SuperNode busy");
        }
        return Utils.mapToString(map_res);
    }

    public void ackRegister(Node node) throws RemoteException{
        if(this._busy==true){
            synchronized(this){
                _busy=false;
                _nodeRing[node.getId()]=node;
                _numNodes++;
                _idList.add(node.getId());
            }
            Logger.info(SuperNode.class,this.toString());
        }
    }

    private void freeSupernode(){
        if(this._busy==true){
            synchronized(this){this._busy=false;}
        }
    }

    public Node getRamdonNode(int id) throws RemoteException{
        //only one node in the ring, the same node means all entries in the ft is itself
        if(_idList.size()<1) return null;

        Random rand = new Random();
        int randID = rand.nextInt(_idList.size());
        int targetIdx = _idList.get(randID);
        
        while(_idList.size()>1 && targetIdx==id){
            randID = rand.nextInt(_idList.size());
            targetIdx = _idList.get(randID);
        }
        return _nodeRing[targetIdx];
    }

    public synchronized void removeNode(int id) throws RemoteException{
       
        if(this._nodeRing[id]!=null){
            Logger.info(SuperNode.class, "removing node "+id);
            this._idList.remove(Integer.valueOf(id));
            _wakeupQ.addLast(this._nodeRing[id]);
            this._nodeRing[id]=null;
            this._numNodes--;
            Logger.info(SuperNode.class,this.toString());
        }
    }

    private void wakeupNodes(){
        Logger.info(SuperNode.class,"current wakeup queue is "+this._wakeupQ.toString());
        while(this._wakeupQ.size()>0){
            Node curNode=this._wakeupQ.pollFirst();
            SocketClient socketClient=null;
            try{
                socketClient=new SocketClient(curNode.getIp(), curNode.getPort());
                socketClient.sendCmd(Constants.P2P_CMD_HEARTBEAT);
                socketClient.setTimeout(10);
                int resCode=socketClient.readCode();
                if(resCode!=Constants.P2P_CODE_ACK) throw new SocketTimeoutException("Predecessor does not ACK.");
                socketClient.sendCmd(Constants.SUPER_CMD_REJOIN);
                Logger.info(PeerNode.class,"received ACK from "+curNode.toString());
            }
            catch (SocketTimeoutException | SocketException se) {
                Logger.error(PeerNode.class,curNode.toString()+" failed. "+se.getMessage());
            }
            catch (Exception e){
                Logger.error(PeerNode.class, "wakeup"+ curNode.toString()+" other exceptions. "+e.getMessage());
            }finally{
                if(socketClient!=null) socketClient.shutdown();
            }
        }
    }

    //for demo only
    public ArrayList<Integer> getIdList() throws RemoteException{
        return new ArrayList<>(this._idList);
    } 

    public Node getNode(int id) throws RemoteException{
        return this._nodeRing[id];
    }
    @Override
    public String toString() {
        StringBuilder sb_ret=new StringBuilder();
        sb_ret.append("{" +" _numNodes='" + _numNodes + "'" +", _nodeRing='");
        for(int i=0;i<_nodeRing.length;i++){
            if(_nodeRing[i]!=null) sb_ret.append(_nodeRing[i].toString());
        }
        sb_ret.append("}");
        return sb_ret.toString();
    }


    public static void main(String[] args) throws Exception{
        if(args.length<1 || !args[0].matches("-?(0|[1-9]\\d*)")){
            Logger.error(SuperNode.class,"Please input a valid identifier (m).");
            System.exit(1);
        }
        
        try
        {
            // Create an object of the interface implementation class
            SuperNode superNode = new SuperNode(Integer.parseInt(args[0]));
  
            // rmiregistry within the server JVM with port number 1900. port needs to be open
            LocateRegistry.createRegistry(1099);
            //ip should change to the real ip of server.
            // Binds the remote object by the name geeksforgeeks
            Naming.rebind("rmi://localhost:1099/SuperNodeRMI",superNode);
            Thread.sleep(1000);
            ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
            Runnable runnable=()->{
                try {    
                    superNode.freeSupernode();
                    TimeUnit.SECONDS.sleep(1);
                    superNode.wakeupNodes();
                } catch (InterruptedException e) {
                    
                    e.printStackTrace();
                }
            };
            scheduledThreadPoolExecutor.scheduleAtFixedRate(runnable, 60,60, TimeUnit.SECONDS);
        }
        catch(Exception e)
        {
            Logger.error(SuperNode.class,"SuperNode does not start. "+e.getMessage());
        }
    }
    

}
