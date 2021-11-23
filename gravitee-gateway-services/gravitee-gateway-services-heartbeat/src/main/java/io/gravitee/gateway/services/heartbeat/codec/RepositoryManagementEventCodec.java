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
package io.gravitee.gateway.services.heartbeat.codec;

import io.gravitee.repository.management.model.Event;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RepositoryManagementEventCodec implements MessageCodec<Event, Event> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryManagementEventCodec.class);

    @Override
    public void encodeToWire(Buffer buffer, Event event) {
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(event);
            oos.flush();

            byte[] data = bos.toByteArray();
            int length = data.length;

            // Write data into given buffer
            buffer.appendInt(length);
            buffer.appendBytes(data);
        } catch (final Exception ex) {
            LOGGER.error("Error while trying to encode an Event object", ex);
        }
    }

    @Override
    public Event decodeFromWire(int position, Buffer buffer) {
        try {
            // My custom message starting from this *position* of buffer
            int pos = position;

            // Length of data
            int length = buffer.getInt(pos);

            pos += 4;
            final int start = pos;
            final int end = pos + length;
            byte[] data = buffer.getBytes(start, end);

            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ObjectInputStream is = new ObjectInputStream(in);
            return (Event) is.readObject();
        } catch (Exception ex) {
            LOGGER.error("Error while trying to decode an Event object", ex);
        }

        return null;
    }

    @Override
    public Event transform(Event event) {
        // If a message is sent *locally* across the event bus.
        // This example sends message just as is
        return event;
    }

    @Override
    public String name() {
        return getClass().getName();
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
