#!/bin/bash
# Metrics
echo "environment.name=$ENVIRONMENT_NAME" >> config.properties
echo "environment.class=$ENVIRONMENT_CLASS" >> config.properties

# Datasource envconfDB
echo "envconfDB.url=$ENVCONFDB_URL" >> config.properties
echo "envconfDB.username=$(cat /secrets/oracle/envconfDB/username)" >> config.properties
echo "envconfDB.password=$(cat /secrets/oracle/envconfDB/password)" >> config.properties

# LDAP ldap
echo "ldap.user.basedn=$LDAP_USER_BASEDN" >> config.properties
echo "ldap.serviceuser.basedn=$LDAP_SERVICEUSER_BASEDN" >> config.properties
echo "ldap.domain=$LDAP_DOMAIN" >> config.properties
echo "ldap.basedn=$LDAP_BASEDN" >> config.properties
echo "ldap.url=$LDAP_URL" >> config.properties
echo "ldap.username=$(cat /secrets/ldap/ldap/username)" >> config.properties
echo "ldap.password=$(cat /secrets/ldap/ldap/password)" >> config.properties

# Credential fasit.encryptionkeys
echo "fasit.encryptionkeys.username=$(cat /secrets/certificate/fasit-keystore/keystorealias)" >> config.properties
echo "fasit.encryptionkeys.password=$(cat /secrets/certificate/fasit-keystore/keystorepassword)" >> config.properties
echo "fasit.encryptionkeys.path=/secrets/certificate/fasit-keystore/keystore.jceks" >> config.properties
cat /secrets/certificate/fasit-keystore/keystore | base64 -d > /secrets/certificate/fasit-keystore/keystore.jceks

# Credential srvfasit
echo "systemuser.srvfasit.username=$(cat /secrets/credential/srvfasit/username)" >> config.properties
echo "systemuser.srvfasit.password=$(cat /secrets/credential/srvfasit/password)" >> config.properties

# BaseUrl vault
echo "vault.url=$VAULT_URL" >> config.properties

# Rest basta_rest_api
echo "basta.url=$BASTA_URL" >> config.properties

# Rest oracle_v1
echo "oracle_v1.url=$ORACLE_V1_URL" >> config.properties

# Rest deployLog_v1
echo "deployLog_v1.url=$DEPLOYLOG_V1_URL" >> config.properties

# RoleMapping fasit.operations
echo "ROLE_OPERATIONS.groups=$ROLE_OPERATIONS_GROUPS" >> config.properties

# RoleMapping fasit.selfservice
echo "ROLE_SELFSERVICE.groups=$ROLE_SELFSERVICE_GROUPS" >> config.properties

# RoleMapping fasit.prodoperations
echo "ROLE_PROD_OPERATIONS.groups=$ROLE_PROD_OPERATIONS_GROUPS" >> config.properties

# RoleMapping fasit.selfservice_prod
echo "ROLE_SELFSERVICE_PROD.groups=$ROLE_SELFSERVICE_PROD_GROUPS" >> config.properties

# RoleMapping fasit.superuser
echo "ROLE_SUPERUSER.groups=$ROLE_SUPERUSER_GROUPS" >> config.properties

# Feature toggle for Kafka integration
echo "publish.deployment.events.to.kafka=$PUBLISH_DEPLOYMENT_EVENTS_TO_KAFKA" >> config.properties

#KafkaConfig
echo "kafka.servers=$KAFKA_SERVERS" >> config.properties
echo "kafka.sasl.enabled=$KAFKA_SASL_ENABLED" >> config.properties
echo "kafka.username=$(cat /secrets/credential/fasit.kafka.user/USERNAME)" >> config.properties
echo "kafka.password=$(cat /secrets/credential/fasit.kafka.user/PASSWORD)" >> config.properties
echo "kafka.deployment.event.topic=$KAFKA_DEPLOYMENT_EVENT_TOPIC" >> config.properties

# Start the server
exec java ${DEFAULT_JVM_OPTS} ${JAVA_OPTS} -jar /app/app.jar ${RUNTIME_OPTS} $@
