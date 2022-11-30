# chord
To compile the project, please run:
```
make
```
After compilation, the bin folder should appear.
To start the SuperNode, run below command and replace #m with an Integer that reprements the number of bits of the identifier. This Integer will denote the maximun PeerNodes allowed in the DHT with 2^m. The SuperNode runs on port 1099 in default.
```
java -cp bin/ project.cs249.src.node.SuperNode #m
```
Before running any PeerNodes, one must change the rmiregistry address (#ip_supernode) in the Config.java file under src\main\java\project\cs249\src\util\. This address should be the IP of the SuperNode and port number 1099. For example, 192.168.0.22:1099 in a private network.
```
public static final String ADDR_SUPERNODE="#ip_supernode:1099";
```
One can start a PeerNode with the command below after replacing the #ip and #port. #ip is the IP of this PeerNode itself and #port is the port that one would like to run on. One can run many PeerNodes as long as the total number of PeerNodes is below the limit of 2^m. Beware that the port numbers must be different for these PeerNodes.
```
java -cp bin project.cs249.src.node.PeerNode #ip #port
```
The Demo offers some interactive operations with the Distributed Hash Table. It can:
1. show all nodes in the ring and their preceding and succeeding peers.
2. print the finger table of a partcular PeerNode in the ring.
3. terminate a PeerNode. This can simulate a PeerNode has failed and its peers should update their finger tables.
```
java -cp bin project.cs249.src.node.Demo
```