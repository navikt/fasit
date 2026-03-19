FROM azul/zulu-openjdk-alpine:17-latest

RUN apk update
RUN addgroup -S apprunner -g 1069 && adduser -S apprunner -G apprunner -u 1069

COPY --chown=apprunner:apprunner /run-java.sh /run-java.sh
COPY --chown=apprunner:apprunner target/fasit.jar /app/app.jar

ENV LC_ALL="nb_NO.UTF-8"
ENV LANG="nb_NO.UTF-8"
ENV TZ="Europe/Oslo"
ENV APP_BINARY=app
ENV APP_JAR=app.jar
ENV MAIN_CLASS="Main"

WORKDIR /app
USER apprunner
EXPOSE 8080

CMD ["sh", "/run-java.sh"]
