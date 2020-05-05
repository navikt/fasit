package no.nav.aura.appconfig.logging;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class Logging {

  @XmlAttribute
  private boolean serviceCalls;

  public boolean hasServiceCalls() {
    return serviceCalls;
  }

}
