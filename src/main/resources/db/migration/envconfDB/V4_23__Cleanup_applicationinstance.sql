alter table APPLICATIONINSTANCE ADD 
(
	lifeCycleStatus VARCHAR(255),
    deletedetails_id  number(19)
);


alter table APPLICATIONINSTANCE_AUD ADD 
(	
	lifeCycleStatus VARCHAR(255),
	deletedetails_id  number(19)
);



