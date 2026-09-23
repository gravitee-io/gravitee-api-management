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
import { Alert, AlertDescription, Button, Textarea, cn } from '@gravitee/graphene-core';
import { useMemo, useState } from 'react';

import type { PageType } from '../../../types/documentation';
import { EDITOR_HEIGHT, markdownToHtml } from './documentation-shared';

function SpecPreview({ type, content }: { type: Exclude<PageType, 'FOLDER' | 'MARKDOWN'>; content: string }) {
    const parsed = useMemo(() => {
        const trimmed = content.trim();
        if (!trimmed) return { error: 'Nothing to preview yet.' as const };
        try {
            const value = trimmed.startsWith('{') ? JSON.parse(trimmed) : trimmed;
            if (typeof value === 'string') {
                const titleMatch = value.match(/^\s*(?:info:[\s\S]*?title:\s*)(.+)$/m);
                return { value: { info: { title: titleMatch?.[1]?.trim() }, raw: true } };
            }
            if (!value || typeof value !== 'object') return { error: 'Could not parse this spec for preview.' as const };
            return { value: value as Record<string, unknown> };
        } catch {
            return { error: 'Could not parse this spec for preview.' as const };
        }
    }, [content]);

    if ('error' in parsed) {
        return <p className="text-sm text-muted-foreground">{parsed.error}</p>;
    }

    const info =
        parsed.value.info && typeof parsed.value.info === 'object'
            ? (parsed.value.info as { title?: string; version?: string; description?: string })
            : {};
    const paths =
        type === 'SWAGGER' && parsed.value.paths && typeof parsed.value.paths === 'object'
            ? Object.keys(parsed.value.paths as object)
            : [];
    const channels =
        type === 'ASYNCAPI' && parsed.value.channels && typeof parsed.value.channels === 'object'
            ? Object.keys(parsed.value.channels as object)
            : [];

    return (
        <div className="space-y-3 text-sm">
            <div>
                <p className="font-semibold">{info.title || 'Untitled spec'}</p>
                {info.version ? <p className="text-xs text-muted-foreground">Version {info.version}</p> : null}
            </div>
            {info.description ? <p className="text-muted-foreground">{info.description}</p> : null}
            {paths.length > 0 ? (
                <div>
                    <p className="mb-1 text-xs font-semibold tracking-wide uppercase">Paths</p>
                    <ul className="space-y-1 font-mono text-xs">
                        {paths.map(path => (
                            <li key={path}>{path}</li>
                        ))}
                    </ul>
                </div>
            ) : null}
            {channels.length > 0 ? (
                <div>
                    <p className="mb-1 text-xs font-semibold tracking-wide uppercase">Channels</p>
                    <ul className="space-y-1 font-mono text-xs">
                        {channels.map(channel => (
                            <li key={channel}>{channel}</li>
                        ))}
                    </ul>
                </div>
            ) : null}
        </div>
    );
}

export function DocumentationContentEditor({
    type,
    content,
    readOnly,
    isExternal,
    error,
    onChange,
}: {
    type: Exclude<PageType, 'FOLDER'>;
    content: string;
    readOnly: boolean;
    isExternal: boolean;
    error?: string | null;
    onChange: (value: string) => void;
}) {
    const [preview, setPreview] = useState(false);
    const html = useMemo(() => (type === 'MARKDOWN' ? markdownToHtml(content) : ''), [content, type]);

    return (
        <div className="space-y-3">
            <div className="flex items-center justify-between gap-3">
                <h2 className="text-base font-semibold">Page content</h2>
                <Button type="button" variant="outline" size="sm" onClick={() => setPreview(current => !current)}>
                    Toggle preview
                </Button>
            </div>

            {isExternal ? (
                <Alert>
                    <AlertDescription>
                        This content is a preview and cannot be edited because the page is linked to an external source.
                    </AlertDescription>
                </Alert>
            ) : null}

            <div className={cn('grid gap-3', preview ? 'lg:grid-cols-2' : 'grid-cols-1')}>
                {isExternal && !content.trim() ? (
                    <div
                        className="flex items-center rounded-lg border p-4 text-sm text-muted-foreground"
                        style={{ minHeight: EDITOR_HEIGHT }}
                    >
                        Preview is unavailable until the gateway fetches this source. Configuration is stored on the page.
                    </div>
                ) : (
                    <Textarea
                        value={content}
                        disabled={readOnly || isExternal}
                        onChange={event => onChange(event.target.value)}
                        className="font-mono text-sm"
                        style={{ minHeight: EDITOR_HEIGHT }}
                        aria-label="Page content"
                    />
                )}

                {preview ? (
                    <div
                        className="overflow-auto rounded-lg border bg-background p-4"
                        style={{ minHeight: EDITOR_HEIGHT, maxHeight: EDITOR_HEIGHT }}
                    >
                        {type === 'MARKDOWN' ? (
                            content.trim() ? (
                                <div dangerouslySetInnerHTML={{ __html: html }} />
                            ) : (
                                <p className="text-sm text-muted-foreground">Nothing to preview yet.</p>
                            )
                        ) : (
                            <SpecPreview type={type} content={content} />
                        )}
                    </div>
                ) : null}
            </div>

            {error ? <p className="text-sm text-destructive">{error}</p> : null}
        </div>
    );
}
