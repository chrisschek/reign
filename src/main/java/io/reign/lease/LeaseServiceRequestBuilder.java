package io.reign.lease;

import java.util.concurrent.TimeUnit;

public class LeaseServiceRequestBuilder {

	public static final int DEFAULT_LEASE_POOL_SIZE = 1;
	public static final int DEFAULT_LEASE_DURATION = 60;
	public static final TimeUnit DEFAULT_LEASE_DURATION_UNIT = TimeUnit.SECONDS;

	private String clusterId;
	private String leaseId;

	private int leaseDuration = DEFAULT_LEASE_DURATION;
	private TimeUnit leaseDurationUnit = DEFAULT_LEASE_DURATION_UNIT;

	private LeaseService leaseService;
	private int poolSize = DEFAULT_LEASE_POOL_SIZE;

	public LeaseServiceRequestBuilder(LeaseService leaseService) {
		this.leaseService = leaseService;
	}

	public LeaseServiceRequestBuilder cluster(String clusterId) {
		this.clusterId = clusterId;
		return this;
	}

	public LeaseServiceRequestBuilder leaseId(String leaseId) {
		this.leaseId = leaseId;
		return this;
	}

	public LeaseServiceRequestBuilder duration(int leaseDuration, TimeUnit leaseDurationUnit) {
		this.leaseDuration = leaseDuration;
		this.leaseDurationUnit = leaseDurationUnit;
		return this;
	}

	public LeaseServiceRequestBuilder poolSize(int poolSize) {
		this.poolSize = poolSize;
		return this;
	}

	public void request(LeaseObserver leaseObserver) {
		leaseService.request(clusterId, leaseId, poolSize, leaseDuration, leaseDurationUnit, leaseObserver);
	}

	public void observe(LeaseObserver leaseObserver) {
		leaseService.observe(clusterId, leaseId, poolSize, leaseDuration, leaseDurationUnit, leaseObserver);
	}
}
