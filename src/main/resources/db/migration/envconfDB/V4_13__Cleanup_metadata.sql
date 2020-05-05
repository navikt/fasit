
create table deleteDetails (
  id  NUMBER(19) NOT NULL,
  entity number(19),
  entityType VARCHAR(255),
  nextActionDate  timestamp(6),
  issue VARCHAR(255)
);

create table deleteDetails_aud (
  id  NUMBER(19) NOT NULL,
  rev             NUMBER(19) NOT NULL,
  revtype         NUMBER(19),
  entity number(19),
  entityType VARCHAR(255),
  nextActionDate  timestamp(6),
  issue VARCHAR(255)
);

alter table node ADD 
(
	lifeCycleStatus VARCHAR(255),
    deletedetails_id  number(19)
);


alter table node_aud ADD 
(	
	lifeCycleStatus VARCHAR(255),
	deletedetails_id  number(19)
);



