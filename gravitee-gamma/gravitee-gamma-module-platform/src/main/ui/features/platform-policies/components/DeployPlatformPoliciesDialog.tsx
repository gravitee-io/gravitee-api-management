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

import { Button, Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@gravitee/graphene-core';

export function DeployPlatformPoliciesDialog({
    open,
    isDeploying,
    onCancel,
    onConfirm,
}: Readonly<{
    open: boolean;
    isDeploying: boolean;
    onCancel: () => void;
    onConfirm: () => void;
}>) {
    return (
        <Dialog
            open={open}
            onOpenChange={isOpen => {
                if (!isOpen && !isDeploying) onCancel();
            }}
        >
            <DialogContent className="max-w-sm">
                <DialogHeader>
                    <DialogTitle>Deploy the policies?</DialogTitle>
                    <DialogDescription>
                        Platform policies are automatically deployed on gateways. Every HTTP API in this organization runs the updated flows
                        as soon as they are saved.
                    </DialogDescription>
                </DialogHeader>
                <DialogFooter className="border-t px-6 py-4 gap-2">
                    <Button type="button" variant="outline" onClick={onCancel} disabled={isDeploying}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={onConfirm} disabled={isDeploying}>
                        {isDeploying ? 'Deploying…' : 'Save and deploy'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
