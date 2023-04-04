/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)&#10;&#10;Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);&#10;you may not use this file except in compliance with the License.&#10;You may obtain a copy of the License at&#10;&#10;        http://www.apache.org/licenses/LICENSE-2.0&#10;&#10;Unless required by applicable law or agreed to in writing, software&#10;distributed under the License is distributed on an &quot;AS IS&quot; BASIS,&#10;WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.&#10;See the License for the specific language governing permissions and&#10;limitations under the License.
 */
package io.gravitee.repository.bridge.server.handler.response;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import java.io.OutputStream;
import java.util.Objects;
import lombok.NonNull;

/**
 * Used in order to write directly to the underlying {@link HttpServerResponse} in order to avoid loading whole data in memory
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OutputWriterStream extends OutputStream {

    private final HttpServerResponse response;

    public OutputWriterStream(final HttpServerResponse response) {
        this.response = response;
    }

    @Override
    public void write(final int b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(byte[] b, int off, int len) {
        Objects.checkFromIndexSize(off, len, b.length);
        if (off == 0 && len == b.length) {
            response.write(Buffer.buffer(b));
            return;
        }

        byte[] bytes = new byte[len];
        System.arraycopy(b, off, bytes, 0, len);
        response.write(Buffer.buffer(bytes));
    }

    @Override
    public void write(byte@NonNull[] bytes) {
        response.write(Buffer.buffer(bytes));
    }

    @Override
    public void close() {
        response.end();
    }
}
