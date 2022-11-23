package project.cs249.src.node;

import java.rmi.*;
import java.util.ArrayList;

public interface SuperNodeRMI extends Remote{
    //public String getLiveInfo(String timestamp, String ip, String port) throws RemoteException;
    public String getNodeInfo(Node node) throws RemoteException;
    public void ackRegister(Node node) throws RemoteException;
    public Node getRamdonNode(int id) throws RemoteException;
    public void removeNode(int id) throws RemoteException;

    //these are for demo only
    public ArrayList<Integer> getIdList() throws RemoteException;
    public Node getNode(int id) throws RemoteException;
}
