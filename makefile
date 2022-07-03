main:
	javac ./com/Server.java 
	javac ./com/Info.java	
	javac ./com/LamportsClock.java
	javac ./com/MessageParser.java
	javac ./com/Mutex.java 
	javac ./com/Client.java
	gcc runner.c -o runner
	gcc check.c -o check
	sh initFiles.sh
	./runner
	./check

