package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlDatasource extends Datasource {

    public XmlDatasource() {
    }

    public XmlDatasource(String alias, DBType type, String jndi) {
        super(alias, type, jndi);
    }
}
