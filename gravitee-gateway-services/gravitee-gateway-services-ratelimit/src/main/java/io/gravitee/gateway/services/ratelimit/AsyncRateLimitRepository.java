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
package io.gravitee.gateway.services.ratelimit;

import io.gravitee.common.service.AbstractService;
import io.gravitee.common.util.BlockingArrayQueue;
import io.gravitee.repository.ratelimit.api.RateLimitRepository;
import io.gravitee.repository.ratelimit.model.RateLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class AsyncRateLimitRepository extends AbstractService implements RateLimitRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncRateLimitRepository.class);

    @Value("${services.ratelimit.async:false}")
    private boolean async;

    @Value("${services.ratelimit.queue.size:10240}")
    private int queueCapacity;

    @Value("${services.ratelimit.queue.pollingTimeout:250}")
    private long pollingTimeout;

    private transient UpdaterThread thread;

    private BlockingQueue<RateLimit> queue;

    private RateLimitRepository delegateRateLimitRepository;
    private RateLimitRepository cachedRateLimitRepository;

    private boolean warnedFull;

    @Override
    protected void doStart() throws Exception {
        if (async) {
            super.doStart();
            LOGGER.info("Start rate-limit updater");
            LOGGER.info("\tQueue capacity: {}", getQueueCapacity());
            LOGGER.info("\tQueue polling timeout: {} ms", getPollingTimeout());
            this.queue = new BlockingArrayQueue<>(getQueueCapacity());
            this.thread = new UpdaterThread();
            this.thread.start();

            LOGGER.info("Start rate-limit updater : DONE");
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (async) {
            LOGGER.info("Stop rate-limit updater ");
            thread.terminate();
            thread.join();
            super.doStop();
            thread = null;
            LOGGER.info("Stop rate-limit updater : DONE");
        }
    }

    @Override
    protected String name() {
        return "Rate-limit updater";
    }

    public void update(RateLimit rateLimit) {
        if (!this.queue.offer(rateLimit)) {
            if (this.warnedFull) {
                // TODO: provide a programmatic overflow to cache feature
                LOGGER.warn("Rate-limit updater's queue overflow !");
            }
            this.warnedFull = true;
        }
    }

    @Override
    public RateLimit get(RateLimit rateLimit) {
        // Get data from cache
        RateLimit cachedRateLimit = cachedRateLimitRepository.get(rateLimit);

        if (cachedRateLimit == null) {
            // Force refresh from repository if no data found in cache
            try {
                cachedRateLimit = delegateRateLimitRepository.get(rateLimit);
            } catch (Exception ex) {
                // No connection to rate-limit repository, fallback to empty values (provided by the policy)
                cachedRateLimit = rateLimit;
            }

            // Save only if it's not a new rate limit entity
            if (cachedRateLimit.getResetTime() != 0L) {
                cachedRateLimitRepository.save(cachedRateLimit);
            }
        }

        return cachedRateLimit;
    }

    @Override
    public void save(RateLimit rateLimit) {
        // Push data in cache
        cachedRateLimitRepository.save(rateLimit);

        if (async) {
            // Push data in repository asynchronously
            update(rateLimit);
        } else {
            // Push data in repository synchronously
            try {
                delegateRateLimitRepository.save(rateLimit);
            } catch (Exception ex) {
                // we can skip it since data are stored in the cache
            }
        }
    }

    public long getPollingTimeout() {
        return pollingTimeout;
    }

    public void setPollingTimeout(long pollingTimeout) {
        this.pollingTimeout = pollingTimeout;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public void setDelegateRateLimitRepository(RateLimitRepository delegateRateLimitRepository) {
        this.delegateRateLimitRepository = delegateRateLimitRepository;
    }

    public void setCachedRateLimitRepository(RateLimitRepository cachedRateLimitRepository) {
        this.cachedRateLimitRepository = cachedRateLimitRepository;
    }

    private class UpdaterThread extends Thread {
        private volatile boolean running = true;

        UpdaterThread() {
            this.setName("ratelimit-updater");
        }

        public void terminate() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    RateLimit rateLimit = AsyncRateLimitRepository.this.queue.poll(getPollingTimeout(), TimeUnit.MILLISECONDS);
                    if (rateLimit != null) {
                        AsyncRateLimitRepository.this.update(rateLimit);
                    }

                    while (!AsyncRateLimitRepository.this.queue.isEmpty()) {
                        rateLimit = AsyncRateLimitRepository.this.queue.poll();
                        if (rateLimit != null) {
                            AsyncRateLimitRepository.this.delegateRateLimitRepository.save(rateLimit);
                        }
                    }
                } catch (InterruptedException ie) {
                    running = false;
                } catch (Exception ex) {
                    // Do nothing here
                }
            }
        }
    }
}
