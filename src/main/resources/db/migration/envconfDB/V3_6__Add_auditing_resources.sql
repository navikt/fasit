CREATE TABLE resource_table_aud
  (
    resourcetype    VARCHAR2(255) NOT NULL,
    entid           NUMBER(19) NOT NULL,
    rev             NUMBER(19) NOT NULL,
    revtype         NUMBER(19),
    resource_alias  VARCHAR2(255),
    env_domain      VARCHAR2(255),
    envclass        VARCHAR2(255),
    environmentname VARCHAR2(255),
    resource_type   VARCHAR2(255)
  );

CREATE UNIQUE INDEX ix_res_aud ON resource_table_aud(entid, rev);

CREATE TABLE resource_properties_aud
  (
    rev                    NUMBER(19) NOT NULL,
    propertyresource_entid NUMBER(19) NOT NULL,
    property_key           VARCHAR2(255) NOT NULL,
    property_value         VARCHAR2(255),
    revtype                NUMBER(19)
  );

CREATE UNIQUE INDEX ix_res_prop_aud ON resource_properties_aud(propertyresource_entid, property_value, property_key, rev);