FROM gradle:7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon

FROM amazoncorretto:17-alpine3.15
EXPOSE 8080:8080
ENV PORT=8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/anywallet-api.jar

ENTRYPOINT ["java","-jar","/app/anywallet-api.jar"]