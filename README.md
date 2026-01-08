# Project Description:

RingStore is a highly available, fault-tolerant, eventually consistent key-value storage system implemented in Java. It consists of multiple storage servers arranged in a logical ring topology, supports replication, failure simulation, and recovery, and is designed to explore real-world distributed systems concepts such as replication, ordering, and consistency trade-offs.

## Architecture:
### Servers
  - 7 Data Servers (S0 to S6) connected in a logical ring. Server Si connects to S(i+1)modulo 7 and S(i−1) modulo 7.
  - Replication factor: 3.
  - Using consistent hash function **H**, an object Ok is stored on **H(Ok), H(Ok)+1 modulo 7, and H(Ok)+2 modulo 7.** The server H(Ok) acts as the ***primary replica*** for write operations.
  - Since no locking is implemented, the system provides **eventual consistency**.
 
### Clients
  - 5 clients (C0 … C4) issuing read and write requests.
  - One sequencer client orders all requests to totally order all requests.

### Operations
#### Writes:
- Clients send writes to the **primary replica**.
- Writes are forwarded sequentially to secondary and tertiary replicas. This preserves total ordering.
- If no live replica is reachable, the operation fails with an error.

#### Reads:
- Clients may read from ***any of the three replicas*** responsible for an object.
- If a contacted server is unreachable, the client retries with another replica.
- If all replicas are unavailable, the read fails with an error message.

![RingStore Architecture](ringstore-diagram.svg)


### Failure & Recovery Model
#### Server Failure
- On server failure, neighboring servers reconnect to preserve the ring.
- On recovery, the server rejoins and synchronizes replicas from nearby nodes.
- **Assumption:**
    - At most one failure or recovery occurs at a time.
    - No write operation is in progress during failure or recovery.

#### Server Recovery
- A recovering server Sr contacts its predecessor (Sq) and successor (Ss) in the ring to reconnect.
- On reestablishing connection with Sr, channels between **Sq** and **Ss** are closed.
- During recovery, the server Si synchronizes data by communicating with:
    - Up to two predecessors, S(i+1)modulo 7 and S(i+2)modulo 7
    - Up to two successors, S(i-1)modulo 7 and S(i-2)modulo 7
- This ensures the server retrieves the latest replicas it is responsible for storing.


Basic Code Structure:
--------------------
There are two packages present in this repository - 

1. The CLIENT package that creates a pairwise connectivity between all the client nodes and elects a sequencer node. The client code currently assumes that the sequencer node does not send any messages of its own and that its only role is to sequence messages. However, most of the code for it to also send messages is in place and extending this functionality is trivial.

2. The SERVER package that creates the ring structure, simulates failure, recovery, diff calculation and so on. The code creates 5 threads per server to handle the different possible functions of a server node. Server nodes can print the data they store, detach themselves from the ring to simulate failure and rejoin the ring.

   RingStore Servers can perform READ, WRITES and UPDATES.
    
   2.1. READ operations can go randomly to any of the three servers that can contain a key-value pair. This depends on the hash function and replication factor chosen.

   2.2. WRITE operations always go to the primary server (represented by H(Ok) in the specification), if present. On receiving a write request, the primary server writes it to its data storage and then relays the write request to the secondary node. The secondary server performs the same action as the primary and subsequently relays that request to the tertiary server. With this logic, a consistent total ordering is always present. However, since there is no locking implemented, the solution is an eventually consistent one. By implementing a version of two-phase locking across replicas, we can convert this system into a causally consistent solution.

   2.3. UPDATE operations are just write operations where the key preexists. The value is overwritten in this case.

Compilation Instructions:
------------------------
0. Modify the sysNodes.properties and serverNodes.Properties files to hold the IPs of the nodes that will be used.

1. Copy the RINGSTORE package to all the machines in the system and extract the package. This can be done by:

    1.1 Create a tarball of the package using 
            tar -uvf ringstore.tar ringstore/

    1.2 Copy the tarball using scp to all the nodes:
            scp -i <ssh-key> ringstore.tar <user>@<nodeName/IP>:~

    1.3 Extract the package in the home directory using
            tar -xvf ringstore.tar

    * Optional:
        scp can go wrong sometimes. A good practice to unsure that the package has not corrupted is to validate the checksum of the tarball at both the sender and the receiver.
        This can be done using the following:
            sha256sum <file>

2. Start the Server nodes by Sshing to all 7 server nodes at the same time. This can be done using terminals like iterm. Execute the following commands

    2.1 Compile the package using
            javac -d . *.java

    2.2 Run the package using
            java Server.RingManager | tee <nodename>.log

3. Start the Client by Sshing to all 5 nodes at the same time. This can be done using terminals like iterm. Execute the following commands

    3.1 Compile the package using
            javac -d . *.java

    3.2 Run the package using
            java Client.comDriver | tee <nodename>.log

4. Follow the instructions visible on the server and the client nodes to operate RingStore.

** RingStore currently has been validated to correctly simulate and recover from up to two consecutive failures (should not be concurrent).

Potential Improvements:
-------------------------
- The current implementation is not using VectorClocks despite the implementation already being present. This was deemed an unnecessary overhead for the initial implementation since versioning is not being done. However, extension to use the vector clock is trivial since most of the code is already present.

- The sequencer client currently cannot send messages to the servers. Its job is only to sequence. Extension for it to also send messages is trivial since most of the code is present already to do so.

- Implement two phase locking to convert this solution from an eventually consistent one to a causally consistent solution.

- Implement solutions to allow updates during node addition and deletion. 
  
- System properties files for the server and the client can be unified.

- Add a key-value pair deletion function. (trivial)

- Use a consistent hash function like Murmur3.

- Reduce the number of listener threads used on each server.

- Standardize Error reporting.

Dependencies:
------------
This project requires Java SE Development Kit (JDK) installed on your system. It was written to run with Java 7.

Contributing:
------------
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
