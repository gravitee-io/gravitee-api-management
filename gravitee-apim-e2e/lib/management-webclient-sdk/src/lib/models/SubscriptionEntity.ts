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
import type { SubscriptionStatus } from './SubscriptionStatus';
import {
    SubscriptionStatusFromJSON,
    SubscriptionStatusFromJSONTyped,
    SubscriptionStatusToJSON,
} from './SubscriptionStatus';

/**
 * 
 * @export
 * @interface SubscriptionEntity
 */
export interface SubscriptionEntity {
    /**
     * 
     * @type {string}
     * @memberof SubscriptionEntity
     */
    api?: string;
    /**
     * 
     * @type {string}
     * @memberof SubscriptionEntity
     */
    application?: string;
    /**
     * 
     * @type {string}
     * @memberof SubscriptionEntity
     */
    client_id?: string;
    /**
     * 
     * @type {Date}
     * @memberof SubscriptionEntity
     */
    closed_at?: Date;
    /**
     * 
     * @type {string}
     * @memberof SubscriptionEntity
     */
    configuration?: string;
    /**
     * 
     * @type {Date}
     * @memberof SubscriptionEntity
     */
    created_at?: Date;
    /**
     * 
     * @type {number}
     * @memberof SubscriptionEntity
     */
    daysToExpirationOnLastNotification?: number;
    /**
     * 
     * @type {Date}
     * @memberof SubscriptionEntity
     */
    ending_at?: Date;
    /**
     * 
     * @type {string}
     * @memberof SubscriptionEntity
     */
    filter?: string;
    /**
     * 
     * @type {string}
     * @memberof SubscriptionEntity
     */
    id?: string;
    /**
     * 
     * @type {Array<string>}
     * @memberof SubscriptionEntity
     */
    keys?: Array<string>;
    /**
     * 
     * @type {{ [key: string]: string; }}
     * @memberof SubscriptionEntity
     */
    metadata?: { [key: string]: string; };
    /**
     * 
     * @type {Date}
     * @memberof SubscriptionEntity
     */
    paused_at?: Date;
    /**
     * 
     * @type {string}
     * @memberof SubscriptionEntity
     */
    plan?: string;
    /**
     * 
     * @type {Date}
     * @memberof SubscriptionEntity
     */
    processed_at?: Date;
    /**
     * 
     * @type {string}
     * @memberof SubscriptionEntity
     */
    processed_by?: string;
    /**
     * 
     * @type {string}
     * @memberof SubscriptionEntity
     */
    reason?: string;
    /**
     * 
     * @type {string}
     * @memberof SubscriptionEntity
     */
    request?: string;
    /**
     * 
     * @type {string}
     * @memberof SubscriptionEntity
     */
    security?: string;
    /**
     * 
     * @type {Date}
     * @memberof SubscriptionEntity
     */
    starting_at?: Date;
    /**
     * 
     * @type {SubscriptionStatus}
     * @memberof SubscriptionEntity
     */
    status?: SubscriptionStatus;
    /**
     * 
     * @type {string}
     * @memberof SubscriptionEntity
     */
    subscribed_by?: string;
    /**
     * 
     * @type {Date}
     * @memberof SubscriptionEntity
     */
    updated_at?: Date;
}

/**
 * Check if a given object implements the SubscriptionEntity interface.
 */
export function instanceOfSubscriptionEntity(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function SubscriptionEntityFromJSON(json: any): SubscriptionEntity {
    return SubscriptionEntityFromJSONTyped(json, false);
}

export function SubscriptionEntityFromJSONTyped(json: any, ignoreDiscriminator: boolean): SubscriptionEntity {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'api': !exists(json, 'api') ? undefined : json['api'],
        'application': !exists(json, 'application') ? undefined : json['application'],
        'client_id': !exists(json, 'client_id') ? undefined : json['client_id'],
        'closed_at': !exists(json, 'closed_at') ? undefined : (new Date(json['closed_at'])),
        'configuration': !exists(json, 'configuration') ? undefined : json['configuration'],
        'created_at': !exists(json, 'created_at') ? undefined : (new Date(json['created_at'])),
        'daysToExpirationOnLastNotification': !exists(json, 'daysToExpirationOnLastNotification') ? undefined : json['daysToExpirationOnLastNotification'],
        'ending_at': !exists(json, 'ending_at') ? undefined : (new Date(json['ending_at'])),
        'filter': !exists(json, 'filter') ? undefined : json['filter'],
        'id': !exists(json, 'id') ? undefined : json['id'],
        'keys': !exists(json, 'keys') ? undefined : json['keys'],
        'metadata': !exists(json, 'metadata') ? undefined : json['metadata'],
        'paused_at': !exists(json, 'paused_at') ? undefined : (new Date(json['paused_at'])),
        'plan': !exists(json, 'plan') ? undefined : json['plan'],
        'processed_at': !exists(json, 'processed_at') ? undefined : (new Date(json['processed_at'])),
        'processed_by': !exists(json, 'processed_by') ? undefined : json['processed_by'],
        'reason': !exists(json, 'reason') ? undefined : json['reason'],
        'request': !exists(json, 'request') ? undefined : json['request'],
        'security': !exists(json, 'security') ? undefined : json['security'],
        'starting_at': !exists(json, 'starting_at') ? undefined : (new Date(json['starting_at'])),
        'status': !exists(json, 'status') ? undefined : SubscriptionStatusFromJSON(json['status']),
        'subscribed_by': !exists(json, 'subscribed_by') ? undefined : json['subscribed_by'],
        'updated_at': !exists(json, 'updated_at') ? undefined : (new Date(json['updated_at'])),
    };
}

export function SubscriptionEntityToJSON(value?: SubscriptionEntity | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'api': value.api,
        'application': value.application,
        'client_id': value.client_id,
        'closed_at': value.closed_at === undefined ? undefined : (value.closed_at.toISOString()),
        'configuration': value.configuration,
        'created_at': value.created_at === undefined ? undefined : (value.created_at.toISOString()),
        'daysToExpirationOnLastNotification': value.daysToExpirationOnLastNotification,
        'ending_at': value.ending_at === undefined ? undefined : (value.ending_at.toISOString()),
        'filter': value.filter,
        'id': value.id,
        'keys': value.keys,
        'metadata': value.metadata,
        'paused_at': value.paused_at === undefined ? undefined : (value.paused_at.toISOString()),
        'plan': value.plan,
        'processed_at': value.processed_at === undefined ? undefined : (value.processed_at.toISOString()),
        'processed_by': value.processed_by,
        'reason': value.reason,
        'request': value.request,
        'security': value.security,
        'starting_at': value.starting_at === undefined ? undefined : (value.starting_at.toISOString()),
        'status': SubscriptionStatusToJSON(value.status),
        'subscribed_by': value.subscribed_by,
        'updated_at': value.updated_at === undefined ? undefined : (value.updated_at.toISOString()),
    };
}

