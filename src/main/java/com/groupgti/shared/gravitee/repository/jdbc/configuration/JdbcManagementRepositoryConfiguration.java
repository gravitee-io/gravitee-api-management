/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.groupgti.shared.gravitee.repository.jdbc.mgmt"})
public class JdbcManagementRepositoryConfiguration extends AbstractJdbcRepositoryConfiguration {    
}
