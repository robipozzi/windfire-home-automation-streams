FROM openjdk:17
LABEL maintainer="Roberto Pozzi <r.robipozzi@gmail.com>"
LABEL version="1.0"
LABEL description="Windfire Kafka Streams application"
COPY target/windfire-home-automation-streams-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]