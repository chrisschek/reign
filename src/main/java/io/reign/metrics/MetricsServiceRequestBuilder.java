package io.reign.metrics;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricRegistry;

/**
 * 
 * @author ypai
 *
 */
public class MetricsServiceRequestBuilder {

    private String clusterId;
    private String serviceId;

    private long rotationInterval = 1;
    private TimeUnit rotationIntervalTimeUnit = TimeUnit.HOURS;

    private long persistInterval = 15;
    private TimeUnit persistIntervalTimeUnit = TimeUnit.SECONDS;

    private MetricsService metricsService;

    public MetricsServiceRequestBuilder(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    public MetricsServiceRequestBuilder cluster(String clusterId) {
        this.clusterId = clusterId;
        return this;
    }

    public MetricsServiceRequestBuilder service(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    /**
     * How often metrics for this node are rotated (to prevent overflows in high throughput situations)
     */
    public MetricsServiceRequestBuilder rotationInterval(long rotationInterval, TimeUnit rotationIntervalTimeUnit) {
        this.rotationInterval = rotationInterval;
        this.rotationIntervalTimeUnit = rotationIntervalTimeUnit;
        return this;
    }

    /**
     * How often metrics for this node are persisted
     */
    public MetricsServiceRequestBuilder persistInterval(long persistInterval, TimeUnit persistIntervalTimeUnit) {
        this.persistInterval = persistInterval;
        this.persistIntervalTimeUnit = persistIntervalTimeUnit;
        return this;
    }

    public void observe(String clusterId, String serviceId, MetricsObserver observer) {
        this.metricsService.observe(clusterId, serviceId, observer);
    }

    public MetricsData serviceMetrics() {
        return this.metricsService.getServiceMetrics(clusterId, serviceId);
    }

    public MetricsData nodeMetrics() {
        return this.metricsService.getNodeMetrics(clusterId, serviceId);
    }

    public void scheduleExport(MetricRegistryManager registryManager) {
        this.metricsService.scheduleExport(clusterId, serviceId, registryManager, persistInterval,
                persistIntervalTimeUnit);
    }

    public MetricRegistryManager scheduleExport(MetricRegistry registry) {
        MetricRegistryManager metricRegistryManager = new RotatingMetricRegistryManager(registry, rotationInterval,
                rotationIntervalTimeUnit);
        scheduleExport(metricRegistryManager);
        return metricRegistryManager;
    }

}
