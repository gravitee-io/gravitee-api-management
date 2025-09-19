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

import static io.gravitee.repository.management.model.Audit.AuditProperties.NOTIFICATION_TEMPLATE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.utils.UUID;
import io.gravitee.node.api.Node;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.CommandTags;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.NotificationTemplateRepository;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Command;
import io.gravitee.repository.management.model.MessageRecipient;
import io.gravitee.repository.management.model.NotificationTemplate;
import io.gravitee.repository.management.model.NotificationTemplateReferenceType;
import io.gravitee.repository.management.model.NotificationTemplateType;
import io.gravitee.rest.api.model.alert.ApplicationAlertEventType;
import io.gravitee.rest.api.model.notification.NotificationTemplateEntity;
import io.gravitee.rest.api.model.notification.NotificationTemplateEvent;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder.EmailTemplate;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.InvalidTemplateException;
import io.gravitee.rest.api.service.exceptions.NotificationTemplateNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.TemplateProcessingException;
import io.gravitee.rest.api.service.notification.ActionHook;
import io.gravitee.rest.api.service.notification.AlertHook;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.notification.Hook;
import io.gravitee.rest.api.service.notification.HookScope;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.notification.PortalHook;
import io.gravitee.rest.api.service.v4.mapper.NotificationTemplateMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.yaml.snakeyaml.Yaml;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class NotificationTemplateServiceImpl extends AbstractService implements NotificationTemplateService, InitializingBean {

    private static final String HTML_TEMPLATE_EXTENSION = "html";
    public static final String TEMPLATES_TO_INCLUDE_SCOPE = "TEMPLATES_TO_INCLUDE";

    @Value("${templates.path:${gravitee.home}/templates}")
    private String templatesPath;

    @Lazy
    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private EventManager eventManager;

    @Lazy
    @Autowired
    private CommandRepository commandRepository;

    @Autowired
    private Node node;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationTemplateMapper notificationTemplateMapper;

    private final Map<String, Configuration> freemarkerConfigurationByOrg = new HashMap<>();
    private final Map<String, StringTemplateLoader> stringTemplateLoaderMapByOrg = new HashMap<>();
    private Map<String, NotificationTemplateEntity> fromFilesNotificationTemplateEntities = new HashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        List<Hook> allHooks = new ArrayList<>();
        Collections.addAll(allHooks, PortalHook.values());
        Collections.addAll(allHooks, ApiHook.values());
        Collections.addAll(allHooks, ApplicationHook.values());
        Collections.addAll(allHooks, ActionHook.values());
        Collections.addAll(allHooks, AlertHook.values());

        this.fromFilesNotificationTemplateEntities = allHooks
            .stream()
            .map(this::loadNotificationTemplatesFromHook)
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(NotificationTemplateEntity::getTemplateName, Function.identity()));

        // Must add HTML template files that are not linked to a Hook. They may be useful for inclusion in others templates (e.g header.html)
        this.fromFilesNotificationTemplateEntities.putAll(this.addExtraHtmlTemplate());
    }

    private Map<String, NotificationTemplateEntity> addExtraHtmlTemplate() {
        try (Stream<Path> streamPath = Files.list(new File(templatesPath).toPath())) {
            return streamPath
                .filter(path -> path.getFileName().toString().endsWith(HTML_TEMPLATE_EXTENSION))
                .filter(path -> EmailTemplate.fromHtmlTemplateName(path.getFileName().toString()) == null)
                .map(this::loadEmailNotificationTemplateFromFile)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(NotificationTemplateEntity::getTemplateName, Function.identity()));
        } catch (IOException e) {
            log.warn("Problem while getting freemarker templates from files", e);
            return Collections.emptyMap();
        }
    }

    @Override
    public String resolveInlineTemplateWithParam(
        String organizationId,
        String name,
        Reader inlineTemplateReader,
        Object params,
        boolean ignoreTplException
    ) {
        Configuration orgFreemarkerConfiguration = getCurrentOrgConfiguration(organizationId);
        try {
            Template template = new Template(name, inlineTemplateReader, orgFreemarkerConfiguration);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
        } catch (freemarker.core.ParseException e) {
            if (ignoreTplException) {
                log.warn("Error while parsing the inline reader:\n{}", e.getMessage());
                return "";
            } else {
                throw new InvalidTemplateException(e.getMessage());
            }
        } catch (TemplateException e) {
            if (ignoreTplException) {
                log.warn("Error while processing the inline reader:\n{}", e.getMessage());
                return "";
            } else {
                throw new TemplateProcessingException(e);
            }
        } catch (IOException e) {
            log.warn("Error while creating template from reader:\n{}", e.getMessage());
            return "";
        }
    }

    @Override
    public String resolveTemplateWithParam(String organizationId, String templateName, Object params) {
        Configuration orgFreemarkerConfiguration = getCurrentOrgConfiguration(organizationId);
        try {
            Template template = orgFreemarkerConfiguration.getTemplate(templateName);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, params);
        } catch (IOException e) {
            log.warn("Error while getting template {}:\n{}", templateName, e.getMessage());
            return "";
        } catch (TemplateException e) {
            log.warn("Error while processing the template {}:\n{}", templateName, e.getMessage());
            return "";
        }
    }

    @NotNull
    private Configuration getCurrentOrgConfiguration(String currentOrganization) {
        Configuration orgFreemarkerConfiguration = freemarkerConfigurationByOrg.get(currentOrganization);
        if (orgFreemarkerConfiguration == null) {
            orgFreemarkerConfiguration = this.initCurrentOrgFreemarkerConfiguration(currentOrganization);
            freemarkerConfigurationByOrg.put(currentOrganization, orgFreemarkerConfiguration);
        }
        return orgFreemarkerConfiguration;
    }

    private Configuration initCurrentOrgFreemarkerConfiguration(String currentOrganization) {
        // Init the configuration
        final freemarker.template.Configuration configuration = new freemarker.template.Configuration(
            freemarker.template.Configuration.VERSION_2_3_22
        );

        configuration.setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER);

        // Get template loaders
        MultiTemplateLoader multiLoader = createMultiTemplateLoaderForOrganization(currentOrganization);
        configuration.setTemplateLoader(multiLoader);

        return configuration;
    }

    @NotNull
    private MultiTemplateLoader createMultiTemplateLoaderForOrganization(String currentOrganization) {
        List<TemplateLoader> loaders = new ArrayList<>();

        // First we add a loader for templates from Database since their priority is higher
        StringTemplateLoader orgCustomizedTemplatesLoader = new StringTemplateLoader();
        this.findAllInDatabase(currentOrganization, NotificationTemplateReferenceType.ORGANIZATION).forEach(template -> {
            if (template.isEnabled()) {
                if (template.getTitle() != null && !template.getTitle().isEmpty()) {
                    orgCustomizedTemplatesLoader.putTemplate(template.getTitleTemplateName(), template.getTitle());
                }
                orgCustomizedTemplatesLoader.putTemplate(template.getContentTemplateName(), template.getContent());
            }
        });
        loaders.add(orgCustomizedTemplatesLoader);

        // Then we also add this loader to a map, so we can access them easily to update or remove a template
        stringTemplateLoaderMapByOrg.put(currentOrganization, orgCustomizedTemplatesLoader);

        // Then we add a loader for portal template in files.
        StringTemplateLoader fileNotificationTemplatesLoader = new StringTemplateLoader();
        this.fromFilesNotificationTemplateEntities.values()
            .stream()
            .forEach(template -> {
                if (template.getTitle() != null && !template.getTitle().isEmpty()) {
                    fileNotificationTemplatesLoader.putTemplate(template.getTitleTemplateName(), template.getTitle());
                }
                fileNotificationTemplatesLoader.putTemplate(template.getContentTemplateName(), template.getContent());
            });
        loaders.add(fileNotificationTemplatesLoader);

        final MultiTemplateLoader multiTemplateLoader = new MultiTemplateLoader(loaders.toArray(new TemplateLoader[loaders.size()]));
        multiTemplateLoader.setSticky(false);
        return multiTemplateLoader;
    }

    @Override
    public Set<NotificationTemplateEntity> findAll(String organizationId) {
        // Load all template from database
        final Set<NotificationTemplateEntity> allFromDatabase = this.findAllInDatabase(
            organizationId,
            NotificationTemplateReferenceType.ORGANIZATION
        );

        Set<NotificationTemplateEntity> all = new HashSet<>();
        all.addAll(allFromDatabase);
        // Will not override existing templates loaded from database
        all.addAll(fromFilesNotificationTemplateEntities.values());

        return all;
    }

    private Set<NotificationTemplateEntity> findAllInDatabase(String referenceId, NotificationTemplateReferenceType referenceType) {
        try {
            return notificationTemplateRepository
                .findAllByReferenceIdAndReferenceType(referenceId, referenceType)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to retrieve notificationTemplates", ex);
            throw new TechnicalManagementException("An error occurs while trying to retrieve notificationTemplates", ex);
        }
    }

    private List<NotificationTemplateEntity> loadNotificationTemplatesFromHook(Hook hook) {
        List<NotificationTemplateEntity> result = new ArrayList<>();

        if (hook.getScope().hasPortalNotification()) {
            final NotificationTemplateEntity portalNotificationTemplateEntity = this.loadPortalNotificationTemplateFromHook(hook);
            if (portalNotificationTemplateEntity != null) {
                result.add(portalNotificationTemplateEntity);
            }
        }

        final NotificationTemplateEntity emailNotificationTemplateEntity = this.loadEmailNotificationTemplateFromHook(hook);
        if (emailNotificationTemplateEntity != null) {
            result.add(emailNotificationTemplateEntity);
        }

        return result;
    }

    private NotificationTemplateEntity loadPortalNotificationTemplateFromHook(Hook hook) {
        final String RELATIVE_TPL_PATH = "notifications/portal/";
        String templateName = hook.getTemplate() + "." + io.gravitee.rest.api.model.notification.NotificationTemplateType.PORTAL.name();
        File portalTemplateFile = new File(templatesPath + "/" + RELATIVE_TPL_PATH + hook.getTemplate() + ".yml");

        try {
            Yaml yaml = new Yaml();
            Map<String, String> load = yaml.load(new FileInputStream(portalTemplateFile));

            return new NotificationTemplateEntity(
                hook.name(),
                hook.getScope().name(),
                templateName,
                hook.getLabel(),
                hook.getDescription(),
                load.get("title"),
                load.get("message"),
                io.gravitee.rest.api.model.notification.NotificationTemplateType.PORTAL
            );
        } catch (IOException e) {
            log.warn("Problem while getting freemarker template {} from file : {}", hook.getTemplate(), e.getMessage());
            return null;
        }
    }

    private NotificationTemplateEntity loadEmailNotificationTemplateFromHook(Hook hook) {
        String templateName = hook.getTemplate() + "." + io.gravitee.rest.api.model.notification.NotificationTemplateType.EMAIL.name();

        final EmailTemplate emailTemplate = EmailTemplate.fromHook(hook);
        if (emailTemplate != null) {
            File emailTemplateFile = new File(templatesPath + "/" + emailTemplate.getHtmlTemplate());

            try {
                String title = (emailTemplate.getSubject() == null ? "unused title" : emailTemplate.getSubject());
                String content = new String(Files.readAllBytes(emailTemplateFile.toPath()));

                return new NotificationTemplateEntity(
                    hook.name(),
                    hook.getScope().name(),
                    templateName,
                    hook.getLabel(),
                    hook.getDescription(),
                    title,
                    content,
                    io.gravitee.rest.api.model.notification.NotificationTemplateType.EMAIL
                );
            } catch (IOException e) {
                log.warn("Problem while getting freemarker template {} from file : {}", hook.getTemplate(), e.getMessage());
            }
        }
        return null;
    }

    private NotificationTemplateEntity loadEmailNotificationTemplateFromFile(Path fileTemplateName) {
        String templateName = fileTemplateName.getFileName().toString();

        try {
            File templateFile = fileTemplateName.toFile();
            String content = new String(Files.readAllBytes(templateFile.toPath()));

            return new NotificationTemplateEntity(
                "",
                TEMPLATES_TO_INCLUDE_SCOPE,
                templateName,
                templateName,
                null,
                null,
                content,
                io.gravitee.rest.api.model.notification.NotificationTemplateType.EMAIL
            );
        } catch (IOException e) {
            log.warn("Problem while getting freemarker template {} from file : {}", templateName, e.getMessage());
        }

        return null;
    }

    @Override
    public Set<NotificationTemplateEntity> findByType(
        String organizationId,
        io.gravitee.rest.api.model.notification.NotificationTemplateType type
    ) {
        try {
            return notificationTemplateRepository
                .findByTypeAndReferenceIdAndReferenceType(
                    NotificationTemplateType.valueOf(type.name()),
                    organizationId,
                    NotificationTemplateReferenceType.ORGANIZATION
                )
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to retrieve notificationTemplates by type", ex);
            throw new TechnicalManagementException("An error occurs while trying to retrieve notificationTemplates by type", ex);
        }
    }

    @Override
    public Set<NotificationTemplateEntity> findByHookAndScope(String organizationId, String hook, String scope) {
        return this.findAll(organizationId)
            .stream()
            .filter(
                notificationTemplateEntity ->
                    notificationTemplateEntity.getHook().equalsIgnoreCase(hook) &&
                    notificationTemplateEntity.getScope().equalsIgnoreCase(scope)
            )
            .collect(Collectors.toSet());
    }

    @Override
    public NotificationTemplateEntity create(ExecutionContext executionContext, NotificationTemplateEntity newNotificationTemplate) {
        try {
            log.debug("Create notificationTemplate {}", newNotificationTemplate);
            newNotificationTemplate.setId(UuidString.generateRandom());
            if (newNotificationTemplate.getCreatedAt() == null) {
                newNotificationTemplate.setCreatedAt(new Date());
            }

            NotificationTemplate createdNotificationTemplate = notificationTemplateRepository.create(
                convert(newNotificationTemplate, executionContext.getOrganizationId())
            );

            this.createAuditLog(
                executionContext,
                NotificationTemplate.AuditEvent.NOTIFICATION_TEMPLATE_CREATED,
                newNotificationTemplate.getCreatedAt(),
                null,
                createdNotificationTemplate
            );

            final NotificationTemplateEntity createdNotificationTemplateEntity = convert(createdNotificationTemplate);

            // Update template in loader cache
            updateFreemarkerCache(createdNotificationTemplateEntity, executionContext.getOrganizationId());

            return createdNotificationTemplateEntity;
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to create or update notificationTemplate {}", newNotificationTemplate, ex);
            throw new TechnicalManagementException("An error occurs while trying to create or update " + newNotificationTemplate, ex);
        }
    }

    @Override
    public NotificationTemplateEntity update(ExecutionContext executionContext, NotificationTemplateEntity updatingNotificationTemplate) {
        try {
            log.debug("Update notificationTemplate {}", updatingNotificationTemplate);
            if (updatingNotificationTemplate.getUpdatedAt() == null) {
                updatingNotificationTemplate.setUpdatedAt(new Date());
            }

            Optional<NotificationTemplate> optNotificationTemplate = notificationTemplateRepository
                .findById(updatingNotificationTemplate.getId())
                .filter(
                    nt ->
                        nt.getReferenceType() == NotificationTemplateReferenceType.ORGANIZATION &&
                        nt.getReferenceId().equalsIgnoreCase(executionContext.getOrganizationId())
                );

            NotificationTemplate notificationTemplateToUpdate = optNotificationTemplate.orElseThrow(() ->
                new NotificationTemplateNotFoundException(updatingNotificationTemplate.getId())
            );
            notificationTemplateToUpdate.setTitle(updatingNotificationTemplate.getTitle());
            notificationTemplateToUpdate.setContent(updatingNotificationTemplate.getContent());
            notificationTemplateToUpdate.setEnabled(updatingNotificationTemplate.isEnabled());
            NotificationTemplate updatedNotificationTemplate = notificationTemplateRepository.update(notificationTemplateToUpdate);

            createAuditLog(
                executionContext,
                NotificationTemplate.AuditEvent.NOTIFICATION_TEMPLATE_UPDATED,
                updatingNotificationTemplate.getUpdatedAt(),
                optNotificationTemplate.orElse(null),
                updatedNotificationTemplate
            );

            final NotificationTemplateEntity updatedNotificationTemplateEntity = convert(updatedNotificationTemplate);

            sendUpdateTemplateCommand(updatedNotificationTemplateEntity, executionContext.getOrganizationId());

            return updatedNotificationTemplateEntity;
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to create or update notificationTemplate {}", updatingNotificationTemplate, ex);
            throw new TechnicalManagementException("An error occurs while trying to create or update " + updatingNotificationTemplate, ex);
        }
    }

    public void updateFreemarkerCache(NotificationTemplateEntity notificationTemplate, String organization) {
        StringTemplateLoader orgCustomizedTemplatesLoader = stringTemplateLoaderMapByOrg.get(organization);
        if (orgCustomizedTemplatesLoader == null) {
            Configuration orgFreemarkerConfiguration = this.initCurrentOrgFreemarkerConfiguration(organization);
            freemarkerConfigurationByOrg.put(organization, orgFreemarkerConfiguration);
            orgCustomizedTemplatesLoader = stringTemplateLoaderMapByOrg.get(organization);
        }

        if (notificationTemplate.isEnabled()) {
            // override the template in the loader
            if (notificationTemplate.getTitle() != null && !notificationTemplate.getTitle().isEmpty()) {
                orgCustomizedTemplatesLoader.putTemplate(notificationTemplate.getTitleTemplateName(), notificationTemplate.getTitle());
            }
            orgCustomizedTemplatesLoader.putTemplate(notificationTemplate.getContentTemplateName(), notificationTemplate.getContent());
        } else {
            // remove template so the template from file will be selected
            orgCustomizedTemplatesLoader.removeTemplate(notificationTemplate.getTitleTemplateName());
            orgCustomizedTemplatesLoader.removeTemplate(notificationTemplate.getContentTemplateName());
        }

        try {
            // force cache to be reloaded
            freemarkerConfigurationByOrg.get(organization).removeTemplateFromCache(notificationTemplate.getTitleTemplateName());
            freemarkerConfigurationByOrg.get(organization).removeTemplateFromCache(notificationTemplate.getName());
        } catch (IOException ex) {
            log.error("An error occurs while trying to update freemarker cache with this template {}", notificationTemplate, ex);
        }

        // Send an event to notify listener to reload template
        if (HookScope.TEMPLATES_FOR_ALERT.name().equals(notificationTemplate.getScope())) {
            eventManager.publishEvent(
                ApplicationAlertEventType.NOTIFICATION_TEMPLATE_UPDATE,
                new NotificationTemplateEvent(organization, notificationTemplate)
            );
        }
    }

    private void sendUpdateTemplateCommand(NotificationTemplateEntity notificationTemplate, String organization) {
        Instant now = Instant.now();
        var command = Command.builder()
            .id(UUID.random().toString())
            .from(node.id())
            .to(MessageRecipient.MANAGEMENT_APIS.name())
            .organizationId(organization)
            .tags(List.of(CommandTags.EMAIL_TEMPLATE_UPDATE.name()))
            .createdAt(Date.from(now))
            .updatedAt(Date.from(now));

        try {
            command.content(
                objectMapper.writeValueAsString(notificationTemplateMapper.toNotificationTemplateCommandEntity(notificationTemplate))
            );
        } catch (JsonProcessingException e) {
            log.error(
                "Failed to serialize notification template [{}] for organization [{}]",
                notificationTemplate.getId(),
                organization,
                e
            );
            return;
        }

        try {
            commandRepository.create(command.build());
        } catch (TechnicalException e) {
            log.error("Failed to create template update command [{}] for organization [{}]", notificationTemplate.getId(), organization, e);
        }
    }

    @Override
    public NotificationTemplateEntity findById(String organizationId, String id) {
        try {
            log.debug("Find notificationTemplate by ID: {}", id);

            Optional<NotificationTemplate> notificationTemplate = notificationTemplateRepository
                .findById(id)
                .filter(
                    nt ->
                        nt.getReferenceType() == NotificationTemplateReferenceType.ORGANIZATION &&
                        nt.getReferenceId().equalsIgnoreCase(organizationId)
                );

            if (notificationTemplate.isPresent()) {
                return convert(notificationTemplate.get());
            }
            throw new NotificationTemplateNotFoundException(id);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find a notificationTemplate using its ID {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to find a notificationTemplate using its ID " + id, ex);
        }
    }

    private void createAuditLog(
        ExecutionContext executionContext,
        Audit.AuditEvent event,
        Date createdAt,
        NotificationTemplate oldValue,
        NotificationTemplate newValue
    ) {
        String notificationTemplateName = oldValue != null ? oldValue.getName() : newValue.getName();
        auditService.createOrganizationAuditLog(
            executionContext,
            executionContext.getOrganizationId(),
            Collections.singletonMap(NOTIFICATION_TEMPLATE, notificationTemplateName),
            event,
            createdAt,
            oldValue,
            newValue
        );
    }

    private NotificationTemplate convert(NotificationTemplateEntity notificationTemplateEntity, String organization) {
        NotificationTemplate notificationTemplate = new NotificationTemplate();

        notificationTemplate.setId(notificationTemplateEntity.getId());
        notificationTemplate.setHook(notificationTemplateEntity.getHook());
        notificationTemplate.setScope(notificationTemplateEntity.getScope());
        notificationTemplate.setReferenceId(organization);
        notificationTemplate.setReferenceType(NotificationTemplateReferenceType.ORGANIZATION);
        notificationTemplate.setName(notificationTemplateEntity.getName());
        notificationTemplate.setDescription((notificationTemplateEntity.getDescription()));
        notificationTemplate.setTitle(notificationTemplateEntity.getTitle());
        notificationTemplate.setContent(notificationTemplateEntity.getContent());
        notificationTemplate.setType(NotificationTemplateType.valueOf(notificationTemplateEntity.getType().name()));
        notificationTemplate.setCreatedAt(notificationTemplateEntity.getCreatedAt());
        notificationTemplate.setUpdatedAt(notificationTemplateEntity.getUpdatedAt());
        notificationTemplate.setEnabled(notificationTemplateEntity.isEnabled());

        return notificationTemplate;
    }

    private NotificationTemplateEntity convert(NotificationTemplate notificationTemplate) {
        NotificationTemplateEntity notificationTemplateEntity = new NotificationTemplateEntity();

        notificationTemplateEntity.setId(notificationTemplate.getId());
        notificationTemplateEntity.setHook(notificationTemplate.getHook());
        notificationTemplateEntity.setScope(notificationTemplate.getScope());
        notificationTemplateEntity.setName(notificationTemplate.getName());
        notificationTemplateEntity.setDescription((notificationTemplate.getDescription()));
        notificationTemplateEntity.setTitle(notificationTemplate.getTitle());
        notificationTemplateEntity.setContent(notificationTemplate.getContent());
        notificationTemplateEntity.setType(
            io.gravitee.rest.api.model.notification.NotificationTemplateType.valueOf(notificationTemplate.getType().name())
        );
        notificationTemplateEntity.setCreatedAt(notificationTemplate.getCreatedAt());
        notificationTemplateEntity.setUpdatedAt(notificationTemplate.getUpdatedAt());
        notificationTemplateEntity.setEnabled(notificationTemplate.isEnabled());

        if (TEMPLATES_TO_INCLUDE_SCOPE.equalsIgnoreCase(notificationTemplate.getScope())) {
            notificationTemplateEntity.setTemplateName(notificationTemplate.getName());
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(notificationTemplate.getScope());
            sb.append(".");
            sb.append(notificationTemplate.getHook());
            sb.append(".");
            sb.append(notificationTemplate.getType());
            notificationTemplateEntity.setTemplateName(sb.toString().toUpperCase());
        }
        return notificationTemplateEntity;
    }
}
