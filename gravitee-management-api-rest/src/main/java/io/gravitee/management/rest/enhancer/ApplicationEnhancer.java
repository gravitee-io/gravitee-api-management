package io.gravitee.management.rest.enhancer;

import io.gravitee.management.model.*;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.UserService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;
import java.util.function.Function;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class ApplicationEnhancer {

    @Inject
    private ApplicationService applicationService;

    @Inject
    private UserService userService;

    public Function<ApplicationEntity, ApplicationEntity> enhance(String currentUser) {
        return application -> {
            // Add primary owner
            Collection<MemberEntity> members = applicationService.getMembers(application.getId(), MembershipType.PRIMARY_OWNER);
            if (! members.isEmpty()) {
                MemberEntity primaryOwner = members.iterator().next();
                UserEntity user = userService.findByName(primaryOwner.getUser());

                PrimaryOwnerEntity owner = new PrimaryOwnerEntity();
                owner.setUsername(user.getUsername());
                owner.setEmail(user.getEmail());
                owner.setFirstname(user.getFirstname());
                owner.setLastname(user.getLastname());
                application.setPrimaryOwner(owner);
            }

            // Add permission for current user (if authenticated)
            if(currentUser != null) {
                MemberEntity member = applicationService.getMember(application.getId(), currentUser);
                if (member != null) {
                    application.setPermission(member.getType());
                } else {
                    throw new IllegalStateException("You should never go there !");
                }
            }

            return application;
        };
    }
}
