JC = javac
JFLAGS = -g
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	./message/HandShake.java \
	./message/ActualMessage.java \
	./fileProcess/FileProcess.java \
	./configuration/Common.java \
	./configuration/PeerInfo.java \
	./fileShare/Neighbor.java \
	./fileShare/P2P.java \
	./fileShare/Choke.java \
	./fileShare/Host.java \
	./fileShare/SyncInfo.java \
	PeerProcess.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
	$(RM) ./fileShare/*.class
	$(RM) ./fileProcess/*.class
	$(RM) ./fileShare/*.class
	$(RM) ./message/*.class