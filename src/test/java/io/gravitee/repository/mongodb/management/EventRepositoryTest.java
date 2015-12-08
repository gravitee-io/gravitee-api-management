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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class EventRepositoryTest extends AbstractMongoDBTest {

	private final static Logger logger = LoggerFactory.getLogger(EventRepositoryTest.class);
	
	@Autowired
	private EventRepository eventRepository;

	@Override
	protected String getTestCasesPath() {
		return "/data/event-tests/";
	}

	@Test
	public void createEventTest() {
		try {
			Event event = new Event();
			event.setType(EventType.PUBLISH_API);
			event.setPayload("{}");
			event.setParentId(null);
			
			Event eventCreated =  eventRepository.create(event);
			
			Assert.assertEquals("Invalid saved event type.", EventType.PUBLISH_API, eventCreated.getType());
			Assert.assertEquals("Invalid saved event paylod.", "{}", eventCreated.getPayload());
		} catch (Exception e) {
			logger.error("Error while calling createEvent", e);
			Assert.fail("Error while calling createEvent");	
		}
	}

	@Test
	public void findByIdTest() {
		try {
			Optional<Event> event= eventRepository.findById("event1");
			Assert.assertTrue(event.isPresent());
			Assert.assertTrue(EventType.PUBLISH_API.equals(event.get().getType()));
		} catch (Exception e) {
			logger.error("Error while finding event event1",e);
			Assert.fail("Error while finding event event1");
		}
	}	
}
