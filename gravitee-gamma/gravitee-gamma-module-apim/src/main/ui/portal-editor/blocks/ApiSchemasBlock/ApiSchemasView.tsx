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
import { useApiSpec } from '../ApiSpecBlock/ApiSpecContext';
import { BlockConfigChip, EmptyState, LoadingState } from '../ApiSpecBlock/shared/ApiSpecShared';
import styles from '../ApiSpecBlock/shared/ApiSpecShared.module.scss';
import { SchemaTree } from '../ApiSpecBlock/shared/SchemaTree';

interface ApiSchemasViewProps {
    readonly tag: string;
    readonly operationId?: string;
    readonly isEditable: boolean;
}

export function ApiSchemasView({ tag, operationId, isEditable }: ApiSchemasViewProps) {
    const { getSchemasByTag, isLoading } = useApiSpec();

    if (isEditable) {
        return <BlockConfigChip label="API Schemas" tag={tag || undefined} operationId={operationId || undefined} />;
    }

    if (isLoading) {
        return <LoadingState />;
    }

    const schemas = getSchemasByTag(tag, operationId || undefined);
    const entries = Object.entries(schemas);

    if (entries.length === 0) {
        return <EmptyState message={tag ? `No schemas found for tag "${tag}".` : 'Configure a tag for this block.'} />;
    }

    return (
        <div>
            {entries.map(([name, schema]) => (
                <section key={name} className={styles.section}>
                    <div className={styles.subsection}>
                        <SchemaTree name={name} schema={schema} />
                    </div>
                </section>
            ))}
        </div>
    );
}
