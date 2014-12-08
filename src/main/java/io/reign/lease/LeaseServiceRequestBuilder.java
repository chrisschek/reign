package io.reign.lease;

import rx.Observable;
import rx.Subscription;

/**
 * 
 * @author ypai
 *
 */
public class LeaseServiceRequestBuilder {

    public static final int DEFAULT_LEASE_POOL_SIZE = 1;

    private String clusterId;
    private String poolId;
    private int poolSize;

    private LeaseService leaseService;

    public LeaseServiceRequestBuilder(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    public LeaseServiceRequestBuilder cluster(String clusterId) {
        this.clusterId = clusterId;
        return this;
    }

    public LeaseServiceRequestBuilder pool(String poolId, int poolSize) {
        this.poolId = poolId;
        this.poolSize = poolSize;
        return this;
    }

    public Subscription tryAcquire(long durationMillis, LeaseEventSubscriber subscriber) {
        return leaseService.tryAcquire(clusterId, poolId, poolSize, durationMillis).subscribe(subscriber);
    }

    public Subscription acquire(long durationMillis, LeaseEventSubscriber subscriber) {
        return leaseService.acquire(clusterId, poolId, poolSize, durationMillis).subscribe(subscriber);
    }

    public Subscription renew(String leaseId, LeaseEventSubscriber subscriber) {
        return leaseService.renew(clusterId, poolId, poolSize, leaseId).subscribe(subscriber);
    }

    public Subscription release(String leaseId, LeaseEventSubscriber subscriber) {
        return leaseService.release(clusterId, poolId, poolSize, leaseId).subscribe(subscriber);
    }

    public Subscription revoke(String leaseId, LeaseEventSubscriber subscriber) {
        return leaseService.revoke(clusterId, poolId, poolSize, leaseId).subscribe(subscriber);
    }

    public Observable<LeaseEvent> tryAcquire(long durationMillis) {
        return leaseService.tryAcquire(clusterId, poolId, poolSize, durationMillis);
    }

    public Observable<LeaseEvent> acquire(long durationMillis) {
        return leaseService.acquire(clusterId, poolId, poolSize, durationMillis);
    }

    public Observable<LeaseEvent> renew(String leaseId) {
        return leaseService.renew(clusterId, poolId, poolSize, leaseId);
    }

    public Observable<LeaseEvent> release(String leaseId) {
        return leaseService.release(clusterId, poolId, poolSize, leaseId);
    }

    public Observable<LeaseEvent> revoke(String leaseId) {
        return leaseService.revoke(clusterId, poolId, poolSize, leaseId);
    }

}
