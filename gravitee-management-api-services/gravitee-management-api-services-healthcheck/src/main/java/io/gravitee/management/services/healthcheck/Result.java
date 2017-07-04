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
package io.gravitee.management.services.healthcheck;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class Result {

    private static final Result HEALTHY = new Result(true, null);

    private static final int PRIME = 31;

    private final boolean healthy;
    private final String message;

    protected Result(boolean isHealthy, String message) {
        this.healthy = isHealthy;
        this.message = message;
    }

    public static Result unhealthy(Throwable error) {
        return new Result(false, error.getMessage());
    }

    public static Result unhealthy(String message) {
        return new Result(false, message);
    }

    public static Result unhealthy(String message, Object... args) {
        return unhealthy(String.format(message, args));
    }

    public static Result healthy(String message, Object... args) {
        return healthy(String.format(message, args));
    }

    public static Result healthy(String message) {
        return new Result(true, message);
    }

    public static Result healthy() {
        return HEALTHY;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        final Result result = (Result) o;
        return healthy == result.healthy &&
                !(message != null ? !message.equals(result.message) : result.message != null);
    }

    @Override
    public int hashCode() {
        int result = (healthy ? 1 : 0);
        result = PRIME * result + (message != null ? message.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("Result{isHealthy=");
        builder.append(healthy);
        if (message != null) {
            builder.append(", message=").append(message);
        }
        builder.append('}');
        return builder.toString();
    }
}
