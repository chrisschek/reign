/*
 Copyright 2014 Yen Pai ypai@reign.io

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

package io.reign.lease;

import io.reign.Service;
import rx.Observable;

/**
 * 
 * @author ypai
 * 
 */
public interface LeaseService extends Service {

    public Observable<LeaseEvent> tryAcquire(final String clusterId, final String poolId, final int poolSize,
            final long durationMillis);

    public Observable<LeaseEvent> acquire(String clusterId, String leasePoolId, int poolSize, long durationMillis);

    public Observable<LeaseEvent> renew(String clusterId, String leasePoolId, int poolSize,
            String leaseId);

    public Observable<LeaseEvent> release(String clusterId, String leasePoolId, int poolSize,
            String leaseId);

    public Observable<LeaseEvent> revoke(String clusterId, String leasePoolId, int poolSize,
            String leaseId);

}
