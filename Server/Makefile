all: java.policy compile-server-tcp

java.policy: FORCE
	@echo "Creating server java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "\tpermission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

../Client/Command.jar: ../Client/Client/Command.java
    make -C ../Client/Command.jar

RMIInterface.jar: Server/Interface/IResourceManager.java
	@echo "Compiling RMI server interface"
	javac Server/Interface/IResourceManager.java
	jar cvf RMIInterface.jar Server/Interface/IResourceManager.class

compile-server-rmi: Server/RMI/*.java Server/Interface/IResourceManager.java Server/Common/*.java
	javac Server/RMI/*.java Server/Interface/IResourceManager.java Server/Common/*.java

compile-server-tcp: ../Client/Command.jar Server/TCP/*.java Server/Interface/IResourceManager.java Server/Common/*.java
	javac -cp ../Client/Command.jar Server/TCP/*.java Server/Interface/IResourceManager.java Server/Common/*.java



clean:
	rm -f Server/Interface/*.class Server/Common/*.class Server/RMI/*.class
	rm -f RMIInterface.jar
	rm -f java.policy 
