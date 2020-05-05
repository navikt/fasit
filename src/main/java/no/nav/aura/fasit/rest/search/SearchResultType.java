package no.nav.aura.fasit.rest.search;

public enum SearchResultType {
        ENVIRONMENT,
        APPLICATION,
        CLUSTER,
        NODE,
        RESOURCE,
        APPCONFIG,
        INSTANCE,
        ALL;


        @Override
        public String toString() {
                return name().toLowerCase();
        }
}
