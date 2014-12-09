package io.reign.lease;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.reign.MasterTestSuite;
import io.reign.Reign;
import io.reign.presence.PresenceService;

import java.util.concurrent.atomic.AtomicBoolean;
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

        final AtomicBoolean wrongEventHandlerCalled = new AtomicBoolean(false);

        final AtomicLong millisToAcquire1 = new AtomicLong(-1L);
        final AtomicLong millisToRelease1 = new AtomicLong(-1L);
        leaseService.acquire("clusterLease", "testAcquire", 2, 10000).subscribe(new LeaseEventSubscriber() {
            @Override
            public void acquired(Lease lease) {
                millisToAcquire1.set(System.currentTimeMillis() - startTimestamp);

                synchronized (millisToAcquire1) {
                    millisToAcquire1.notifyAll();
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                lease.release().subscribe();
            }

            @Override
            public void renewed(Lease lease) {
                wrongEventHandlerCalled.set(true);
            }

            @Override
            public void released(Lease lease) {
                millisToRelease1.set(System.currentTimeMillis() - startTimestamp);
            }

            @Override
            public void revoked(Lease lease) {
                wrongEventHandlerCalled.set(true);
            }

            @Override
            public void error(Throwable e) {
                wrongEventHandlerCalled.set(true);
            }

        });

        while (millisToAcquire1.get() < 0) {
            synchronized (millisToAcquire1) {
                millisToAcquire1.wait(2000);
            }
        }

        final AtomicLong millisToAcquire2 = new AtomicLong(-1L);
        leaseService.acquire("clusterLease", "testAcquire", 2, 10000).subscribe(new LeaseEventSubscriber() {
            @Override
            public void acquired(Lease lease) {
                millisToAcquire2.set(System.currentTimeMillis() - startTimestamp);

                synchronized (millisToAcquire2) {
                    millisToAcquire2.notifyAll();
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                lease.release().subscribe();
            }
        });

        while (millisToAcquire2.get() < 0) {
            synchronized (millisToAcquire2) {
                millisToAcquire2.wait(2000);
            }
        }

        final AtomicLong millisToAcquire3 = new AtomicLong(-1L);
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

        while (millisToAcquire3.get() < 0) {
            synchronized (millisToAcquire3) {
                millisToAcquire3.wait(2000);
            }
        }

        assertFalse(wrongEventHandlerCalled.get());

        assertTrue(millisToRelease1.get() > 0);
        assertTrue(millisToAcquire1.get() < millisToAcquire3.get());
        assertTrue(millisToAcquire2.get() < millisToAcquire3.get());
        assertTrue(millisToAcquire3.get() >= 1000);
    }

    @Test
    public void testRevocation() throws Exception {
        final long startTimestamp = System.currentTimeMillis();

        final AtomicLong millisToAcquire1 = new AtomicLong(-1L);
        final AtomicLong millisToRevoke1 = new AtomicLong(-1L);
        leaseService.acquire("clusterLease", "testRevocation", 1, 5000).subscribe(new LeaseEventSubscriber() {
            @Override
            public void acquired(Lease lease) {
                logger.debug("testRevocation():  ACQUIRED");
                millisToAcquire1.set(System.currentTimeMillis() - startTimestamp);

                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void revoked(Lease lease) {
                logger.debug("testRevocation():  REVOKED");
                millisToRevoke1.set(System.currentTimeMillis() - startTimestamp);

                synchronized (millisToRevoke1) {
                    millisToRevoke1.notifyAll();
                }
            }
        });

        while (millisToRevoke1.get() < 0L) {
            Thread.sleep(1000);
        }

        assertTrue("millisToAcquire should be less than millisToRevoke", millisToAcquire1.get() < millisToRevoke1.get());
        assertTrue("millisToRevoke should be >= lease duration", millisToRevoke1.get() >= 5000);
    }

}
