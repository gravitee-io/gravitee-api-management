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
package io.gravitee.gateway.services.sync.process.common.deployer;

import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.CustomLog;

/**
 * Keeps one item's failure from costing more than that item.
 *
 * <p>The deployers run their work inside {@code Completable.fromRunnable} / {@code Completable.defer},
 * and RxJava rethrows what it considers fatal from {@code subscribeActual} rather than routing it to
 * {@code onError}. Two failure shapes matter here, and neither is an {@link Exception}:
 *
 * <ul>
 *   <li>{@link LinkageError} — registering a client certificate links BouncyCastle classes lazily, so a
 *       mismatched provider on the class path surfaces as an Error. RxJava rethrows it, it escapes the
 *       runnable and kills the sync thread: initial sync never completes and the node never turns ready.
 *   <li>{@link AssertionError} — BouncyCastle FIPS raises {@code FipsUnapprovedOperationError}, which
 *       extends it, when a key or algorithm is outside the approved set. RxJava does <em>not</em> treat
 *       that as fatal, so it errors the whole Completable instead: the remaining items of the same
 *       deployable are never processed and the failure propagates into the synchronizer.
 * </ul>
 *
 * <p>{@link VirtualMachineError} is always rethrown: it says nothing about the item being processed.
 */
@CustomLog
final class SyncIsolation {

    private SyncIsolation() {}

    /**
     * Runs one item of a step whose other items must still be processed.
     *
     * <p>A {@link LinkageError} is logged at error level rather than handed to {@code onFailure}: the class
     * path is inconsistent, so every item is about to fail the same way. Only the log level changes — the
     * item is still skipped, the synchronization still completes and the node still turns ready. What the
     * error level buys is that a gateway missing its BouncyCastle jars stops reporting a total outage as a
     * handful of warnings.
     *
     * @param what describes the item, evaluated only when something failed
     * @param onFailure receives anything else the action threw, for the caller to log with its own context
     * @return whether the action ran to completion
     */
    static boolean isolate(final Supplier<String> what, final Runnable action, final Consumer<Throwable> onFailure) {
        try {
            action.run();
            return true;
        } catch (VirtualMachineError e) {
            throw e;
        } catch (LinkageError e) {
            log.error("Class path is inconsistent, cannot {}", what.get(), e);
            return false;
        } catch (Throwable t) {
            onFailure.accept(t);
            return false;
        }
    }

    /**
     * Runs a step whose caller turns a failure into its own error and acts on it — undeploying an API
     * Product, where the caller must not go on to mark the product gone.
     *
     * <p>Only Errors are trapped, and they are translated rather than swallowed: the thread survives, and
     * the caller still sees the failure in the shape it already handles. Exceptions keep propagating
     * untouched, so the existing contract is unchanged.
     */
    static void translateErrors(final Supplier<String> what, final Runnable action) {
        try {
            action.run();
        } catch (VirtualMachineError e) {
            throw e;
        } catch (Error e) {
            throw new SyncException("Failed to %s".formatted(what.get()), e);
        }
    }
}
