package no.nav.aura.appconfig.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class SchedulerConfiguration extends AbstractJndiCapableResource {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "datasourceJndi")
    private String datasourceJndi;

    @XmlAttribute(name = "tablePrefix", required = false)
    private String tablePrefix = "SCH_";

    @XmlAttribute(name = "pollInterval", required = false)
    private int pollInterval = 30;

    @XmlAttribute(name = "datasourceAlias", required = false)
    private String datasourceAlias;

    @XmlAttribute(name = "workManagerJndi")
    private String workManagerJndi;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDatasourceJndi() {
        return datasourceJndi;
    }

    public void setDatasourceJndi(String datasourceJndi) {
        this.datasourceJndi = datasourceJndi;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public int getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }

    public String getDatasourceAlias() {
        return datasourceAlias;
    }

    public void setDatasourceAlias(String datasourceAlias) {
        this.datasourceAlias = datasourceAlias;
    }

    public String getWorkManagerJndi() {
        return workManagerJndi;
    }

    public void setWorkManagerJndi(String workManagerJndi) {
        this.workManagerJndi = workManagerJndi;
    }

}
