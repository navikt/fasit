package no.nav.aura.envconfig.filter;

import no.nav.aura.envconfig.auditing.NavUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.Hashtable;

public class LdapUserLookup implements UserLookup {
    private static final Logger log = LoggerFactory.getLogger(LdapUserLookup.class);

    private final String ldapSearchBase;
    final String bindUser;
    final String bindUserPassword;
    final String ldapUrl;

    private Hashtable<String, Object> env;

    @SuppressWarnings("restriction")
    public LdapUserLookup(Environment springEnv) {
        // System.setProperty("com.sun.jndi.ldap.connect.pool.debug", "fine");
        System.setProperty("com.sun.jndi.ldap.connect.pool.protocol", "ssl plain");
        System.setProperty("com.sun.jndi.ldap.connect.pool.maxsize", "20");
        System.setProperty("com.sun.jndi.ldap.connect.pool.timeout", "300000");

        ldapSearchBase = getMandatoryLdapProperty(springEnv, "ldap.user.basedn");
        bindUser = getMandatoryLdapProperty(springEnv, "ldap.username");
        bindUserPassword = getMandatoryLdapProperty(springEnv, "ldap.password");
        ldapUrl = getMandatoryLdapProperty(springEnv, "ldap.url");
        log.info("Binding to LDAP {} with user {}", ldapUrl, bindUser);
        env = new Hashtable<String, Object>();
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, bindUser);
        env.put(Context.SECURITY_CREDENTIALS, bindUserPassword);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.INITIAL_CONTEXT_FACTORY, com.sun.jndi.ldap.LdapCtxFactory.class.getName());
        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put("com.sun.jndi.ldap.connect.pool", "true");

    }

    private String getMandatoryLdapProperty(Environment env, String propertyName) {
        String property = env.getProperty(propertyName);
        if (property == null) {
            throw new IllegalArgumentException("Missing propery " + propertyName + " in  ldap configuration");
        }
        return property;

    }

    @Override
    public NavUser lookup(String id) {
        LdapContext ldapCtx = null;
        try {
            ldapCtx = new InitialLdapContext(env, new Control[0]);
            NavUser user = new NavUser(id);
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            // Support search for both AD ident (x123456) and UPN first@last.nav.no
            final String searchAttribute = id.contains("@") ? "userprincipalname" : "cn";

            NamingEnumeration<SearchResult> searchResults = ldapCtx.search(ldapSearchBase, String.format("(%s=%s)", searchAttribute, id), searchControls);
            if (searchResults.hasMore()) {
                user.setExists(true);
                SearchResult searchResult = searchResults.next();
                user.setName(searchResult.getAttributes().get("givenname").get().toString(), searchResult.getAttributes().get("sn").get().toString());
            }
            searchResults.close();
            ldapCtx.close();
            return user;
        } catch (NamingException e) {
            log.error("Error looking up user {} in ldap: {} ", id, e.getMessage());
            throw new RuntimeException(e);
        } finally {
            try {
                if (ldapCtx != null) {
                    ldapCtx.close();
                }
            } catch (NamingException e) {
                throw new RuntimeException("Error closing ldap context");
            }
        }
    }
}
