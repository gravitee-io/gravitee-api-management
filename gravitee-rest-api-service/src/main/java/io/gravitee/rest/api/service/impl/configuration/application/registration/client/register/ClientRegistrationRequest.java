/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
package io.gravitee.rest.api.service.impl.configuration.application.registration.client.register;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientRegistrationRequest {

    //See https://tools.ietf.org/html/rfc6749#section-3.3
    public final static String SCOPE_DELIMITER = " ";

    /*******************************************************************************
     * Metadata in same order than the openid specification
     * https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata
     ********************************************************************************/

    @JsonProperty("redirect_uris")
    private List<String> redirectUris;

    @JsonProperty("response_types")
    private List<String> responseTypes;

    @JsonProperty("grant_types")
    private List<String>grantTypes;

    @JsonProperty("application_type")
    private String applicationType;

    @JsonProperty("contacts")
    private List<String> contacts;

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("logo_uri")
    private String logoUri;

    @JsonProperty("client_uri")
    private String clientUri;

    @JsonProperty("policy_uri")
    private String policyUri;

    @JsonProperty("tos_uri")
    private String tosUri;

    @JsonProperty("jwks_uri")
    private String jwksUri;

    @JsonProperty("sector_identifier_uri")
    private String sectorIdentifierUri;

    @JsonProperty("subject_type")
    private String subjectType;

    @JsonProperty("id_token_signed_response_alg")
    private String idTokenSignedResponseAlg;

    @JsonProperty("id_token_encrypted_response_alg")
    private String idTokenEncryptedResponseAlg;

    @JsonProperty("id_token_encrypted_response_enc")
    private String idTokenEncryptedResponseEnc;

    @JsonProperty("userinfo_signed_response_alg")
    private String userinfoSignedResponseAlg;

    @JsonProperty("userinfo_encrypted_response_alg")
    private String userinfoEncryptedResponseAlg;

    @JsonProperty("userinfo_encrypted_response_enc")
    private String userinfoEncryptedResponseEnc;

    @JsonProperty("request_object_signing_alg")
    private String requestObjectSigningAlg;

    @JsonProperty("request_object_encryption_alg")
    private String requestObjectEncryptionAlg;

    @JsonProperty("request_object_encryption_enc")
    private String requestObjectEncryptionEnc;

    @JsonProperty("token_endpoint_auth_method")
    private String tokenEndpointAuthMethod;

    @JsonProperty("token_endpoint_auth_signing_alg")
    private String tokenEndpointAuthSigningAlg;

    @JsonProperty("default_max_age")
    private Integer defaultMaxAge;

    @JsonProperty("require_auth_time")
    private Boolean requireAuthTime;

    @JsonProperty("default_acr_values")
    private List<String> defaultACRvalues;

    @JsonProperty("initiate_login_uri")
    private String initiateLoginUri;

    @JsonProperty("request_uris")
    private List<String> requestUris;


    /*******************************************************************************
     * Oauth2 metadata in addition to RFC specification
     * https://tools.ietf.org/html/rfc7591#section-2
     * https://tools.ietf.org/html/rfc7591#section-3.1.1
     ********************************************************************************/

    //https://tools.ietf.org/html/rfc7591#section-4.1.2 : scope is String space delimited
    @JsonProperty("scope")
    private String scope;

    @JsonProperty("software_id")
    private String softwareId; //Should be UUID

    @JsonProperty("software_version")
    private String softwareVersion;

    @JsonProperty("software_statement")
    private String softwareStatement; //Should be JWT

    // GETTER AND SETTERS //

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public List<String> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(List<String> responseTypes) {
        this.responseTypes = responseTypes;
    }

    public List<String> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(List<String> grantTypes) {
        this.grantTypes = grantTypes;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public List<String> getContacts() {
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getLogoUri() {
        return logoUri;
    }

    public void setLogoUri(String logoUri) {
        this.logoUri = logoUri;
    }

    public String getClientUri() {
        return clientUri;
    }

    public void setClientUri(String clientUri) {
        this.clientUri = clientUri;
    }

    public String getPolicyUri() {
        return policyUri;
    }

    public void setPolicyUri(String policyUri) {
        this.policyUri = policyUri;
    }

    public String getTosUri() {
        return tosUri;
    }

    public void setTosUri(String tosUri) {
        this.tosUri = tosUri;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getSectorIdentifierUri() {
        return sectorIdentifierUri;
    }

    public void setSectorIdentifierUri(String sectorIdentifierUri) {
        this.sectorIdentifierUri = sectorIdentifierUri;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getIdTokenSignedResponseAlg() {
        return idTokenSignedResponseAlg;
    }

    public void setIdTokenSignedResponseAlg(String idTokenSignedResponseAlg) {
        this.idTokenSignedResponseAlg = idTokenSignedResponseAlg;
    }

    public String getIdTokenEncryptedResponseAlg() {
        return idTokenEncryptedResponseAlg;
    }

    public void setIdTokenEncryptedResponseAlg(String idTokenEncryptedResponseAlg) {
        this.idTokenEncryptedResponseAlg = idTokenEncryptedResponseAlg;
    }

    public String getIdTokenEncryptedResponseEnc() {
        return idTokenEncryptedResponseEnc;
    }

    public void setIdTokenEncryptedResponseEnc(String idTokenEncryptedResponseEnc) {
        this.idTokenEncryptedResponseEnc = idTokenEncryptedResponseEnc;
    }

    public String getUserinfoSignedResponseAlg() {
        return userinfoSignedResponseAlg;
    }

    public void setUserinfoSignedResponseAlg(String userinfoSignedResponseAlg) {
        this.userinfoSignedResponseAlg = userinfoSignedResponseAlg;
    }

    public String getUserinfoEncryptedResponseAlg() {
        return userinfoEncryptedResponseAlg;
    }

    public void setUserinfoEncryptedResponseAlg(String userinfoEncryptedResponseAlg) {
        this.userinfoEncryptedResponseAlg = userinfoEncryptedResponseAlg;
    }

    public String getUserinfoEncryptedResponseEnc() {
        return userinfoEncryptedResponseEnc;
    }

    public void setUserinfoEncryptedResponseEnc(String userinfoEncryptedResponseEnc) {
        this.userinfoEncryptedResponseEnc = userinfoEncryptedResponseEnc;
    }

    public String getRequestObjectSigningAlg() {
        return requestObjectSigningAlg;
    }

    public void setRequestObjectSigningAlg(String requestObjectSigningAlg) {
        this.requestObjectSigningAlg = requestObjectSigningAlg;
    }

    public String getRequestObjectEncryptionAlg() {
        return requestObjectEncryptionAlg;
    }

    public void setRequestObjectEncryptionAlg(String requestObjectEncryptionAlg) {
        this.requestObjectEncryptionAlg = requestObjectEncryptionAlg;
    }

    public String getRequestObjectEncryptionEnc() {
        return requestObjectEncryptionEnc;
    }

    public void setRequestObjectEncryptionEnc(String requestObjectEncryptionEnc) {
        this.requestObjectEncryptionEnc = requestObjectEncryptionEnc;
    }

    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    public String getTokenEndpointAuthSigningAlg() {
        return tokenEndpointAuthSigningAlg;
    }

    public void setTokenEndpointAuthSigningAlg(String tokenEndpointAuthSigningAlg) {
        this.tokenEndpointAuthSigningAlg = tokenEndpointAuthSigningAlg;
    }

    public Integer getDefaultMaxAge() {
        return defaultMaxAge;
    }

    public void setDefaultMaxAge(Integer defaultMaxAge) {
        this.defaultMaxAge = defaultMaxAge;
    }

    public Boolean getRequireAuthTime() {
        return requireAuthTime;
    }

    public void setRequireAuthTime(Boolean requireAuthTime) {
        this.requireAuthTime = requireAuthTime;
    }

    public List<String> getDefaultACRvalues() {
        return defaultACRvalues;
    }

    public void setDefaultACRvalues(List<String> defaultACRvalues) {
        this.defaultACRvalues = defaultACRvalues;
    }

    public String getInitiateLoginUri() {
        return initiateLoginUri;
    }

    public void setInitiateLoginUri(String initiateLoginUri) {
        this.initiateLoginUri = initiateLoginUri;
    }

    public List<String> getRequestUris() {
        return requestUris;
    }

    public void setRequestUris(List<String> requestUris) {
        this.requestUris = requestUris;
    }

    public List<String> getScope() {
        if (this.scope == null) return null; //Keep null to avoid patch...
        return Arrays.asList(scope.split(SCOPE_DELIMITER));
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getSoftwareId() {
        return softwareId;
    }

    public void setSoftwareId(String softwareId) {
        this.softwareId = softwareId;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public String getSoftwareStatement() {
        return softwareStatement;
    }

    public void setSoftwareStatement(String softwareStatement) {
        this.softwareStatement = softwareStatement;
    }

    @Override
    public String toString() {
        return "ClientRegistrationRequest{clientName='" + clientName + "\'}";
    }
}
