/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package testhelpers;

import io.gravitee.apim.rest.api.common.apiservices.events.DynamicPropertiesEvent;
import io.gravitee.apim.rest.api.common.apiservices.events.ManagementApiServiceEvent;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TestEventListener implements EventListener<ManagementApiServiceEvent, DynamicPropertiesEvent> {

    private final ReplaySubject<Event<ManagementApiServiceEvent, DynamicPropertiesEvent>> events = ReplaySubject.create();
    private final AtomicInteger counter = new AtomicInteger();

    private TestEventListener(EventManager eventManager) {
        eventManager.subscribeForEvents(this, ManagementApiServiceEvent.class);
    }

    public static TestEventListener with(EventManager eventManager) {
        return new TestEventListener(eventManager);
    }

    /**
     * Complete the internal ReplaySubject when the expected number of events has been published
     * @param numberOfEventsExpected
     * @return this instance
     */
    public TestEventListener completeAfter(int numberOfEventsExpected) {
        counter.set(numberOfEventsExpected);
        return this;
    }

    public TestEventListener completeImmediatly() {
        events.onComplete();
        return this;
    }

    @Override
    public void onEvent(Event<ManagementApiServiceEvent, DynamicPropertiesEvent> event) {
        events.onNext(event);
        // if counter is not set thanks to completeAfter(int) method, then we can decrement under 0
        if (counter.decrementAndGet() <= 0) {
            events.onComplete();
        }
    }

    public TestObserver<Event<ManagementApiServiceEvent, DynamicPropertiesEvent>> test() {
        return events.test();
    }
}
