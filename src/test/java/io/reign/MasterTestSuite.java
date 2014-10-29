package io.reign;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.reign.conf.ConfServiceTestSuite;
import io.reign.coord.CoordServiceTestSuite;
import io.reign.data.DataServiceTestSuite;
import io.reign.mesg.MessagingServiceTestSuite;
import io.reign.metrics.MetricsServiceTestSuite;
import io.reign.presence.PresenceServiceTestSuite;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Suite.class)
@SuiteClasses({ PresenceServiceTestSuite.class, DataServiceTestSuite.class, CoordServiceTestSuite.class,
        ConfServiceTestSuite.class, MetricsServiceTestSuite.class, MessagingServiceTestSuite.class,
        ObserverManagerTest.class, DefaultPathSchemeTest.class })
public class MasterTestSuite {

    private static final Logger logger = LoggerFactory.getLogger(MasterTestSuite.class);

    private static final AtomicBoolean startedFlag = new AtomicBoolean(false);
    private static final AtomicBoolean stoppedFlag = new AtomicBoolean(false);

    private static Reign reign;

    public static final int ZK_TEST_SERVER_PORT = 22181;

    public static ExecutorService executorService = Executors.newFixedThreadPool(10);

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    public static synchronized Reign getReign() {
        setUpClass();
        return reign;
    }

    @BeforeClass
    @Test
    public static void setUpClass() {

        if (reign != null) {
            return;
        }

        /** init and start reign using builder with test ZK instance **/
        reign = Reign.maker().findPortAutomatically(true)
                .zkClient("localhost:" + MasterTestSuite.ZK_TEST_SERVER_PORT, 30000)
                .zkTestServerPort(ZK_TEST_SERVER_PORT).pathCache(1024, 8).startZkTestServer(true)
                .onStart(startHook()).onStop(stopHook()).get();

        // test started hook
        assertFalse("Unexpected before start:  " + startedFlag.get(), startedFlag.get());
        reign.start();
        assertTrue("Unexpected after start:  " + startedFlag.get(), startedFlag.get());

    }

    public static Runnable startHook() {
        return new Runnable() {
            @Override
            public void run() {
                startedFlag.set(true);
            }
        };
    }

    public static Runnable stopHook() {
        return new Runnable() {
            @Override
            public void run() {
                stoppedFlag.set(true);
            }
        };
    }

    @AfterClass
    @Test
    public static void tearDownClass() {
        try {
            // wait a bit for any async tasks to finish
            Thread.sleep(5000);

            // shut down utility executor
            executorService.shutdown();

            // stop reign
            // test stopped hook
            assertFalse("Unexpected before stop:  " + stoppedFlag.get(), stoppedFlag.get());
            reign.stop();
            assertTrue("Unexpected after stop:  " + stoppedFlag.get(), stoppedFlag.get());

        } catch (Exception e) {
            logger.error("Trouble starting test ZooKeeper instance:  " + e, e);
        }
    }
}