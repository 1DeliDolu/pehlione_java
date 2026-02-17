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

# MailHog (local SMTP)

docker run -d --name mailhog --restart unless-stopped -p 1025:1025 -p 8025:8025 mailhog/mailhog:v1.0.1

# UI

http://localhost:8025

# stop/start

docker stop mailhog
docker start mailhog

# note

Bu proje localde MySQL kullanir, Docker MSSQL kullanimi yoktur.
