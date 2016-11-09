# StreamCombiner

To create the jar file run:
$ mvn clean package assembly:single

To execute the jar file run:
$ java -jar target/streamcombiner-1.0-jar-with-dependencies.jar {number of inputs}

To connect to the sockets you can do this:
$ cat examples/file0.log | telnet localhost 25555 & cat examples/file1.log | telnet localhost 25555