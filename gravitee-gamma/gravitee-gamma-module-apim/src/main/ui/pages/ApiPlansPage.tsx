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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { useParams } from 'react-router-dom';

import { PlansPage } from './api-plans/PlansPage';
import type { PlanContext } from '../features/apis/types/plan';

export function ApiPlansPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const ctx: PlanContext = { type: 'api', entityId: apiId ?? '' };
    const canRead = useHasPermission({ anyOf: ['api-plan-r'] });
    const canCreate = useHasPermission({ anyOf: ['api-plan-c'] });
    const canUpdate = useHasPermission({ anyOf: ['api-plan-u'] });
    const canDelete = useHasPermission({ anyOf: ['api-plan-d'] });
    return <PlansPage ctx={ctx} canRead={canRead} canCreate={canCreate} canUpdate={canUpdate} canDelete={canDelete} />;
}
