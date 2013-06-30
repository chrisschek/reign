package io.reign;

import io.reign.conf.ConfService;
import io.reign.coord.CoordinationService;
import io.reign.messaging.DefaultMessagingService;
import io.reign.messaging.MessagingService;
import io.reign.presence.PresenceService;
import io.reign.util.PathCache;
import io.reign.util.SimplePathCache;
import io.reign.zookeeper.ResilientZooKeeper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenient way to quickly configure the main framework object.
 * 
 * @author ypai
 * 
 */
public class ReignMaker {

    private static final Logger logger = LoggerFactory.getLogger(ReignMaker.class);

    private String zkConnectString;
    private int zkSessionTimeout = 30000;

    private int pathCacheMaxSize = 1024;
    private int pathCacheMaxConcurrencyLevel = 2;

    private PathCache pathCache = null;
    private ZkClient zkClient = null;

    private PathScheme pathScheme = null;

    private String reservedClusterId;

    private Integer messagingPort = null;

    private CanonicalIdMaker canonicalIdMaker = null;

    private final Map<String, Service> serviceMap = new HashMap<String, Service>();

    public ReignMaker allCoreServices() {
        PresenceService presenceService = new PresenceService();
        serviceMap.put("presence", presenceService);

        ConfService confService = new ConfService();
        serviceMap.put("conf", confService);

        CoordinationService coordService = new CoordinationService();
        serviceMap.put("coord", coordService);

        // DataService dataService = new DataService();
        // serviceMap.put("data", dataService);

        MessagingService messagingService = new DefaultMessagingService();
        serviceMap.put("messaging", messagingService);
        messagingPort = messagingService.getPort();

        return this;
    }

    public ReignMaker canonicalIdMaker(CanonicalIdMaker canonicalIdMaker) {
        this.canonicalIdMaker = canonicalIdMaker;
        return this;
    }

    public ReignMaker zkClient(String zkConnectString, int zkSessionTimeout) {
        this.zkConnectString = zkConnectString;
        this.zkSessionTimeout = zkSessionTimeout;
        return this;

    }

    public ReignMaker registerService(String serviceName, Service service) {
        serviceMap.put(serviceName, service);
        return this;
    }

    public ReignMaker pathCache(int maxSize, int concurrencyLevel) {
        this.pathCacheMaxSize = maxSize;
        this.pathCacheMaxConcurrencyLevel = concurrencyLevel;
        return this;
    }

    public ReignMaker pathCache(PathCache pathCache) {
        this.pathCache = pathCache;
        return this;
    }

    public ReignMaker zkClient(ZkClient zkClient) {
        this.zkClient = zkClient;
        return this;
    }

    public ReignMaker reservedClusterId(String reservedClusterId) {
        this.reservedClusterId = reservedClusterId;
        return this;
    }

    public Reign build() {

        Reign s = null;

        // see if we need to set defaults
        if (reservedClusterId == null) {
            reservedClusterId = "reign";
        }
        if (zkClient == null) {
            zkClient = defaultZkClient();
        }
        if (pathCache == null) {
            pathCache = defaultPathCache();
        }
        if (pathScheme == null) {
            pathScheme = defaultPathScheme(reservedClusterId);
        }
        if (canonicalIdMaker == null) {
            canonicalIdMaker = defaultCanonicalIdMaker();
        }

        // build
        s = new Reign(reservedClusterId, zkClient, pathScheme, pathCache, canonicalIdMaker);
        s.registerServices(serviceMap);

        return s;
    }

    ZkClient defaultZkClient() {
        if (zkConnectString == null || zkSessionTimeout <= 0) {
            throw new IllegalStateException("zkConnectString and zkSessionTimeout not configured!");
        }

        ZkClient zkClient = null;
        try {
            zkClient = new ResilientZooKeeper(zkConnectString, zkSessionTimeout);
        } catch (IOException e) {
            throw new IllegalStateException("Fatal error:  could not initialize Zookeeper client!");
        }
        return zkClient;
    }

    PathCache defaultPathCache() {
        if (pathCacheMaxSize < 1 || pathCacheMaxConcurrencyLevel < 1 || zkClient == null) {
            throw new IllegalStateException(
                    "zkClient, pathCacheMaxSize, and pathCacheMaxConcurrencyLevel must be configured to create default path cache!");
        }

        return new SimplePathCache(this.pathCacheMaxSize, this.pathCacheMaxConcurrencyLevel, zkClient);
    }

    PathScheme defaultPathScheme(String reservedClusterId) {
        return new DefaultPathScheme("/" + reservedClusterId, messagingPort);
    }

    CanonicalIdMaker defaultCanonicalIdMaker() {
        DefaultCanonicalIdMaker idMaker = new DefaultCanonicalIdMaker(messagingPort);
        return idMaker;
    }

}
