package no.nav.aura.appconfig.monitoring;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

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
