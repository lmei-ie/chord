package project.cs249.src.comm;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import project.cs249.src.node.Node;
import project.cs249.src.node.PeerNode;
import project.cs249.src.util.Logger;



class ServerThread implements Runnable{
    private Socket socket;
    private PeerNode curNode;
    public ServerThread(Socket socket, PeerNode node){
        this.socket=socket;
        this.curNode=node;
    }
    @Override
    public void run() {
        ObjectInputStream ois=null;
        try{
            ois=new ObjectInputStream(this.socket.getInputStream());
            int code = ois.readInt();
			PeerNode peerNode = (PeerNode) ois.readObject();
            Logger.info(ServerThread.class, "Received " + peerNode.toString());
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            try{
                if(socket!=null) socket.close();
            }catch(Exception e){
                e.printStackTrace();
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
            e.printStackTrace();
        }finally{
            try{
                serverSocket.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}