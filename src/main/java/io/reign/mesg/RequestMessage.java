package io.reign.mesg;

import io.reign.NodeAddress;

/**
 * 
 * @author ypai
 * 
 */
public interface RequestMessage<T> extends Message<T> {

    public String getTargetService();

    public RequestMessage<T> setTargetService(String targetService);

    /**
     * @return the sender of the request
     */
    public NodeAddress getSenderInfo();

    public RequestMessage<T> setSenderInfo(NodeAddress senderInfo);
}
