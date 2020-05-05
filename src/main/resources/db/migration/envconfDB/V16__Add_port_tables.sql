CREATE TABLE PORT (
  port_entid    NUMBER(19)    NOT NULL,
  appinst_entid NUMBER(19)    NOT NULL,
  hostname      VARCHAR2(255) NOT NULL,
  portnumber    NUMBER(19)    NOT NULL,
  type          VARCHAR2(255) NOT NULL,
  CONSTRAINT pk_appinst_port_entid PRIMARY KEY (port_entid),
  CONSTRAINT fk_appinst_port_appinst_entid FOREIGN KEY (appinst_entid) REFERENCES applicationinstance (entid)
);

CREATE TABLE PORT_AUD (
  port_entid NUMBER(19) NOT NULL,
  rev        NUMBER(19) NOT NULL,
  revtype    NUMBER(19) NOT NULL,
  hostname   VARCHAR2(255),
  portnumber NUMBER(19),
  type       VARCHAR2(255)
);

CREATE TABLE APPINST_PORTREF_AUD (
  port_entid    NUMBER(19) NOT NULL,
  appinst_entid NUMBER(19) NOT NULL,
  rev           NUMBER(19) NOT NULL,
  revtype       NUMBER(19) NOT NULL
);