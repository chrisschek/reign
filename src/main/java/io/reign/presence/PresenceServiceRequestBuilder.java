package io.reign.presence;

import io.reign.ServiceNodeInfo;

import java.util.List;

public class PresenceServiceRequestBuilder {

    private PresenceService presenceService;
    private String clusterId;
    private String serviceId;
    private String nodeId;
    private boolean autoUpdate = false;

    public PresenceServiceRequestBuilder(PresenceService presenceService) {

    }

    public PresenceServiceRequestBuilder autoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
        return this;
    }

    public PresenceServiceRequestBuilder cluster(String clusterId) {
        this.clusterId = clusterId;
        return this;
    }

    public PresenceServiceRequestBuilder service(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public PresenceServiceRequestBuilder node(String nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    public void await() {
        presenceService.waitUntilAvailable(clusterId, serviceId, -1);
    }

    public void await(long timeoutMillis) {
        presenceService.waitUntilAvailable(clusterId, serviceId, timeoutMillis);
    }

    public List<String> clusters() {
        return presenceService.getClusters();
    }

    public List<String> services() {
        return presenceService.getServices(clusterId);
    }

    public void show() {
        presenceService.show(clusterId, serviceId);
    }

    public void hide() {
        presenceService.hide(clusterId, serviceId);
    }

    public void dead() {
        if (nodeId == null) {
            presenceService.dead(clusterId, serviceId);
        } else {
            presenceService.dead(clusterId, serviceId, nodeId);
        }
    }

    public ServiceInfo serviceInfo() {
        if (!autoUpdate) {
            return presenceService.getServiceInfo(clusterId, serviceId);
        } else {
            ServiceInfo serviceInfo = new UpdatingServiceInfo(clusterId, serviceId, presenceService.getContext());
            return serviceInfo;
        }
    }

    public ServiceNodeInfo nodeInfo() {
        if (!autoUpdate) {
            return presenceService.getNodeInfo(clusterId, serviceId, nodeId);
        } else {
            ServiceNodeInfo serviceNodeInfo = new UpdatingServiceNodeInfo(clusterId, serviceId, nodeId,
                    presenceService.getContext());
            return serviceNodeInfo;
        }
    }
}
