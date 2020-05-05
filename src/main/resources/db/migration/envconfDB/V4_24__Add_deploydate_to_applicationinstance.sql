alter table APPLICATIONINSTANCE ADD 
(
	 deployDate  timestamp(6)
);


alter table APPLICATIONINSTANCE_AUD ADD 
(	
	 deployDate  timestamp(6)
);

-- Set deploydate initially to last updated
UPDATE APPLICATIONINSTANCE
SET deployDate = updated;



