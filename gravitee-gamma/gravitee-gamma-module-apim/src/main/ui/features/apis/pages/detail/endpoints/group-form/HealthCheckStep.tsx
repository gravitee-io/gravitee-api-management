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
import { FlaskConicalIcon } from '@gravitee/graphene-core/icons';

export function HealthCheckStep() {
    return (
        <div className="flex flex-col items-center justify-center gap-4 py-12 text-center">
            <div
                className="flex size-14 items-center justify-center rounded-xl"
                style={{ backgroundColor: 'color-mix(in oklab, var(--color-primary) 10%, transparent)' }}
            >
                <FlaskConicalIcon className="size-7 text-primary" aria-hidden />
            </div>
            <div className="space-y-1 max-w-xs">
                <p className="text-sm font-semibold text-foreground">Health-check — coming soon</p>
                <p className="text-sm text-muted-foreground">
                    Automated health-check configuration will be available in a future release. The gateway will be able to probe endpoints
                    and automatically take unhealthy backends out of rotation.
                </p>
            </div>
        </div>
    );
}
