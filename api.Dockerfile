FROM gradle:7-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon -PmainClass=API

FROM amazoncorretto:17-alpine3.15
EXPOSE 8080:8080
ENV PORT=8080
RUN mkdir /app
ARG CQRS
COPY --from=build /home/gradle/src/build/libs/*.jar /app/anywallet-api.jar
COPY ./src/main/resources/$CQRS-application.conf /app/
RUN ln -s /app/$CQRS-application.conf /app/application.conf

ENTRYPOINT ["java","-jar","/app/anywallet-api.jar", "-config=/app/application.conf"]