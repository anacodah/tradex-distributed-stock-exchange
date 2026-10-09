FROM maven:3.9.6-eclipse-temurin-21-alpine as build
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/
COPY common/src common/src
COPY distributed-node/pom.xml distributed-node/
COPY distributed-node/src distributed-node/src
COPY gateway/pom.xml gateway/

RUN mvn -pl common,distributed-node clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/distributed-node/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
