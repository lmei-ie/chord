package project.cs249.src.comm;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import project.cs249.src.node.Node;
import project.cs249.src.node.PeerNode;
import project.cs249.src.util.Constants;
import project.cs249.src.util.Logger;



class ServerThread implements Runnable{
    private Socket socket;
    private PeerNode curNode;
    private ObjectInputStream ois=null;
    private ObjectOutputStream oos=null;
    public ServerThread(Socket socket, PeerNode node) throws IOException{
        this.socket=socket;
        this.curNode=node;
        this.ois=new ObjectInputStream(this.socket.getInputStream());
        this.oos=new ObjectOutputStream(this.socket.getOutputStream()); 
    }

    public void sendNode(Node suc_queryNode) throws IOException {
		oos.writeObject(suc_queryNode);
		oos.flush();
	}

    public void sendCode(int code) throws IOException{
        oos.writeInt(code);
        oos.flush();
    }

    public void sendInfo(String str_ft) throws IOException {
        oos.writeObject(str_ft);
		oos.flush();
    }

    @Override
    public void run() {
        try{
            int code = ois.readInt();
            switch (code){
                case Constants.P2P_CMD_FINDSUCCESSOR:
                    Node queryNode = (Node) ois.readObject();
                    int key = ois.readInt();
                    Logger.info(ServerThread.class, curNode.toString()+ " find successor for " + queryNode.toString());
                    Node suc_queryNode=curNode.find_successor(queryNode,key);
                    Logger.info(ServerThread.class, queryNode.toString()+" 's successor is "+suc_queryNode.toString());
                    this.sendNode(suc_queryNode);
                break;
                case Constants.P2P_CMD_RECEIVESUCCESSOR:
                break;
                case Constants.P2P_CMD_RECEIVEPREDECESSOR:
                    Node predecessor = (Node) ois.readObject();
                    Logger.info(ServerThread.class, curNode.toString()+" 's predecessor is "+predecessor.toString());
                    curNode.setPredecessor(predecessor);
                break;
                case Constants.P2P_CMD_GETPREDECESSOR:
                    this.sendNode(curNode.getPredecessor());
                break;
                case Constants.P2P_CMD_NOTIFY:
                    curNode.notifys((Node) ois.readObject());
                break;
                case Constants.P2P_CMD_FIXENTRY:
                    Node retNode=curNode.find_successor((Node) ois.readObject(),ois.readInt());
                    this.sendNode(retNode);
                break;
                case Constants.P2P_CMD_HEARTBEAT:
                    this.sendCode(Constants.P2P_CODE_ACK);
                break;
                case Constants.SUPER_CMD_REJOIN:
                    curNode.join();
                break;
                case Constants.DEMO_CMD_GETFT:
                    String str_ft=curNode.getStrFT();
                    this.sendInfo(str_ft);
                break;
                case Constants.DEMO_CMD_SHUTDOWN:
                    curNode.removeNode(curNode.getId());
                    System.exit(0);
                break;
            }

        }catch(Exception e){
            Logger.error(ServerThread.class, e.getMessage());
        }finally{
            try{
                if(this.ois!=null) this.ois.close();
			    if(this.oos!=null) this.oos.close();
                if(socket!=null) socket.close();
            }catch(Exception e){
                Logger.error(Socket.class, "socket close "+e.getMessage());
            }
        }
    }

}

public class SocketServer {
    
    private int _port;
    private PeerNode _node;



    public SocketServer(int port, PeerNode node){
        this._port=port;
        this._node=node;
    }
    public void start(){

        ServerSocket serverSocket=null;
        try{
           
            serverSocket = new ServerSocket(_port);
            Logger.info(SocketServer.class, _node.toString()+" is listening");
            //always accept new connection.
            while(true){
                Socket socket_cur=serverSocket.accept();
                Runnable r_cur=new ServerThread(socket_cur,this._node);
                Thread thread_cur=new Thread(r_cur, socket_cur.toString());
                thread_cur.start();
            }
        }catch(IOException e){
            Logger.error(SocketServer.class, e.getMessage());
        }finally{
            try{
                serverSocket.close();
            }catch(IOException e){
                Logger.error(ServerSocket.class, "serverSocket close "+e.getMessage());
            }
        }
    }
}