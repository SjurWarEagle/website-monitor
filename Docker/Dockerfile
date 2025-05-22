FROM maven:3.9.9-eclipse-temurin-17-alpine
WORKDIR /src
ADD . /src
RUN \
    cd client && mvn clean package \
    && mkdir -p /project \
    && cp -v /src/target/client-1.0-SNAPSHOT-jar-with-dependencies.jar /project/website-monitor.jar \
    && rm -rf /src \
    && ls -hal /project
WORKDIR /project
ENTRYPOINT ["java", "-jar", "/project/website-monitor.jar"]