FROM gradle:7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon -PmainClass=RMU

FROM amazoncorretto:17-alpine3.15
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/anywallet-task.jar

ENTRYPOINT ["java","-jar","/app/anywallet-task.jar"]