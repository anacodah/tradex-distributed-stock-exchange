FROM maven:3.9.6-eclipse-temurin-21-alpine as build
WORKDIR /app
COPY pom.xml .
COPY distributed-node/pom.xml distributed-node/
COPY gateway/pom.xml gateway/
COPY distributed-node/src distributed-node/src

RUN mvn -pl distributed-node clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/distributed-node/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
