package io.reign.presence;

import io.reign.ServiceNodeInfo;

import java.util.List;

public class PresenceServiceRequestBuilder {

    private PresenceService presenceService;
    private String clusterId;
    private boolean autoUpdate = false;

    public PresenceServiceRequestBuilder(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    public PresenceServiceRequestBuilder autoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
        return this;
    }

    public PresenceServiceRequestBuilder cluster(String clusterId) {
        this.clusterId = clusterId;
        return this;
    }

    public void await(String serviceId) {
        presenceService.waitUntilAvailable(clusterId, serviceId, -1);
    }

    public void await(String serviceId, long timeoutMillis) {
        presenceService.waitUntilAvailable(clusterId, serviceId, timeoutMillis);
    }

    public List<String> clusters() {
        return presenceService.getClusters();
    }

    public List<String> services() {
        return presenceService.getServices(clusterId);
    }

    public void show(String serviceId) {
        presenceService.show(clusterId, serviceId);
    }

    public void hide(String serviceId) {
        presenceService.hide(clusterId, serviceId);
    }

    public void dead(String serviceId) {
        presenceService.dead(clusterId, serviceId);
    }

    public void dead(String serviceId, String nodeId) {
        presenceService.dead(clusterId, serviceId, nodeId);
    }

    public boolean memberOf(String clusterOrServiceId) {
        if (clusterId == null) {
            // interpret as clusterId
            return presenceService.isMemberOf(clusterOrServiceId);
        } else {
            // interpret as serviceId
            return presenceService.isMemberOf(clusterId, clusterOrServiceId);
        }
    }

    public ServiceInfo serviceInfo(String serviceId) {
        if (!autoUpdate) {
            return presenceService.getServiceInfo(clusterId, serviceId);
        } else {
            ServiceInfo serviceInfo = new UpdatingServiceInfo(clusterId, serviceId, presenceService.getContext());
            return serviceInfo;
        }
    }

    public ServiceNodeInfo nodeInfo(String serviceId, String nodeId) {
        if (!autoUpdate) {
            return presenceService.getNodeInfo(clusterId, serviceId, nodeId);
        } else {
            ServiceNodeInfo serviceNodeInfo = new UpdatingServiceNodeInfo(clusterId, serviceId, nodeId,
                    presenceService.getContext());
            return serviceNodeInfo;
        }
    }
}
