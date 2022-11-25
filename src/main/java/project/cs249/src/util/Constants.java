package project.cs249.src.util;

public class Constants {
    public static final int RMI_CODE_HASH_ERROR=1;
    public static final int RMI_CODE_HASH_COLLISION=2;
    public static final int RMI_CODE_HASH_SUCCESS=3;
    public static final int RMI_CODE_REHASH_SUCCESS=4;
    public static final int RMI_CODE_SNODE_BUSY=5;

    public static final int P2P_CMD_JOIN=111;
    public static final int P2P_CMD_FINDSUCCESSOR = 112;
    public static final int P2P_CMD_RECEIVESUCCESSOR = 113;
    public static final int P2P_CMD_RECEIVEPREDECESSOR = 114;
    public static final int P2P_CMD_GETPREDECESSOR = 115;
    public static final int P2P_CMD_NOTIFY = 116;
    public static final int P2P_CMD_FIXENTRY = 117;
    public static final int P2P_CMD_HEARTBEAT = 118;

    public static final int P2P_CODE_ACK = 1111;

    public static final int SUPER_CMD_REJOIN = 11111;

    public static final int DEMO_CMD_GETFT = 111111;
    public static final int DEMO_CMD_SHUTDOWN = 111112;
    
}
