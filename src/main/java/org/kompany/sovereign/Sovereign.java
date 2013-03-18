package org.kompany.sovereign;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.kompany.sovereign.util.PathCache;
import org.kompany.sovereign.util.SimplePathCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point into framework functionality.
 * 
 * Should be set up in a single thread.
 * 
 * @author ypai
 * 
 */
public class Sovereign implements Watcher {

    private static final Logger logger = LoggerFactory.getLogger(Sovereign.class);

    private static final List<ACL> DEFAULT_ACL_LIST = new ArrayList<ACL>();
    static {
        DEFAULT_ACL_LIST.add(new ACL(ZooDefs.Perms.ALL, new Id("world", "anyone")));
    }

    private ZkClient zkClient;

    private final Map<String, ServiceWrapper> serviceMap = new ConcurrentHashMap<String, ServiceWrapper>(8, 0.9f, 2);

    private final ServiceDirectory serviceDirectory = new ServiceDirectory() {
        @Override
        public Service getService(String serviceName) {
            if (shutdown) {
                throw new IllegalStateException("Already shutdown:  cannot get service.");
            }
            waitForInitializationIfNecessary();

            ServiceWrapper serviceWrapper = serviceMap.get(serviceName);
            if (serviceWrapper != null) {
                return serviceWrapper.getService();
            } else {
                return null;
            }
        }
    };

    private final Map<String, Future<?>> futureMap = new HashMap<String, Future<?>>();

    private PathScheme pathScheme = new DefaultPathScheme("/sovereign/user", "/sovereign/internal");

    private PathCache pathCache;

    private ScheduledExecutorService executorService;

    private volatile boolean started = false;
    private volatile boolean shutdown = false;

    private int threadPoolSize = 3;

    private Thread adminThread = null;

    /** List to ensure Watcher(s) are called in a specific order */
    private final List<Watcher> watcherList = new ArrayList<Watcher>();

    private List<ACL> defaultAclList = DEFAULT_ACL_LIST;

    private volatile String sovereignId;

    public static SovereignBuilder builder() {
        return new SovereignBuilder();
    }

    public Sovereign() {
    }

    public Sovereign(ZkClient zkClient, PathCache pathCache) {
        this.sovereignId = sovereignId;
        this.zkClient = zkClient;

        // initialize cache instance
        this.pathCache = pathCache;
    }

    public List<ACL> getDefaultAclList() {
        return defaultAclList;
    }

    public void setDefaultAclList(List<ACL> defaultAclList) {
        this.defaultAclList = defaultAclList;
    }

    public String getSovereignId() {
        return sovereignId;
    }

    public void setSovereignId(String sovereignId) {
        if (started) {
            throw new IllegalStateException("Cannot set sovereignId once started!");
        }
        this.sovereignId = sovereignId;

    }

    @Override
    public void process(WatchedEvent event) {
        // log if DEBUG
        if (logger.isDebugEnabled()) {
            logger.debug("***** Received ZooKeeper Event:  {}",
                    ReflectionToStringBuilder.toString(event, ToStringStyle.DEFAULT_STYLE));

        }

        for (Watcher watcher : watcherList) {
            watcher.process(event);
        }
    }

    public ZkClient getZkClient() {
        return zkClient;
    }

    public void setZkClient(ZkClient zkClient) {
        if (started) {
            throw new IllegalStateException("Cannot set zkClient once started!");
        }
        this.zkClient = zkClient;
    }

    public PathScheme getPathScheme() {
        return pathScheme;
    }

    public void setPathScheme(PathScheme pathScheme) {
        if (started) {
            throw new IllegalStateException("Cannot set pathScheme once started!");
        }
        this.pathScheme = pathScheme;
    }

    public PathCache getPathCache() {
        return pathCache;
    }

    public void setPathCache(SimplePathCache pathCache) {
        if (started) {
            throw new IllegalStateException("Cannot set pathCache once started!");
        }
        this.pathCache = pathCache;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        if (started) {
            throw new IllegalStateException("Cannot set threadPoolSize once started!");
        }
        this.threadPoolSize = threadPoolSize;
    }

    public <T extends Service> T getService(String serviceName) {
        // waitForInitializationIfNecessary();
        return serviceDirectory.getService(serviceName);
    }

    void register(String serviceName, Service service) {
        throwExceptionIfNotOkayToRegister();

        // check that we don't have duplicate services
        if (serviceMap.get(serviceName) != null) {
            throw new IllegalStateException("An existing service already exists under the same name:  serviceName="
                    + serviceName);
        }

        logger.info("Registering service:  serviceName={}", serviceName);

        // set with path scheme
        service.setPathScheme(pathScheme);
        service.setZkClient(zkClient);
        service.setPathCache(pathCache);
        service.setServiceDirectory(serviceDirectory);
        service.setDefaultAclList(defaultAclList);
        service.setSovereignId(sovereignId);
        service.init();

        // add to zkClient's list of watchers if Watcher interface is
        // implemented
        if (service instanceof Watcher) {
            logger.info("Adding as ZooKeeper watcher:  serviceName={}", serviceName);
            watcherList.add((Watcher) service);
        }

        serviceMap.put(serviceName, new ServiceWrapper(service));
    }

    public synchronized void registerServices(Map<String, Service> serviceMap) {
        throwExceptionIfNotOkayToRegister();

        for (String serviceName : serviceMap.keySet()) {
            register(serviceName, serviceMap.get(serviceName));
        }

    }

    void throwExceptionIfNotOkayToRegister() {
        if (started) {
            throw new IllegalStateException("Cannot register services once started!");
        }
        if (zkClient == null || pathCache == null) {
            throw new IllegalStateException(
                    "Cannot register services before zkClient and pathCache has been populated!");
        }
    }

    public synchronized void start() {
        if (started) {
            logger.debug("start():  already started...");
            return;
        }

        logger.info("START:  begin");

        /** create graceful shutdown hook **/
        logger.info("START:  add shutdown hook");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Sovereign.this.stop();
            }
        });

        /** generate sovereign id if necessary **/
        if (sovereignId == null) {
            sovereignId = generateSovereignId();
            logger.info("START:  using default sovereignId:  {}", sovereignId);
            for (ServiceWrapper serviceWrapper : serviceMap.values()) {
                serviceWrapper.getService().setSovereignId(sovereignId);
            }
        }

        /** watcher set-up **/
        logger.info("START:  registering watchers");
        // register self as watcher
        this.zkClient.register(this);

        // pathCache should be first in list if it is a Watcher
        if (pathCache instanceof Watcher) {
            this.watcherList.add(0, (Watcher) pathCache);
        }

        /** init executor **/
        logger.info("START:  initializing executor");
        this.executorService = this.createExecutorService();

        /** start services running **/
        logger.info("START:  schedule service tasks");
        for (String serviceName : serviceMap.keySet()) {
            ServiceWrapper serviceWrapper = serviceMap.get(serviceName);
            logger.debug("Checking service:  {}", serviceWrapper.getService().getClass().getName());

            // execute if not a continuously running service and not shutdown
            if (!this.shutdown && serviceWrapper.isSubmittable()) {
                logger.debug("Submitting service:  {}", serviceWrapper.getService().getClass().getName());
                Future<?> future = executorService.scheduleWithFixedDelay(serviceWrapper,
                        serviceWrapper.getIntervalMillis(), serviceWrapper.getIntervalMillis(), TimeUnit.MILLISECONDS);
                futureMap.put(serviceName, future);
            }// if
        }// for

        /** start main thread **/
        logger.info("START:  start admin thread");
        adminThread = this.createAdminThread();
        adminThread.start();

        started = true;

        /** notify any waiters **/
        logger.info("START:  notifying all waiters");
        this.notifyAll();

        logger.info("START:  done");
    }

    public synchronized void stop() {
        if (shutdown) {
            logger.debug("stop():  already stopped...");
            return;
        }
        shutdown = true;

        logger.info("SHUTDOWN:  begin");

        /** cancel all futures **/
        logger.info("SHUTDOWN:  cancelling scheduled service tasks");
        for (Future<?> future : futureMap.values()) {
            future.cancel(false);
        }

        /** stop admin thread **/
        logger.info("SHUTDOWN:  shutting down admin thread");
        try {
            if (adminThread != null) {
                adminThread.join();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted during shutdown:  " + e, e);
        }

        /** stop executor **/
        logger.info("SHUTDOWN:  shutting down executor");
        executorService.shutdown();

        /** clean up services **/
        logger.info("SHUTDOWN:  cleaning up services");
        for (ServiceWrapper serviceWrapper : serviceMap.values()) {
            serviceWrapper.getService().destroy();
        }

        /** clean up zk client **/
        logger.info("SHUTDOWN:  closing Zookeeper client");
        this.zkClient.close();

        logger.info("SHUTDOWN:  done");
    }

    private void waitForInitializationIfNecessary() {
        if (!started) {
            try {
                logger.info("Waiting for notification of start() completion...");
                synchronized (Sovereign.this) {
                    Sovereign.this.wait(30000);
                }
            } catch (InterruptedException e) {
                logger.warn("Interrupted while waiting for start:  " + e, e);
            }
            if (!started) {
                throw new IllegalStateException("Not yet initialized:  check environment and ZK settings.");
            }
            logger.info("Received notification of start() completion");
        }// if
    }

    /**
     * Creates and appropriately sizes executor service for services configured.
     * 
     * @return
     */
    private ScheduledExecutorService createExecutorService() {
        // get number of continuously running services
        int continuouslyRunningCount = 0;
        for (ServiceWrapper serviceWrapper : serviceMap.values()) {
            if (serviceWrapper.isActiveService() && serviceWrapper.isAlwaysActive()) {
                continuouslyRunningCount++;
            }
        }
        return new ScheduledThreadPoolExecutor(continuouslyRunningCount + this.getThreadPoolSize());
    }

    /**
     * Creates the thread that submits the services for regular execution.
     * 
     * @return
     */
    private Thread createAdminThread() {
        Thread adminThread = new Thread() {
            @Override
            public void run() {
                while (!shutdown) {
                    // TODO: perform any framework maintenance?

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.warn("Interrupted while in mainThread:  " + e, e);
                    }
                }// while
            }// run()
        };
        adminThread.setName(this.getClass().getSimpleName() + "." + "admin");
        adminThread.setDaemon(true);
        return adminThread;
    }

    private static String generateSovereignId() {
        // get pid
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        String[] ids = jvmName.split("@");
        String pid = ids[0];

        // try to get hostname and ip address
        String hostname = null;
        String ipAddress = null;
        try {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
            ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("Could not get hostname:  " + e, e);
        }

        // fill in unknown values
        if (pid == null) {
            pid = "UNKNOWN_PID";
        }
        if (hostname == null) {
            hostname = "UNKNOWN_HOSTNAME";
        }
        if (ipAddress == null) {
            ipAddress = "UNKNOWN_IP_ADDRESS";
        }

        return "pid_" + pid + "@" + hostname + "[" + ipAddress + "]";
    }

    /**
     * Convenience wrapper providing methods for interpreting service metadata.
     * 
     * @author ypai
     * 
     */
    private static class ServiceWrapper implements Runnable {
        private final Service service;
        private boolean running = false;

        public ServiceWrapper(Service service) {
            this.service = service;
        }

        public Service getService() {
            return service;
        }

        public boolean isActiveService() {
            return service instanceof ActiveService;
        }

        public boolean isSubmittable() {
            return isActiveService() && !isRunning();
        }

        public synchronized boolean isAlwaysActive() {
            return ((ActiveService) this.service).getExecutionIntervalMillis() < 1;
        }

        public synchronized boolean isRunning() {
            return running;
        }

        public long getIntervalMillis() {
            return ((ActiveService) service).getExecutionIntervalMillis();
        }

        @Override
        public synchronized void run() {
            this.running = true;

            logger.debug("Calling {}.perform()", service.getClass().getName());
            ((ActiveService) this.service).perform();

            this.running = false;
        }

    }
}