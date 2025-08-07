package no.nav.aura.envconfig.client;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "nodes")
@XmlAccessorType(XmlAccessType.FIELD)
public class NodeListDO {
	
    @XmlElement(name = "node")
    private List<NodeDO> nodes;

    public NodeListDO() {}

	public NodeListDO(List<NodeDO> nodes) {
		super();
		this.nodes = nodes;
	}

	public List<NodeDO> getNodes() {
		return nodes;
	}

	public void setNodes(List<NodeDO> nodes) {
		this.nodes = nodes;
	}
    
    
}
