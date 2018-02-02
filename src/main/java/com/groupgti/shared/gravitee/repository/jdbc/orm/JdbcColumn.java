/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.groupgti.shared.gravitee.repository.jdbc.orm;

import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author njt
 */
public class JdbcColumn {

    @SuppressWarnings("constantname")
    private static final Logger logger = LoggerFactory.getLogger(JdbcColumn.class);
    
    public final String name;
    public final int jdbcType;
    public final Class javaType;
    public final Method getter;
    public final Method setter;

    public JdbcColumn(String name, int jdbcType, Class fieldType, Method getter, Method setter) {
        this.name = name;
        this.jdbcType = jdbcType;
        this.javaType = fieldType;
        this.getter = getter;
        this.setter = setter;
    }

    public JdbcColumn(String name, int jdbcType, Class owningClass, Class fieldType) {
        this.name = name;
        this.jdbcType = jdbcType;
        this.javaType = fieldType;
        String getterName = "get";
        if ((fieldType == Boolean.class) || (fieldType == boolean.class)) {
            getterName = "is";
        }
        getterName += name;

        try {
            this.getter = owningClass.getMethod(getterName);
        } catch (NoSuchMethodException ex) {
            logger.error("Method {} not found: ", getterName, ex);
            throw new IllegalStateException("Method " + owningClass.getSimpleName() + "." + getterName + " not found", ex);
        }
        try {
            this.setter = owningClass.getMethod("set" + name, fieldType);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Method " + owningClass.getSimpleName() + ".set" + name + "( " + fieldType.getSimpleName() + ") not found", ex);
        }
    }

}
