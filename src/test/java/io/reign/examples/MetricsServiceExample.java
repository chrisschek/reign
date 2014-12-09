package io.reign.examples;

import io.reign.Reign;
import io.reign.metrics.MetricsData;
import io.reign.metrics.MetricsService;
import io.reign.presence.PresenceService;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

public class MetricsServiceExample {
    private static final Logger logger = LoggerFactory.getLogger(MetricsServiceExample.class);

    public static void main(String[] args) throws Exception {
        // logger.info("MetricsData JSON = {}", JacksonUtil.getObjectMapper().writeValueAsString(new MetricsData()));

        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

        /** init and start reign using builder **/
        Reign reign = Reign.maker().zkConnectString("localhost:22181").zkTestServerPort(22181).startZkTestServer(true)
                .get();
        reign.start();

        PresenceService presenceService = reign.getService("presence");
        presenceService.announce("clusterA", "serviceA", true);

        MetricsService metricsService = reign.getService("metrics");

        final MetricRegistry metricRegistry = new MetricRegistry();
        Counter counter1 = metricRegistry.counter(MetricRegistry.name("counter1"));
        Counter counter2 = metricRegistry.counter(MetricRegistry.name("counter2"));
        counter1.inc();
        counter2.inc(3);

        // fluent equivalent of
        // metricsService.scheduleExport("clusterA", "serviceA", registryManager, 2, TimeUnit.SECONDS);
        reign.metrics().cluster("clusterA").persistFrequency(2, TimeUnit.SECONDS)
                .rotateFrequency(120, TimeUnit.SECONDS).exportAs("serviceA", metricRegistry);

        MetricsData metricsData = null;
        while ((metricsData = metricsService.getNodeMetrics("clusterA", "serviceA")) == null) {
            Thread.sleep(1000);
        }

        logger.debug("counter1={}", metricsData.getCounter("counter1").getCount());

        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Counter c1 = metricRegistry.counter(MetricRegistry.name("c1"));
                Counter c2 = metricRegistry.counter(MetricRegistry.name("c2"));
                c1.inc();
                c2.inc(3);

            }
        }, 0, 1, TimeUnit.SECONDS);

        Thread.sleep(35000);
        Counter counter3 = metricRegistry.counter(MetricRegistry.name(MetricsService.class, "counter3"));
        counter3.inc(5);

        Thread.sleep(600000);
    }
}
