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

package io.reign.presence;

import io.reign.AbstractObserver;
import io.reign.DataSerializer;
import io.reign.JsonDataSerializer;
import io.reign.NodeId;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author ypai
 * 
 * @param <T>
 */
public abstract class PresenceObserver<T> extends AbstractObserver {

    private static final DataSerializer<Map<String, String>> nodeAttributeSerializer = new JsonDataSerializer<Map<String, String>>();

    private String clusterId = null;
    private String serviceId = null;
    private NodeId nodeId = null;

    public abstract void updated(T updated, T previous);

    void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    void setNodeId(NodeId nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public void nodeChildrenChanged(List<String> updatedChildList, List<String> previousChildList) {
        if (serviceId != null) {
            ServiceInfo updated = new StaticServiceInfo(clusterId, serviceId, updatedChildList);
            ServiceInfo previous = new StaticServiceInfo(clusterId, serviceId, previousChildList);
            updated((T) updated, (T) previous);
        } else if (clusterId != null) {
            updated((T) updatedChildList, (T) previousChildList);
        }
    }

    @Override
    public void nodeDataChanged(byte[] updatedData, byte[] previousData) {
        if (nodeId != null) {
            Map<String, String> attributeMap = nodeAttributeSerializer.deserialize(updatedData);
            NodeInfo updated = new StaticNodeInfo(clusterId, serviceId, nodeId, attributeMap);
            Map<String, String> previousAttributeMap = nodeAttributeSerializer.deserialize(previousData);
            NodeInfo previous = new StaticNodeInfo(clusterId, serviceId, nodeId, previousAttributeMap);
            updated((T) updated, (T) previous);
        }
    }

    @Override
    public void nodeDeleted(byte[] previousData, List<String> previousChildList) {
        if (nodeId != null) {
            Map<String, String> previousAttributeMap = nodeAttributeSerializer.deserialize(previousData);
            NodeInfo previous = new StaticNodeInfo(clusterId, serviceId, nodeId, previousAttributeMap);
            updated(null, (T) previous);

        } else if (serviceId != null) {
            ServiceInfo previous = new StaticServiceInfo(clusterId, serviceId, previousChildList);
            updated(null, (T) previous);

        }
    }

    @Override
    public void nodeCreated(byte[] data, List<String> childList) {
        if (nodeId != null) {
            Map<String, String> attributeMap = nodeAttributeSerializer.deserialize(data);
            NodeInfo updated = new StaticNodeInfo(clusterId, serviceId, nodeId, attributeMap);
            updated((T) updated, null);

        } else if (serviceId != null) {
            if (childList != null && childList.size() > 0) {
                ServiceInfo updated = new StaticServiceInfo(clusterId, serviceId, childList);
                updated((T) updated, null);
            }
        }
    }

}
