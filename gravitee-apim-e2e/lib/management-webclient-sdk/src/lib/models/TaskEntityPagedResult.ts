/* tslint:disable */
/* eslint-disable */
/**
 * Gravitee.io - Management API
 * Some news resources are in alpha version. This implies that they are likely to be modified or even removed in future versions. They are marked with the 🧪 symbol
 *
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
import type { Page } from './Page';
import {
    PageFromJSON,
    PageFromJSONTyped,
    PageToJSON,
} from './Page';
import type { TaskEntity } from './TaskEntity';
import {
    TaskEntityFromJSON,
    TaskEntityFromJSONTyped,
    TaskEntityToJSON,
} from './TaskEntity';

/**
 * 
 * @export
 * @interface TaskEntityPagedResult
 */
export interface TaskEntityPagedResult {
    /**
     * 
     * @type {Array<TaskEntity>}
     * @memberof TaskEntityPagedResult
     */
    data?: Array<TaskEntity>;
    /**
     * 
     * @type {{ [key: string]: { [key: string]: any; }; }}
     * @memberof TaskEntityPagedResult
     */
    metadata?: { [key: string]: { [key: string]: any; }; };
    /**
     * 
     * @type {Page}
     * @memberof TaskEntityPagedResult
     */
    page?: Page;
}

/**
 * Check if a given object implements the TaskEntityPagedResult interface.
 */
export function instanceOfTaskEntityPagedResult(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function TaskEntityPagedResultFromJSON(json: any): TaskEntityPagedResult {
    return TaskEntityPagedResultFromJSONTyped(json, false);
}

export function TaskEntityPagedResultFromJSONTyped(json: any, ignoreDiscriminator: boolean): TaskEntityPagedResult {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'data': !exists(json, 'data') ? undefined : ((json['data'] as Array<any>).map(TaskEntityFromJSON)),
        'metadata': !exists(json, 'metadata') ? undefined : json['metadata'],
        'page': !exists(json, 'page') ? undefined : PageFromJSON(json['page']),
    };
}

export function TaskEntityPagedResultToJSON(value?: TaskEntityPagedResult | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'data': value.data === undefined ? undefined : ((value.data as Array<any>).map(TaskEntityToJSON)),
        'metadata': value.metadata,
        'page': PageToJSON(value.page),
    };
}

