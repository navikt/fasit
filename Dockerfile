FROM navikt/java:17

COPY nais/run-java.sh /run-java.sh

COPY target/fasit.jar /app/app.jar
