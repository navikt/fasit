package no.nav.aura.appconfig.resource;

import no.nav.aura.appconfig.JaxbPropertyHelper;
import no.nav.aura.appconfig.jaxb.JaxbPropertySet;
import no.nav.aura.appconfig.jaxb.ParentConfigObject;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Connectionpool settings for datasource
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ConnectionPool {

    /**
     * Specifies how to purge connections when a stale connection or fatal connection error is detected
     */
    @XmlEnum
    public enum PurgePolicy {
        /**
         * All connections in the pool are marked stale
         */
        ENTIRE_POOL("EntirePool"),
        /**
         * Only the connection that caused the StaleConnectionException is closed
         */
        FAILING_CONNECTIONS("FailingConnectionOnly");
        private String policy;

        PurgePolicy(String policy) {
            this.policy = policy;
        }

        public String getPolicy() {
            return policy;
        }
    }

    @XmlAttribute(required = false)
    private Integer maxPoolSize;

    @XmlAttribute(required = false)
    private Integer minPoolSize;

    @XmlAttribute(required = false)
    private Integer connectionTimeout;

    @XmlAttribute(required = false)
    private Integer reapTime;

    @XmlAttribute(required = false)
    private Integer unusedTimeout;

    @XmlAttribute(required = false)
    private Integer agedTimeout;

    @XmlAttribute(required = false)
    private Integer queryTimeout;

    @XmlAttribute(required = false)
    private PurgePolicy purgePolicy;

    @XmlElementRef
    protected List<JaxbPropertySet> customProperties = new ArrayList<>();

    public ConnectionPool() {
    }

    /**
     * @param maxPoolSize
     *            - Specifies the maximum number of physical connections that you can create in this pool.
     * @param minPoolSize
     *            - Specifies the minimum number of physical connections to maintain.
     * @param connectionTimeout
     *            - Specifies the interval, in seconds, after which a connection request times out
     * @param reapTime
     *            - Specifies the interval, in seconds, between runs of the pool maintenance thread.
     * @param unusedTimeout
     *            - Specifies the interval in seconds after which an unused or idle connection is discarded.
     * @param agedTimeout
     *            - Specifies the interval in seconds before a physical connection is discarded
     * @param queryTimeout
     *            - Specifies the interval in seconds before a query times out
     * @param purgePolicy
     *            - Specifies how to purge connections when a stale connection or fatal connection error is detected
     */
    public ConnectionPool(Integer maxPoolSize, Integer minPoolSize, Integer connectionTimeout, Integer reapTime,
                          Integer unusedTimeout, Integer agedTimeout, Integer queryTimeout,
                          PurgePolicy purgePolicy) {
        this.maxPoolSize = maxPoolSize;
        this.minPoolSize = minPoolSize;
        this.connectionTimeout = connectionTimeout;
        this.reapTime = reapTime;
        this.unusedTimeout = unusedTimeout;
        this.agedTimeout = agedTimeout;
        this.queryTimeout = queryTimeout;
        this.purgePolicy = purgePolicy;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public Integer getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(Integer minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public Integer getReapTime() {
        return reapTime;
    }

    public Integer getUnusedTimeout() {
        return unusedTimeout;
    }

    public Integer getAgedTimeout() {
        return agedTimeout;
    }

    public Integer getQueryTimeout() {
        return queryTimeout;
    }

    public String getPurgePolicy() {
        return purgePolicy != null ? purgePolicy.getPolicy() : null;
    }

    public void setCustomProperties(List<JaxbPropertySet> customProperties) {
        this.customProperties = customProperties;
    }

    public Map<String, String> getCustomProperties() {
        return JaxbPropertyHelper.getCustomProperties(ParentConfigObject.ConnectionPool, customProperties);
    }

}
