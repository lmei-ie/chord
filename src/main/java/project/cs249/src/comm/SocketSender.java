package project.cs249.src.comm;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import project.cs249.src.node.PeerNode;

public class SocketSender {
    private ObjectOutputStream oos;
	private Socket socket;


    public SocketSender(String str_ip, String str_port) throws IOException {
        this.socket=new Socket(str_ip, Integer.parseInt(str_port));
        oos=new ObjectOutputStream(this.socket.getOutputStream());
    }


    public void sendNode(int code, PeerNode node) throws IOException {
		oos.writeInt(code);
		oos.writeObject(node);
		oos.flush();
		socket.shutdownOutput();
	}

    public void sendKey(int code, int key) throws IOException {
		oos.writeInt(code);
		oos.writeInt(key);
		oos.flush();
		socket.shutdownOutput();
	}
}
