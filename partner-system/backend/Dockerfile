# Build FastShip backend cho Render (Root Directory = partner-system/backend, Runtime = Docker)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# gio Viet Nam cho LocalDateTime (moc hanh trinh)
ENV JAVA_OPTS="-Duser.timezone=Asia/Ho_Chi_Minh -XX:MaxRAMPercentage=75"
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
