/**
 * Gravitee.io Portal Rest API
 * API dedicated to the devportal part of Gravitee
 *
 * Contact: contact@graviteesource.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
import { MessageLogContent } from './messageLogContent';


export interface MessageLog { 
    /**
     * The id of the request.
     */
    requestId?: string;
    /**
     * The id of the request.
     */
    apiId?: string;
    /**
     * The date (as timestamp) of the log.
     */
    timestamp?: Date;
    /**
     * The client identifier of the request.
     */
    clientIdentifier?: string;
    /**
     * The correlation id.
     */
    correlationId?: string;
    /**
     * The parent correlation id.
     */
    parentCorrelationId?: string;
    /**
     * The operation.
     */
    operation?: string;
    entrypoint?: MessageLogContent;
    endpoint?: MessageLogContent;
}

