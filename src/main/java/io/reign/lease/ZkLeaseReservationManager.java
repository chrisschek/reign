/*
 Copyright 2013 Yen Pai ypai@reign.io

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package io.reign.lease;

import io.reign.PathScheme;
import io.reign.ZkClient;
import io.reign.util.ZkClientUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.ACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains basic functionality for creating Lock/Semaphore functionality using ZooKeeper.
 * 
 * @author ypai
 * 
 */
class ZkLeaseReservationManager {

    private static final Logger logger = LoggerFactory.getLogger(ZkLeaseReservationManager.class);

    private final ZkClient zkClient;
    private final PathScheme pathScheme;
    private final ZkClientUtil zkUtil = new ZkClientUtil();
    private volatile boolean shutdown = false;

    ZkLeaseReservationManager(ZkClient zkClient, PathScheme pathScheme) {
        super();
        this.zkClient = zkClient;
        this.pathScheme = pathScheme;

    }

    public void shutdown() {
        this.shutdown = true;
    }

    /**
     * @return path of acquired lease or null
     */
    public String acquire(String holderId, String poolPath,
            int poolSize, long durationMillis, List<ACL> aclList, long waitTimeoutMs, boolean interruptible)
            throws InterruptedException {

        if (poolSize < 1) {
            throw new IllegalArgumentException("poolSize must be >= 1!");
        }

        if (waitTimeoutMs < -1) {
            throw new IllegalArgumentException("waitTimeoutMs must be -1 (no limit) or >= 0!");
        }

        ZkReservationWatcher reservationWatcher = null;
        try {
            long startTimestamp = System.currentTimeMillis();

            // holder data in JSON
            byte[] reservationData = LeaseUtil.reservationData(holderId, durationMillis);

            // path to reservation node (to "get in line")
            String reservationPrefix = LeaseUtil.reservationPathPrefix(pathScheme, poolPath, durationMillis);

            // create reservation sequential node
            String reservationPath = zkUtil.updatePath(zkClient, pathScheme, reservationPrefix,
                    reservationData, aclList, CreateMode.EPHEMERAL_SEQUENTIAL, -1);

            // path token (last part of path)
            String reservationToken = reservationPath.substring(reservationPath.lastIndexOf('/') + 1);

            if (logger.isDebugEnabled()) {
                logger.debug("Attempting to acquire:  holderId={}; reservationPath={}",
                        holderId, reservationPath);
            }

            String acquiredPath = null;
            do {
                try {
                    /** get child (reservation) list and watch **/

                    // create reservation watcher for wait/notify
                    if (reservationWatcher == null) {
                        reservationWatcher = new ZkReservationWatcher(poolPath, reservationPath);
                    }

                    // get reservation list with watch
                    List<String> reservationList = zkClient.getChildren(poolPath, reservationWatcher);

                    /** see if can acquire right away **/
                    if (reservationList.size() <= poolSize) {
                        acquiredPath = reservationPath;
                        break;
                    }

                    /** check to see if reservation makes the pool size cut-off **/
                    // sort child list
                    reservationList = LeaseUtil.sortReservationList(reservationList);

                    // loop through children and see if we have acquired
                    for (int i = 0; i < poolSize; i++) {
                        String currentReservation = reservationList.get(i);

                        logger.trace(
                                "Checking if acquired:  i={}; poolSize={}; currentReservation={}; reservationToken={}",
                                i, poolSize, currentReservation, reservationToken);

                        if (reservationToken.equals(currentReservation)) {
                            acquiredPath = reservationPath;
                            logger.trace("Acquired:  i={}; poolSize={}; acquiredPath={}",
                                    i, poolSize, acquiredPath);
                            break;
                        }
                    }

                    /** see if we acquired **/
                    if (acquiredPath == null) {
                        // wait to acquire if not yet acquired
                        logger.debug(
                                "Waiting to acquire:  holderId={}; reservationPath={}; watchPath={}",
                                holderId, reservationPath, poolPath);

                        // always call wait() with timeout in case we missed an
                        // update that occurred before setting the watch
                        reservationWatcher.waitForEvent(waitTimeoutMs == -1 ? 15000 : waitTimeoutMs);

                    } else {
                        logger.debug("Acquired:  holderId={}; acquiredPath={}", holderId, acquiredPath);

                        // set watch on node representing the acquired lease so that we are notified if it
                        // is deleted outside of JVM and can notify observers
                        zkClient.exists(acquiredPath, true);
                        break;
                    }

                } catch (InterruptedException e) {
                    if (interruptible) {
                        throw e;
                    } else {
                        logger.info(
                                "Ignoring attempted interrupt while waiting for acquisition:  holderId={}; poolPath={}",
                                holderId, poolPath);
                    }
                }

            } while (!this.shutdown && acquiredPath == null
                    && (waitTimeoutMs == -1 || startTimestamp + waitTimeoutMs > System.currentTimeMillis()));

            // log if not acquired
            if (acquiredPath == null) {
                boolean releasedReservationPath = this.release(reservationPath);
                if (!releasedReservationPath) {
                    logger.warn(
                            "Unable to release the reservationPath; holderId={}; releasedReservationPath={};",
                            holderId, releasedReservationPath);

                    throw new RuntimeException("Unable to relinquish the reservationPath; holderId=" + holderId
                            + "; reservationPath=" + reservationPath
                            + "; releasedReservationPath=" + releasedReservationPath);
                }

                logger.warn(
                        "Could not acquire:  holderId={}; poolPath={}; waitTimeoutMillis={}; releasedReservationPath={};",
                        holderId, poolPath, waitTimeoutMs,
                        releasedReservationPath);

            }

            return acquiredPath;

        } catch (KeeperException e) {
            logger.error("Error trying to acquire:  " + e + ":  holderId=" + holderId + "; poolPath=" + poolPath, e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            logger.error("Error trying to acquire:  " + e + ":  holderId=" + holderId + "; poolPath=" + poolPath, e);
            throw new RuntimeException(e);
        } finally {
            // clean up reservation watcher as necessary
            if (reservationWatcher != null) {
                reservationWatcher.destroy();
            }
        }

    }

    /**
     * 
     * @param reservationPath
     * @return
     */
    public boolean release(String reservationPath) {
        if (reservationPath == null) {
            // likely already deleted
            logger.trace("Trying to delete ZK reservation node with invalid path:  path={}", reservationPath);
            return true;
        }// if

        try {
            logger.trace("Releasing:  path={}", reservationPath);

            zkClient.delete(reservationPath, -1);

            logger.debug("Releasing:  path={}", reservationPath);
            return true;

        } catch (KeeperException e) {
            if (e.code() == KeeperException.Code.NONODE) {
                // already deleted, so just log
                if (logger.isDebugEnabled()) {
                    logger.debug("Already deleted ZK reservation node:  " + e + "; path=" + reservationPath, e);
                }

                return true;

            } else {
                logger.error("Error while deleting ZK reservation node:  " + e + "; path=" + reservationPath, e);
                throw new IllegalStateException("Error while deleting ZK reservation node:  " + e + "; path="
                        + reservationPath, e);
            }
        } catch (Exception e) {
            logger.error("Error while deleting ZK reservation node:  " + e + "; path=" + reservationPath, e);
            throw new IllegalStateException("Error while deleting ZK reservation node:  " + e + "; path="
                    + reservationPath, e);
        }// try

    }// end method

    public static class ZkReservationWatcher implements Watcher {
        private static final Logger logger = LoggerFactory.getLogger(ZkReservationWatcher.class);

        private static AtomicInteger INSTANCES_OUTSTANDING = new AtomicInteger(0);

        private final String poolPath;

        private final String reservationPath;

        public static long instancesOutstanding() {
            return INSTANCES_OUTSTANDING.get();
        }

        public ZkReservationWatcher(String poolPath, String reservationPath) {
            INSTANCES_OUTSTANDING.incrementAndGet();

            this.poolPath = poolPath;
            this.reservationPath = reservationPath;

            if (logger.isDebugEnabled()) {
                logger.debug("Created:  instancesOutstanding={}; poolPath={}; reservationPath={}",
                        INSTANCES_OUTSTANDING.get(), poolPath, reservationPath);
            }
        }

        public void destroy() {
            // notify all waiters: shouldn't be any at this point
            synchronized (this) {
                this.notifyAll();
            }

            INSTANCES_OUTSTANDING.decrementAndGet();
            if (logger.isDebugEnabled()) {
                logger.debug("Destroyed:  instancesOutstanding={}; poolPath={}; reservationPath={}",
                        INSTANCES_OUTSTANDING.get(), poolPath, reservationPath);
            }
        }

        public void waitForEvent(long waitTimeoutMs) throws InterruptedException {
            if (waitTimeoutMs == 0) {
                return;
            }

            logger.debug("waitForEvent():  instancesOutstanding={}; poolPath={}; reservationPath={}",
                    INSTANCES_OUTSTANDING.get(), poolPath, reservationPath);

            synchronized (this) {
                if (waitTimeoutMs == -1) {
                    this.wait();

                } else {
                    this.wait(waitTimeoutMs);
                }
            }
        }

        @Override
        public void process(WatchedEvent event) {
            // log if DEBUG
            if (logger.isDebugEnabled()) {
                logger.debug("***** Received ZooKeeper Event:  {}",
                        ReflectionToStringBuilder.toString(event, ToStringStyle.DEFAULT_STYLE));

            }

            // process events
            switch (event.getType()) {
            case NodeCreated:
            case NodeChildrenChanged:
            case NodeDataChanged:
            case NodeDeleted:
                synchronized (this) {
                    this.notifyAll();

                    if (logger.isDebugEnabled()) {
                        logger.debug("Notifying waiting threads:  hashcode()=" + this.hashCode()
                                + "; instancesOutstanding=" + INSTANCES_OUTSTANDING.get() + "; poolPath=" + poolPath
                                + "; reservationPath=" + reservationPath);
                    }
                }
                break;

            case None:
                Event.KeeperState eventState = event.getState();
                if (eventState == Event.KeeperState.SyncConnected) {
                    // connection event: check children
                    synchronized (this) {
                        this.notifyAll();
                    }

                } else if (eventState == Event.KeeperState.Disconnected || eventState == Event.KeeperState.Expired) {
                    // disconnected: notifyAll so we can check children again on
                    // reconnection
                    synchronized (this) {
                        this.notifyAll();
                    }

                } else {
                    logger.warn("Unhandled event state:  "
                            + ReflectionToStringBuilder.toString(event, ToStringStyle.DEFAULT_STYLE));
                }
                break;

            default:
                logger.warn("Unhandled event type:  "
                        + ReflectionToStringBuilder.toString(event, ToStringStyle.DEFAULT_STYLE));
            }

            // }// if

        }// process()
    }// class

}
