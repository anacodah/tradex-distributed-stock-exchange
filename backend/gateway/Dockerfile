FROM maven:3.9.6-eclipse-temurin-21-alpine as build
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/
COPY common/src common/src
COPY distributed-node/pom.xml distributed-node/
COPY gateway/pom.xml gateway/
COPY gateway/src gateway/src

RUN mvn -pl common,gateway clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/gateway/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
