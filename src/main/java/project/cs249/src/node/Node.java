package project.cs249.src.node;

import java.io.Serializable;

import project.cs249.src.util.Utils;

public class Node implements Serializable{
    
    private String _ip;
    private String _port;
    private String _timestamp; //in hex, act as initial identifier
    private int _id;

    public Node(String ip, String port) {
        this._ip = ip;
        this._port = port;
        this._timestamp = Utils.dateTimeToHex();
        this._id=-1;
    }

    public void setId(int id){
        this._id=id;
    }

    public int getId(){
        return this._id;
    }

    public String getTimestamp() {
        return this._timestamp;
    }

    public void setTimestamp(String _timestamp) {
        this._timestamp = _timestamp;
    }

    
    public String getIp() {
        return this._ip;
    }

    public void setIp(String _ip) {
        this._ip = _ip;
    }

    public String getPort() {
        return this._port;
    }

    public void setPort(String _port) {
        this._port = _port;
    }


    @Override
    public String toString() {
        return "{" +
            "_id='" + getId() + "'" +
            " _ip='" + getIp() + "'" +
            ", _port='" + getPort() + "'" +
            ", _timestamp='" + getTimestamp() + "'" +
            "}";
    }

}
