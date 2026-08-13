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
package io.gravitee.gateway.reactive.core.context.diagnostic;

import io.gravitee.gateway.reactive.api.ExecutionWarn;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.reporter.api.v4.metric.Diagnostic;
import io.gravitee.reporter.api.v4.metric.Metrics;
import org.slf4j.Logger;
import org.springframework.core.NestedExceptionUtils;

/**
 * Reports the last-resort failure path of an API reactor: an error that reached the end of the chain without having
 * been turned into a proper execution failure, and which the reactor answers with a bare 500.
 *
 * <p>Left alone, that path is close to undiagnosable. It logs "Unexpected error while handling request" and nothing
 * else, while overwriting whatever status the request had reached — so the reported outcome can be a 500 carrying an
 * error key that belongs to a completely different status (a {@code GATEWAY_CLIENT_*} key normally answered with a
 * 502, for instance), with an empty body because the error handling itself never got to write one. The combination is
 * indistinguishable, from analytics alone, from the gateway simply returning 500.
 *
 * <p>This records what is needed to tell those apart, on both sides:
 * <ul>
 *   <li>in the gateway log, a line naming the request and transaction ids, the status being replaced, the error
 *       already attributed, the endpoint reached and the cause — enough to conclude without correlating anything;</li>
 *   <li>in the analytics, a key of its own when nothing was attributed, so the request stops being reported as a
 *       success that happens to carry a 500 — and otherwise a warning carrying the exception, which would otherwise
 *       exist nowhere but the gateway's own log file.</li>
 * </ul>
 */
public final class UnexpectedErrorReporter {

    /** Key recorded when the failure reached the reactor with nothing attributed to it yet. */
    public static final String UNEXPECTED_ERROR_KEY = "GATEWAY_UNEXPECTED_ERROR";

    private UnexpectedErrorReporter() {}

    /**
     * Reports the failure <em>and</em> answers it with the given status, in that order.
     * <p>
     * The two are done here together on purpose: every message below states the status the request is answered with,
     * and the status the request had reached before that. Leaving the caller to apply the status separately would let
     * the two drift apart, and the reports would then describe a response that was never sent.
     *
     * @param statusCode the status to answer with; the caller still decides which one, it is simply written once.
     */
    public static void recordAndAnswer(
        final HttpPlainExecutionContext ctx,
        final int statusCode,
        final String reasonPhrase,
        final Throwable throwable,
        final Logger log
    ) {
        final Metrics metrics = ctx.metrics();
        final Diagnostic failure = metrics != null ? metrics.getFailure() : null;
        final String attributedKey = attributedKey(metrics, failure);
        // Read before the answer below overwrites it.
        final int replacedStatus = ctx.response().status();

        // The contextual logger already carries apiId, apiName, envId, orgId, appId, planId and user, but neither
        // request nor transaction id — and those are what tie this line to the analytics entry it explains. The rest
        // is what one needs to decide whether the answered status is the real outcome or an overwrite: the status it
        // replaces, the error already attributed, where the request had got to, and the cause spelled out for the
        // cases where the stack trace is truncated by whatever collects these logs.
        ctx
            .withLogger(log)
            .error(
                "Unexpected error while handling request, answering {} [requestId={}, transactionId={}, " +
                    "statusBeingReplaced={}, attributedErrorKey={}, attributedFailure={}, endpoint={}, elapsed={}ms]: {}",
                statusCode,
                metrics != null ? metrics.getRequestId() : null,
                metrics != null ? metrics.getTransactionId() : null,
                replacedStatus,
                attributedKey,
                failure != null,
                metrics != null ? metrics.getEndpoint() : null,
                elapsedMillis(metrics),
                mostSpecificCause(throwable),
                throwable
            );

        if (metrics != null) {
            if (attributedKey == null) {
                // Nothing was attributed: without a key of its own the request is reported as a success that happens
                // to carry an error status.
                metrics.setErrorKey(UNEXPECTED_ERROR_KEY);
                metrics.setErrorMessage(
                    "The request failed unexpectedly while being handled by the gateway, and was answered with a " +
                        statusCode +
                        " (" +
                        mostSpecificCause(throwable) +
                        ")"
                );
            } else {
                // Something was already attributed, and it is the more precise diagnosis: it names what actually went
                // wrong, whereas reaching this path only says the gateway could not handle the consequence. Keep it,
                // and record the exception as a warning — otherwise it exists nowhere but the gateway's log file.
                ctx.warnWith(
                    new ExecutionWarn(UNEXPECTED_ERROR_KEY)
                        .message(
                            "The gateway could not complete the handling of this request and answered " +
                                statusCode +
                                ", replacing status " +
                                replacedStatus
                        )
                        .cause(throwable)
                );
            }
        }

        ctx.response().status(statusCode);
        ctx.response().reason(reasonPhrase);
    }

    /** How long the request had been running, so a recurring duration (a shutdown countdown) stands out. */
    private static long elapsedMillis(final Metrics metrics) {
        if (metrics == null || metrics.timestamp() == null) {
            return -1L;
        }
        return System.currentTimeMillis() - metrics.timestamp().toEpochMilli();
    }

    /**
     * Description of the failure to carry into analytics: the message of the most specific cause, which is the one
     * naming the actual fault, or its type when it carries no message.
     */
    private static String mostSpecificCause(final Throwable throwable) {
        final Throwable cause = NestedExceptionUtils.getMostSpecificCause(throwable);
        return cause.getMessage() != null && !cause.getMessage().isBlank() ? cause.getMessage() : cause.getClass().getSimpleName();
    }

    /**
     * The error already attributed to the request, wherever it was recorded: connectors set {@code errorKey} on the
     * metrics, while {@code interruptWith} records a {@link Diagnostic} failure without touching it.
     */
    private static String attributedKey(final Metrics metrics, final Diagnostic failure) {
        if (metrics == null) {
            return null;
        }
        if (metrics.getErrorKey() != null) {
            return metrics.getErrorKey();
        }
        if (failure != null) {
            return failure.getKey();
        }
        return null;
    }
}
