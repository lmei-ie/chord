package project.cs249.src.comm;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

import project.cs249.src.node.PeerNode;
import project.cs249.src.util.Logger;

public class SocketClient {
    private ObjectOutputStream oos;
	private Socket socket;
	private ObjectInputStream ois;

    public SocketClient(String str_ip, String str_port) throws IOException {
        this.socket=new Socket(str_ip, Integer.parseInt(str_port));
        oos=new ObjectOutputStream(this.socket.getOutputStream());
		ois=new ObjectInputStream(this.socket.getInputStream());
	}


    public void sendNode(int code, PeerNode node) throws IOException {
		oos.writeInt(code);
		oos.writeObject(node);
		oos.flush();
	}

	public void sendNodeAndKey(int code, PeerNode node, int key) throws IOException {
		oos.writeInt(code);
		oos.writeObject(node);
		oos.writeInt(key);
		oos.flush();
	}

    public void sendKey(int code, int key) throws IOException {
		oos.writeInt(code);
		oos.writeInt(key);
		oos.flush();
	}

	public PeerNode readReturnNode(){
		PeerNode ret=null;
		try{
			ret=(PeerNode)ois.readObject();
		}catch(Exception e){
			Logger.error(SocketClient.class, "readReturnNode "+e.getMessage());
		}
		return ret;
	}

	public int readCode(){
		int ret=-1;
		try{
			ret=ois.readInt();
		}catch(Exception e){
			Logger.error(SocketClient.class, "readCode "+e.getMessage());
		}
		return ret;
	}

	public void shutdown(){
		try {
			if(this.ois!=null) this.ois.close();
			if(this.oos!=null) this.oos.close();
			if(this.socket!=null) this.socket.close();
		} catch (IOException e) {
			Logger.error(SocketClient.class, "shutdown "+e.getMessage());
		}
	}


	public void sendCmd(int code) throws IOException {
		oos.writeInt(code);
		oos.flush();
	}

	public void setTimeout(int timeout) throws SocketException{

		this.socket.setSoTimeout(timeout);

	}
}
