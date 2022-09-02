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
import type { DashboardReferenceType } from './DashboardReferenceType';
import {
    DashboardReferenceTypeFromJSON,
    DashboardReferenceTypeFromJSONTyped,
    DashboardReferenceTypeToJSON,
} from './DashboardReferenceType';

/**
 * 
 * @export
 * @interface NewDashboardEntity
 */
export interface NewDashboardEntity {
    /**
     * 
     * @type {string}
     * @memberof NewDashboardEntity
     */
    definition?: string;
    /**
     * 
     * @type {boolean}
     * @memberof NewDashboardEntity
     */
    enabled?: boolean;
    /**
     * 
     * @type {string}
     * @memberof NewDashboardEntity
     */
    name: string;
    /**
     * 
     * @type {string}
     * @memberof NewDashboardEntity
     */
    query_filter?: string;
    /**
     * 
     * @type {string}
     * @memberof NewDashboardEntity
     */
    reference_id: string;
    /**
     * 
     * @type {DashboardReferenceType}
     * @memberof NewDashboardEntity
     */
    reference_type: DashboardReferenceType;
}

/**
 * Check if a given object implements the NewDashboardEntity interface.
 */
export function instanceOfNewDashboardEntity(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "reference_id" in value;
    isInstance = isInstance && "reference_type" in value;

    return isInstance;
}

export function NewDashboardEntityFromJSON(json: any): NewDashboardEntity {
    return NewDashboardEntityFromJSONTyped(json, false);
}

export function NewDashboardEntityFromJSONTyped(json: any, ignoreDiscriminator: boolean): NewDashboardEntity {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'definition': !exists(json, 'definition') ? undefined : json['definition'],
        'enabled': !exists(json, 'enabled') ? undefined : json['enabled'],
        'name': json['name'],
        'query_filter': !exists(json, 'query_filter') ? undefined : json['query_filter'],
        'reference_id': json['reference_id'],
        'reference_type': DashboardReferenceTypeFromJSON(json['reference_type']),
    };
}

export function NewDashboardEntityToJSON(value?: NewDashboardEntity | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'definition': value.definition,
        'enabled': value.enabled,
        'name': value.name,
        'query_filter': value.query_filter,
        'reference_id': value.reference_id,
        'reference_type': DashboardReferenceTypeToJSON(value.reference_type),
    };
}

