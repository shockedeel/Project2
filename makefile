main:
	javac ./com/Server.java 
	javac ./com/Info.java	
	javac ./com/LamportsClock.java
	javac ./com/MessageParser.java
	javac ./com/Mutex.java 
	javac ./com/Client.java
	gcc runner.c	
	sh initFiles.sh
