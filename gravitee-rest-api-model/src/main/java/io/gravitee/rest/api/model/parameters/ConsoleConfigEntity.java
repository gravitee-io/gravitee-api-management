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
package io.gravitee.rest.api.model.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.rest.api.model.annotations.ParameterKey;

import java.util.List;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConsoleConfigEntity extends AbstractCommonConfigEntity {

    private Alert alert;
    private ConsoleAuthentication authentication;
    private ConsoleCors cors;
    private ConsoleReCaptcha reCaptcha;
    private ConsoleScheduler scheduler;
    private Logging logging;
    private Maintenance maintenance;
    private Management management;
    private Newsletter newsletter;
    private Theme theme;

    public ConsoleConfigEntity() {
        super();
        alert = new Alert();
        authentication = new ConsoleAuthentication();
        cors = new ConsoleCors();
        logging = new Logging();
        maintenance = new Maintenance();
        management = new Management();
        newsletter = new Newsletter();
        reCaptcha = new ConsoleReCaptcha();
        scheduler = new ConsoleScheduler();
        theme = new Theme();
    }

    // Getters & setters
    public Alert getAlert() {
        return alert;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }

    public ConsoleAuthentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(ConsoleAuthentication authentication) {
        this.authentication = authentication;
    }

    public ConsoleCors getCors() {
        return cors;
    }

    public void setCors(ConsoleCors cors) {
        this.cors = cors;
    }

    public ConsoleReCaptcha getReCaptcha() {
        return reCaptcha;
    }

    public void setReCaptcha(ConsoleReCaptcha reCaptcha) {
        this.reCaptcha = reCaptcha;
    }

    public ConsoleScheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(ConsoleScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Logging getLogging() {
        return logging;
    }

    public void setLogging(Logging logging) {
        this.logging = logging;
    }

    public Maintenance getMaintenance() {
        return maintenance;
    }

    public void setMaintenance(Maintenance maintenance) {
        this.maintenance = maintenance;
    }

    public Management getManagement() {
        return management;
    }

    public void setManagement(Management management) {
        this.management = management;
    }

    public Newsletter getNewsletter() {
        return newsletter;
    }

    public void setNewsletter(Newsletter newsletter) {
        this.newsletter = newsletter;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    //Classes
    public static class Alert {
        @ParameterKey(Key.ALERT_ENABLED)
        private Boolean enabled;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class ConsoleAuthentication extends CommonAuthentication{
        @ParameterKey(Key.CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED)
        private Enabled localLogin;

        public Enabled getLocalLogin() {
            return localLogin;
        }

        public void setLocalLogin(Enabled localLogin) {
            this.localLogin = localLogin;
        }
    }

    public static class ConsoleCors {
        @ParameterKey(Key.CONSOLE_HTTP_CORS_ALLOW_ORIGIN)
        private List<String> allowOrigin;

        @ParameterKey(Key.CONSOLE_HTTP_CORS_ALLOW_HEADERS)
        private List<String> allowHeaders;

        @ParameterKey(Key.CONSOLE_HTTP_CORS_ALLOW_METHODS)
        private List<String> allowMethods;

        @ParameterKey(Key.CONSOLE_HTTP_CORS_EXPOSED_HEADERS)
        private List<String> exposedHeaders;

        @ParameterKey(Key.CONSOLE_HTTP_CORS_MAX_AGE)
        private Integer maxAge;

        public List<String> getAllowOrigin() {
            return allowOrigin;
        }

        public void setAllowOrigin(List<String> allowOrigin) {
            this.allowOrigin = allowOrigin;
        }

        public List<String> getAllowHeaders() {
            return allowHeaders;
        }

        public void setAllowHeaders(List<String> allowHeaders) {
            this.allowHeaders = allowHeaders;
        }

        public List<String> getAllowMethods() {
            return allowMethods;
        }

        public void setAllowMethods(List<String> allowMethods) {
            this.allowMethods = allowMethods;
        }

        public List<String> getExposedHeaders() {
            return exposedHeaders;
        }

        public void setExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        public Integer getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(Integer maxAge) {
            this.maxAge = maxAge;
        }
    }

    public static class ConsoleReCaptcha {
        @ParameterKey(Key.CONSOLE_RECAPTCHA_ENABLED)
        private Boolean enabled;
        @ParameterKey(Key.CONSOLE_RECAPTCHA_SITE_KEY)
        private String siteKey;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getSiteKey() {
            return siteKey;
        }

        public void setSiteKey(String siteKey) {
            this.siteKey = siteKey;
        }
    }

    public static class ConsoleScheduler {
        @JsonProperty("tasks")
        @ParameterKey(Key.CONSOLE_SCHEDULER_TASKS)
        private Integer tasksInSeconds;

        @JsonProperty("notifications")
        @ParameterKey(Key.CONSOLE_SCHEDULER_NOTIFICATIONS)
        private Integer notificationsInSeconds;

        public Integer getTasksInSeconds() {
            return tasksInSeconds;
        }

        public void setTasksInSeconds(Integer tasksInSeconds) {
            this.tasksInSeconds = tasksInSeconds;
        }

        public Integer getNotificationsInSeconds() {
            return notificationsInSeconds;
        }

        public void setNotificationsInSeconds(Integer notificationsInSeconds) {
            this.notificationsInSeconds = notificationsInSeconds;
        }
    }

    public static class Logging {
        @ParameterKey(Key.LOGGING_DEFAULT_MAX_DURATION)
        private Long maxDurationMillis;
        private Audit audit = new Audit();
        private User user = new User();

        public Long getMaxDurationMillis() {
            return maxDurationMillis;
        }

        public void setMaxDurationMillis(Long maxDurationMillis) {
            this.maxDurationMillis = maxDurationMillis;
        }

        public Audit getAudit() {
            return audit;
        }

        public void setAudit(Audit audit) {
            this.audit = audit;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public static class Audit {
            @ParameterKey(Key.LOGGING_AUDIT_ENABLED)
            private Boolean enabled;
            private AuditTrail trail = new AuditTrail();

            public Boolean getEnabled() {
                return enabled;
            }

            public void setEnabled(Boolean enabled) {
                this.enabled = enabled;
            }

            public AuditTrail getTrail() {
                return trail;
            }

            public void setTrail(AuditTrail trail) {
                this.trail = trail;
            }

            public static class AuditTrail {
                @ParameterKey(Key.LOGGING_AUDIT_TRAIL_ENABLED)
                private Boolean enabled;

                public Boolean getEnabled() {
                    return enabled;
                }

                public void setEnabled(Boolean enabled) {
                    this.enabled = enabled;
                }
            }
        }

        public static class User {
            @ParameterKey(Key.LOGGING_USER_DISPLAYED)
            private Boolean displayed;

            public Boolean getDisplayed() {
                return displayed;
            }

            public void setDisplayed(Boolean displayed) {
                this.displayed = displayed;
            }
        }
    }

    public static class Maintenance {
        @ParameterKey(Key.MAINTENANCE_MODE_ENABLED)
        private Boolean enabled;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Management {
        @ParameterKey(Key.CONSOLE_SUPPORT_ENABLED)
        private Enabled support;
        @ParameterKey(Key.MANAGEMENT_TITLE)
        private String title;
        @ParameterKey(Key.MANAGEMENT_URL)
        private String url;
        @ParameterKey(Key.CONSOLE_USERCREATION_ENABLED)
        private Enabled userCreation;
        @ParameterKey(Key.CONSOLE_USERCREATION_AUTOMATICVALIDATION_ENABLED)
        private Enabled automaticValidation;

        public Enabled getSupport() {
            return support;
        }

        public void setSupport(Enabled support) {
            this.support = support;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Enabled getUserCreation() {
            return userCreation;
        }

        public void setUserCreation(Enabled userCreation) {
            this.userCreation = userCreation;
        }

        public Enabled getAutomaticValidation() {
            return automaticValidation;
        }

        public void setAutomaticValidation(Enabled automaticValidation) {
            this.automaticValidation = automaticValidation;
        }
    }

    public static class Newsletter {
        @ParameterKey(Key.NEWSLETTER_ENABLED)
        private Boolean enabled;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Theme {
        @ParameterKey(Key.THEME_NAME)
        private String name;
        @ParameterKey(Key.THEME_LOGO)
        private String logo;
        @ParameterKey(Key.THEME_LOADER)
        private String loader;
        @ParameterKey(Key.THEME_CSS)
        private String css;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLogo() {
            return logo;
        }

        public void setLogo(String logo) {
            this.logo = logo;
        }

        public String getLoader() {
            return loader;
        }

        public void setLoader(String loader) {
            this.loader = loader;
        }

        public String getCss() {
            return css;
        }

        public void setCss(String css) {
            this.css = css;
        }
    }
}
