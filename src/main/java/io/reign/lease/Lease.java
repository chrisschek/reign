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

import rx.Observable;
import rx.Subscription;

/**
 * 
 * @author ypai
 * 
 */
public interface Lease {

    public String holderId();

    public String clusterId();

    public String poolId();

    public String id();

    public long durationMillis();

    public long acquiredTimestamp();

    public long expiryTimestamp();

    public boolean expired();

    public Observable<LeaseEvent> renew();

    public Observable<LeaseEvent> release();

    public Subscription renew(LeaseEventSubscriber subscriber);

    public Subscription release(LeaseEventSubscriber subscriber);

}
