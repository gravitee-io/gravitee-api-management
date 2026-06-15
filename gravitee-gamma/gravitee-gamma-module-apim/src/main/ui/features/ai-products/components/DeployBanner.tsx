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
import { Button } from '@gravitee/graphene-core';
import { RocketIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';

import { notify } from '../../../shared/notify';
import { useDeployAiProduct } from '../hooks/useAiProductHooks';
import type { AiProduct } from '../types/aiProduct';

/**
 * "Out of sync" banner shown on detail pages when the product needs a redeploy
 * (components/plans changed since the last gateway deployment).
 */
export function DeployBanner({ product }: { product: AiProduct | null }) {
    const { mutate: deploy, isPending } = useDeployAiProduct();

    if (!product || product.deploymentState !== 'NEED_REDEPLOY') return null;

    function handleDeploy() {
        deploy(product!.id, {
            onSuccess: () => notify.success('AI Product deployed to the gateway'),
            onError: error => notify.error(error, 'Failed to deploy the AI Product.'),
        });
    }

    return (
        <div className="mx-6 mt-4 flex items-center justify-between gap-4 rounded-lg border border-warning/30 bg-warning/5 px-4 py-3">
            <div className="flex items-center gap-2.5">
                <TriangleAlertIcon className="size-4 shrink-0 text-warning" aria-hidden />
                <p className="text-sm">
                    <span className="font-medium">Out of sync</span>
                    <span className="text-muted-foreground"> — changes have not been deployed to the gateway yet.</span>
                </p>
            </div>
            <Button size="sm" onClick={handleDeploy} disabled={isPending} className="shrink-0">
                <RocketIcon className="size-4" aria-hidden />
                {isPending ? 'Deploying…' : 'Deploy'}
            </Button>
        </div>
    );
}
