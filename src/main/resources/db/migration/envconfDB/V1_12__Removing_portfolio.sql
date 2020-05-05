-- remove portfolio + references

alter table application drop column portfolio_entid;

drop table portfolio;