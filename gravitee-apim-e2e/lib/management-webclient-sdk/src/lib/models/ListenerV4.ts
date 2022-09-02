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

import {
    HttpListenerV4,
    instanceOfHttpListenerV4,
    HttpListenerV4FromJSON,
    HttpListenerV4FromJSONTyped,
    HttpListenerV4ToJSON,
} from './HttpListenerV4';
import {
    SubscriptionListenerV4,
    instanceOfSubscriptionListenerV4,
    SubscriptionListenerV4FromJSON,
    SubscriptionListenerV4FromJSONTyped,
    SubscriptionListenerV4ToJSON,
} from './SubscriptionListenerV4';
import {
    TcpListenerV4,
    instanceOfTcpListenerV4,
    TcpListenerV4FromJSON,
    TcpListenerV4FromJSONTyped,
    TcpListenerV4ToJSON,
} from './TcpListenerV4';

/**
 * @type ListenerV4
 * A list of listeners used to describe our you api could be reached.
 * @export
 */
export type ListenerV4 = { type: 'http' } & HttpListenerV4 | { type: 'subscription' } & SubscriptionListenerV4 | { type: 'tcp' } & TcpListenerV4;

export function ListenerV4FromJSON(json: any): ListenerV4 {
    return ListenerV4FromJSONTyped(json, false);
}

export function ListenerV4FromJSONTyped(json: any, ignoreDiscriminator: boolean): ListenerV4 {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    switch (json['type']) {
        case 'http':
            return {...HttpListenerV4FromJSONTyped(json, true), type: 'http'};
        case 'subscription':
            return {...SubscriptionListenerV4FromJSONTyped(json, true), type: 'subscription'};
        case 'tcp':
            return {...TcpListenerV4FromJSONTyped(json, true), type: 'tcp'};
        default:
            throw new Error(`No variant of ListenerV4 exists with 'type=${json['type']}'`);
    }
}

export function ListenerV4ToJSON(value?: ListenerV4 | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    switch (value['type']) {
        case 'http':
            return HttpListenerV4ToJSON(value);
        case 'subscription':
            return SubscriptionListenerV4ToJSON(value);
        case 'tcp':
            return TcpListenerV4ToJSON(value);
        default:
            throw new Error(`No variant of ListenerV4 exists with 'type=${value['type']}'`);
    }

}

