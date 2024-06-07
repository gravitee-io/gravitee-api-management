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
import { ConfigurationPortal } from './configurationPortal';
import { ConfigurationPlan } from './configurationPlan';
import { ConfigurationAnalytics } from './configurationAnalytics';
import { ConfigurationAuthentication } from './configurationAuthentication';
import { ConfigurationReCaptcha } from './configurationReCaptcha';
import { ConfigurationApplication } from './configurationApplication';
import { Enabled } from './enabled';
import { ConfigurationPortalNext } from './configurationPortalNext';
import { ConfigurationScheduler } from './configurationScheduler';
import { ConfigurationDocumentation } from './configurationDocumentation';


export interface ConfigurationResponse { 
    portal?: ConfigurationPortal;
    portalNext?: ConfigurationPortalNext;
    authentication?: ConfigurationAuthentication;
    scheduler?: ConfigurationScheduler;
    documentation?: ConfigurationDocumentation;
    plan?: ConfigurationPlan;
    apiReview?: Enabled;
    analytics?: ConfigurationAnalytics;
    application?: ConfigurationApplication;
    recaptcha?: ConfigurationReCaptcha;
    alert?: Enabled;
}

