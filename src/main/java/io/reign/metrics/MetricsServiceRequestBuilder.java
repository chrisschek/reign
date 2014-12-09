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

    private boolean rotateFrequencySet = false;
    private long rotateFrequency = 1;
    private TimeUnit rotateFrequencyTimeUnit = TimeUnit.HOURS;

    private long persistFrequency = 15;
    private TimeUnit persistFrequencyTimeUnit = TimeUnit.SECONDS;

    private MetricsService metricsService;

    public MetricsServiceRequestBuilder(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    public MetricsServiceRequestBuilder cluster(String clusterId) {
        this.clusterId = clusterId;
        return this;
    }

    /**
     * How often metrics for this node are rotated (to prevent overflows in high throughput situations and to be able to
     * apply thresholds that alter application behavior: e.g. requests per hour, etc.)
     */
    public MetricsServiceRequestBuilder rotateFrequency(long rotateFrequency, TimeUnit rotateFrequencyTimeUnit) {
        this.rotateFrequency = rotateFrequency;
        this.rotateFrequencyTimeUnit = rotateFrequencyTimeUnit;
        this.rotateFrequencySet = true;
        return this;
    }

    /**
     * How often metrics for this node are persisted
     */
    public MetricsServiceRequestBuilder persistFrequency(long persistFrequency, TimeUnit persistFrequencyTimeUnit) {
        this.persistFrequency = persistFrequency;
        this.persistFrequencyTimeUnit = persistFrequencyTimeUnit;
        return this;
    }

    public void observe(String serviceId, MetricsObserver observer) {
        this.metricsService.observe(clusterId, serviceId, observer);
    }

    public MetricsData serviceMetrics(String serviceId) {
        return this.metricsService.getServiceMetrics(clusterId, serviceId);
    }

    public MetricsData nodeMetrics(String serviceId) {
        return this.metricsService.getNodeMetrics(clusterId, serviceId);
    }

    public void exportAs(String serviceId, MetricRegistryManager registryManager) {
        if (rotateFrequencySet) {
            throw new IllegalArgumentException(
                    "rotateFrequency should NOT be specified when reporting with custom registryManager!");
        }
        this.metricsService.scheduleExport(clusterId, serviceId, registryManager, persistFrequency,
                persistFrequencyTimeUnit);
    }

    /**
     * Convenience method that wraps given MetricRegistry with a RotatingMetricRegistryManager
     */
    public MetricRegistryManager exportAs(String serviceId, MetricRegistry registry) {
        MetricRegistryManager metricRegistryManager = new RotatingMetricRegistryManager(registry, rotateFrequency,
                rotateFrequencyTimeUnit);
        this.metricsService.scheduleExport(clusterId, serviceId, metricRegistryManager, persistFrequency,
                persistFrequencyTimeUnit);
        return metricRegistryManager;
    }

}
