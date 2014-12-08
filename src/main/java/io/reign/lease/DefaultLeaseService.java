/*
 Copyright 2014 Yen Pai ypai@reign.io

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

import io.reign.AbstractObserver;
import io.reign.AbstractService;
import io.reign.PathType;
import io.reign.lease.LeaseEvent.LeaseEventType;
import io.reign.mesg.RequestMessage;
import io.reign.mesg.ResponseMessage;
import io.reign.presence.ServiceInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * 
 * @author ypai
 *
 */
public class DefaultLeaseService extends AbstractService implements LeaseService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultLeaseService.class);

    private ExecutorService executorService;

    private final Cache<String, Boolean> releasedLeases = CacheBuilder.newBuilder().concurrencyLevel(1)
            .maximumSize(256)
            .expireAfterWrite(120, TimeUnit.SECONDS).build();

    private ZkLeaseReservationManager reservationManager = null;

    @Override
    public ResponseMessage handleMessage(RequestMessage message) {
        // TODO: implement
        return null;
    }

    @Override
    public void init() {
        executorService = Executors.newSingleThreadExecutor();
        reservationManager = new ZkLeaseReservationManager(getContext().getZkClient(), getContext().getPathScheme());

        Runnable supervisorRunnable = new LeaseSupervisorRunnable();
        executorService.submit(supervisorRunnable);
    }

    public class LeaseSupervisorRunnable implements Runnable {

        private static final long DEFAULT_GROOM_INTERVAL_MS = 10000;
        private static final long DEFAULT_SUPERVISOR_CHECK_INTERVAL_MS = 60000;

        private volatile long nextSupervisorCheckTimestamp = -1;

        private volatile long nextGroomTimestamp = -1;

        private volatile long nextGroomBaseTimestamp = System.currentTimeMillis();

        long getNextGroomTimestamp(long baseTimestamp, List<String> supervisorNodeIdList, String nodeId) {
            logger.trace("LEASE_SUPERVISOR:  sorting supervisorNodeIdList");
            Collections.sort(supervisorNodeIdList);
            long tmpNextGroomTimestamp = baseTimestamp;
            for (String supervisorNodeId : supervisorNodeIdList) {
                tmpNextGroomTimestamp += DEFAULT_GROOM_INTERVAL_MS;
                logger.trace("LEASE_SUPERVISOR:  nodeId={}; supervisorNodeId={}; tmpNextGroomTimestamp={}", nodeId,
                        supervisorNodeId, tmpNextGroomTimestamp);
                if (supervisorNodeId.equals(nodeId)) {
                    break;
                }
            }
            return tmpNextGroomTimestamp;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    doRun();
                } catch (Exception e) {
                    logger.error("" + e, e);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error("" + e, e);
                }
            }
        }

        public void doRun() {
            long currentTimestamp = System.currentTimeMillis();

            // get list of candidate supervisor nodes
            if (currentTimestamp >= nextSupervisorCheckTimestamp || nextGroomTimestamp == -1) {
                logger.trace(
                        "LEASE_SUPERVISOR:  calculating supervisor check/groom execution times:  currentTimestamp={}; nextSupervisorCheckTimestamp={}",
                        currentTimestamp, nextSupervisorCheckTimestamp);

                // get list of supervisor nodes
                ServiceInfo supervisorServiceInfo = getContext().presence()
                        .cluster(getContext().getPathScheme().getFrameworkClusterId())
                        .serviceInfo("server");
                List<String> supervisorNodeIdList = supervisorServiceInfo.getNodeIdList();
                if (supervisorNodeIdList.size() > 0) {

                    // copy list because it's read-only and we need to be able to sort it in place
                    // logger.trace("LEASE_SUPERVISOR:  supervisorNodeIdList.size={}", supervisorNodeIdList.size());
                    List<String> supervisorNodeIdListCopy = new ArrayList<String>(supervisorNodeIdList);

                    // get candidate next groom timestamp
                    long candidateNextGroomTimestamp = getNextGroomTimestamp(nextGroomBaseTimestamp,
                            supervisorNodeIdListCopy,
                            getContext().getNodeId());

                    // logger.trace("LEASE_SUPERVISOR:  candidateNextGroomTimestamp={}", candidateNextGroomTimestamp);

                    // only set next groom execution if it has been reset or we want to execute sooner
                    if (nextGroomTimestamp < 0 || nextGroomTimestamp > candidateNextGroomTimestamp) {
                        nextGroomTimestamp = candidateNextGroomTimestamp;

                        // set next groom base timestamp
                        nextGroomBaseTimestamp = nextGroomTimestamp + (supervisorNodeIdList.size() - 1)
                                * DEFAULT_GROOM_INTERVAL_MS;
                    }

                    // set next supervisor list check
                    nextSupervisorCheckTimestamp = currentTimestamp + DEFAULT_SUPERVISOR_CHECK_INTERVAL_MS;

                    logger.info(
                            "LEASE_SUPERVISOR:  setting supervisor check/groom execution times:  supervisorNodeIdList.size={}; currentTimestamp={}; nextSupervisorCheckTimestamp={}; nextGroomBaseTimestamp={}; nextGroomTimestamp={}",
                            supervisorNodeIdList.size(), currentTimestamp, nextSupervisorCheckTimestamp,
                            nextGroomBaseTimestamp,
                            nextGroomTimestamp);
                }
            }

            // do grooming of leases
            currentTimestamp = System.currentTimeMillis();
            if (currentTimestamp >= nextGroomTimestamp && nextGroomTimestamp > 0) {
                // list clusters
                List<String> clusterIds = getContext().presence().clusters();
                for (final String clusterId : clusterIds) {
                    // only groom if this node is a member of cluster
                    if (!getContext().presence().memberOf(clusterId)) {
                        logger.info("LEASE_SUPERVISOR:  not a member of cluster:  skipping:  clusterId={}", clusterId);
                        continue;
                    }

                    // go through lease pools and delete expired leases
                    List<String> poolIds = poolIds(clusterId);
                    for (final String poolId : poolIds) {
                        logger.info("LEASE_SUPERVISOR:  grooming lease pool:  clusterId={}; poolId={}", clusterId,
                                poolId);
                        List<String> poolSizes = poolSizes(clusterId, poolId);
                        for (String poolSizeString : poolSizes) {
                            final int poolSize = Integer.parseInt(poolSizeString);

                            List<String> leaseIds = leaseIds(clusterId, poolId, poolSize);
                            leaseIds = LeaseUtil.sortReservationList(leaseIds);
                            int limit = Math.min(leaseIds.size(), poolSize);
                            for (int i = 0; i < limit; i++) {
                                String leaseId = leaseIds.get(i);
                                try {
                                    long durationMillis = LeaseUtil.durationMillisFromLeaseId(leaseId);
                                    String leasePath = LeaseUtil.leasePath(getContext().getPathScheme(),
                                            clusterId,
                                            poolId, poolSize,
                                            leaseId);
                                    Stat stat = getContext().getZkClient().exists(leasePath, false);
                                    long acquiredTimestamp = stat.getMtime();
                                    if (stat != null) {
                                        long checkTimestamp = System.currentTimeMillis();
                                        if (checkTimestamp > acquiredTimestamp + durationMillis) {
                                            // expired
                                            logger.warn(
                                                    "LEASE_SUPERVISOR:  REVOKING expired lease:  path={}; checkTimestamp={}; durationMillis={}; acquiredTimestamp={}; heldMillis={}",
                                                    leasePath, checkTimestamp, durationMillis, acquiredTimestamp,
                                                    checkTimestamp - acquiredTimestamp);
                                            getContext().getZkClient().delete(leasePath, -1);
                                        } else {
                                            logger.trace(
                                                    "LEASE_SUPERVISOR:  lease is still valid:  path={}; checkTimestamp={}; durationMillis={}; acquiredTimestamp={}; heldMillis={}",
                                                    leasePath, checkTimestamp, durationMillis, acquiredTimestamp,
                                                    checkTimestamp - acquiredTimestamp);
                                        }
                                    }// if
                                } catch (InterruptedException e) {
                                    logger.warn("Interrupted:  " + e, e);
                                } catch (Exception e) {
                                    logger.error("" + e, e);
                                }
                            }// for leaseIds

                        }// for poolSizes
                    }// for poolIds
                }// for clusterIds

                // reset so that another execution timestamp will get set
                nextGroomTimestamp = -1;
            }
        }// run()
    }

    @Override
    public void destroy() {
        if (reservationManager != null) {
            reservationManager.shutdown();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    List<String> poolSizes(String clusterId, String poolId) {
        String partialLeasePoolPath = getContext().getPathScheme().getAbsolutePath(PathType.LEASE, clusterId, poolId);
        try {
            return getContext().getZkClient().getChildren(partialLeasePoolPath, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    List<String> poolIds(String clusterId) {
        String leasePoolsPath = getContext().getPathScheme().getAbsolutePath(PathType.LEASE, clusterId);
        try {
            return getContext().getZkClient().getChildren(leasePoolsPath, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    List<String> leaseIds(String clusterId, String poolId, int poolSize) {
        String fullLeasePoolPath = LeaseUtil.leasePoolPath(getContext().getPathScheme(), clusterId, poolId, poolSize);
        try {
            return getContext().getZkClient().getChildren(fullLeasePoolPath, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> pools(String clusterId, String poolId) {
        String clusterLeasePoolBasePath = getContext().getPathScheme().getAbsolutePath(PathType.LEASE, clusterId);
        try {
            return getContext().getZkClient().getChildren(clusterLeasePoolBasePath, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Observable<LeaseEvent> acquire(final String clusterId, final String poolId, final int poolSize,
            final long durationMillis) {
        return acquire(clusterId, poolId, poolSize, durationMillis, -1);
    }

    @Override
    public Observable<LeaseEvent> tryAcquire(final String clusterId, final String poolId, final int poolSize,
            final long durationMillis) {
        return acquire(clusterId, poolId, poolSize, durationMillis, 0);
    }

    Observable<LeaseEvent> acquire(final String clusterId, final String poolId, final int poolSize,
            final long durationMillis, final long timeoutMillis) {
        throwExceptionIfInvalidParameters(clusterId, poolId, poolSize, durationMillis);

        Observable<LeaseEvent> observable = Observable.create(new Observable.OnSubscribe<LeaseEvent>() {

            @Override
            public void call(final Subscriber<? super LeaseEvent> subscriber) {
                String poolPath = LeaseUtil.leasePoolPath(getContext().getPathScheme(), clusterId, poolId, poolSize);

                logger.trace("Acquiring:  poolPath={}", poolPath);

                String acquiredPath = null;
                try {
                    acquiredPath = reservationManager.acquire(getContext().getNodeId(), poolPath, poolSize,
                            durationMillis,
                            getContext()
                                    .getDefaultZkAclList(), timeoutMillis,
                            true);

                    // update data to update modified timestamp
                    byte[] reservationData = LeaseUtil.reservationData(getContext().getNodeId(), durationMillis);
                    Stat stat = getContext().getZkClient().setData(acquiredPath, reservationData, -1);
                    final String leaseId = getContext().getPathScheme().lastToken(acquiredPath);

                    // create lease object
                    final Lease lease = lease(DefaultLeaseService.this, getContext().getNodeId(), clusterId, poolId,
                            poolSize,
                            leaseId,
                            durationMillis,
                            stat.getMtime());

                    // notify subscriber
                    subscriber.onNext(new LeaseEvent(LeaseEventType.ACQUIRED, lease));

                    final String finalAcquiredPath = acquiredPath;
                    getContext().getObserverManager().put(acquiredPath, new AbstractObserver() {

                        @Override
                        public void nodeDataChanged(byte[] updatedData, byte[] previousData, Stat updatedStat) {
                            Lease lease = lease(DefaultLeaseService.this, getContext().getNodeId(), clusterId, poolId,
                                    poolSize,
                                    leaseId,
                                    durationMillis,
                                    updatedStat.getMtime());
                            subscriber.onNext(new LeaseEvent(LeaseEventType.RENEWED, lease));
                        }

                        @Override
                        public void nodeDeleted(byte[] previousData, List<String> previousChildList) {
                            try {
                                if (releasedLeases.getIfPresent(finalAcquiredPath) != null) {
                                    subscriber.onNext(new LeaseEvent(LeaseEventType.RELEASED, lease));
                                } else {
                                    subscriber.onNext(new LeaseEvent(LeaseEventType.REVOKED, lease));
                                }
                                subscriber.onCompleted();
                            } finally {
                                getContext().getObserverManager().remove(finalAcquiredPath, this);
                            }
                        }

                    });

                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });

        return observable.subscribeOn(Schedulers.io()).observeOn(Schedulers.computation());
    }

    @Override
    public Observable<LeaseEvent> renew(final String clusterId, final String poolId, final int poolSize,
            final String leaseId) {
        throwExceptionIfInvalidParameters(clusterId, poolId, poolSize, leaseId);

        final String leasePath = LeaseUtil
                .leasePath(getContext().getPathScheme(), clusterId, poolId, poolSize, leaseId);
        Observable<LeaseEvent> observable = Observable.create(new Observable.OnSubscribe<LeaseEvent>() {
            @Override
            public void call(final Subscriber<? super LeaseEvent> subscriber) {
                logger.trace("Renewing:  leasePath={}", leasePath);
                try {
                    // update data to update modified timestamp
                    long durationMillis = LeaseUtil.durationMillisFromLeaseId(leaseId);
                    byte[] reservationData = LeaseUtil.reservationData(getContext().getNodeId(),
                            durationMillis);
                    Stat stat = getContext().getZkClient().setData(leasePath, reservationData, -1);
                    final Lease lease = lease(DefaultLeaseService.this, getContext().getNodeId(), clusterId, poolId,
                            poolSize,
                            leaseId,
                            durationMillis,
                            stat.getMtime());
                    subscriber.onNext(new LeaseEvent(LeaseEventType.RENEWED, lease));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });

        return observable.subscribeOn(Schedulers.io()).observeOn(Schedulers.computation());
    }

    @Override
    public Observable<LeaseEvent> release(String clusterId, String poolId, int poolSize, String leaseId) {
        throwExceptionIfInvalidParameters(clusterId, poolId, poolSize, leaseId);

        String leasePath = LeaseUtil.leasePath(getContext().getPathScheme(), clusterId, poolId, poolSize,
                leaseId);

        releasedLeases.put(leasePath, true);

        return delete(leasePath, clusterId, poolId, poolSize, leaseId);
    }

    @Override
    public Observable<LeaseEvent> revoke(final String clusterId, final String poolId, final int poolSize,
            final String leaseId) {
        throwExceptionIfInvalidParameters(clusterId, poolId, poolSize, leaseId);

        final String leasePath = LeaseUtil.leasePath(getContext().getPathScheme(), clusterId, poolId, poolSize,
                leaseId);

        return delete(leasePath, clusterId, poolId, poolSize, leaseId);
    }

    Observable<LeaseEvent> delete(final String leasePath, final String clusterId, final String poolId,
            final int poolSize, final String leaseId) {

        Observable<LeaseEvent> observable = Observable.create(new Observable.OnSubscribe<LeaseEvent>() {
            @Override
            public void call(final Subscriber<? super LeaseEvent> subscriber) {
                logger.trace("Deleting:  leasePath={}", leasePath);
                try {
                    try {
                        getContext().getZkClient().delete(leasePath, -1);
                    } catch (KeeperException e) {
                        if (e.code() == KeeperException.Code.NONODE) {
                            // already deleted, so just log
                            if (logger.isTraceEnabled()) {
                                logger.trace("Already deleted ZK node:  " + e + ":  path=" + leasePath);
                            }
                        } else {
                            throw e;
                        }
                    }
                    final Lease lease = lease(DefaultLeaseService.this, null, clusterId, poolId,
                            poolSize,
                            leaseId,
                            LeaseUtil.durationMillisFromLeaseId(leaseId),
                            -1);
                    if (releasedLeases.getIfPresent(leasePath) != null) {
                        subscriber.onNext(new LeaseEvent(LeaseEventType.RELEASED, lease));
                    } else {
                        subscriber.onNext(new LeaseEvent(LeaseEventType.REVOKED, lease));
                    }
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
        return observable.subscribeOn(Schedulers.io()).observeOn(Schedulers.computation());
    }

    void throwExceptionIfInvalidParameters(final String clusterId, final String poolId, final int poolSize,
            final long durationMillis) {
        if (StringUtils.isBlank(clusterId)) {
            throw new IllegalArgumentException("clusterId cannot be null or blank!");
        }
        if (StringUtils.isBlank(poolId)) {
            throw new IllegalArgumentException("poolId cannot be null or empty!");
        }
        if (poolSize < 1) {
            throw new IllegalArgumentException("poolSize must be >=1!");
        }
        if (durationMillis < 100) {
            throw new IllegalArgumentException("durationMillis must be >=100!");
        }
    }

    void throwExceptionIfInvalidParameters(final String clusterId, final String poolId, final int poolSize,
            String leaseId) {
        if (StringUtils.isBlank(clusterId)) {
            throw new IllegalArgumentException("clusterId cannot be null or blank!");
        }
        if (StringUtils.isBlank(poolId)) {
            throw new IllegalArgumentException("poolId cannot be null or empty!");
        }
        if (poolSize < 1) {
            throw new IllegalArgumentException("poolSize must be >=1!");
        }
        if (StringUtils.isBlank(leaseId)) {
            throw new IllegalArgumentException("leaseId cannot be null or empty!");
        }
    }

    Lease lease(final LeaseService leaseService, final String holderId, final String clusterId, final String poolId,
            final int poolSize,
            final String leaseId,
            final long durationMillis, final long acquiredTimestamp) {
        return new Lease() {

            @Override
            public Observable<LeaseEvent> renew() {
                return leaseService.renew(clusterId, poolId, poolSize, leaseId);
            }

            @Override
            public Observable<LeaseEvent> release() {
                return leaseService.release(clusterId, poolId, poolSize, leaseId);
            }

            @Override
            public Subscription renew(LeaseEventSubscriber subscriber) {
                return renew().subscribe(subscriber);
            }

            @Override
            public Subscription release(LeaseEventSubscriber subscriber) {
                return release().subscribe(subscriber);
            }

            @Override
            public String holderId() {
                if (holderId == null) {
                    throw new UnsupportedOperationException("Not available!");
                }
                return holderId;
            }

            @Override
            public String clusterId() {
                return clusterId;
            }

            @Override
            public String poolId() {
                return poolId;
            }

            @Override
            public String id() {
                return leaseId;
            }

            @Override
            public long durationMillis() {
                return durationMillis;
            }

            @Override
            public long acquiredTimestamp() {
                if (acquiredTimestamp < 1) {
                    throw new UnsupportedOperationException("Not available!");
                }
                return acquiredTimestamp;
            }

            @Override
            public long expiryTimestamp() {
                return acquiredTimestamp() + durationMillis;
            }

            @Override
            public boolean expired() {
                return expiryTimestamp() - System.currentTimeMillis() < 0;
            }

        };
    }
}
