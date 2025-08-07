package no.nav.aura.envconfig.spring;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import no.nav.aura.envconfig.FasitRepository;
import no.nav.aura.envconfig.model.ModelEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@SpringJUnitConfig(classes = {SpringUnitTestConfig.class, SpringSecurityTestConfig.class})
@Transactional
public abstract class SpringTest {

    @Inject
    protected UserDetailsService inMemoryUserDetails;

    @Inject
    protected AuthenticationManager authenticationManager;

    @Inject
    protected FasitRepository unwrappedRepository;

    protected FasitRepository repository;
    
    @Inject 
    private TestingAuthenticationProvider testingAuthProvider;

    @BeforeAll
    static public void setupSystemProperties() {
        System.setProperty("ldap.url", "ldap://ldapgw.test.local");
        System.setProperty("ldap.domain", "test.local");
        System.setProperty("deployLog_v1.url", "http://somehost.com");
        System.setProperty("environment.name", "dev");
        System.setProperty("environment.class", "u");
    }

    
    @Inject
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(testingAuthProvider);

    }

    @BeforeEach
    public void authenticateRepository() {
        repository = SecurityByPass.wrapWithByPass(FasitRepository.class, unwrappedRepository);
    }

    public <T extends ModelEntity> T storeAs(String userName, String password, final T entity) {
        T storedEntity = runAsUser(userName, password, new Function<Void, T>() {
            public T apply(Void input) {
                return repository.store(entity);
            }

        });
        return storedEntity;
    }

    public <O> O runAsUser(String userName, String password, Function<Void, O> function) {
        Authentication authentication = createAuthentication(userName, password, getRolesForUser(userName), new HashSet<String>());
        return runAs(authentication, function);
    }
    
    public <O> O runAsUserWithGroup(String userName, String password, String group, Function<Void, O> function) {
        Authentication authentication = createAuthentication(userName, password, getRolesForUser(userName), Sets.newHashSet(group));
        return runAs(authentication, function);
    }

    private Collection<? extends GrantedAuthority> getRolesForUser(String username) {
        return inMemoryUserDetails.loadUserByUsername(username).getAuthorities();
    }

    protected Authentication createAuthentication(String username, String password, Collection<? extends GrantedAuthority> roles, Set<String> adGroups) {
        LdapUserDetails ldapUserDetails = createLdapPrincipal(username, adGroups);
        TestingAuthenticationToken n = new TestingAuthenticationToken(ldapUserDetails, password, Lists.newArrayList(roles));
        Authentication auth = authenticationManager.authenticate(n);
        auth.setAuthenticated(true);
        return auth;

    }

    protected static LdapUserDetails createLdapPrincipal(String username, Set<String> groups) {
        LdapUserDetailsImpl.Essence p = new LdapUserDetailsImpl.Essence();
        p.setUsername(username);
        p.setAuthorities(FluentIterable.from(groups).transform(new Function<String, GrantedAuthority>() {
            public GrantedAuthority apply(String o) {
                return new SimpleGrantedAuthority(o);
            }
        }).toList());
        p.setDn(username);
        return p.createUserDetails();
    }

    protected static <O> O runAs(Authentication authentication, Function<Void, O> function) {
        Authentication original = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            return function.apply(null);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(original);
        }
    }

}
