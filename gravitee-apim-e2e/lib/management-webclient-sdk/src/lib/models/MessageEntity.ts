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
import type { MessageChannel } from './MessageChannel';
import {
    MessageChannelFromJSON,
    MessageChannelFromJSONTyped,
    MessageChannelToJSON,
} from './MessageChannel';
import type { MessageRecipientEntity } from './MessageRecipientEntity';
import {
    MessageRecipientEntityFromJSON,
    MessageRecipientEntityFromJSONTyped,
    MessageRecipientEntityToJSON,
} from './MessageRecipientEntity';

/**
 * 
 * @export
 * @interface MessageEntity
 */
export interface MessageEntity {
    /**
     * 
     * @type {MessageChannel}
     * @memberof MessageEntity
     */
    channel?: MessageChannel;
    /**
     * 
     * @type {{ [key: string]: string; }}
     * @memberof MessageEntity
     */
    params?: { [key: string]: string; };
    /**
     * 
     * @type {MessageRecipientEntity}
     * @memberof MessageEntity
     */
    recipient?: MessageRecipientEntity;
    /**
     * 
     * @type {string}
     * @memberof MessageEntity
     */
    text?: string;
    /**
     * 
     * @type {string}
     * @memberof MessageEntity
     */
    title?: string;
    /**
     * 
     * @type {boolean}
     * @memberof MessageEntity
     */
    useSystemProxy?: boolean;
}

/**
 * Check if a given object implements the MessageEntity interface.
 */
export function instanceOfMessageEntity(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function MessageEntityFromJSON(json: any): MessageEntity {
    return MessageEntityFromJSONTyped(json, false);
}

export function MessageEntityFromJSONTyped(json: any, ignoreDiscriminator: boolean): MessageEntity {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'channel': !exists(json, 'channel') ? undefined : MessageChannelFromJSON(json['channel']),
        'params': !exists(json, 'params') ? undefined : json['params'],
        'recipient': !exists(json, 'recipient') ? undefined : MessageRecipientEntityFromJSON(json['recipient']),
        'text': !exists(json, 'text') ? undefined : json['text'],
        'title': !exists(json, 'title') ? undefined : json['title'],
        'useSystemProxy': !exists(json, 'useSystemProxy') ? undefined : json['useSystemProxy'],
    };
}

export function MessageEntityToJSON(value?: MessageEntity | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'channel': MessageChannelToJSON(value.channel),
        'params': value.params,
        'recipient': MessageRecipientEntityToJSON(value.recipient),
        'text': value.text,
        'title': value.title,
        'useSystemProxy': value.useSystemProxy,
    };
}

