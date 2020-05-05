FROM navikt/java:8

COPY nais/run-java.sh /run-java.sh

COPY src/main/webapp src/main/webapp

COPY target/apidocs target/apidocs

COPY target/fasit.jar /app/app.jar
