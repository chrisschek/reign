package io.reign.examples;

import io.reign.Reign;
import io.reign.conf.ConfService;
import io.reign.coord.ConfiguredPermitPoolSize;
import io.reign.coord.CoordinationService;
import io.reign.coord.DistributedLock;
import io.reign.coord.DistributedReadWriteLock;
import io.reign.coord.DistributedReentrantLock;
import io.reign.coord.DistributedSemaphore;
import io.reign.coord.SimpleLockObserver;
import io.reign.coord.SimpleSemaphoreObserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author ypai
 * 
 */
public class CoordinationServiceExample {
    private static final Logger logger = LoggerFactory.getLogger(CoordinationServiceExample.class);

    public static void main(String[] args) throws Exception {
        /** init and start reign using builder **/
        Reign reign = Reign.maker().zkClient("localhost:2181", 15000).pathCache(1024, 8).allCoreServices().build();
        reign.start();

        /** coordination service example **/
        coordinationServiceExclusiveLockExample(reign);
        coordinationServiceReentrantLockExample(reign);
        coordinationServiceReadWriteLockExample(reign);
        coordinationServiceFixedSemaphoreExample(reign);
        coordinationServiceConfiguredSemaphoreExample(reign);

        /** sleep to allow examples to run for a bit **/
        logger.info("Sleeping before shutting down...");
        Thread.sleep(120000);

        /** shutdown reign **/
        reign.stop();

        /** sleep a bit to observe observer callbacks **/
        Thread.sleep(10000);
    }

    public static void coordinationServiceReentrantLockExample(Reign reign) throws Exception {
        // this is how you would normally get a service
        final CoordinationService coordService = (CoordinationService) reign.getService("coord");

        final int lockHoldTimeMillis = 5000;

        coordService.observe("examples", "exclusive_lock1", new SimpleLockObserver() {
            @Override
            public void revoked(DistributedLock lock, String reservationId) {
                logger.info("***** Observer:  lock REVOKED:  reservationId={}", reservationId);
            }
        });

        Thread t1 = new Thread() {
            @Override
            public void run() {
                DistributedReentrantLock lock = coordService.getReentrantLock("examples", "exclusive_lock1");
                logger.info(this.getName() + ":  attempting to acquire lock...");
                lock.lock();
                try {
                    long sleepInterval = (long) (lockHoldTimeMillis * Math.random());
                    logger.info("{}:  acquired lock:  will hold for {} millis:  holdCount={}", new Object[] {
                            this.getName(), sleepInterval, (lock).getHoldCount() });
                    Thread.sleep(sleepInterval);
                } catch (InterruptedException e) {
                    logger.info("Interrupted:  " + e, e);
                } finally {
                    lock.unlock();
                    lock.destroy();
                }
            }
        };
        t1.setName("T1");
        t1.setDaemon(true);
        t1.start();

        Thread t2 = new Thread() {
            @Override
            public void run() {
                DistributedReentrantLock lock = coordService.getReentrantLock("examples", "exclusive_lock1");
                logger.info(this.getName() + ":  attempting to acquire lock...");
                lock.lock();
                try {
                    long sleepInterval = (long) (lockHoldTimeMillis * Math.random());
                    logger.info("{}:  acquired lock:  will hold for {} millis:  holdCount={}", new Object[] {
                            this.getName(), sleepInterval, (lock).getHoldCount() });
                    Thread.sleep(sleepInterval);
                } catch (InterruptedException e) {
                    logger.info("Interrupted:  " + e, e);
                } finally {
                    lock.unlock();
                    lock.destroy();
                }
            }
        };
        t2.setName("T2");
        t2.setDaemon(true);
        t2.start();

        Thread t3 = new Thread() {
            @Override
            public void run() {
                DistributedReentrantLock lock = coordService.getReentrantLock("examples", "exclusive_lock1");
                logger.info(this.getName() + ":  attempting to acquire lock...");
                lock.lock();
                try {
                    long sleepInterval = (long) (lockHoldTimeMillis * Math.random());
                    logger.info("{}:  acquired lock:  will hold for {} millis:  holdCount={}", new Object[] {
                            this.getName(), sleepInterval, (lock).getHoldCount() });
                    Thread.sleep(sleepInterval);
                } catch (InterruptedException e) {
                    logger.info("Interrupted:  " + e, e);
                } finally {
                    lock.unlock();
                    lock.destroy();
                }
            }
        };
        t3.setName("T3");
        t3.setDaemon(true);
        t3.start();
    }

    public static void coordinationServiceConfiguredSemaphoreExample(Reign reign) throws Exception {
        // this is how you would normally get a service
        final CoordinationService coordService = reign.getService("coord");
        final ConfService confService = reign.getService("conf");

        // configure semaphore
        ConfiguredPermitPoolSize.setSemaphoreConf(confService, "examples", "semaphore2", 5);

        coordService.observe("examples", "semaphore2", new SimpleSemaphoreObserver() {
            @Override
            public void revoked(DistributedSemaphore semaphore, String reservationId) {
                logger.info("***** Observer:  permit REVOKED:  reservationId={}", reservationId);
            }
        });

        // wait a few seconds to make sure semaphore configuration is persisted
        // to ZK
        Thread.sleep(5000);

        final int lockHoldTimeMillis = 15000;

        Thread t1 = new Thread() {
            @Override
            public void run() {
                DistributedSemaphore semaphore = coordService
                        .getConfiguredSemaphore("examples", "semaphore2", 4, false);
                logger.info(this.getName() + ":  attempting to acquire lock...");

                int permitsToAcquire = 4;
                try {
                    semaphore.acquire(permitsToAcquire);
                    logger.info("{}:  acquired {} permit(s):  will hold for {} seconds...", new Object[] {
                            this.getName(), permitsToAcquire, lockHoldTimeMillis / 1000 });
                    Thread.sleep(lockHoldTimeMillis);
                } catch (InterruptedException e) {
                    logger.info("Interrupted:  " + e, e);
                } finally {
                    semaphore.release(permitsToAcquire);
                }
            }
        };
        t1.setName("T1");
        t1.setDaemon(true);
        t1.start();

        Thread t2 = new Thread() {
            @Override
            public void run() {
                DistributedSemaphore semaphore = coordService
                        .getConfiguredSemaphore("examples", "semaphore2", 4, false);
                logger.info(this.getName() + ":  attempting to acquire lock...");

                int permitsToAcquire = 2;
                try {
                    semaphore.acquire(permitsToAcquire);
                    logger.info("{}:  acquired {} permit(s):  will hold for {} seconds...", new Object[] {
                            this.getName(), permitsToAcquire, lockHoldTimeMillis / 1000 });
                    Thread.sleep(lockHoldTimeMillis);
                } catch (InterruptedException e) {
                    logger.info("Interrupted:  " + e, e);
                } finally {
                    semaphore.release(permitsToAcquire);
                }
            }
        };
        t2.setName("T2");
        t2.setDaemon(true);
        t2.start();

        Thread t3 = new Thread() {
            @Override
            public void run() {
                DistributedSemaphore semaphore = coordService
                        .getConfiguredSemaphore("examples", "semaphore2", 4, false);
                logger.info(this.getName() + ":  attempting to acquire lock...");

                try {
                    semaphore.acquire();
                    logger.info("{}:  acquired permit:  will hold for {} seconds...", this.getName(),
                            lockHoldTimeMillis / 1000);
                    Thread.sleep(lockHoldTimeMillis);
                } catch (InterruptedException e) {
                    logger.info("Interrupted:  " + e, e);
                } finally {
                    semaphore.release();
                }
            }
        };
        t3.setName("T3");
        t3.setDaemon(true);
        t3.start();

        Thread t4 = new Thread() {
            @Override
            public void run() {
                DistributedSemaphore semaphore = coordService
                        .getConfiguredSemaphore("examples", "semaphore2", 4, false);
                logger.info(this.getName() + ":  attempting to acquire lock...");

                try {
                    semaphore.acquire();
                    logger.info("{}:  acquired permit:  will hold for {} seconds...", this.getName(),
                            lockHoldTimeMillis / 1000);
                    Thread.sleep(lockHoldTimeMillis);
                } catch (InterruptedException e) {
                    logger.info("Interrupted:  " + e, e);
                } finally {
                    semaphore.release();
                }
            }
        };
        t4.setName("T4");
        t4.setDaemon(true);
        t4.start();

    }

    public static void coordinationServiceFixedSemaphoreExample(Reign reign) throws Exception {
        // this is how you would normally get a service
        final CoordinationService coordService = (CoordinationService) reign.getService("coord");

        final int lockHoldTimeMillis = 15000;

        Thread t1 = new Thread() {
            @Override
            public void run() {
                DistributedSemaphore semaphore = coordService.getFixedSemaphore("examples", "semaphore1", 4);
                logger.info(this.getName() + ":  attempting to acquire lock...");

                int permitsToAcquire = 4;
                try {
                    semaphore.acquire(permitsToAcquire);
                    logger.info("{}:  acquired {} permit(s):  will hold for {} seconds...", new Object[] {
                            this.getName(), permitsToAcquire, lockHoldTimeMillis / 1000 });
                    Thread.sleep(lockHoldTimeMillis);
                } catch (InterruptedException e) {
                    logger.info("Interrupted:  " + e, e);
                } finally {
                    semaphore.release(permitsToAcquire);
                    semaphore.destroy();
                }
            }
        };
        t1.setName("T1");
        t1.setDaemon(true);
        t1.start();

        Thread t2 = new Thread() {
            @Override
            public void run() {
                DistributedSemaphore semaphore = coordService.getFixedSemaphore("examples", "semaphore1", 4);
                logger.info(this.getName() + ":  attempting to acquire lock...");

                int permitsToAcquire = 2;
                try {
                    semaphore.acquire(permitsToAcquire);
                    logger.info("{}:  acquired {} permit(s):  will hold for {} seconds...", new Object[] {
                            this.getName(), permitsToAcquire, lockHoldTimeMillis / 1000 });
                    Thread.sleep(lockHoldTimeMillis);
                } catch (InterruptedException e) {
                    logger.info("Interrupted:  " + e, e);
                } finally {
                    semaphore.release(permitsToAcquire);
                    semaphore.destroy();
                }
            }
        };
        t2.setName("T2");
        t2.setDaemon(true);
        t2.start();

        Thread t3 = new Thread() {
            @Override
            public void run() {
                DistributedSemaphore semaphore = coordService.getFixedSemaphore("examples", "semaphore1", 4);
                logger.info(this.getName() + ":  attempting to acquire lock...");

                try {
                    semaphore.acquire();
                    logger.info("{}:  acquired permit:  will hold for {} seconds...", this.getName(),
                            lockHoldTimeMillis / 1000);
                    Thread.sleep(lockHoldTimeMillis);
                } catch (InterruptedException e) {
                    logger.info("Interrupted:  " + e, e);
                } finally {
                    semaphore.release();
                    semaphore.destroy();
                }
            }
        };
        t3.setName("T3");
        t3.setDaemon(true);
        t3.start();

        Thread t4 = new Thread() {
            @Override
            public void run() {
                DistributedSemaphore semaphore = coordService.getFixedSemaphore("examples", "semaphore1", 4);
                logger.info(this.getName() + ":  attempting to acquire lock...");

                try {
                    semaphore.acquire();
                    logger.info("{}:  acquired permit:  will hold for {} seconds...", this.getName(),
                            lockHoldTimeMillis / 1000);
                    Thread.sleep(lockHoldTimeMillis);
                } catch (InterruptedException e) {
                    logger.info("Interrupted:  " + e, e);
                } finally {
                    semaphore.release();
                    semaphore.destroy();
                }
            }
        };
        t4.setName("T4");
        t4.setDaemon(true);
        t4.start();

    }

    public static void coordinationServiceExclusiveLockExample(Reign reign) throws Exception {
        // this is how you would normally get a service
        final CoordinationService coordService = (CoordinationService) reign.getService("coord");

        final int lockHoldTimeMillis = 5000;

        Thread t1 = new Thread() {
            @Override
            public void run() {
                DistributedLock lock = coordService.getLock("examples", "exclusive_lock1");
                logger.info(this.getName() + ":  attempting to acquire lock...");
                for (int i = 0; i < 3; i++) {
                    lock.lock();
                    try {
                        long sleepInterval = (long) (lockHoldTimeMillis * Math.random());
                        logger.info("{}:  acquired lock:  will hold for {} millis...", this.getName(), sleepInterval);
                        Thread.sleep(sleepInterval);
                    } catch (InterruptedException e) {
                        logger.info("Interrupted:  " + e, e);
                    } finally {
                        lock.unlock();
                    }
                } // for
                lock.destroy();
            }
        };
        t1.setName("T1");
        t1.setDaemon(true);
        t1.start();

        Thread t2 = new Thread() {
            @Override
            public void run() {
                DistributedLock lock = coordService.getLock("examples", "exclusive_lock1");
                logger.info(this.getName() + ":  attempting to acquire lock...");
                for (int i = 0; i < 3; i++) {
                    lock.lock();
                    try {
                        long sleepInterval = (long) (lockHoldTimeMillis * Math.random());
                        logger.info("{}:  acquired lock:  will hold for {} millis...", this.getName(), sleepInterval);
                        Thread.sleep(sleepInterval);
                    } catch (InterruptedException e) {
                        logger.info("Interrupted:  " + e, e);
                    } finally {
                        lock.unlock();
                    }
                }// for
                lock.destroy();
            }
        };
        t2.setName("T2");
        t2.setDaemon(true);
        t2.start();

        Thread t3 = new Thread() {
            @Override
            public void run() {
                DistributedLock lock = coordService.getLock("examples", "exclusive_lock1");
                logger.info(this.getName() + ":  attempting to acquire lock...");
                for (int i = 0; i < 3; i++) {
                    lock.lock();
                    try {
                        long sleepInterval = (long) (lockHoldTimeMillis * Math.random());
                        logger.info("{}:  acquired lock:  will hold for {} millis...", this.getName(), sleepInterval);
                        Thread.sleep(sleepInterval);
                    } catch (InterruptedException e) {
                        logger.info("Interrupted:  " + e, e);
                    } finally {
                        lock.unlock();
                    }
                }// for
                lock.destroy();
            }
        };
        t3.setName("T3");
        t3.setDaemon(true);
        t3.start();
    }

    public static void coordinationServiceReadWriteLockExample(Reign reign) throws Exception {
        // this is how you would normally get a service
        final CoordinationService coordService = (CoordinationService) reign.getService("coord");

        final int lockHoldTimeMillis = 30000;

        Thread t1 = new Thread() {
            @Override
            public void run() {
                DistributedReadWriteLock rwLock = coordService.getReadWriteLock("examples", "rw_lock1");
                logger.info(this.getName() + ":  attempting to acquire lock...");
                rwLock.readLock().lock();
                try {
                    long sleepInterval = (long) (lockHoldTimeMillis * Math.random());
                    logger.info("{}:  acquired lock:  will hold for {} millis...", this.getName(), sleepInterval);
                    Thread.sleep(sleepInterval);
                } catch (InterruptedException e) {
                    logger.info("Interrupted:  " + e, e);
                } finally {
                    rwLock.readLock().unlock();
                    rwLock.destroy();
                }
            }
        };
        t1.setName("T1");
        t1.setDaemon(true);
        t1.start();

        Thread t2 = new Thread() {
            @Override
            public void run() {
                DistributedReadWriteLock rwLock = coordService.getReadWriteLock("examples", "rw_lock1");
                logger.info(this.getName() + ":  attempting to acquire lock...");
                rwLock.readLock().lock();
                try {
                    long sleepInterval = (long) (lockHoldTimeMillis * Math.random());
                    logger.info("{}:  acquired lock:  will hold for {} millis...", this.getName(), sleepInterval);
                    Thread.sleep(sleepInterval);
                } catch (InterruptedException e) {
                    logger.info("Interrupted:  " + e, e);
                } finally {
                    rwLock.readLock().unlock();
                    rwLock.destroy();
                }
            }
        };
        t2.setName("T2");
        t2.setDaemon(true);
        t2.start();

        Thread t3 = new Thread() {
            @Override
            public void run() {
                DistributedReadWriteLock rwLock = coordService.getReadWriteLock("examples", "rw_lock1");
                logger.info(this.getName() + ":  attempting to acquire lock...");
                rwLock.writeLock().lock();
                try {
                    long sleepInterval = (long) (lockHoldTimeMillis * Math.random());
                    logger.info("{}:  acquired lock:  will hold for {} millis...", this.getName(), sleepInterval);
                    Thread.sleep(sleepInterval);
                } catch (InterruptedException e) {
                    logger.info("Interrupted:  " + e, e);
                } finally {
                    rwLock.writeLock().unlock();
                    rwLock.destroy();
                }
            }
        };
        t3.setName("T3");
        t3.setDaemon(true);
        t3.start();

        Thread t4 = new Thread() {
            @Override
            public void run() {
                DistributedReadWriteLock rwLock = coordService.getReadWriteLock("examples", "rw_lock1");
                logger.info(this.getName() + ":  attempting to acquire lock...");
                rwLock.writeLock().lock();
                try {
                    long sleepInterval = (long) (lockHoldTimeMillis * Math.random());
                    logger.info("{}:  acquired lock:  will hold for {} millis...", this.getName(), sleepInterval);
                    Thread.sleep(sleepInterval);
                } catch (InterruptedException e) {
                    logger.info("Interrupted:  " + e, e);
                } finally {
                    rwLock.writeLock().unlock();
                    rwLock.destroy();
                }
            }
        };
        t4.setName("T4");
        t4.setDaemon(true);
        t4.start();
    }
}