cd "${0%/*}" && mvn clean install && onos-app localhost reinstall! org.onosproject.statistics target/linkstatistics-app-1.0-SNAPSHOT.oar
