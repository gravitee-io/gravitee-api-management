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
package io.gravitee.management.rest.enhancer;

import io.gravitee.management.model.*;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.UserService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class ApplicationEnhancer {

    @Inject
    private ApplicationService applicationService;
    
    @Inject
    private ApiService apiService;

    @Inject
    private UserService userService;

    public Function<ApplicationEntity, ApplicationEntity> enhance(SecurityContext securityContext) {
        return application -> {
            // Add primary owner
            Collection<MemberEntity> members = applicationService.getMembers(application.getId(), null);
            Optional<MemberEntity> primaryOwnerOpt = members.stream().filter(m -> MembershipType.PRIMARY_OWNER.equals(m.getType())).findFirst();
            if (primaryOwnerOpt.isPresent()) {
                MemberEntity primaryOwner = primaryOwnerOpt.get();
                UserEntity user = userService.findByName(primaryOwner.getUsername());

                PrimaryOwnerEntity owner = new PrimaryOwnerEntity();
                owner.setUsername(user.getUsername());
                owner.setEmail(user.getEmail());
                owner.setFirstname(user.getFirstname());
                owner.setLastname(user.getLastname());
                application.setPrimaryOwner(owner);
            }
            
            // Add Members size
            application.setMembersSize(members.size());

            // Add permission for current user (if authenticated)
            if (securityContext.isUserInRole("ADMIN")) {
                application.setPermission(MembershipType.PRIMARY_OWNER);
            } else if(securityContext.getUserPrincipal() != null) {
                MemberEntity member = applicationService.getMember(
                        application.getId(), securityContext.getUserPrincipal().getName());
                if (member != null) {
                    application.setPermission(member.getType());
                }
            }
            
            // Add APIs size
            application.setApisSize(apiService.countByApplication(application.getId()));

            return application;
        };
    }
}
