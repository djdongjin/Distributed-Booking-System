all: java.policy compile-client

java.policy: FORCE
	@echo "Creating client java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "\tpermission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

../Server/RMIInterface.jar: ../Server/Server/Interface/IResourceManager.java
	make -C ../Server/ RMIInterface.jar

Command.jar: Client/Command.java
	@echo "Compiling Command class"
	javac Client/Command.java
	jar cvf Command.jar Client/Command.class

compile-client: ../Server/RMIInterface.jar
	javac -cp ../Server/RMIInterface.jar Client/*.java

clean:
	rm -f Client/*.class
	rm -f ../Server/RMIInterface.jar
	rm -f java.policy
