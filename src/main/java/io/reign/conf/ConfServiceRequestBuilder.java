package io.reign.conf;

import java.util.Map;

public class ConfServiceRequestBuilder {

    private ConfService confService;
    private String clusterId;
    private String serviceId;
    private String confName;
    private boolean autoUpdate = false;

    public ConfServiceRequestBuilder(ConfService confService) {
        this.confService = confService;
    }

    public ConfServiceRequestBuilder autoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
        return this;
    }

    public ConfServiceRequestBuilder cluster(String clusterId) {
        this.clusterId = clusterId;
        return this;
    }

    public ConfServiceRequestBuilder service(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public ConfServiceRequestBuilder confName(String confName) {
        this.confName = confName;
        return this;
    }

    public <T extends Map> void observe(ConfObserver<T> observer) {
        confService.observeServiceConf(clusterId, serviceId, confName, observer);
    }

    public <T extends Map> T get() {
        if (autoUpdate) {
            UpdatingConf conf = new UpdatingConf(clusterId, serviceId, confName, confService.getContext());
            return (T) conf;
        } else {
            return confService.getServiceConf(clusterId, serviceId, confName);
        }
    }

    public <T extends Map> void put(T conf) {
        confService.putServiceConf(clusterId, serviceId, confName, conf);
    }

    public void remove() {
        confService.removeServiceConf(clusterId, serviceId, confName);
    }

}
