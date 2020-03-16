cd /home/felix/Desktop/onos
source set_java.sh
cd tools/dev
source bash_profile
cd /home/felix/Desktop/toposync/toposync-app
mvn clean install 
onos-app localhost reinstall! hiwi.tm.toposync-app target/toposync-app-1.0-SNAPSHOT.oar
