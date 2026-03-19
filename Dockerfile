#FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-17
#ENV TZ="Europe/Oslo"
#COPY target/fasit.jar /app/app.jar
#CMD ["-jar","app.jar"]

FROM azul/zulu-openjdk-alpine:17-latest

RUN apk update
RUN addgroup -S apprunner -g 1069 && adduser -S apprunner -G apprunner -u 1069

COPY --chown=apprunner:apprunner /run-java.sh /run-java.sh
COPY --chown=apprunner:apprunner target/fasit.jar /app/app.jar
#COPY entrypoint.sh /entrypoint.sh

ENV LC_ALL="nb_NO.UTF-8"
ENV LANG="nb_NO.UTF-8"
ENV TZ="Europe/Oslo"
ENV APP_BINARY=app
ENV APP_JAR=app.jar
ENV MAIN_CLASS="Main"

WORKDIR /app
USER apprunner
EXPOSE 8080

#ENTRYPOINT ["/entrypoint.sh"]
CMD ["sh", "/run-java.sh"]
