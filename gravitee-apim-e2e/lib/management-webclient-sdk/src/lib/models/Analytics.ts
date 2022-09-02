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
 * @interface Analytics
 */
export interface Analytics {
    /**
     * 
     * @type {number}
     * @memberof Analytics
     */
    clientTimeout?: number;
}

/**
 * Check if a given object implements the Analytics interface.
 */
export function instanceOfAnalytics(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function AnalyticsFromJSON(json: any): Analytics {
    return AnalyticsFromJSONTyped(json, false);
}

export function AnalyticsFromJSONTyped(json: any, ignoreDiscriminator: boolean): Analytics {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'clientTimeout': !exists(json, 'clientTimeout') ? undefined : json['clientTimeout'],
    };
}

export function AnalyticsToJSON(value?: Analytics | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'clientTimeout': value.clientTimeout,
    };
}

