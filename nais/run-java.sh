#!/bin/bash
# Metrics

CONFIG_DIR=/tmp/config
mkdir -p $CONFIG_DIR
export CLASSPATH=$CONFIG_DIR:$CLASSPATH

echo "environment.name=$ENVIRONMENT_NAME" >> $CONFIG_DIR/config.properties
echo "environment.class=$ENVIRONMENT_CLASS" >> $CONFIG_DIR/config.properties

# Datasource envconfDB
echo "envconfDB.url=$ENVCONFDB_URL" >> $CONFIG_DIR/config.properties
echo "envconfDB.username=$(cat /secrets/oracle/envconfDB/username)" >> $CONFIG_DIR/config.properties
echo "envconfDB.password=$(cat /secrets/oracle/envconfDB/password)" >> $CONFIG_DIR/config.properties

# LDAP ldap
echo "ldap.user.basedn=$LDAP_USER_BASEDN" >> $CONFIG_DIR/config.properties
echo "ldap.serviceuser.basedn=$LDAP_SERVICEUSER_BASEDN" >> $CONFIG_DIR/config.properties
echo "ldap.domain=$LDAP_DOMAIN" >> $CONFIG_DIR/config.properties
echo "ldap.basedn=$LDAP_BASEDN" >> $CONFIG_DIR/config.properties
echo "ldap.url=$LDAP_URL" >> $CONFIG_DIR/config.properties
echo "ldap.username=$(cat /secrets/ldap/ldap/username)" >> $CONFIG_DIR/config.properties
echo "ldap.password=$(cat /secrets/ldap/ldap/password)" >> $CONFIG_DIR/config.properties

# Credential fasit.encryptionkeys
echo "fasit.encryptionkeys.username=$(cat /secrets/certificate/fasit-keystore/keystorealias)" >> $CONFIG_DIR/config.properties
echo "fasit.encryptionkeys.password=$(cat /secrets/certificate/fasit-keystore/keystorepassword)" >> $CONFIG_DIR/config.properties
echo "fasit.encryptionkeys.path=$CONFIG_DIR/keystore.jceks" >> $CONFIG_DIR/config.properties
cat /secrets/certificate/fasit-keystore/keystore | base64 -d > $CONFIG_DIR/keystore.jceks

# Credential srvfasit
echo "systemuser.srvfasit.username=$(cat /secrets/credential/srvfasit/username)" >> $CONFIG_DIR/config.properties
echo "systemuser.srvfasit.password=$(cat /secrets/credential/srvfasit/password)" >> $CONFIG_DIR/config.properties

# BaseUrl vault
echo "vault.url=$VAULT_URL" >> $CONFIG_DIR/config.properties

# Rest deployLog_v1
echo "deployLog_v1.url=$DEPLOYLOG_V1_URL" >> $CONFIG_DIR/config.properties

# RoleMapping fasit.operations
echo "ROLE_OPERATIONS.groups=$ROLE_OPERATIONS_GROUPS" >> $CONFIG_DIR/config.properties

# RoleMapping fasit.prodoperations
echo "ROLE_PROD_OPERATIONS.groups=$ROLE_PROD_OPERATIONS_GROUPS" >> $CONFIG_DIR/config.properties

# RoleMapping fasit.superuser
echo "ROLE_SUPERUSER.groups=$ROLE_SUPERUSER_GROUPS" >> $CONFIG_DIR/config.properties

# Kafka configuration from Kafkarator
echo "kafka.servers=$KAFKA_BROKERS" >> $CONFIG_DIR/config.properties
echo "kafka.credstore.password=$KAFKA_CREDSTORE_PASSWORD" >> $CONFIG_DIR/config.properties
echo "kafka.keystore.path=$KAFKA_KEYSTORE_PATH" >> $CONFIG_DIR/config.properties
echo "kafka.truststore.path=$KAFKA_TRUSTSTORE_PATH" >> $CONFIG_DIR/config.properties
echo "kafka.deployment.event.topic=$KAFKA_DEPLOYMENT_EVENT_TOPIC" >> $CONFIG_DIR/config.properties

# Start the server
exec java ${DEFAULT_JVM_OPTS} ${JAVA_OPTS} -jar /app/app.jar ${RUNTIME_OPTS} $@
