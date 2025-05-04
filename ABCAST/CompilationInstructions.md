Compilation Instructions:
------------------------

1. Copy the totalbroadcast package to all the machines to run to communication on and extract the package. This can be done by:
    1.1 Create a tarball of the package using 
            tar -uvf totalbroadcast.tar totalbroadcast/
    1.2 Copy the tarball using scp to all the nodes:
            scp -i <ssh-key> totalbroadcast.tar <user>@dc<nodeNo>.utdallas.edu:~
    1.3 Extract the package in the home directory using
            tar -xvf totalbroadcast.tar
    * Optional:
        scp can go wrong sometimes. A good practice to unsure that the package has not corrupted is to validate the checksum of the tarball at both the sender and the receiver.
        This can be done using the following:
            sha256sum <file>

2. Ssh to all 4 nodes at the same time. This can be done using terminals like iterm. Execute the following commands
    2.1 Compile the package using
            javac -d . *.java
    2.2 Run the package using
            java totalbroadcast.comDriver | tee <nodename>.log

3. To verify the causal ordering, collect the logfile you want to verify and run the message verifier.
    3.1 Compile the verifier using
            javac MessageVerifier.java
    3.2 Run the verifier using
            java MessageVerifier

4. To verify the total ordering across all the logfiles, place them all in the same folder and run the abVerifier.
    4.1 Compile the verifier using
            javac abVerifier.java
    4.2 Run the verifier using
            java abVerifier <path to log folder>

Footnote:
--------
 To manually view all the delivered messages only (VSCode Regex formula):
        Run find with the following regex - ^(?!Delivered:\s\d+(?:,\d+)*:Message\sno\.\d+\sfrom\s\d+$).*
