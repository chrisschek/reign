/*
 * Copyright 2013 Yen Pai ypai@reign.io
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.reign.conf;

import io.reign.Service;

/**
 * Remote/centralized configuration service.
 * 
 * @author ypai
 * 
 */
public interface ConfService extends Service {

    public <T> void observeServiceConf(String clusterId, String serviceId, String confName, ConfObserver<T> observer);

    public <T> void observe(String clusterId, String relativeConfPath, ConfObserver<T> observer);

    public <T> T getServiceConf(String clusterId, String serviceId, String confName);

    public <T> T getServiceConf(String clusterId, String serviceId, String confName, ConfObserver<T> observer);

    public <T> T getConf(String clusterId, String relativeConfPath);

    public <T> T getConf(String clusterId, String relativeConfPath, ConfObserver<T> observer);

    public <T> void putServiceConf(String clusterId, String serviceId, String confName, T conf);

    public <T> void putServiceConf(String clusterId, String serviceId, String confName, T conf, ConfObserver<T> observer);

    public <T> void putConf(String clusterId, String relativeConfPath, T conf);

    public <T> void putConf(String clusterId, String relativeConfPath, T conf, ConfObserver<T> observer);

    public void removeServiceConf(String clusterId, String serviceId, String confName);

    public void removeConf(String clusterId, String relativeConfPath);

}