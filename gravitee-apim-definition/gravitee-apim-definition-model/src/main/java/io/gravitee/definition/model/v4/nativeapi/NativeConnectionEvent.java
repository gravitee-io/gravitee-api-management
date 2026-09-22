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
package io.gravitee.definition.model.v4.nativeapi;

import java.util.Set;

/**
 * Which connection lifecycle events an API reports, as chosen by its owner.
 *
 * <p>Coarser than the statuses the gateway emits: {@code CONNECTION_ERROR}, {@code SESSION_ERROR} and
 * {@code INTERNAL_ERROR} all fall under {@link #ERROR}. Which of the three a given failure produces is a
 * detail of where the gateway caught it, and not something an API owner should have to reason about to
 * decide whether failures are worth reporting.
 */
public enum NativeConnectionEvent {
    /** A connection was established. */
    CONNECTED,
    /** A connection closed cleanly. */
    DISCONNECTED,
    /** A connection failed, whatever the phase it failed in. */
    ERROR;

    /**
     * What an API reports when it has never been configured — that is, what every API deployed before this
     * setting existed already does today.
     *
     * <p>Treating "absent" as "everything" would turn the upgrade into a silent behaviour change: every
     * existing API would start emitting {@link #DISCONNECTED} and roughly double the documents it writes,
     * without anyone asking for it.
     *
     * <p>The same set applies to an API created after this shipped. A richer default for new APIs was
     * considered and dropped: the only seam that could apply it runs on update as well as on create, so an
     * existing API updated without an analytics block would have been pinned to it — the exact harm the
     * nullable field exists to prevent. {@link #DISCONNECTED} is therefore opt-in for everyone, which is
     * also the honest position for a setting whose cost is doubling a healthy API's document volume.
     */
    public static final Set<NativeConnectionEvent> LEGACY_DEFAULTS = Set.of(CONNECTED, ERROR);
}
