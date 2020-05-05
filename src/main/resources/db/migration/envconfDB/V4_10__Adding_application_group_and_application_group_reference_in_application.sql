CREATE TABLE APPLICATION_GROUP
(
  entid      NUMBER(19) NOT NULL,
  group_name VARCHAR2(255),
  created    TIMESTAMP(6),
  createdby  VARCHAR2(255 CHAR),
  dataorigin VARCHAR2(255 CHAR),
  updated    TIMESTAMP(6),
  updatedby  VARCHAR2(255 CHAR),
  CONSTRAINT pk_application_group_entid PRIMARY KEY (entid)
);


ALTER TABLE APPLICATION ADD
(
port_offset NUMBER(10),
app_group_id NUMBER(19),
CONSTRAINT fk_application_app_group_id FOREIGN KEY (app_group_id) REFERENCES application_group (entid)
);

UPDATE APPLICATION
SET port_offset = 0
WHERE port_offset IS NULL;

ALTER TABLE APPLICATION_AUD ADD
(
port_offset NUMBER(10)
);

CREATE TABLE APPLICATION_GROUP_AUD
(
  entid      NUMBER(19) NOT NULL,
  rev        NUMBER(19) NOT NULL,
  revtype    NUMBER(19),
  group_name VARCHAR2(255)
);

CREATE TABLE APP_GROUP_APPLICATION_AUD (
  entid             NUMBER(19) NOT NULL,
  rev               NUMBER(19) NOT NULL,
  revtype           NUMBER(19),
  app_group_id      NUMBER(19),
  application_entid NUMBER(19)
);

CREATE UNIQUE INDEX ix_app_grp_aud ON application_group_aud (entid, rev);
