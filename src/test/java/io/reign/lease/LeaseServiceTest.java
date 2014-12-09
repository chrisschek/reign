package io.reign.lease;

import static org.junit.Assert.assertTrue;
import io.reign.MasterTestSuite;
import io.reign.Reign;
import io.reign.presence.PresenceService;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeaseServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(LeaseServiceTest.class);

    private LeaseService leaseService;
    private PresenceService presenceService;
    private Reign reign;

    @Before
    public void setUp() throws Exception {
        reign = MasterTestSuite.getReign();

        leaseService = reign.getService("lease");

        presenceService = reign.getService("presence");
        presenceService.announce("clusterLease", "serviceA", true);
    }

    @Test
    public void testAcquire() throws Exception {
        final long startTimestamp = System.currentTimeMillis();

        final AtomicLong millisToAcquire1 = new AtomicLong(0L);
        leaseService.acquire("clusterLease", "testAcquire", 2, 10000).subscribe(new LeaseEventSubscriber() {
            @Override
            public void acquired(Lease lease) {
                millisToAcquire1.set(System.currentTimeMillis() - startTimestamp);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                lease.release().subscribe();
            }
        });

        final AtomicLong millisToAcquire2 = new AtomicLong(0L);
        leaseService.acquire("clusterLease", "testAcquire", 2, 10000).subscribe(new LeaseEventSubscriber() {
            @Override
            public void acquired(Lease lease) {
                millisToAcquire2.set(System.currentTimeMillis() - startTimestamp);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                lease.release().subscribe();
            }
        });

        final AtomicLong millisToAcquire3 = new AtomicLong(0L);
        leaseService.acquire("clusterLease", "testAcquire", 2, 10000).subscribe(new LeaseEventSubscriber() {
            @Override
            public void acquired(Lease lease) {
                millisToAcquire3.set(System.currentTimeMillis() - startTimestamp);
                lease.release().subscribe();
            }

            @Override
            public void released(Lease lease) {
                synchronized (millisToAcquire3) {
                    millisToAcquire3.notifyAll();
                }
            }
        });

        synchronized (millisToAcquire3) {
            millisToAcquire3.wait(20000);
        }

        assertTrue(millisToAcquire1.get() < millisToAcquire3.get());
        assertTrue(millisToAcquire2.get() < millisToAcquire3.get());
        assertTrue(millisToAcquire3.get() >= 1000);
    }

}
