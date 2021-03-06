Reign Framework
===============
A toolkit for building distributed applications, leveraging open source projects such as [Apache ZooKeeper](http://zookeeper.apache.org/), [Netty](http://netty.io/), [Codahale Metrics](http://metrics.codahale.com/).

The Reign Framework is licensed under the Apache License, Version 2.0.  Specific details are available in LICENSE.txt.


Features
--------

Out of the box, the framework provides the following:
* Service presence - monitor for nodes coming up and going down in services:  can be used to create smart clients that can detect when service nodes are up and down and shift requests elsewhere, etc.
* Messaging - nodes can message each other directly and/or broadcast a message to member nodes of a specific service.
* Constructs for distributed coordination - read/write locks, exclusive locks, semaphores, and barriers (coming soon).
* Reign integrates with Codahale Metrics to allow services in a distributed application to publish data to each other via ZooKeeper.
* Reliable ZooKeeper client wrapper that handles common ZooKeeper connection/session errors and re-connects as necessary.
* Support for management and storage of application configuration in ZooKeeper:  ideal for feature toggles, etc.
* A standardized way of organizing information in ZooKeeper for ease of maintenance and consistency between deployments.
* ZooKeeper-based Maps, Queues, Stacks, Lists to support common patterns such as queue/worker pool; sharing common state between nodes.

Common use cases:
* Zero configuration applications - deploy to different environments or change application properties without needing to edit configuration files or restart services.  Edit configuration in one place and push changes out to many nodes at once. 
* Dynamic service discovery - nodes in one service can discover nodes in other services without configuration changes. 
* Service redundancy - for services where only one process/node can run at the same time, a stand-by process/node can be brought up and will automatically take over if the currently running process/node fails.
* Capacity monitoring - services can monitor each other and ensure that they do not overwhelm each other:  for example, a frontline service may slow down its rate of requests to a backend service to prevent a "domino effect" where a spike in traffic brings down the whole application. 
* Coordination and division of labor between nodes using locks, queues, or some combination thereof.
* Application logic based on service states - services can share data via ZooKeeper:  for example, nodes in one service may go into "safety mode" based on information provided by another service (error rates, etc.).
  
Applications using Reign quickly gain a high level of cluster-awareness and coordination capabilities.



Quick Start
-----------

### Prerequisites

Ideally, have a running ZooKeeper cluster.  For a quick guide on how to set up ZooKeeper on OS X, try 
http://blog.kompany.org/2013/02/23/setting-up-apache-zookeeper-on-os-x-in-five-minutes-or-less/

Reign can also be started in bootstrap mode where it will spin up an in-process ZooKeeper -- this should only be used for testing/development.

### Inclusion in Maven projects

Reign is not yet in a central repository, but if built and uploaded to a private repository, it can be included in a Maven project with the following POM file snippet.  Using classifier "shaded" will use a jar that hides most of Reign's dependencies from your project:  this may help with dependency conflicts in more complex projects and is the preferred way of integrating Reign.

        <dependency>
            <groupId>io.reign</groupId>
            <artifactId>reign</artifactId>
            <version>REIGN_VERSION</version>
            
            <!-- "shaded" classifier available as of 0.2.15 -->
            <classifier>shaded</classifier>
        </dependency>        

### Initialize and start up examples
        /** init and start using in-process ZooKeeper on port 12181 **/
        Reign reign = Reign.maker().zkClientTestMode(12181, 30000).get();
        reign.start()

        /**
         * init and start with core services -- connecting to ZooKeeper on localhost at port 2181 with 30 second
         * ZooKeeper session timeout
         **/
        Reign reign = Reign.maker().zkClient("localhost:2181", 30000).get();
        reign.start();
        
        /**
         * init and start with core services -- connecting to a ZooKeeper cluster at port 2181 with 30 second
         * ZooKeeper session timeout
         **/
        Reign reign = Reign.maker().zkClient("zk-host1:2181,zk-host2:2181,zk-host3:2181", 30000).get();
        reign.start();      
        
        /**
         * init and start with core services -- connecting to a ZooKeeper cluster at port 2181 with 30 second
         * ZooKeeper session timeout using a custom root path, effectively "chroot-ing" the ZooKeeper session:  
         * this is one way to share a ZooKeeper cluster without worrying about path collision  
         **/
        Reign reign = Reign.maker().zkClient("zk-host1:2181,zk-host2:2181,zk-host3:2181/custom_root_path", 30000).get();
        reign.start();           

### Equivalent configuration using Spring

##### Example Spring Bean XML
    <!-- Reign bean configuration -->
    <bean id="reignMaker" class="io.reign.util.spring.SpringReignMaker"  
        init-method="init"  
        destroy-method="destroy">
        <property name="zkConnectString" value="localhost:2181"/>
        <property name="zkSessionTimeout" value="30000"/>
        
        <!-- convenient way of passing misc. related variables -->
        <property name="attributeMap">
            <map>
                <entry key="clusterId" value="my-app"/>
                <entry key="serviceId" value="backend-api-service"/>
            </map>
        </property>	        
    </bean>
        
##### Usage in Java code...
    // get and start Reign object
    SpringReignMaker springReignMaker = ...injected dependency...;
    Reign reign = springReignMaker.get();
    
    // may not have to do this if bean init-method is specified as "initStart"
    // in Spring configuration
    reign.start();
    
    // get misc. related variables
    String clusterId = springReignMaker.getAttribute("clusterId");
    String serviceId = springReignMaker.getAttribute("serviceId");

### Announcing availability of a service on a node

        /** presence service example **/
        // get the presence service
        PresenceService presenceService = reign.getService("presence");

        // announce this node's membership in a given service, immediately visible
        presenceService.announce("my-app", "backend-api-service", true);

        // announce this node's membership in a given service, not immediately visible
        presenceService.announce("my-app", "backend-api-service");
        presenceService.announce("my-app", "backend-api-service", false);

        // hide this node as member of service
        presenceService.hide("my-app", "backend-api-service");

        // show this node as member of service
        presenceService.show("my-app", "backend-api-service");
        
        // get information about nodes available in a given service
        ServiceInfo serviceInfo = presenceService.getServiceInfo("my-app", "backend-api-service");
        List<String> nodeList = serviceInfo.getNodeList();
        
        // watch for changes in a service with observer callback
        presenceService.observe("my-app", "backend-api-service", new PresenceObserver<ServiceInfo>() {
            @Override
            public void updated(ServiceInfo updated, ServiceInfo previous) {
                if (updated != null) {
                    System.out.println("Service updated!");
                } else {
                    System.out.println("Service deleted or removed!");
                }
            }
        });

### Using the Web UI
On any node running the framework, the Web UI is available at port 33033 (assuming the default port was not changed).  For example, if you are running the framework locally, point your browser to 
[http://localhost:33033](http://localhost:33033).  
  
Take a look at a [UI screenshot](docs/ui-screenshot-1.png).
  
Run one of the examples and in the terminal, you should be able to send the following messages and see the corresponding responses (more information is available on the "Terminal Guide" tab):

List available services in cluster namespace "examples":  
`presence:/examples`  

List nodes comprising "service1":  
`presence:/examples/service1`  

List nodes comprising "service2":  
`presence:/examples/service2`  

### Storing configuration in ZooKeeper

        /** configuration service example **/
        // get the configuration service
        ConfService confService = (ConfService) reign.getService("conf");

        // store configuration as properties file
        Map<String,String> props = new HashMap<String,String>();
        props.setProperty("capacity.min", "111");
        props.setProperty("capacity.max", "999");
        props.setProperty("lastSavedTimestamp", System.currentTimeMillis() + "");

        // save configuration to ZooKeeper (uses JSON as standard serialization)
        confService.putConf("examples", "config1.props", props);

        // retrieve configuration 
        Map<String,String> loadedProperties = confService.getConf("examples", "config1.props");

        // another example:  store configuration (uses utility buildable Map for conciseness)
        confService.putConf("examples", "config1.js", Structs.<String, String> map().kv("capacity.min", "222").kv("capacity.max", "888")
                        .kv("lastSavedTimestamp", System.currentTimeMillis() + ""));

        // another example:  retrieve configuration
        Map<String, String> loadedJson = confService.getConf("examples", "config1.js");
        
        // Use read-only utility class that self updates using an internal observer:  
        // eventually consistent with latest configuration values in ZooKeeper
        ReignContext context = reign.getContext();
        UpdatingConf<String,String> updatingConf = new UpdatingConf<String,String>("test", "service1/test1.conf", context);
        
        // will be null initially
        String value1 = updatingConf.get("key1");
        
        // update the value
        confService.putConf("test", "service1/test1.conf",
                    Structs.<String, String> map().kv("key1", "value1"));
        
        // value for "key1" will eventually be visible
        String checkingValue1Again = updatingConf.get("key1");
        
        // call destroy when done with object:  does NOT affect config values set in ZooKeeper
        updatingConf.destroy();
        

### Messaging between nodes

        /** messaging example **/
        // get the messaging service
        MessagingService messagingService = reign.getService("messaging");

        // wait indefinitely for at least one node in "service1" to become available
        presenceService.waitUntilAvailable("examples", "service1", -1);

        // send message to a single node in the "service1" service in the "examples" cluster;
        // in this example, we are just messaging ourselves
        CanonicalId canonicalId = reign.getCanonicalId();
        String canonicalIdString = reign.getPathScheme().toPathToken(canonicalId);
        ResponseMessage responseMessage = messagingService.sendMessage("examples", "service1", canonicalIdString,
                new SimpleRequestMessage("presence", "/"));

        // broadcast a message to all nodes belonging to the "service1" service in the examples cluster
        Map<String, ResponseMessage> responseMap = messagingService.sendMessage("examples", "service1",
                new SimpleRequestMessage("presence", "/examples"));


### Getting and using distributed locks

        /** coordination service example **/
        // get the coordination service
        CoordinationService coordService = (CoordinationService) reign.getService("coord");

        // get a distributed reentrant lock and use it
        DistributedReentrantLock lock = coordService.getReentrantLock("examples", "exclusive_lock1");
        lock.lock();
        try {
            // do some stuff here... (just sleeping 5 seconds)
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // do something here...

        } finally {
            lock.unlock();
            
            // don't have to do this if re-using this lock object
            lock.destroy();
        }

        // get a read/write distributed lock and use it
        DistributedReadWriteLock rwLock = coordService.getReadWriteLock("examples", "rw_lock1");
        rwLock.readLock().lock();
        try {
            // do some stuff here... (just sleeping 5 seconds)
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // do something here...

        } finally {
            rwLock.readLock().unlock();
            
            // don't have to do this if re-using this lock object
            rwLock.destroy();
        }

### Publishing and accessing service metrics
Reign integrates [Codahale Metrics](http://metrics.codahale.com/) to allow services to publish application metrics to each other.
This information can be used for decisioning and/or monitoring within your distributed application.

See [Codahale Metrics](http://metrics.codahale.com/) for specific details on different types of metrics (counters, histograms, etc.).

Basic usage:

        /** EXAMPLE #1:  metrics service example **/
        // get metrics service
        MetricsService metricsService = reign.getService("metrics");

		// get a MetricRegistry manager which will rotate data every 60 seconds
        RotatingMetricRegistryManager registryManager = new RotatingMetricRegistryManager(60, TimeUnit.SECONDS);       
        
        // export data from the service node to ZooKeeper every 10 seconds
        metricsService.scheduleExport("clusterA", "serviceA", registryManager, 10, TimeUnit.SECONDS);
        
        // get some counters and increment
        Counter counter1 = registryManager.get().counter(MetricRegistry.name("requests"));
        Counter counter2 = registryManager.get().counter(MetricRegistry.name("errors"));        
        counter1.inc();
        counter2.inc(3);
        
        // get aggregated/combined metrics data for all nodes in a given service
        MetricsData metricsData = metricsService.getServiceMetrics("clusterA", "serviceA")
        CounterData requestCounterData = metricsData.getCounter("requests");
        System.out.println("Number of requests across the service is " + requestCounterData.getCount()); 
        
Instrumenting with Metrics and using a callback to receive notifications when metrics are rotated:        
        
        /** EXAMPLE #2:  metrics service example using a callback when metrics are rotated **/
        // a class that we want to instrument with metrics and we want to use references to Metric counters 
        // instead of always retrieving them from a MetricRegistryManager instance
        public class MyClass {
            private volatile Counter requestCounter;
            private volatile Counter errorCounter;
            
            public void setRequestCounter( Counter requestCounter ) {
                this.requestCounter = requestCounter;
            }
            
            public void setErrorCounter( Counter errorCounter ) {
                this.errorCounter = errorCounter;
            }
        }
          
        // get metrics service
        MetricsService metricsService = reign.getService("metrics");

		// define a metrics rotation callback that updates counters used in the MyClass instance as metrics are rotated
		MetricRegistryManagerCallback callback = new MetricRegistryManagerCallback() {
			private MyClass myClass;
		
		    public void setMyClass(MyClass myClass) {
		        this.myClass = myClass;
		    }
		
		    public void rotated(MetricRegistry current, MetricRegistry previous) {
		        myClass.setRequestCounter(current.counter("requestCounter"));
		        myClass.setErrorCounter(current.counter("errorCounter"));
		    }
		};
		
		// set instance into the callback
		MyClass myClassInstance = new MyClass();		
		callback.setMyClass( myClassInstance );

		// get a MetricRegistry manager which will rotate data every 60 seconds, and notify when rotating via a callback
        RotatingMetricRegistryManager registryManager = new RotatingMetricRegistryManager(60, TimeUnit.SECONDS, callback);
        
        // register another callback as necessary
        registryManager.registerCallback( new MetricRegistryManagerCallback() {
		    public void rotated(MetricRegistry current, MetricRegistry previous) {
		        // do something else here
		    }
		};   
        
        // export data from the service node to ZooKeeper every 10 seconds
        metricsService.scheduleExport("clusterA", "serviceA", registryManager, 10, TimeUnit.SECONDS);                        

### Shutting down 

        /** shutdown reign **/
        reign.stop();



Design Notes
------------

Part of Reign's value is derived from how data is organized in ZooKeeper.  By creating a standard layout of information, the framework sets up the possibility of an ecosystem that allows non-Java services/applications to easily coordinate/monitor each other in a standard fashion.  Monitoring and administration is also made simpler, as the framework's "magic" is made more transparent for easier debugging:  for example, in the case of locks, one can force a node to release a lock by deleting its lock node just using the standard Zookeeper shell -- of course, this may have unintended consequences and should be done with care.

The default data layout in ZooKeeper is outlined below.  Custom layouts may be created as necessary by implementing your own or customizing the provided `PathScheme` implementation.

###Base paths:

* `/reign` - the root directory, configurable
* `/reign/_DATA_TYPE_/_CLUSTER_ID_` - user-created service data, configuration, locks, etc.


####`_DATA_TYPE_` can be one of the following:

* `presence` - service discovery information
* `conf` - configuration data
* `coord` - data describing distributed locks, semaphores, etc.
* `data` - data supporting distributed interprocess-safe maps, lists, stacks, and queues
* `metrics` - service node metrics data (uses [Codahale Metrics](http://metrics.codahale.com/))

#### `_CLUSTER_ID_` is a namespace for presence data, configuration, locks, etc. 

Web Sockets Protocol
--------------------

By default, services in the framework can receive and respond to messages via Web Sockets.

####General Message Format
`[TARGET_SERVICE]:[RESOURCE]?[QUERY_STRING]#[META_COMMAND]`

####Example Messages
`presence:/my_cluster/foo_service` - this message would get information on the `foo_service`.  More information is available in the Web UI available on any node running the framework at port 33033 (default port).


Upcoming in 0.2.x
-----------------

* Web Socket API support for coordination service 
* Web Socket API support for metrics service


Upcoming in 0.3.x
-----------------

* Consistent hash service
* Leases instead of Locking
* Migration of UI to AngularJS?
* Clarification of data service API
* Clarification of request/response protocol
* Clarification/cleaning up of String nodeId vs. node ID as a data structure
* Formalization of client concept




