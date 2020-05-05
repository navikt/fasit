package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlDatasource extends Datasource {

    public XmlDatasource() {
    }

    public XmlDatasource(String alias, DBType type, String jndi) {
        super(alias, type, jndi);
    }
}
