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
package io.gravitee.rest.api.idp.api.authentication;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserDetails extends User implements org.springframework.security.core.userdetails.UserDetails {

    private String id;
    private String email;
    private String firstname;
    private String lastname;
    private String source;
    private String sourceId;
    private boolean isPrimaryOwner;
    private List<UserDetailRole> roles;
    private Map<String, Set<String>> groupsByEnvironment;
    private String username;
    private byte[] picture;
    private boolean firstLogin;
    private boolean displayNewsletterSubscription;
    private Map<String, Object> customFields;
    /**
     * The user creation date
     */
    @JsonProperty("created_at")
    private Date createdAt;
    /**
     * The user creation date
     */
    @JsonProperty("updated_at")
    private Date updatedAt;
    /**
     * The user last connection date
     */
    @JsonProperty("last_connection_at")
    private Date lastConnectionAt;

    public UserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.username = username;
    }

    public UserDetails(String username, String password, String email, Collection<? extends GrantedAuthority> authorities) {
        this(username,password,authorities);
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return username;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return super.getPassword();
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled();
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return super.isAccountNonExpired();
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return super.isAccountNonLocked();
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return super.isCredentialsNonExpired();
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getDisplayName() {
        String displayName;

        if ((firstname != null && !firstname.isEmpty()) || (lastname != null && !lastname.isEmpty())) {
            if (firstname != null && !firstname.isEmpty()) {
                displayName = firstname + ((lastname != null && !lastname.isEmpty()) ? ' ' + lastname : "");
            } else {
                displayName = lastname;
            }
        } else {
            if (email != null && !email.isEmpty() && !"memory".equals(source)){
                displayName = email;
            } else {
                displayName = sourceId;
            }
        }

        return displayName;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public List<UserDetailRole> getRoles() {
        return roles;
    }

    public void setRoles(List<UserDetailRole> roles) {
        this.roles = roles;
    }

    public Map<String, Set<String>> getGroupsByEnvironment() {
        return groupsByEnvironment;
    }

    public void setGroupsByEnvironment(Map<String, Set<String>> groupsByEnvironment) {
        this.groupsByEnvironment = groupsByEnvironment;
    }

    public boolean isFirstLogin() {
        return firstLogin;
    }

    public void setFirstLogin(boolean firstLogin) {
        this.firstLogin = firstLogin;
    }

    public void setDisplayNewsletterSubscription(boolean displayNewsletterSubscription) {
        this.displayNewsletterSubscription = displayNewsletterSubscription;
    }

    public boolean isDisplayNewsletterSubscription() {
        return displayNewsletterSubscription;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastConnectionAt() {
        return lastConnectionAt;
    }

    public void setLastConnectionAt(Date lastConnectionAt) {
        this.lastConnectionAt = lastConnectionAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", email='" + email + '\'' +
                ", firstname='" + firstname + '\'' +
                ", lastname='" + lastname + '\'' +
                ", source='" + lastname + '\'' +
                ", external_reference='" + lastname + '\'' +
                "}";
    }

    public byte[] getPicture() {
        return picture;
    }

    public void setPicture(byte[] picture) {
        this.picture = picture;
    }

    public void setPassword(String password) {
        throw new UnsupportedOperationException();
    }

    public boolean isPrimaryOwner() {
        return isPrimaryOwner;
    }

    public void setPrimaryOwner(boolean primaryOwner) {
        isPrimaryOwner = primaryOwner;
    }
}
