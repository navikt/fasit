FROM navikt/java:8

ADD nais/run-java.sh /run-java.sh

ADD src/main/webapp src/main/webapp

ADD target/apidocs target/apidocs

COPY target/fasit.jar /app/app.jar
