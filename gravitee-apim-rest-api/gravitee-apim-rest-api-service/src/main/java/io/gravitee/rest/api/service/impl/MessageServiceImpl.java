/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.service.impl.MessageServiceImpl.MessageEvent.MESSAGE_SENT;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.v4.api.GenericApiModel;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.Hook;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.notification.PortalHook;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MessageServiceImpl extends AbstractService implements MessageService, InitializingBean {

    private static final String API_SUBSCRIBERS = "API_SUBSCRIBERS";
    private final Logger LOGGER = LoggerFactory.getLogger(MessageServiceImpl.class);

    @Lazy
    @Autowired
    ApiRepository apiRepository;

    @Autowired
    MembershipService membershipService;

    @Lazy
    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    PortalNotificationService portalNotificationService;

    @Autowired
    UserService userService;

    @Autowired
    AuditService auditService;

    @Autowired
    EmailService emailService;

    @Autowired
    ApiTemplateService apiTemplateService;

    @Autowired
    ApplicationService applicationService;

    @Autowired
    RoleService roleService;

    @Autowired
    GroupService groupService;

    @Autowired
    HttpClientService httpClientService;

    @Autowired
    SubscriptionService subscriptionService;

    @Autowired
    private NotificationTemplateService notificationTemplateService;

    @Value("${notifiers.webhook.enabled:true}")
    private boolean httpEnabled;

    @Autowired
    private Environment environment;

    @Autowired
    private ParameterService parameterService;

    private List<String> httpWhitelist;

    @Override
    public void afterPropertiesSet() {
        int i = 0;
        httpWhitelist = new ArrayList<>();

        String whitelistUrl;

        while ((whitelistUrl = environment.getProperty("notifiers.webhook.whitelist[" + i + "]")) != null) {
            httpWhitelist.add(whitelistUrl);
            i++;
        }
    }

    @Override
    public int create(final ExecutionContext executionContext, String apiId, MessageEntity message) {
        assertMessageNotEmpty(message);
        try {
            Optional<Api> optionalApi = apiRepository.findById(apiId);
            if (!optionalApi.isPresent()) {
                throw new ApiNotFoundException(apiId);
            }
            Api api = optionalApi.get();

            int msgSize = send(executionContext, api, message, getRecipientsId(executionContext, api, message));

            auditService.createApiAuditLog(
                executionContext,
                AuditService.AuditLogData.builder()
                    .properties(Collections.emptyMap())
                    .event(MESSAGE_SENT)
                    .createdAt(new Date())
                    .oldValue(null)
                    .newValue(message)
                    .build(),
                apiId
            );
            return msgSize;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get create a message", ex);
            throw new TechnicalManagementException("An error occurs while trying to create a message", ex);
        }
    }

    @Override
    public int create(final ExecutionContext executionContext, MessageEntity message) {
        assertMessageNotEmpty(message);

        int msgSize = send(executionContext, null, message, getRecipientsId(executionContext, message));

        auditService.createAuditLog(
            executionContext,
            AuditService.AuditLogData.builder()
                .properties(Collections.emptyMap())
                .event(MESSAGE_SENT)
                .createdAt(new Date())
                .oldValue(null)
                .newValue(message)
                .build()
        );
        return msgSize;
    }

    private int send(ExecutionContext executionContext, Api api, MessageEntity message, Set<String> recipientsId) {
        switch (message.getChannel()) {
            case MAIL:
                Set<String> mails = getRecipientsEmails(executionContext, recipientsId);
                if (!mails.isEmpty()) {
                    emailService.sendAsyncEmailNotification(
                        executionContext,
                        new EmailNotificationBuilder()
                            .to(EmailService.DEFAULT_MAIL_TO)
                            .bcc(mails.toArray(new String[0]))
                            .template(EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_GENERIC_MESSAGE)
                            .param("message", message.getText())
                            .param("messageSubject", message.getTitle())
                            .build()
                    );
                }
                return mails.size();
            case PORTAL:
                Hook hook = api == null ? PortalHook.MESSAGE : ApiHook.MESSAGE;
                portalNotificationService.create(executionContext, hook, new ArrayList<>(recipientsId), getPortalParams(api, message));
                return recipientsId.size();
            case HTTP:
                if (!httpEnabled) {
                    throw new NotifierDisabledException();
                }

                String url = recipientsId.iterator().next();

                environment.getProperty("notifiers.webhook.whitelist");

                if (httpWhitelist != null && !httpWhitelist.isEmpty()) {
                    // Check the provided url is allowed.
                    if (
                        httpWhitelist
                            .stream()
                            .noneMatch(whitelistUrl ->
                                whitelistUrl.endsWith("/")
                                    ? url.startsWith(whitelistUrl)
                                    : (url.equals(whitelistUrl) || url.startsWith(whitelistUrl + '/'))
                            )
                    ) {
                        throw new MessageUrlForbiddenException();
                    }
                }

                httpClientService.request(
                    HttpMethod.POST,
                    url,
                    message.getParams(),
                    getPostMessage(executionContext, api, message, executionContext.getOrganizationId()),
                    Boolean.valueOf(message.isUseSystemProxy())
                );
                return 1;
            default:
                return 0;
        }
    }

    @Override
    public Set<String> getRecipientsId(final ExecutionContext context, MessageEntity message) {
        if (MessageChannel.HTTP.equals(message.getChannel())) {
            return Collections.singleton(message.getRecipient().getUrl());
        }
        return getRecipientsId(context, null, message);
    }

    @Override
    public Set<String> getRecipientsId(final ExecutionContext context, Api api, MessageEntity message) {
        if (message != null && MessageChannel.HTTP.equals(message.getChannel())) {
            return Collections.singleton(message.getRecipient().getUrl());
        }
        assertRecipientsNotEmpty(message);
        MessageRecipientEntity recipientEntity = message.getRecipient();
        // 2 cases are implemented :
        // - global sending (no apiId provided) + scope ENVIRONMENT
        // - api consumer (apiId provided) + scope APPLICATION
        // the first 2 cases are for admin communication, the last one for the api publisher communication.

        try {
            final Set<String> recipientIds = new HashSet<>();
            // CASE 1 : global sending
            if (api == null && RoleScope.ENVIRONMENT.name().equals(recipientEntity.getRoleScope())) {
                for (String roleName : recipientEntity.getRoleValues()) {
                    Optional<RoleEntity> optRole = roleService.findByScopeAndName(
                        RoleScope.valueOf(recipientEntity.getRoleScope()),
                        roleName,
                        context.getOrganizationId()
                    );
                    if (optRole.isPresent()) {
                        recipientIds.addAll(
                            membershipService
                                .getMembershipsByReferenceAndRole(
                                    MembershipReferenceType.ENVIRONMENT,
                                    context.getEnvironmentId(),
                                    optRole.get().getId()
                                )
                                .stream()
                                .map(MembershipEntity::getMemberId)
                                .collect(Collectors.toSet())
                        );
                    }
                }
            }
            // CASE 2 : specific api consumers
            else if (api != null && RoleScope.APPLICATION.name().equals(recipientEntity.getRoleScope())) {
                // Get apps allowed to consume the api
                List<String> applicationIds = subscriptionRepository
                    .search(
                        SubscriptionCriteria.builder()
                            .apis(Collections.singleton(api.getId()))
                            .statuses(List.of(Subscription.Status.ACCEPTED.name()))
                            .build()
                    )
                    .stream()
                    .map(Subscription::getApplication)
                    .collect(Collectors.toList());

                // Get members of the applications (direct members and group members)
                for (String roleName : recipientEntity.getRoleValues()) {
                    Optional<RoleEntity> optRole = roleService.findByScopeAndName(
                        RoleScope.APPLICATION,
                        roleName,
                        context.getOrganizationId()
                    );
                    if (optRole.isPresent()) {
                        // get all directs members
                        recipientIds.addAll(
                            membershipService
                                .getMembershipsByReferencesAndRole(
                                    MembershipReferenceType.APPLICATION,
                                    applicationIds,
                                    optRole.get().getId()
                                )
                                .stream()
                                .map(MembershipEntity::getMemberId)
                                .collect(Collectors.toSet())
                        );

                        recipientIds.addAll(this.getIndirectMemberIds(context, applicationIds, optRole.get()));
                    } else if (roleName.equals(API_SUBSCRIBERS)) {
                        Collection<SubscriptionEntity> subscriptions = subscriptionService.findByApi(context, api.getId());
                        List<String> subscribersId = subscriptions
                            .stream()
                            .map(SubscriptionEntity::getSubscribedBy)
                            .distinct()
                            .collect(Collectors.toList());

                        recipientIds.addAll(subscribersId);
                    }
                }
            }
            return recipientIds;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get recipients", ex);
            throw new TechnicalManagementException("An error occurs while trying to get recipients", ex);
        }
    }

    private Set<String> getIndirectMemberIds(ExecutionContext context, List<String> applicationIds, RoleEntity roleEntity) {
        if (applicationIds.isEmpty() || SystemRole.PRIMARY_OWNER.name().equals(roleEntity.getName())) {
            return Collections.emptySet();
        }

        // get all indirect members
        List<String> applicationsGroups = applicationService
            .findByIdsAndStatus(context, applicationIds, ApplicationStatus.ACTIVE)
            .stream()
            .filter(application -> application.getGroups() != null)
            .flatMap((ApplicationListItem applicationListItem) -> applicationListItem.getGroups().stream())
            .distinct()
            .collect(Collectors.toList());

        if (applicationsGroups.isEmpty()) {
            return Collections.emptySet();
        }

        return membershipService
            .getMembershipsByReferencesAndRole(MembershipReferenceType.GROUP, applicationsGroups, roleEntity.getId())
            .stream()
            .map(MembershipEntity::getMemberId)
            .collect(Collectors.toSet());
    }

    private Set<String> getRecipientsEmails(ExecutionContext executionContext, Set<String> recipientsId) {
        if (recipientsId.isEmpty()) {
            return Collections.emptySet();
        }

        final boolean isTrialInstance = parameterService.findAsBoolean(executionContext, Key.TRIAL_INSTANCE, ParameterReferenceType.SYSTEM);
        final Predicate<UserEntity> excludeIfTrialAndNotOptedIn = userEntity -> !isTrialInstance || userEntity.optedIn();

        return userService
            .findByIds(executionContext, new ArrayList<>(recipientsId))
            .stream()
            .filter(userEntity -> !StringUtils.isEmpty(userEntity.getEmail()))
            .filter(excludeIfTrialAndNotOptedIn)
            .map(UserEntity::getEmail)
            .collect(Collectors.toSet());
    }

    private void assertMessageNotEmpty(MessageEntity messageEntity) {
        if (messageEntity == null || (StringUtils.isEmpty(messageEntity.getTitle()) && StringUtils.isEmpty(messageEntity.getText()))) {
            throw new MessageEmptyException();
        }
    }

    private void assertRecipientsNotEmpty(MessageEntity messageEntity) {
        if (
            messageEntity == null ||
            messageEntity.getRecipient() == null ||
            messageEntity.getChannel() == null ||
            messageEntity.getRecipient().getRoleScope() == null ||
            messageEntity.getRecipient().getRoleValues() == null ||
            messageEntity.getRecipient().getRoleValues().isEmpty()
        ) {
            throw new MessageRecipientFormatException();
        }
    }

    private Map<String, Object> getPortalParams(Api api, MessageEntity message) {
        Map<String, Object> params = new HashMap<>();
        params.put("title", message.getTitle());
        params.put("message", message.getText());
        if (api != null) {
            Api paramApi = new Api();
            paramApi.setId(api.getId());
            paramApi.setName(api.getName());
            paramApi.setVersion(api.getVersion());
            params.put("api", paramApi);
        }
        return params;
    }

    private String getPostMessage(ExecutionContext executionContext, Api api, MessageEntity message, String organizationId) {
        if (message.getText() == null || api == null) {
            return message.getText();
        }

        GenericApiModel genericApiModel = apiTemplateService.findByIdForTemplates(executionContext, api.getId());
        Map<String, Object> model = new HashMap<>();
        model.put("api", genericApiModel);

        return this.notificationTemplateService.resolveInlineTemplateWithParam(
            organizationId,
            new Date().toString(),
            message.getText(),
            model
        );
    }

    public enum MessageEvent implements Audit.AuditEvent {
        MESSAGE_SENT,
    }
}
