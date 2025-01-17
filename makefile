main:
	rm ./com/Client.class
	rm ./com/Generator.class
	rm ./com/Info.class
	rm ./com/LamportsClock.class
	rm ./com/MessageParse.class
	rm ./com/MessageParser.class
	rm ./com/Request.class
	rm ./com/ServerInfo.class
	rm ./com/Server.class
	rm ./com/ServerQueue.class
	rm ./com/ServerService.class
	rm ./com/ServerToServerService.class
	rm ./com/State.class
	rm ./com/Utils.class
	javac ./com/Server.java 
	javac ./com/Info.java	
	javac ./com/LamportsClock.java
	javac ./com/MessageParser.java
	javac ./com/Client.java
	javac ./com/Utils.java
	gcc runner.c -o runner
	gcc check.c -o check
	sh initFiles.sh
	./runner
	./check
	sh reset.sh