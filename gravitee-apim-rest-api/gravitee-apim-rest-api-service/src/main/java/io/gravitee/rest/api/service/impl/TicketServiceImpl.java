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

import static io.gravitee.rest.api.service.builder.EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_SUPPORT_TICKET;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.TicketRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.TicketCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.repository.management.model.Ticket;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.TicketQuery;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiModel;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import io.gravitee.rest.api.service.notification.PortalHook;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import jakarta.inject.Inject;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class TicketServiceImpl extends AbstractService implements TicketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketServiceImpl.class);

    private static final String UNKNOWN_REFERENCE = "Unknown";

    @Inject
    private UserService userService;

    @Inject
    private MetadataService metadataService;

    @Inject
    private ApiSearchService apiSearchService;

    @Inject
    private ApiTemplateService apiTemplateService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private EmailService emailService;

    @Inject
    private ParameterService parameterService;

    @Inject
    private NotifierService notifierService;

    @Lazy
    @Inject
    private TicketRepository ticketRepository;

    private static final PolicyFactory HTML_SANITIZER = new HtmlPolicyBuilder().allowElements("br").toFactory();

    private boolean isEnabled(ExecutionContext executionContext, String referenceId, ParameterReferenceType referenceType) {
        if (referenceType == ParameterReferenceType.ENVIRONMENT) {
            return parameterService.findAsBoolean(executionContext, Key.PORTAL_SUPPORT_ENABLED, referenceId, referenceType);
        }
        return parameterService.findAsBoolean(executionContext, Key.CONSOLE_SUPPORT_ENABLED, referenceId, referenceType);
    }

    @Override
    public TicketEntity create(
        final ExecutionContext executionContext,
        final String userId,
        final NewTicketEntity ticketEntity,
        final String referenceId,
        final ParameterReferenceType referenceType
    ) {
        try {
            if (!isEnabled(executionContext, referenceId, referenceType)) {
                throw new SupportUnavailableException();
            }
            LOGGER.info("Creating a support ticket: {}", ticketEntity);

            final Map<String, Object> parameters = new HashMap<>();

            final UserEntity user = userService.findById(executionContext, userId);
            if (user.getEmail() == null) {
                throw new EmailRequiredException(userId);
            }
            parameters.put("user", user);

            final String emailTo;
            final GenericApiModel api;
            final ApplicationEntity applicationEntity;
            if (ticketEntity.getApi() == null || ticketEntity.getApi().isEmpty()) {
                api = null;
                final MetadataEntity emailMetadata = metadataService.findByKeyAndReferenceTypeAndReferenceId(
                    MetadataService.METADATA_EMAIL_SUPPORT_KEY,
                    MetadataReferenceType.ENVIRONMENT,
                    executionContext.getEnvironmentId()
                );
                if (emailMetadata == null) {
                    throw new IllegalStateException("The support email metadata has not been found");
                }
                emailTo = emailMetadata.getValue();
            } else {
                api = apiTemplateService.findByIdForTemplates(executionContext, ticketEntity.getApi(), true);
                final String apiMetadataEmailSupport = api.getMetadata().get(MetadataService.METADATA_EMAIL_SUPPORT_KEY);
                if (apiMetadataEmailSupport == null) {
                    throw new IllegalStateException("The support email API metadata has not been found");
                }
                emailTo = apiMetadataEmailSupport;
                parameters.put("api", api);
            }

            if (MetadataService.DEFAULT_METADATA_EMAIL_SUPPORT.equals(emailTo)) {
                throw new IllegalStateException("The support email API metadata has not been changed");
            }

            if (ticketEntity.getApplication() != null && !ticketEntity.getApplication().isEmpty()) {
                applicationEntity = applicationService.findById(executionContext, ticketEntity.getApplication());
                parameters.put("application", applicationEntity);
            } else {
                applicationEntity = null;
            }

            final String content = HTML_SANITIZER.sanitize(ticketEntity.getContent().replaceAll("(\r\n|\n)", "<br />"));
            parameters.put("content", content);
            parameters.put("ticketSubject", ticketEntity.getSubject());
            final String fromName = user.getFirstname() == null ? user.getEmail() : user.getFirstname() + ' ' + user.getLastname();
            emailService.sendEmailNotification(
                executionContext,
                new EmailNotificationBuilder()
                    .replyTo(user.getEmail())
                    .fromName(fromName)
                    .to(emailTo)
                    .copyToSender(ticketEntity.isCopyToSender())
                    .template(TEMPLATES_FOR_ACTION_SUPPORT_TICKET)
                    .params(parameters)
                    .build()
            );
            sendUserNotification(executionContext, user, api, applicationEntity);

            Ticket ticket = convert(ticketEntity);
            ticket.setId(UuidString.generateRandom());
            ticket.setCreatedAt(new Date());
            ticket.setFromUser(userId);

            Ticket createdTicket = ticketRepository.create(ticket);

            return convert(createdTicket);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create a ticket {}", ticketEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying to create a ticket " + ticketEntity, ex);
        }
    }

    @Override
    public Page<TicketEntity> search(final ExecutionContext executionContext, TicketQuery query, Sortable sortable, Pageable pageable) {
        try {
            LOGGER.debug("search tickets");

            TicketCriteria criteria = queryToCriteriaBuilder(query).build();

            var tickets = ticketRepository
                .search(
                    criteria,
                    buildSortable(sortable),
                    new PageableBuilder().pageNumber(pageable.getPageNumber() - 1).pageSize(pageable.getPageSize()).build()
                )
                .map(ticket -> this.getApiNameAndApplicationName(executionContext, ticket))
                .map(this::convert);

            LOGGER.debug("search tickets - Done with {} elements", tickets.getContent().size());

            return new Page<>(
                tickets.getContent(),
                tickets.getPageNumber() + 1,
                (int) tickets.getPageElements(),
                tickets.getTotalElements()
            );
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to search tickets", ex);
            throw new TechnicalManagementException("An error occurs while trying to search tickets", ex);
        }
    }

    @Override
    public TicketEntity findById(final ExecutionContext executionContext, String ticketId) {
        try {
            return ticketRepository
                .findById(ticketId)
                .filter(t -> t.getFromUser().equalsIgnoreCase(getAuthenticatedUsername()))
                .map(ticket -> this.getApiNameAndApplicationName(executionContext, ticket))
                .map(this::convert)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to search ticket {}", ticketId, ex);
            throw new TechnicalManagementException("An error occurs while trying to search ticket " + ticketId, ex);
        }
    }

    private void sendUserNotification(
        ExecutionContext executionContext,
        final UserEntity user,
        final GenericApiModel genericApiModel,
        final ApplicationEntity application
    ) {
        notifierService.trigger(
            executionContext,
            PortalHook.NEW_SUPPORT_TICKET,
            new NotificationParamsBuilder().user(user).api(genericApiModel).application(application).build()
        );

        if (genericApiModel != null) {
            notifierService.trigger(
                executionContext,
                ApiHook.NEW_SUPPORT_TICKET,
                genericApiModel.getId(),
                new NotificationParamsBuilder().user(user).api(genericApiModel).application(application).build()
            );
        }

        if (application != null) {
            notifierService.trigger(
                executionContext,
                ApplicationHook.NEW_SUPPORT_TICKET,
                application.getId(),
                new NotificationParamsBuilder().user(user).api(genericApiModel).application(application).build()
            );
        }
    }

    private TicketEntity convert(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        TicketEntity ticketEntity = new TicketEntity();
        ticketEntity.setId(ticket.getId());
        ticketEntity.setApi(ticket.getApi());
        ticketEntity.setApplication(ticket.getApplication());
        ticketEntity.setSubject(ticket.getSubject());
        ticketEntity.setContent(ticket.getContent());
        ticketEntity.setCreatedAt(ticket.getCreatedAt());
        ticketEntity.setFromUser(ticket.getFromUser());

        return ticketEntity;
    }

    private Ticket convert(NewTicketEntity ticketEntity) {
        if (ticketEntity == null) {
            return null;
        }

        Ticket ticket = new Ticket();
        ticket.setApi(ticketEntity.getApi());
        ticket.setApplication(ticketEntity.getApplication());
        ticket.setSubject(ticketEntity.getSubject());
        ticket.setContent(ticketEntity.getContent());

        return ticket;
    }

    private Ticket getApiNameAndApplicationName(final ExecutionContext executionContext, Ticket ticket) {
        // Retrieve application name
        if (StringUtils.isNotEmpty(ticket.getApplication())) {
            try {
                ApplicationEntity application = applicationService.findById(executionContext, ticket.getApplication());
                ticket.setApplication(application.getName());
            } catch (ApplicationNotFoundException e) {
                ticket.setApplication(UNKNOWN_REFERENCE);
            }
        }

        // Retrieve API name
        if (StringUtils.isNotEmpty(ticket.getApi())) {
            try {
                GenericApiEntity api = apiSearchService.findGenericById(executionContext, ticket.getApi());
                ticket.setApi(api.getName());
            } catch (ApiNotFoundException e) {
                ticket.setApi(UNKNOWN_REFERENCE);
            }
        }

        return ticket;
    }

    private TicketCriteria.Builder queryToCriteriaBuilder(TicketQuery query) {
        final TicketCriteria.Builder builder = new TicketCriteria.Builder().fromUser(query.getFromUser());

        if (!isBlank(query.getApi())) {
            builder.api(query.getApi());
        }
        if (!isBlank(query.getApplication())) {
            builder.application(query.getApplication());
        }

        return builder;
    }

    private io.gravitee.repository.management.api.search.Sortable buildSortable(Sortable sortable) {
        if (sortable == null) {
            return null;
        }
        return new SortableBuilder().field(sortable.getField()).order(sortable.isAscOrder() ? Order.ASC : Order.DESC).build();
    }
}
