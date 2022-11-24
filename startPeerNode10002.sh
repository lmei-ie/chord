#!/bin/bash
ip='localhost'
port='10002'
echo 'start a peerNode. Address is '${ip}':'${port}
java -cp bin/ project.cs249.src.node.PeerNode ${ip} ${port}