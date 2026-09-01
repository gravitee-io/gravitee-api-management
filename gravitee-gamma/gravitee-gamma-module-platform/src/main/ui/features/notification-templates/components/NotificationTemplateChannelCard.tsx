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

import { Badge, Card, CardContent, CardHeader, CardTitle, Input, Label, Switch } from '@gravitee/graphene-core';
import { CodeEditor } from '@gravitee/graphene-core/code-editor';
import { MailIcon, MonitorIcon } from '@gravitee/graphene-core/icons';

import type { NotificationTemplateDraft, NotificationTemplateType } from '../types/notificationTemplate';

const CHANNEL_COPY: Record<NotificationTemplateType, { title: string; description: string }> = {
    EMAIL: { title: 'Email notification', description: 'Sent by email to everyone subscribed to this event.' },
    PORTAL: {
        title: 'Portal notification',
        description: 'Shown in the notification center of the console and the Developer Portal.',
    },
};

export function NotificationTemplateChannelCard({
    type,
    draft,
    isInclude,
    disabled,
    showErrors,
    onChange,
}: Readonly<{
    type: NotificationTemplateType;
    draft: NotificationTemplateDraft;
    isInclude: boolean;
    disabled: boolean;
    showErrors: boolean;
    onChange: (next: NotificationTemplateDraft) => void;
}>) {
    const copy = CHANNEL_COPY[type];
    const titleId = `${type.toLowerCase()}-title`;
    const titleErrorId = `${titleId}-error`;
    const contentId = `${type.toLowerCase()}-content`;
    const contentLabelId = `${contentId}-label`;
    const contentErrorId = `${contentId}-error`;
    const titleError = showErrors && draft.enabled && !isInclude && !draft.title.trim();
    const contentError = showErrors && draft.enabled && !draft.content.trim();
    const fieldsDisabled = disabled || !draft.enabled;
    const Icon = type === 'EMAIL' ? MailIcon : MonitorIcon;

    return (
        <Card>
            <CardHeader className="space-y-2">
                <div className="flex flex-wrap items-center gap-2">
                    <Icon className="size-4" aria-hidden />
                    <CardTitle className="text-base">{copy.title}</CardTitle>
                    {draft.enabled ? <Badge variant="secondary">Custom</Badge> : null}
                </div>
                <p className="text-sm text-muted-foreground">{copy.description}</p>
            </CardHeader>
            <CardContent className="space-y-4">
                <div className="flex items-center justify-between gap-4 rounded-lg border px-3 py-2">
                    <Label htmlFor={`${type.toLowerCase()}-override`}>Override default template</Label>
                    <Switch
                        id={`${type.toLowerCase()}-override`}
                        checked={draft.enabled}
                        disabled={disabled}
                        onCheckedChange={checked => onChange({ ...draft, enabled: checked })}
                    />
                </div>
                {isInclude ? null : (
                    <div className="space-y-2">
                        <Label htmlFor={titleId}>Title of the notification</Label>
                        <Input
                            id={titleId}
                            value={draft.title}
                            disabled={fieldsDisabled}
                            onChange={event => onChange({ ...draft, title: event.target.value })}
                            aria-invalid={titleError || undefined}
                            aria-describedby={titleError ? titleErrorId : undefined}
                        />
                        {titleError ? (
                            <p id={titleErrorId} className="text-sm text-destructive">
                                Title of the notification is required.
                            </p>
                        ) : null}
                    </div>
                )}
                <div className="space-y-2">
                    <Label id={contentLabelId}>Content</Label>
                    {/* eslint-disable-next-line jsx-a11y/role-supports-aria-props -- CodeEditor is not a native control; this group is the labelled invalid wrapper. */}
                    <div
                        id={contentId}
                        role="group"
                        aria-labelledby={contentLabelId}
                        aria-describedby={contentError ? contentErrorId : undefined}
                        aria-invalid={contentError || undefined}
                    >
                        <CodeEditor
                            language="html"
                            value={draft.content}
                            onChange={value => onChange({ ...draft, content: value ?? '' })}
                            height={280}
                            disabled={fieldsDisabled}
                            readOnly={fieldsDisabled}
                        />
                    </div>
                    {contentError ? (
                        <p id={contentErrorId} className="text-sm text-destructive">
                            Content is required.
                        </p>
                    ) : null}
                </div>
            </CardContent>
        </Card>
    );
}
