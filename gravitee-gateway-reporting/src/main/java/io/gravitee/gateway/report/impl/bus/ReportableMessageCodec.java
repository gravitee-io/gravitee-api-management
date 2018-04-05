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
package io.gravitee.gateway.report.impl.bus;

import io.gravitee.reporter.api.Reportable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReportableMessageCodec implements MessageCodec<Reportable, Reportable> {

    static final String CODEC_NAME = "reportable-codec";

    @Override
    public void encodeToWire(Buffer buffer, Reportable reportable) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(reportable);
            oos.flush();

            byte[] data = bos.toByteArray();
            int length = data.length;

            // Write data into given buffer
            buffer.appendInt(length);
            buffer.appendBytes(data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Reportable decodeFromWire(int position, Buffer buffer) {
        try {
            // My custom message starting from this *position* of buffer
            int pos = position;

            // Length of data
            int length = buffer.getInt(pos);

            byte[] data = buffer.getBytes(pos += 4, pos += length);

            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ObjectInputStream is = new ObjectInputStream(in);
            return (Reportable) is.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    @Override
    public Reportable transform(Reportable reportable) {
        // If a message is sent *locally* across the event bus.
        // This example sends message just as is
        return reportable;
    }

    @Override
    public String name() {
        return CODEC_NAME;
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
