# StreamCombiner

How does it work:

    There is a server socket listening new connections till the value of -sockets parameter. I use blocking IO and there is one thread per open socket.
    You can stop it at any time writing STOP in any client connection. That will safely shutdown the application and it will close the current connections.
    To disconnect one client connection, you can write CLOSE
    All the valid inputs are stored in a sorted collection and this is the only point where we need to compare.
    This comparator uses a cache. In my tests with heavy load, only the 1/4 of all the comaparations are expensive because the other 3/4 part is coming from the cache.
    The output will wait to have at least 2 different timestamps of each input. Then it will pull to the output till the second last per input to guarantee that it will not receive repeated timestamps that were already sent out.
    There is a limit of the size to avoid overflow of memory, and stuck inputs. When the size of the collection is bigger than the limit, it will unregister the latest input.
    All the job is done in the current thread, so if there is any error we can report it back synchronously to the input. For example, if there is and error writing in the output it will be notified to the input in the same request.
    There are only 2 ways that the server can close the connections:
        IO errors or the input actively sent STOP or CLOSE
        The input is unregistered and sends something else (different than STOP or CLOSE).
    It is tolerated to send old timestamps or invalid inputs. The server will not process it and it will sent back an error. If this continues for a long time, finally is going to reach the limit and the point 7 will make a solution.
    I carefully did an abstraction in all the elements. You will not see any long, timestamps, amounts in the class StreamCombinerImpl. It would be quite easy to do other kind of stream comparations.
    I also did the tests, even concurrently.

To make the jar:
$ mvn clean package assembly:single

To run it:

$ java -jar target/streamcombiner-1.0-jar-with-dependencies.jar -port 25555 -out_file test.txt -sockets 5

Then you can connect for example running:

$ telnet localhost 25555


