FROM gradle:6.7.1-jdk8 AS build
WORKDIR /app
COPY --chown=gradle:gradle . /app
RUN gradle clean build --no-daemon

FROM openjdk:8
COPY --from=build /app/out/bin/ /tmp
WORKDIR /tmp
ENTRYPOINT ["java","-jar","Beadocker gle.jar"]