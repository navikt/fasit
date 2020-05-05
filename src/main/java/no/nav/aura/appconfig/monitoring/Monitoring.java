package no.nav.aura.appconfig.monitoring;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import no.nav.aura.appconfig.Namespaces;

@XmlAccessorType(XmlAccessType.FIELD)
public class Monitoring {

    @XmlElement(namespace = Namespaces.DEFAULT, name = "selftest")
    private SelftestMonitoring selftest;

    public SelftestMonitoring getSelftest() {
        return selftest;
    }

    @XmlElement(namespace = Namespaces.DEFAULT, name = "metric")
    private List<Metric> metrics = new ArrayList<>();

    public List<Metric> getMetrics() {
        return metrics;
    }
}
