FROM gradle:7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon

FROM amazoncorretto:17-alpine3.15
EXPOSE 8000:8000
ENV PORT=8000
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/ktor-docker-sample.jar

ENTRYPOINT ["java","-jar","/app/ktor-docker-sample.jar"]