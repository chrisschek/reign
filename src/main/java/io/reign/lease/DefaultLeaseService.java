package io.reign.lease;

import io.reign.ObserverManager;
import io.reign.PathScheme;
import io.reign.ReignContext;
import io.reign.ZkClient;
import io.reign.mesg.RequestMessage;
import io.reign.mesg.ResponseMessage;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.data.ACL;

public class DefaultLeaseService implements LeaseService {

    @Override
    public ReignContext getContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setContext(ReignContext serviceDirectory) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setDefaultZkAclList(List<ACL> defaultZkAclList) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setObserverManager(ObserverManager observerManager) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setPathScheme(PathScheme pathScheme) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setZkClient(ZkClient zkClient) {
        // TODO Auto-generated method stub

    }

    @Override
    public ResponseMessage handleMessage(RequestMessage message) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void init() {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public void request(String clusterId, String leaseId, int poolSize, long leaseDuration, TimeUnit leaseDurationUnit,
            LeaseObserver leaseObserver) {
        // TODO Auto-generated method stub

    }

    @Override
    public void observe(String clusterId, String leaseId, int poolSize, long leaseDuration, TimeUnit leaseDurationUnit,
            LeaseObserver leaseObserver) {
        // TODO Auto-generated method stub

    }

}
