-- Har fjernet domener som ikke finnes lenger
update clusters set domain='Oera' where domain='InternSone';
update clusters set domain='OeraQ' where domain='PreProdIntern';
update clusters set domain='OeraT' where domain='TestInternLocal';

update clusters_aud set domain='Oera' where domain='InternSone';
update clusters_aud set domain='OeraQ' where domain='PreProdIntern';
update clusters_aud set domain='OeraT' where domain='TestInternLocal';

update resource_table set env_domain='Oera' where env_domain='InternSone';
update resource_table set env_domain='OeraQ' where env_domain='PreProdIntern';
update resource_table set env_domain='OeraT' where env_domain='TestInternLocal';

update resource_table_aud set env_domain='Oera' where env_domain='InternSone';
update resource_table_aud set env_domain='OeraQ' where env_domain='PreProdIntern';
update resource_table_aud set env_domain='OeraT' where env_domain='TestInternLocal';

