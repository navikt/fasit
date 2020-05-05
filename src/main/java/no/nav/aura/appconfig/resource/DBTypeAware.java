package no.nav.aura.appconfig.resource;

public interface DBTypeAware {

     enum DBType {
        ORACLE, DB2
    }

    DBType getType();
}
