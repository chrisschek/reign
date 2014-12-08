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

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Subscriber;

/**
 * 
 * @author ypai
 * 
 */
public abstract class LeaseEventSubscriber extends Subscriber<LeaseEvent> {

    private static final Logger logger = LoggerFactory.getLogger(LeaseEventSubscriber.class);

    private static final AtomicInteger INSTANCES_OUTSTANDING = new AtomicInteger(0);

    public LeaseEventSubscriber() {
        super();
        INSTANCES_OUTSTANDING.incrementAndGet();
    }

    public void acquired(Lease lease) {
    }

    public void renewed(Lease lease) {
    }

    public void released(Lease lease) {
    }

    public void revoked(Lease lease) {
    }

    public void notAcquired() {
    }

    public void error(Throwable e) {
    }

    @Override
    public final void onNext(LeaseEvent event) {
        Lease lease = event.getLease();
        switch (event.getType()) {
        case ACQUIRED:
            acquired(lease);
            break;
        case RENEWED:
            renewed(lease);
            break;
        case RELEASED:
            released(lease);
            break;
        case REVOKED:
            revoked(lease);
            break;
        case NOT_ACQUIRED:
            notAcquired();
            break;
        default:
            logger.error("Unrecognized event type:  " + event.getType());
        }
    }

    @Override
    public final void onError(Throwable e) {
        error(e);
    }

    @Override
    public final void onCompleted() {
        unsubscribe();
        INSTANCES_OUTSTANDING.decrementAndGet();
        logger.debug("onCompleted():  INSTANCES_OUTSTANDING={}", INSTANCES_OUTSTANDING.get());
    }
}
