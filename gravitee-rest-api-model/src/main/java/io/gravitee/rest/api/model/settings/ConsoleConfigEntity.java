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
package io.gravitee.rest.api.model.settings;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConsoleConfigEntity {

    private Alert alert;
    private ConsoleAuthentication authentication;
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
}
