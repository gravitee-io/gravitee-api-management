/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.core.event.impl;

import io.gravitee.gateway.core.event.Event;
import io.gravitee.gateway.core.event.EventListener;
import io.gravitee.gateway.core.event.EventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class EventManagerImpl implements EventManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventManagerImpl.class);

  private Map<ComparableEventType, Set<EventListenerWrapper>> listenersMap = new TreeMap();

  public void publishEvent(Enum type, Object content) {
    this.publishEvent(new SimpleEvent(type, content));
  }

  public void publishEvent(Event event) {
    LOGGER.debug("Publish event {} - {}", event.type(), event.content());

    Set<EventListenerWrapper> listeners = getEventListeners(event.type().getClass());
    for(EventListenerWrapper listener : listeners) {
      listener.eventListener().onEvent(event);
    }
  }

  public <T extends Enum> void subscribeForEvents(EventListener<T, ?> eventListener, T... events) {
    for( T event : events) {
      addEventListener(eventListener, (Class<T>) event.getClass(), Arrays.asList(events));
    }
  }

  public <T extends Enum> void subscribeForEvents(EventListener<T, ?> eventListener, Class<T> events) {
    addEventListener(eventListener, events, EnumSet.allOf(events));
  }

  private <T extends Enum> void addEventListener(EventListener<T, ?> eventListener, Class<T> enumClass, Collection<T> events) {
    LOGGER.info("Register new listener {} for event type {}", eventListener.getClass().getSimpleName(), enumClass);

    Set<EventListenerWrapper> listeners = getEventListeners(enumClass);
    listeners.add(new EventListenerWrapper(eventListener, events));
  }

  private <T extends Enum> Set<EventListenerWrapper> getEventListeners(Class<T> eventType) {
    Set<EventListenerWrapper> listeners = this.listenersMap.get(new ComparableEventType(eventType));

    if (listeners == null) {
      listeners = new HashSet();
      this.listenersMap.put(new ComparableEventType(eventType), listeners);
    }

    return listeners;
  }

  private class EventListenerWrapper<T extends Enum> {

    private final EventListener<T, ?> eventListener;
    private final Set<T> events;

    public EventListenerWrapper(EventListener<T, ?> eventListener, Collection<T> events) {
      this.eventListener = eventListener;
      this.events = new HashSet(events);
    }

    public EventListener<T, ?> eventListener() {
      return eventListener;
    }

    public Set<T> events() {
      return events;
    }
  }

  private class ComparableEventType<T> implements Comparable<ComparableEventType<T>> {

    private static final int HASH = 7 * 89;
    private final Class<? extends T> wrappedClass;

    public ComparableEventType(Class<? extends T> wrappedClass) {
      this.wrappedClass = wrappedClass;
    }

    @Override
    public int compareTo(ComparableEventType<T> o) {
      return wrappedClass.getCanonicalName().compareTo(o.wrappedClass.getCanonicalName());
    }

    @Override
    public int hashCode() {
      return HASH + (this.wrappedClass != null ? this.wrappedClass.hashCode() : 0);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof ComparableEventType)) {
        return false;
      }

      return compareTo((ComparableEventType<T>) o) == 0;
    }
  }
}
