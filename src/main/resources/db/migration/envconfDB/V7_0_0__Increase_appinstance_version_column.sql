-- Somebody's using branch names as version
ALTER TABLE applicationinstance MODIFY(version VARCHAR2(4000));
