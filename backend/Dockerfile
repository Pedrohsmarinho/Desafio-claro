# Build multi-stage: a imagem final so leva o JAR + JRE, sem Maven/JDK completo,
# o que reduz bastante o tamanho e a superficie da imagem publicada.

FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copia primeiro so o que define as dependencias, para o Docker cachear a
# camada de "mvnw dependency:go-offline" enquanto so o codigo-fonte muda.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
