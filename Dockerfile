FROM maven:3-eclipse-temurin-17-alpine AS build
COPY . /opt/
RUN mvn -f /opt/pom.xml clean package

FROM eclipse-temurin:17-jre-alpine
COPY --from=build /opt/target/serviceoffering-orchestrator-*.jar /opt/serviceoffering-orchestrator.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/opt/serviceoffering-orchestrator.jar"]