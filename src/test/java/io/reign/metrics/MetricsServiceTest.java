package io.reign.metrics;

import static org.junit.Assert.assertTrue;
import io.reign.MasterTestSuite;
import io.reign.presence.PresenceService;
import io.reign.util.JacksonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class MetricsServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(MetricsServiceTest.class);

    private MetricsService metricsService;
    private PresenceService presenceService;

    @Before
    public void setUp() throws Exception {

        metricsService = MasterTestSuite.getReign().getService("metrics");
        metricsService.setUpdateIntervalMillis(1000);

        presenceService = MasterTestSuite.getReign().getService("presence");
        presenceService.announce("clusterMetrics", "serviceA", true);
        presenceService.announce("clusterMetrics", "serviceB", true);
        presenceService.announce("clusterMetrics", "serviceC", true);
        presenceService.announce("clusterMetrics", "serviceD", true);
    }

    @Test
    public void testObserver() throws Exception {
        presenceService.waitUntilAvailable("clusterMetrics", "serviceC", 30000);

        MetricRegistryManager registryManager = getMetricRegistryManager(new RotatingMetricRegistryManager(300,
                TimeUnit.SECONDS));
        Counter observerTestCounter = registryManager.get().counter("observerTestCounter");

        final AtomicInteger calledCount = new AtomicInteger(0);
        final AtomicReference<MetricsData> latest = new AtomicReference<MetricsData>();
        metricsService.observe("clusterMetrics", "serviceC", new MetricsObserver() {
            @Override
            public void updated(MetricsData updated, MetricsData previous) {

                // only count updates, not created
                if (previous != null) {
                    calledCount.incrementAndGet();
                }

                if (updated.getCounter("observerTestCounter") != null
                        && previous.getCounter("observerTestCounter") != null) {
                    logger.debug(
                            "*** OBSERVER (testObserver):  calledCount={}; updated.observerTestCounter={}; previous.observerTestCounter={}",
                            calledCount.get(), updated != null ? updated.getCounter("observerTestCounter").getCount()
                                    : null,
                            previous != null ? previous.getCounter("observerTestCounter").getCount() : null);
                }

                latest.set(updated);
                synchronized (calledCount) {
                    calledCount.notifyAll();
                }
            }
        });

        metricsService.scheduleExport("clusterMetrics", "serviceC", registryManager, 1, TimeUnit.SECONDS);

        // has not been updated yet
        assertTrue("calledCount should be 0, but is " + calledCount.get(), calledCount.get() == 0);

        // wait for update
        synchronized (calledCount) {
            for (int i = 0; i < 40 && calledCount.get() == 0; i++) {
                calledCount.wait(metricsService.getUpdateIntervalMillis() / 4);
            }
        }
        assertTrue("Expected >=1, got " + calledCount.get(), calledCount.get() >= 1);

        // force a change so observer will be called again
        long previousValue = observerTestCounter.getCount();
        observerTestCounter.inc();

        // wait for update
        synchronized (calledCount) {
            for (int i = 0; i < 40
                    && (latest.get().getCounter("observerTestCounter") == null || latest.get()
                            .getCounter("observerTestCounter").getCount() == 0); i++) {
                calledCount.wait(metricsService.getUpdateIntervalMillis() / 4);
            }
        }
        assertTrue("calledCount should be >1, but is " + calledCount.get(), calledCount.get() > 1);
        assertTrue("latest.observerTestCounter should be " + (previousValue + 1) + ", but is "
                + latest.get().getCounter("observerTestCounter").getCount() + "; calledCount=" + calledCount.get(),
                latest.get().getCounter("observerTestCounter").getCount() == (previousValue + 1));

    }

    @Test
    public void testIntervalNodes() throws Exception {
        presenceService.waitUntilAvailable("clusterMetrics", "serviceD", 30000);

        int secondsToWait = metricsService.getUpdateIntervalMillis() / 1000 * 8;

        // test that each interval creates its own data node
        final MetricRegistryManager registryManager = new RotatingMetricRegistryManager(secondsToWait,
                TimeUnit.SECONDS);

        // lock object for rotations
        final Object rotationLockObject = new Object();

        // lock object for service metrics updates
        final Object serviceMetricsUpdatesLockObject = new Object();

        registryManager.registerCallback(new MetricRegistryManagerCallback() {
            @Override
            public void rotated(MetricRegistry current, MetricRegistry previous) {
                synchronized (rotationLockObject) {
                    rotationLockObject.notifyAll();
                }
            }
        });

        metricsService.observe("clusterMetrics", "serviceD", new MetricsObserver() {
            @Override
            public void updated(MetricsData updated, MetricsData previous) {
                synchronized (serviceMetricsUpdatesLockObject) {
                    serviceMetricsUpdatesLockObject.notifyAll();
                }
            }
        });

        Counter counter1 = registryManager.counter("c1");
        counter1.inc();

        metricsService.scheduleExport("clusterMetrics", "serviceD", registryManager, 1, TimeUnit.SECONDS);

        // wait for metrics to be updated
        MetricsData metricsData = null;
        for (int i = 0; i < secondsToWait; i++) {
            synchronized (serviceMetricsUpdatesLockObject) {
                serviceMetricsUpdatesLockObject.wait(metricsService.getUpdateIntervalMillis());
            }
            metricsData = metricsService.getServiceMetrics("clusterMetrics", "serviceD");
            if (metricsData != null && metricsData.getCounter("c1") != null
                    && metricsData.getCounter("c1").getCount() == 1) {
                break;
            }
        }

        logger.debug("testIntervalNodes():  1:  secondsToWait={};  metricsData={}", secondsToWait,
                JacksonUtil.getObjectMapper().writeValueAsString(metricsData));
        logger.debug("MY! metricsData.counter(c1)={}", metricsService.getMyMetrics("clusterMetrics", "serviceD")
                .getCounter("c1")
                .getCount());

        assertTrue("Unexpected value:  " + metricsData.getDataNodeCount(), metricsData.getDataNodeCount() >= 1);
        assertTrue("Unexpected value:  " + metricsData.getDataNodeInWindowCount(),
                metricsData.getDataNodeInWindowCount() <= 1);
        assertTrue("Unexpected value:  " + metricsData.getCounter("c1").getCount(), metricsData.getCounter("c1")
                .getCount() == 1);

        synchronized (rotationLockObject) {
            rotationLockObject.wait(secondsToWait * 1000);
        }

        counter1 = registryManager.counter("c1");
        counter1.inc(2);

        // wait for metrics to be updated
        for (int i = 0; i < secondsToWait; i++) {
            synchronized (serviceMetricsUpdatesLockObject) {
                serviceMetricsUpdatesLockObject.wait(metricsService.getUpdateIntervalMillis());
            }
            metricsData = metricsService.getServiceMetrics("clusterMetrics", "serviceD");
            if (metricsData != null && metricsData.getCounter("c1") != null
                    && metricsData.getCounter("c1").getCount() == 2) {
                break;
            }
        }

        logger.debug("testIntervalNodes():  2:  secondsToWait={}; metricsData={}", secondsToWait,
                JacksonUtil.getObjectMapper().writeValueAsString(metricsData));

        assertTrue("Unexpected value:  " + metricsData.getDataNodeCount(), metricsData.getDataNodeCount() >= 1);
        assertTrue("Unexpected value:  " + metricsData.getDataNodeInWindowCount(),
                metricsData.getDataNodeInWindowCount() <= 1);
        assertTrue("Unexpected value:  " + metricsData.getCounter("c1").getCount(), metricsData.getCounter("c1")
                .getCount() == 2);
    }

    @Test
    public void testExportSelfMetrics() throws Exception {
        MetricRegistryManager registryManager = getMetricRegistryManager(new StaticMetricRegistryManager());
        metricsService.scheduleExport("clusterMetrics", "serviceA", registryManager, 5, TimeUnit.SECONDS);

        MetricsData metricsData = metricsService.getMyMetrics("clusterMetrics", "serviceA");
        for (int i = 0; i < 20 && metricsData == null; i++) {
            metricsData = metricsService.getMyMetrics("clusterMetrics", "serviceA");
            Thread.sleep(1000);
        }

        // counters
        CounterData counter1Data = metricsData.getCounter("counter1");
        CounterData counter2Data = metricsData.getCounter("counter2");
        assertTrue("Unexpected value:  " + counter1Data.getCount(), counter1Data.getCount() == 1L);
        assertTrue("Unexpected value:  " + counter2Data.getCount(), counter2Data.getCount() == 2L);

        // gauges
        GaugeData gauge1 = metricsData.getGauge("gauge1");
        GaugeData gauge2 = metricsData.getGauge("gauge2");
        GaugeData gauge3 = metricsData.getGauge("gauge3");
        GaugeData gauge4 = metricsData.getGauge("gauge4");
        assertTrue(((Double) gauge1.getValue()) == 1.0);
        assertTrue(((Double) gauge2.getValue()) == 2.0);
        assertTrue("Gauges that return non-numeric values should not be exported", gauge3 == null);
        assertTrue("Gauges that return non-numeric values should not be exported", gauge4 == null);

        // meters
        MeterData meter1 = metricsData.getMeter("meter1");
        MeterData meter2 = metricsData.getMeter("meter2");
        assertTrue(meter1.getCount() == 1000);
        assertTrue(meter2.getCount() == 4000);

        // timers
        TimerData timer1 = metricsData.getTimer("timer1");
        TimerData timer2 = metricsData.getTimer("timer2");
        assertTrue(timer1.getCount() == 1);
        assertTrue(timer2.getCount() == 2);
        assertTrue("Unexpected value:  +" + timer1.getMax(), Math.floor(timer1.getMax()) - 100 < 5);
        assertTrue("Unexpected value:  +" + timer2.getMax(), Math.floor(timer2.getMax()) - 200 < 5);

        // histograms
        HistogramData histo1 = metricsData.getHistogram("histo1");
        HistogramData histo2 = metricsData.getHistogram("histo2");
        assertTrue(histo1.getCount() == 3);
        assertTrue(histo1.getMin() == 10);
        assertTrue(histo1.getMean() == 20);
        assertTrue(histo1.getMax() == 30);
        assertTrue(histo1.getP999() == 30);
        assertTrue(histo2.getCount() == 6);
        assertTrue(histo2.getMin() == 100);
        assertTrue(histo2.getMean() == 350);
        assertTrue(histo2.getMax() == 600);
        assertTrue(histo2.getP999() == 600);
    }

    @Test
    public void testExportServiceMetrics() throws Exception {
        MetricRegistryManager registryManager1 = getMetricRegistryManager(new StaticMetricRegistryManager());
        metricsService.scheduleExport("clusterMetrics", "serviceB", "node1", registryManager1, 1, TimeUnit.SECONDS);

        MetricRegistryManager registryManager2 = getMetricRegistryManager(new StaticMetricRegistryManager());
        metricsService.scheduleExport("clusterMetrics", "serviceB", "node2", registryManager2, 1, TimeUnit.SECONDS);

        // get service metrics, but wait for both service nodes to be there
        // before checking values
        MetricsData metricsData = null;
        while ((metricsData = metricsService.getServiceMetrics("clusterMetrics", "serviceB")) == null
                || metricsData.getDataNodeCount() < 2) {
            // wait for aggregation to happen
            Thread.sleep(metricsService.getUpdateIntervalMillis() / 2);
        }

        // counters
        CounterData counter1Data = metricsData.getCounter("counter1");
        CounterData counter2Data = metricsData.getCounter("counter2");
        assertTrue("Unexpected value:  " + counter1Data.getCount(), counter1Data.getCount() == 2L);
        assertTrue(counter2Data.getCount() == 4L);

        // gauges
        GaugeData gauge1 = metricsData.getGauge("gauge1");
        GaugeData gauge2 = metricsData.getGauge("gauge2");
        GaugeData gauge3 = metricsData.getGauge("gauge3");
        GaugeData gauge4 = metricsData.getGauge("gauge4");
        assertTrue(((Double) gauge1.getValue()) == 1.0);
        assertTrue(((Double) gauge2.getValue()) == 2.0);
        assertTrue("Gauges that return non-numeric values should not be exported", gauge3 == null);
        assertTrue("Gauges that return non-numeric values should not be exported", gauge4 == null);

        // meters
        MeterData meter1 = metricsData.getMeter("meter1");
        MeterData meter2 = metricsData.getMeter("meter2");
        assertTrue(meter1.getCount() == 2000);
        assertTrue(meter2.getCount() == 8000);

        // timers
        TimerData timer1 = metricsData.getTimer("timer1");
        TimerData timer2 = metricsData.getTimer("timer2");
        assertTrue(timer1.getCount() == 2);
        assertTrue(timer2.getCount() == 4);

        // check thresholds instead of exact values since these are estimations
        assertTrue(Math.floor(timer1.getMax()) - 100 < 50);
        assertTrue(Math.floor(timer2.getMax()) - 200 < 50);

        // histograms
        HistogramData histo1 = metricsData.getHistogram("histo1");
        HistogramData histo2 = metricsData.getHistogram("histo2");
        assertTrue(histo1.getCount() == 6);
        assertTrue(histo1.getMin() == 10);
        assertTrue(histo1.getMean() == 20);
        assertTrue(histo1.getMax() == 30);
        assertTrue(histo1.getP999() == 30);
        assertTrue(histo2.getCount() == 12);
        assertTrue(histo2.getMin() == 100);
        assertTrue(histo2.getMean() == 350);
        assertTrue(histo2.getMax() == 600);
        assertTrue(histo2.getP999() == 600);

    }

    MetricRegistryManager getMetricRegistryManager(MetricRegistryManager registryManager) throws Exception {
        // RotatingMetricRegistryManager registryManager = new
        // RotatingMetricRegistryManager(300, TimeUnit.SECONDS);
        // MetricRegistryManager registryManager = new
        // StaticMetricRegistryManager();

        // counters
        Counter counter1 = registryManager.get().counter(MetricRegistry.name("counter1"));
        Counter counter2 = registryManager.get().counter(MetricRegistry.name("counter2"));
        counter1.inc();
        counter2.inc(2);

        // gauges that should be serialized
        Gauge<Integer> gauge1 = registryManager.get().register(MetricRegistry.name("gauge1"),
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return 1;
                    }

                });
        Gauge<Integer> gauge2 = registryManager.get().register(MetricRegistry.name("gauge2"),
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return 2;
                    }
                });

        // gauges that should not be serialized
        Gauge<String> gauge3 = registryManager.get().register(MetricRegistry.name("gauge3"),
                new Gauge<String>() {
                    @Override
                    public String getValue() {
                        return "shouldNotBeExported";
                    }
                });
        Gauge<List<Number>> gauge4 = registryManager.get().register(MetricRegistry.name("gauge4"),
                new Gauge<List<Number>>() {
                    @Override
                    public List<Number> getValue() {
                        List<Number> list = new ArrayList<Number>(1);
                        return list;
                    }
                });

        // meters
        Meter meter1 = registryManager.get().meter(MetricRegistry.name("meter1"));
        Meter meter2 = registryManager.get().meter(MetricRegistry.name("meter2"));

        meter1.mark(1000);
        meter2.mark(2000);
        meter2.mark(2000);

        // wait 5 secs after initial mark so meters can fill out some rates
        Thread.sleep(5000);

        // timers
        Timer timer1 = registryManager.get().timer("timer1");
        Timer timer2 = registryManager.get().timer("timer2");
        final Timer.Context context1 = timer1.time();
        try {
            Thread.sleep(100);
        } finally {
            context1.stop();
        }
        final Timer.Context context2 = timer2.time();
        try {
            Thread.sleep(200);
        } finally {
            context2.stop();
        }
        final Timer.Context context3 = timer2.time();
        try {
            Thread.sleep(200);
        } finally {
            context3.stop();
        }

        // histograms
        Histogram histo1 = registryManager.get().histogram("histo1");
        Histogram histo2 = registryManager.get().histogram("histo2");
        histo1.update(10);
        histo1.update(20);
        histo1.update(30);
        histo2.update(100);
        histo2.update(200);
        histo2.update(300);
        histo2.update(400);
        histo2.update(500);
        histo2.update(600);

        return registryManager;
    }
}
