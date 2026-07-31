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
package io.gravitee.repository.ratelimit.api;

import java.io.Serializable;

/**
 * Outcome of a single {@link TokenBucketRateLimitRepository#refillAndTryConsume} operation.
 *
 * @param allowed              whether the request was permitted (a token was available and consumed)
 * @param remainingTokens      whole tokens left in the bucket after the operation, floored to an integer
 * @param nextAvailableAtMillis epoch millis at which at least one token will be available; equal to the
 *                              supplied {@code now} when tokens already remain
 * @author GraviteeSource Team
 */
public record TokenBucketConsumeResult(boolean allowed, long remainingTokens, long nextAvailableAtMillis) implements Serializable {}
