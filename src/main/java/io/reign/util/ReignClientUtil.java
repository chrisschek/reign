package io.reign.util;

import io.reign.DefaultNodeIdProvider.DefaultNodeId;

/**
 * 
 * @author ypai
 *
 */
public class ReignClientUtil {

    public static String getNodeId(String processId, String clientIpAddress, String clientHostname,
            int clientMessagingPort) {
        DefaultNodeId nodeId = new DefaultNodeId(processId, clientIpAddress, clientHostname, clientMessagingPort);
        return nodeId.toString();
    }
}
