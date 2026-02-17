# start
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# alternative 
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev"

# Jar 

mvn -DskipTests package

java -jar target/pehlione-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# package 

mvn -U clean package

# dependency

mvn dependency:tree | grep mysql
