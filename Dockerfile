FROM maven:3-eclipse-temurin-17-alpine AS build
COPY . /opt/
RUN --mount=type=secret,id=GIT_AUTH_TOKEN env GITHUB_TOKEN=$(cat /run/secrets/GIT_AUTH_TOKEN) mvn -ntp -f /opt/pom.xml -s /opt/settings.xml clean package

FROM eclipse-temurin:17-jre-alpine
COPY --from=build /opt/target/serviceoffering-orchestrator-*.jar /opt/serviceoffering-orchestrator.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/opt/serviceoffering-orchestrator.jar"]