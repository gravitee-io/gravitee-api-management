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
package io.gravitee.gateway.core.http.client.ahc;

import com.ning.http.client.Body;
import com.ning.http.client.BodyGenerator;
import io.gravitee.gateway.api.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A {@link BodyGenerator} which use an {@link InputStream} for reading bytes, without having to read the entire
 * stream in memory.
 * <p/>
 * NOTE: The {@link InputStream} must support the {@link InputStream#mark} and {@link java.io.InputStream#reset()} operation.
 * If not, mechanisms like authentication, redirect, or resumable download will not works.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RequestBodyGenerator implements BodyGenerator {

    private final Logger LOGGER = LoggerFactory.getLogger(AHCHttpClient.class);

    private final static byte[] END_PADDING = "\r\n".getBytes();
    private final static byte[] ZERO = "0".getBytes();
    private final Request request;
    private final static Logger logger = LoggerFactory.getLogger(RequestBodyGenerator.class);
    private boolean patchNettyChunkingIssue = false;

    public RequestBodyGenerator(Request request) {
        this.request = request;
    }

    @Override
    public Body createBody() throws IOException {
        return new ISBody();
    }

    protected class ISBody implements Body {
        private boolean eof = false;
        private int endDataCount = 0;
        private byte[] chunk;

        @Override
        public long getContentLength() {
            return -1;
        }

        @Override
        public long read(ByteBuffer buffer) throws IOException {

            // To be safe.
            chunk = new byte[buffer.remaining() - 10];

            int read = -1;

            try {
                read = request.inputStream().read(chunk);
                LOGGER.debug("{} proxying content to upstream: {} bytes", request.id(), read);
            } catch (IOException ex) {
                logger.warn("Unable to read", ex);
            }

            if (patchNettyChunkingIssue) {
                if (read >= 0) {
                    // Netty 3.2.3 doesn't support chunking encoding properly, so we chunk encoding ourself.
                    buffer.put(Integer.toHexString(read).getBytes());
                    // Chunking is separated by "<bytesreads>\r\n"
                    buffer.put(END_PADDING);
                    buffer.put(chunk, 0, read);
                    // Was missing the final chunk \r\n.
                    buffer.put(END_PADDING);
                } else if (!eof) {
                    // read == -1)
                    // Since we are chunked, we must output extra bytes before considering the input stream closed.
                    // chunking requires to end the chunking:
                    // - A Terminating chunk of  "0\r\n".getBytes(),
                    // - Then a separate packet of "\r\n".getBytes()
                    endDataCount++;

                    if (endDataCount == 1)
                        buffer.put(ZERO);
                    else if (endDataCount == 2)
                        eof = true;

                    buffer.put(END_PADDING);
                }
            } else if (read > 0) {
                buffer.put(chunk, 0, read);
            }
            return read;
        }

        @Override
        public void close() throws IOException {
            request.inputStream().close();
        }
    }

    /**
     * HACK: This is required because Netty has issues with chunking.
     *
     * @param patchNettyChunkingIssue
     */
    public void patchNettyChunkingIssue(boolean patchNettyChunkingIssue) {
        this.patchNettyChunkingIssue = patchNettyChunkingIssue;
    }
}