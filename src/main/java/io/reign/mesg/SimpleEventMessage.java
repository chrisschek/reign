package io.reign.mesg;

import org.codehaus.jackson.annotate.JsonPropertyOrder;

/**
 * 
 * @author ypai
 * 
 */
@JsonPropertyOrder({ "event", "clusterId", "serviceId", "nodeId", "body" })
public class SimpleEventMessage<T> implements EventMessage<T> {

    private String event = null;
    private String clusterId = null;
    private String serviceId = null;
    private String nodeId = null;
    private T body = null;

    @Override
    public String getEvent() {
        return event;
    }

    @Override
    public String getClusterId() {
        return clusterId;
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public T getBody() {
        return body;
    }

    @Override
    public EventMessage<T> setEvent(String event) {
        this.event = event;
        return this;
    }

    @Override
    public EventMessage<T> setClusterId(String clusterId) {
        this.clusterId = clusterId;
        return this;
    }

    @Override
    public EventMessage setServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    @Override
    public EventMessage setNodeId(String nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    @Override
    public EventMessage<T> setBody(T body) {
        this.body = body;
        return this;
    }
}
