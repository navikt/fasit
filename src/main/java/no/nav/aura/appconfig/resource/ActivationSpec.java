package no.nav.aura.appconfig.resource;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlEnum;

@XmlAccessorType(XmlAccessType.FIELD)
public class ActivationSpec {

    @XmlAttribute(required = true)
    private String name;

    @XmlAttribute(required = true)
    private String jndi;

    @XmlAttribute(required = false)
    private SslType sslType;

    @XmlAttribute(required = false)
    private String sslCipherSuite;

    @XmlAttribute(required = false)
    private CompressHeaders compressHeaders;

    @XmlAttribute(required = false)
    private CompressionAlgorithm compressionAlgorithm;

    @XmlAttribute(required = false)
    private MsgRetention msgRetention;

    @XmlAttribute(required = false)
    private String messageSelector;

    @XmlAttribute(required = false)
    private Integer rescanInterval;

    @XmlAttribute(required = false)
    private Integer maxPoolSize;

    @XmlAttribute(required = false)
    private Integer startTimeout;

    @XmlAttribute(required = false)
    private Integer poolTimeout;

    @XmlAttribute(required = false)
    private Integer ccsid;

    @XmlAttribute(required = false)
    private Integer failureDeliveryCount;

    @XmlAttribute(required = false)
    private Boolean failIfQuiescing;

    @XmlAttribute(required = false)
    private Boolean stopEndpointIfDeliveryFails;

    @XmlAttribute(required = false)
    private WasEndpointInitialState wasEndpointInitialState;

    public ActivationSpec() {
    }

    public ActivationSpec(String name, String jndi, SslType sslType, String sslCipherSuite, CompressHeaders compressHeaders, CompressionAlgorithm compressionAlgorithm, MsgRetention msgRetention, int rescanInterval,
            int maxPoolSize,
            int startTimeout,
            int poolTimeout, int ccsid, Integer failureDeliveryCount, Boolean failIfQuiescing, Boolean stopEndpointIfDeliveryFails, WasEndpointInitialState wasEndpointInitialState) {
        this.name = name;
        this.sslType = sslType;
        this.sslCipherSuite = sslCipherSuite;
        this.compressHeaders = compressHeaders;
        this.compressionAlgorithm = compressionAlgorithm;
        this.msgRetention = msgRetention;
        this.rescanInterval = rescanInterval;
        this.maxPoolSize = maxPoolSize;
        this.startTimeout = startTimeout;
        this.poolTimeout = poolTimeout;
        this.failureDeliveryCount = failureDeliveryCount;
        this.ccsid = ccsid;
        this.failIfQuiescing = failIfQuiescing;
        this.stopEndpointIfDeliveryFails = stopEndpointIfDeliveryFails;
        this.wasEndpointInitialState = wasEndpointInitialState;
    }

    public SslType getSslType() {
        return sslType;
    }

    public String getSslCipherSuite() {
        return sslCipherSuite;
    }

    public CompressHeaders getCompressHeaders() {
        return compressHeaders;
    }

    public CompressionAlgorithm getCompressionAlgorithm() {
        return compressionAlgorithm;
    }

    public MsgRetention getMsgRetention() {
        return msgRetention;
    }

    public Integer getRescanInterval() {
        return rescanInterval;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public Integer getStartTimeout() {
        return startTimeout;
    }

    public Integer getPoolTimeout() {
        return poolTimeout;
    }

    public Integer getCcsid() {
        return ccsid;
    }

    public Integer getFailureDeliveryCount() {
        return failureDeliveryCount;
    }

    public Boolean getFailIfQuiescing() {
        return failIfQuiescing;
    }

    public Boolean getStopEndpointIfDeliveryFails() {
        return stopEndpointIfDeliveryFails;
    }

    public WasEndpointInitialState getWasEndpointInitialState() {
        return wasEndpointInitialState;
    }

    @XmlEnum
    public enum CompressionAlgorithm {
        NONE, RLE, ZLIBHIGH, ZLIBFAST
    }

    @XmlEnum
    public enum CompressHeaders {
        NONE, SYSTEM
    }

    @XmlEnum
    public enum SslType {
        NONE, CENTRAL, SPECIFIC
    }

    @XmlEnum
    public enum MsgRetention {
        YES, NO
    }

    @XmlEnum
    public enum WasEndpointInitialState {
        ACTIVE, ACTIVE_SINGLE_NODE, INACTIVE
    }
}
