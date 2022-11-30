# chord
To compile the project, please run:
```
make
```
After compilation, the bin folder should appear.
To start the SuperNode, run below command and replace #m with an Integer that reprements the number of bits of the identifier. This Integer will denote the maximun PeerNodes allowed in the DHT with 2^m.
```
java -cp bin/ project.cs249.src.node.SuperNode #m
```