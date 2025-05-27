Project Description:
-------------------

RingStore is a highly available, fault tolerant, eventually consistent storage solution that consists of multiple servers arranged in a ring structure. Following are the specifications of the project-

    Let there be seven data servers, S0, S1, . . . S6 and five clients, C0, C1, . . . , C4. The servers are connected in a cyclic manner, with server Si connected to servers S(i+1)!modulo 7 and S(i−1) modulo 7. When a client wishes to perform a read or write, it establishes a communication channel with a server chosen as per the description below. This client- server channel lasts for the duration it takes to complete the read or write operation. All communication channels are FIFO and reliable when they are operational: implemented using sockets. Occasionally, a channel may be disrupted in which case no message can be communicated across that channel. There exists a hash function, H, such that for each object, Ok, H(Ok) yields a value in the range 0 − 6.

    • Under normal (fault-free) circumstances, when a client, Ci has to insert/update an object, Ok, this operation must be performed at three servers numbered: H(Ok), H(Ok)+1 modulo 7, and H(Ok)+2 modulo 7. Instead of contacting all three replicas directly, the client establishes a channel with server identified by H(Ok) and sends its requested operation to that server. Then, that server, acting on behalf of the client, ensures that the insert/update operation is performed at the three replicas. When server H(Ok) completes the insert/update operation at the appropriate servers, it sends a response to the client at which point the client can terminate the connection with the server.

    • If server H(Ok) is not accessible from the client, the client then tries to access server H(Ok)+1 modulo 7, asking it to perform the update on the two live replicas. If neither of these two replicas are accessible from the client, the client should declare an error.

    • When a client, Cj has to read an object, Ok, it can read the value from any one of the three servers: H(Ok), H(Ok)+1 modulo 7, or H(Ok)+2 modulo 7. If the contacted server is down, the client tries one of the remaining servers. If all three servers that are supposed to host an object are down, the client should display an error message corresponding to the read operation.
    
    • The system should be able to simulate failure of a server, Si, as follows: Si terminates its communication channels with the two servers on either side of it in the cycle. When those two servers come to know of the failure (indicated by the termination of the communication channel), the one with the lower server id establishes a channel with the other with the higher server id, thus restoring the cycle. Similarly, a failed server can recover and rejoin the cycle by establishing links and sending join messages to the servers that would precede and succeed it in the cycle. Those two servers would then break the link they had between them. 
    
    • As part of recovery, when a server rejoins the cycle, it communicates with at most two servers before it in the cycle, and at most two servers after it in the cycle to obtain the latest replica of all the objects it is expected to store.

    • For the sake of simplicity, it is assumed that at any given time, at most one failure or recovery takes place; and no write operation is in progress during a failure or recovery.

Basic Code Structure:
--------------------
There are two packages present in this repository - 
    1. The CLIENT package that creates a pairwise connectivity between all the client nodes and elects a sequencer node. The client code currently assumes that the sequencer node does not send any messages of its own and that its only role is to sequence messages. However, most of the code for it to also send messages is in place and extending this functionality is trivial.

    2. The SERVER package that creates the ring structure, simulates failure, recovery, diff calculation and so on. The code creates 5 threads per server to handle the different possible functions of a server node. Server nodes can print the data they store, detach themselves from the ring to simulate failure and rejoin the ring.

    RingStore Servers can perform READ, WRITES and UPDATES.
        2.1. READ operations can go randomly to any of the three servers that can contain a key-value pair. This depends on the hash function and replication factor chosen.

        2.2. WRITE operations always go to the primary server (represented by H(Ok) in the specification), if present. On receiving a write request, the primary server writes it to its data storage and then relays the write request to the secondary node. The secondary server performs the same action as the primary and subsequently relays that request to the tertiary server. 

        2.3. UPDATE operations are just write operations where the key preexists. The value is overwritten in this case.

Compilation Instructions:
------------------------

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

- System properties files for the server and the client can be unified.

- Add a key-value pair deletion function. (trivial)

- Reduce the number of listener threads used on each server.

- Standardize Error reporting.

Dependencies:
------------
This project requires Java SE Development Kit (JDK) installed on your system. It was written to run with Java 7.

Contributing:
------------
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.