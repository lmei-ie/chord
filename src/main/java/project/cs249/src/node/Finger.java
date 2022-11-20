package project.cs249.src.node;

public class Finger {
    private int lowerBound; // donates by (curNode Id+2^(i-1))%(2^m). successor Id must >= lowerBound
    private PeerNode successor;
    private int keyRangeStart;
    private int keyRangeEnd;


    public Finger(int lowerBound, PeerNode successor, int keyRangeStart, int keyRangeEnd) {
        this.lowerBound = lowerBound;
        this.successor = successor;
        this.keyRangeStart = keyRangeStart;
        this.keyRangeEnd = keyRangeEnd;
    }

    public Finger(int lowerBound, PeerNode successor) {
        this.lowerBound = lowerBound;
        this.successor = successor;
    }

    public Finger(int lowerBound) {
        this.lowerBound = lowerBound;
    }


    public int getDistance() {
        return this.lowerBound;
    }

    public void setDistance(int lowerBound) {
        this.lowerBound = lowerBound;
    }

    public PeerNode getSuccessor() {
        return this.successor;
    }

    public void setSuccessor(PeerNode successor) {
        this.successor = successor;
    }

    public int getKeyRangeStart() {
        return this.keyRangeStart;
    }

    public void setKeyRangeStart(int keyRangeStart) {
        this.keyRangeStart = keyRangeStart;
    }

    public int getKeyRangeEnd() {
        return this.keyRangeEnd;
    }

    public void setKeyRangeEnd(int keyRangeEnd) {
        this.keyRangeEnd = keyRangeEnd;
    }

    @Override
    public String toString(){
        return lowerBound+" "+successor.toString()+" ("+keyRangeStart+","+keyRangeEnd+")";
        
    }
}
