package io.reign.mesg;

/**
 * 
 * @author ypai
 * 
 */
public interface EventMessage<T> {

    public String getEvent();

    public String getClusterId();

    public String getServiceId();

    public String getNodeId();

    public T getBody();

    public EventMessage<T> setEvent(String event);

    public EventMessage<T> setClusterId(String clusterId);

    public EventMessage<T> setServiceId(String serviceId);

    public EventMessage<T> setNodeId(String nodeId);

    public EventMessage<T> setBody(T body);

}
