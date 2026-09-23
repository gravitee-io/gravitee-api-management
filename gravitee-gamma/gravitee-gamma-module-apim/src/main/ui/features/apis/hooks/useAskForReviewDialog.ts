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
import { useState } from 'react';

import { useAskApiReview } from './useApiReviewMutations';
import type { ConfirmDialogProps } from '../../../shared/components';
import { notify } from '../../../shared/notify';

/**
 * The ask-for-review confirmation, shared by the API detail banner and the General page so both
 * ask with the same copy, the same mutation and the same toasts.
 */
export function useAskForReviewDialog(apiId: string | undefined) {
    const [open, setOpen] = useState(false);
    const mutation = useAskApiReview(apiId);

    const dialogProps: ConfirmDialogProps = {
        open,
        onOpenChange: setOpen,
        title: 'Review API',
        description: 'Are you sure you want to ask for a review of the API?',
        confirmLabel: 'Ask for review',
        pendingLabel: 'Asking…',
        isPending: mutation.isPending,
        onConfirm: () =>
            mutation.mutate(undefined, {
                onSuccess: () => {
                    setOpen(false);
                    notify.success('Review has been asked.');
                },
                onError: error => notify.error(error, 'Review has not been asked.'),
            }),
    };

    return { openDialog: () => setOpen(true), isPending: mutation.isPending, dialogProps };
}
