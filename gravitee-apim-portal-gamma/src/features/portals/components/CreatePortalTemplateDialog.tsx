/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from '@gravitee/graphene-core';
import {
    ActivityIcon,
    CreditCardIcon,
    FileTextIcon,
    RocketIcon,
    type LucideIcon,
} from '@gravitee/graphene-core/icons';
import type { ReactNode } from 'react';

import { PORTAL_TEMPLATE_OPTIONS, type PortalTemplateId } from '../templates/portal-templates';
import styles from './CreatePortalTemplateDialog.module.scss';

const TEMPLATE_ICONS: Record<PortalTemplateId, LucideIcon> = {
    blank: FileTextIcon,
    starter: RocketIcon,
    payments: CreditCardIcon,
    'active-fitness': ActivityIcon,
};

interface CreatePortalTemplateDialogProps {
    readonly open: boolean;
    readonly isPending: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onSelect: (templateId: PortalTemplateId) => void;
}

function TemplateIcon({ icon: Icon }: { readonly icon: LucideIcon }): ReactNode {
    return (
        <span className={styles.iconWrap} aria-hidden="true">
            <Icon className={styles.icon} />
        </span>
    );
}

export function CreatePortalTemplateDialog({
    open,
    isPending,
    onOpenChange,
    onSelect,
}: CreatePortalTemplateDialogProps) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent
                className={styles.content}
                style={{ width: 'min(92vw, 42rem)', maxWidth: 'min(92vw, 42rem)' }}
            >
                <DialogHeader>
                    <DialogTitle>Choose portal template</DialogTitle>
                    <DialogDescription>
                        {isPending
                            ? 'Creating portal…'
                            : 'Start from scratch or pick a demo template with sample navigation and content.'}
                    </DialogDescription>
                </DialogHeader>

                <div
                    className={styles.grid}
                    role="listbox"
                    aria-label="Portal templates"
                    aria-busy={isPending}
                >
                    {PORTAL_TEMPLATE_OPTIONS.map(template => {
                        const Icon = TEMPLATE_ICONS[template.id];
                        return (
                            <button
                                key={template.id}
                                type="button"
                                className={styles.option}
                                role="option"
                                disabled={isPending}
                                onClick={() => onSelect(template.id)}
                            >
                                <TemplateIcon icon={Icon} />
                                <span className={styles.label}>{template.label}</span>
                                <span className={styles.description}>{template.description}</span>
                            </button>
                        );
                    })}
                </div>
            </DialogContent>
        </Dialog>
    );
}
