package no.nav.aura.envconfig.client;

import org.junit.jupiter.api.Test;

import no.nav.aura.envconfig.model.infrastructure.Domain;
import no.nav.aura.envconfig.model.infrastructure.PlatformType;
import no.nav.aura.envconfig.model.resource.ResourceType;

public class DOEnumConverterTest {
    
    @Test
    public void convertResoureTypeFromDO(){
        for (ResourceTypeDO type : ResourceTypeDO.values()) {
            ResourceType.valueOf(type.name());
        }
    }

    @Test
    public void convertResourceTypeDOFromEnum(){
        for (ResourceType type : ResourceType.values()) {
            ResourceTypeDO.valueOf(type.name());
        }
    }
    
    @Test
    public void convertDomainsFromDO(){
        for (DomainDO domainDO : DomainDO.values()) {
            Domain.valueOf(domainDO.name());
        }
    }
    
    @Test
    public void convertDomainDOsFromEnum(){
        for (Domain domain : Domain.values()) {
            DomainDO.valueOf(domain.name());
        }
    }
    
    
    @Test
    public void convertPlatformTypeFromDO(){
        for (PlatformTypeDO type : PlatformTypeDO.values()) {
            PlatformType.valueOf(type.name());
        }
    }

    @Test
    public void convertPlatformTypeDOFromEnum(){
        for (PlatformType type : PlatformType.values()) {
            PlatformTypeDO.valueOf(type.name());
        }
    }
   


}
