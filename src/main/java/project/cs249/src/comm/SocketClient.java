package project.cs249.src.comm;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import project.cs249.src.node.PeerNode;

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
			e.printStackTrace();
		}
		return ret;
	}

	public void shutdown(){
		try {
			if(this.socket!=null) this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public void sendCmd(int code) throws IOException {
		oos.writeInt(code);
		oos.flush();
	}
}
