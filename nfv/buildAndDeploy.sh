cd "${0%/*}" && mvn clean install -DskipTests && onos-app localhost reinstall! org.onosproject.nfv target/nfv-app-1.0-SNAPSHOT.oar
