package no.nav.aura.appconfig.logging;

import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class Logging {

  @XmlAttribute
  private boolean serviceCalls;

  public boolean hasServiceCalls() {
    return serviceCalls;
  }

}
