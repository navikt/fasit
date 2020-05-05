CREATE TABLE additionalrevisioninfo
(
  revision           NUMBER(19) NOT NULL,
  author             VARCHAR2(255),
  dataorigin         VARCHAR2(255),
  message            VARCHAR2(255),
  modifiedentitytype VARCHAR2(255),
  timestamp          NUMBER(19) NOT NULL,
  CONSTRAINT pk_addrevinfo_revision PRIMARY KEY (revision)
);

CREATE TABLE application_aud
(
  entid      NUMBER(19) NOT NULL,
  rev        NUMBER(19) NOT NULL,
  revtype    NUMBER(19),
  artifactid VARCHAR2(255),
  groupid    VARCHAR2(255),
  app_name   VARCHAR2(255)
);

CREATE UNIQUE INDEX ix_app_aud ON application_aud(entid, rev);