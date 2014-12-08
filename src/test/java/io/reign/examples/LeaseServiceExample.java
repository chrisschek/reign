package io.reign.examples;

import io.reign.Reign;
import io.reign.lease.Lease;
import io.reign.lease.LeaseEventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeaseServiceExample {
    private static final Logger logger = LoggerFactory.getLogger(LeaseServiceExample.class);

    public static void main(String[] args) throws Exception {

        /** init and start reign using builder **/
        final Reign reign = Reign.maker().zkConnectString("localhost:22181").zkTestServerPort(22181)
                .startZkTestServer(true)
                .get();
        reign.start();

        /** use lease service **/
        reign.lease().cluster("reign").pool("leasePool1", 2).acquire(5000, newSubscriber());
        reign.lease().cluster("reign").pool("leasePool1", 2).acquire(5000, newSubscriber());
        reign.lease().cluster("reign").pool("leasePool1", 2).acquire(5000).subscribe(newSubscriber());
        reign.lease().cluster("reign").pool("leasePool1", 2).acquire(5000).subscribe(newSubscriber());
        reign.lease().cluster("reign").pool("leasePool1", 2).acquire(5000).subscribe(newSubscriber());

        Thread.sleep(600000);
        reign.stop();
    }

    static LeaseEventSubscriber newSubscriber() {
        return new LeaseEventSubscriber() {

            @Override
            public void acquired(Lease lease) {
                logger.debug("ACQUIRED:  leaseId={}", lease.id());

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    logger.warn("" + e, e);
                }

                // for (int i = 0; i < 1; i++) {
                // try {
                // Thread.sleep(10000);
                // } catch (InterruptedException e) {
                // logger.warn("" + e, e);
                // }
                // lease.renew().subscribe();
                // }
                //
                lease.release().subscribe();
            }

            @Override
            public void renewed(Lease lease) {
                logger.debug("RENEWED:  leaseId={}; acquired/renewed={}; durationMillis={}; expiryTimestamp={}",
                        lease.id(), lease.acquiredTimestamp(), lease.durationMillis(), lease.expiryTimestamp());

            }

            @Override
            public void released(Lease lease) {
                logger.debug("RELEASED:  leaseId={}", lease.id());

            }

            @Override
            public void revoked(Lease lease) {
                logger.debug("REVOKED:  leaseId={}", lease.id());

            }

            @Override
            public void error(Throwable e) {
                logger.debug("ERROR:  " + e, e);

            }
        };
    }
}
