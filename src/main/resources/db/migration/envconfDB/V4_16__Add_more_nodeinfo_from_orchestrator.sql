alter table NODE add (
  dataCenter VARCHAR2(255),
  memorymb NUMBER(10),
  cpucount NUMBER(10)
);

alter table NODE_AUD add (
  dataCenter VARCHAR2(255),
  memorymb NUMBER(10),
  cpucount NUMBER(10)
);
