package no.nav.aura.envconfig.client;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "clusters")
@XmlAccessorType(XmlAccessType.FIELD)
public class ClusterListDO {
	
    @XmlElement(name = "cluster")
    private List<ClusterDO> clusters;

    public ClusterListDO() {}
    
    public ClusterListDO(List<ClusterDO> clusters) {
        this.clusters = clusters;
    }

	public List<ClusterDO> getClusters() {
		return clusters;
	}

	public void setClusters(List<ClusterDO> clusters) {
		this.clusters = clusters;
	}
    
}
