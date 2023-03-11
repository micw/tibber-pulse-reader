FROM openjdk:17-alpine

ADD target/tibber-pulse-reader-1.0.0-SNAPSHOT.jar /tibber-pulse-reader.jar

ENTRYPOINT ["java","-jar","/tibber-pulse-reader.jar"]
