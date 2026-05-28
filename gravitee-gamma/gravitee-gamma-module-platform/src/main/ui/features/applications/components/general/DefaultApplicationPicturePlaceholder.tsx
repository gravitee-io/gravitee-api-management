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

/** Default application picture placeholder (matches console `gio-avatar` pattern). */
export function DefaultApplicationPicturePlaceholder({ className }: Readonly<{ className?: string }>) {
    return (
        <svg viewBox="0 0 108 108" className={className} aria-hidden style={{ width: 108, height: 108 }}>
            <rect width="108" height="108" fill="#ffffff" />
            <rect x="36" y="36" width="36" height="36" fill="#c86b7a" />
            <circle cx="45" cy="45" r="4.5" fill="#ffffff" />
            <circle cx="63" cy="45" r="4.5" fill="#ffffff" />
            <circle cx="45" cy="63" r="4.5" fill="#ffffff" />
            <circle cx="63" cy="63" r="4.5" fill="#ffffff" />
            <polygon points="54,8 64,18 54,28 44,18" fill="#c86b7a" />
            <polygon points="54,80 64,90 54,100 44,90" fill="#c86b7a" />
            <polygon points="8,54 18,64 28,54 18,44" fill="#c86b7a" />
            <polygon points="80,54 90,64 100,54 90,44" fill="#c86b7a" />
            <circle cx="54" cy="18" r="5" fill="#5c5c5c" />
            <circle cx="54" cy="30" r="5" fill="#5c5c5c" />
            <circle cx="54" cy="78" r="5" fill="#5c5c5c" />
            <circle cx="54" cy="90" r="5" fill="#5c5c5c" />
            <circle cx="18" cy="54" r="5" fill="#5c5c5c" />
            <circle cx="30" cy="54" r="5" fill="#5c5c5c" />
            <circle cx="78" cy="54" r="5" fill="#5c5c5c" />
            <circle cx="90" cy="54" r="5" fill="#5c5c5c" />
        </svg>
    );
}
