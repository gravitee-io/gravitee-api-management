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
/**
 * 
 * @export
 * @interface ApiHeaderEntity
 */
export interface ApiHeaderEntity {
    /**
     * 
     * @type {Date}
     * @memberof ApiHeaderEntity
     */
    created_at?: Date;
    /**
     * 
     * @type {string}
     * @memberof ApiHeaderEntity
     */
    id?: string;
    /**
     * 
     * @type {string}
     * @memberof ApiHeaderEntity
     */
    name?: string;
    /**
     * 
     * @type {number}
     * @memberof ApiHeaderEntity
     */
    order?: number;
    /**
     * 
     * @type {Date}
     * @memberof ApiHeaderEntity
     */
    updated_at?: Date;
    /**
     * 
     * @type {string}
     * @memberof ApiHeaderEntity
     */
    value?: string;
}

/**
 * Check if a given object implements the ApiHeaderEntity interface.
 */
export function instanceOfApiHeaderEntity(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function ApiHeaderEntityFromJSON(json: any): ApiHeaderEntity {
    return ApiHeaderEntityFromJSONTyped(json, false);
}

export function ApiHeaderEntityFromJSONTyped(json: any, ignoreDiscriminator: boolean): ApiHeaderEntity {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'created_at': !exists(json, 'created_at') ? undefined : (new Date(json['created_at'])),
        'id': !exists(json, 'id') ? undefined : json['id'],
        'name': !exists(json, 'name') ? undefined : json['name'],
        'order': !exists(json, 'order') ? undefined : json['order'],
        'updated_at': !exists(json, 'updated_at') ? undefined : (new Date(json['updated_at'])),
        'value': !exists(json, 'value') ? undefined : json['value'],
    };
}

export function ApiHeaderEntityToJSON(value?: ApiHeaderEntity | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'created_at': value.created_at === undefined ? undefined : (value.created_at.toISOString()),
        'id': value.id,
        'name': value.name,
        'order': value.order,
        'updated_at': value.updated_at === undefined ? undefined : (value.updated_at.toISOString()),
        'value': value.value,
    };
}

