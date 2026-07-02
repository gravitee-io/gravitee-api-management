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
import type { OpenAPIV3 } from 'openapi-types';

import styles from './ApiSpecShared.module.scss';

function schemaTypeLabel(schema: OpenAPIV3.SchemaObject): string {
    if (schema.type) {
        if (Array.isArray(schema.type)) {
            return schema.type.join(' | ');
        }
        if (schema.type === 'array' && schema.items && !('$ref' in schema.items)) {
            return `array<${schemaTypeLabel(schema.items as OpenAPIV3.SchemaObject)}>`;
        }
        return schema.type;
    }
    if (schema.enum) {
        return `enum(${schema.enum.map(String).join(' | ')})`;
    }
    return 'object';
}

interface SchemaTreeProps {
    readonly name: string;
    readonly schema: OpenAPIV3.SchemaObject;
    readonly depth?: number;
}

export function SchemaTree({ name, schema, depth = 0 }: SchemaTreeProps) {
    const properties = schema.properties ?? {};
    const propertyEntries = Object.entries(properties);

    return (
        <div className={styles.schemaTree} style={{ marginLeft: depth > 0 ? 12 : 0 }}>
            <div>
                <span className={styles.schemaName}>{name}</span>
                <span className={styles.schemaMeta}>{schemaTypeLabel(schema)}</span>
                {schema.required?.includes(name) ? <span className={styles.required}> required</span> : null}
                {schema.description ? <div className={styles.description}>{schema.description}</div> : null}
            </div>
            {propertyEntries.length > 0 ? (
                <div className={styles.schemaNode}>
                    {propertyEntries.map(([propertyName, propertySchema]) => {
                        if ('$ref' in propertySchema) {
                            return (
                                <div key={propertyName}>
                                    <span className={styles.schemaName}>{propertyName}</span>
                                    <span className={styles.schemaMeta}>{propertySchema.$ref}</span>
                                </div>
                            );
                        }
                        return (
                            <SchemaTree
                                key={propertyName}
                                name={propertyName}
                                schema={propertySchema}
                                depth={depth + 1}
                            />
                        );
                    })}
                </div>
            ) : null}
        </div>
    );
}
