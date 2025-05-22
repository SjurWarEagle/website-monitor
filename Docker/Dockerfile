FROM maven:3.9.9-eclipse-temurin-21
WORKDIR /src
ADD . /src
RUN \
    mvn clean package \
    && mkdir -p /project \
    && cp -v /src/target/twitch-stalker-1.0-SNAPSHOT-jar-with-dependencies.jar /project/twitch-stalker.jar \
    && rm -rf /src \
    && ls -hal /project
WORKDIR /project
ENTRYPOINT ["java", "-jar", "/project/twitch-stalker.jar"]