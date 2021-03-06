/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Mocks;
import com.newrelic.agent.database.SqlObfuscator;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HighSecurityTest {
    
    private static final String PERMITTED_MODULE = "permitted-module";
    private static final String PERMITTED_MODULE_2 = "permitted-module2";

    @Test
    public void isEnableHighSecurity() throws Exception {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.HIGH_SECURITY, !AgentConfigImpl.DEFAULT_HIGH_SECURITY);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.COLLECT_SLOW_QUERIES_FROM, PERMITTED_MODULE);
        localMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(!AgentConfigImpl.DEFAULT_HIGH_SECURITY, config.isHighSecurity());
        // record sql should be off or obfuscated
        assertEquals(SqlObfuscator.OBFUSCATED_SETTING, config.getTransactionTracerConfig().getRecordSql());
        // permitted-module should be present
        assertTrue(config.getTransactionTracerConfig().getCollectSlowQueriesFromModules().contains(PERMITTED_MODULE));
    }

    @Test
    public void isEnableHighSecurityRecordSqlOff() throws Exception {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.HIGH_SECURITY, true);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.RECORD_SQL, "off");
        ttMap.put(TransactionTracerConfigImpl.COLLECT_SLOW_QUERIES_FROM, PERMITTED_MODULE);
        localMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertTrue(config.isHighSecurity());
        // record sql should be off or obfuscated
        assertEquals(SqlObfuscator.OFF_SETTING, config.getTransactionTracerConfig().getRecordSql());
        // permitted-module should be present
        assertTrue(config.getTransactionTracerConfig().getCollectSlowQueriesFromModules().contains(PERMITTED_MODULE));
    }

    @Test
    public void isEnableHighSecurityOff() throws Exception {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.HIGH_SECURITY, false);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.RECORD_SQL, "raw");
        ttMap.put(TransactionTracerConfigImpl.COLLECT_SLOW_QUERIES_FROM, PERMITTED_MODULE);
        localMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertFalse(config.isHighSecurity());
        assertEquals("raw", config.getTransactionTracerConfig().getRecordSql());
        // list to keep should be empty since high_security is disabled
        assertTrue(config.getTransactionTracerConfig().getCollectSlowQueriesFromModules().isEmpty());
    }

    @Test
    public void isEnableHighSecurityCheckFlattenProps() throws Exception {
        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.HIGH_SECURITY, Boolean.TRUE);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.COLLECT_SLOW_QUERIES_FROM, PERMITTED_MODULE);
        localMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertEquals(true, config.getValue(AgentConfigImpl.HIGH_SECURITY));
        // record sql should be off or obfuscated
        assertEquals(SqlObfuscator.OBFUSCATED_SETTING, config.getValue("transaction_tracer.record_sql"));
        // permitted-module should be present
        assertTrue(config.getTransactionTracerConfig().getCollectSlowQueriesFromModules().contains(PERMITTED_MODULE));
    }

    @Test
    public void isEnableHighSecurityCheckFlattenPropsWithSystemProps() throws Exception {
        try {
            Map<String, String> properties = new HashMap<>();
            String key = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.TRANSACTION_TRACER + "."
                    + TransactionTracerConfigImpl.RECORD_SQL;
            String val = "raw";
            properties.put(key, val);
            key = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.TRANSACTION_TRACER + "."
                    + TransactionTracerConfigImpl.COLLECT_SLOW_QUERIES_FROM;
            val = PERMITTED_MODULE;
            properties.put(key, val);
            Mocks.createSystemPropertyProvider(properties);

            Map<String, Object> localMap = new HashMap<>();
            localMap.put(AgentConfigImpl.HIGH_SECURITY, Boolean.TRUE);
            AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

            assertEquals(true, config.getValue(AgentConfigImpl.HIGH_SECURITY));
            // record sql should be off or obfuscated
            assertEquals(SqlObfuscator.OBFUSCATED_SETTING, config.getValue("transaction_tracer.record_sql"));
            // permitted-module should be present
            assertTrue(config.getTransactionTracerConfig().getCollectSlowQueriesFromModules().contains(PERMITTED_MODULE));
        } finally {
            Mocks.createSystemPropertyProvider(new HashMap<String, String>());
        }
    }

    @Test
    public void isEnableHighSecuritySystemProperty() throws Exception {
        try {
            Map<String, String> properties = new HashMap<>();
            String key = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.HIGH_SECURITY;
            String val = String.valueOf(true);
            properties.put(key, val);
            Mocks.createSystemPropertyProvider(properties);
            Map<String, Object> localMap = new HashMap<>();
            localMap.put(AgentConfigImpl.HIGH_SECURITY, AgentConfigImpl.DEFAULT_HIGH_SECURITY);
            AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

            assertTrue(config.isHighSecurity());
            assertEquals(SqlObfuscator.OBFUSCATED_SETTING, config.getTransactionTracerConfig().getRecordSql());
            // list to keep should be empty since high_security is disabled
            assertTrue(config.getTransactionTracerConfig().getCollectSlowQueriesFromModules().isEmpty());
        } finally {
            Mocks.createSystemPropertyProvider(new HashMap<String, String>());
        }
    }

    @Test
    public void isEnableHighSecurityDefault() throws Exception {
        Map<String, Object> localMap = new HashMap<>();
        AgentConfig config = AgentConfigImpl.createAgentConfig(localMap);

        assertFalse(config.isHighSecurity());
    }

    @Test
    public void isEnableHighSecurityServerSystemProperty() throws Exception {
        try {
            Map<String, String> properties = new HashMap<>();
            String key = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.HIGH_SECURITY;
            String val = String.valueOf(true);
            properties.put(key, val);
            key = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.TRANSACTION_TRACER + "."
                    + TransactionTracerConfigImpl.RECORD_SQL;
            val = "raw";
            properties.put(key, val);
            key = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.TRANSACTION_TRACER + "."
                    + TransactionTracerConfigImpl.COLLECT_SLOW_QUERIES_FROM;
            val = PERMITTED_MODULE;
            properties.put(key, val);
            Mocks.createSystemPropertyProvider(properties);
            Map<String, Object> serverMap = new HashMap<>();
            serverMap.put(AgentConfigImpl.HIGH_SECURITY, Boolean.FALSE);
            AgentConfig config = AgentConfigFactory.createAgentConfig(new HashMap<String, Object>(), serverMap, null);

            assertTrue(config.isHighSecurity());
            // record sql should be off or obfuscated
            assertEquals(SqlObfuscator.OBFUSCATED_SETTING, config.getTransactionTracerConfig().getRecordSql());
            // permitted-module should be present
            assertTrue(config.getTransactionTracerConfig().getCollectSlowQueriesFromModules().contains(PERMITTED_MODULE));
        } finally {
            Mocks.createSystemPropertyProvider(new HashMap<String, String>());
        }
    }

    @Test
    public void isDisableHighSecurityServerSystemProperty() throws Exception {
        try {
            Map<String, String> properties = new HashMap<>();
            String key = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.HIGH_SECURITY;
            String val = String.valueOf(false);
            properties.put(key, val);
            Mocks.createSystemPropertyProvider(properties);
            Map<String, Object> serverMap = new HashMap<>();
            serverMap.put(AgentConfigImpl.HIGH_SECURITY, Boolean.TRUE);
            Map<String, Object> localMap = new HashMap<>();
            Map<String, Object> ttMap = new HashMap<>();
            ttMap.put(TransactionTracerConfigImpl.RECORD_SQL, "raw");
            ttMap.put(TransactionTracerConfigImpl.COLLECT_SLOW_QUERIES_FROM, PERMITTED_MODULE);
            localMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
            AgentConfig config = AgentConfigFactory.createAgentConfig(localMap, serverMap, null);

            assertFalse(config.isHighSecurity());
            assertEquals(SqlObfuscator.RAW_SETTING, config.getTransactionTracerConfig().getRecordSql());
            // list to keep should be empty since high_security is disabled
            assertTrue(config.getTransactionTracerConfig().getCollectSlowQueriesFromModules().isEmpty());
        } finally {
            Mocks.createSystemPropertyProvider(new HashMap<String, String>());
        }
    }

    @Test
    public void localWithServerPropEnabledOff() throws Exception {
        Map<String, Object> serverMap = new HashMap<>();
        Map<String, Object> serverInnerData = new HashMap<>();
        serverMap.put(AgentConfigFactory.AGENT_CONFIG, serverInnerData);
        serverInnerData.put(AgentConfigImpl.HIGH_SECURITY, Boolean.FALSE);

        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.HIGH_SECURITY, true);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.RECORD_SQL, "off");
        ttMap.put(TransactionTracerConfigImpl.COLLECT_SLOW_QUERIES_FROM, PERMITTED_MODULE);
        localMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);

        AgentConfig config = AgentConfigFactory.createAgentConfig(localMap, serverMap, null);

        // off setting okay with high security
        assertTrue(config.isHighSecurity());
        assertEquals(SqlObfuscator.OFF_SETTING, config.getTransactionTracerConfig().getRecordSql());
        // permitted-module should be present
        assertTrue(config.getTransactionTracerConfig().getCollectSlowQueriesFromModules().contains(PERMITTED_MODULE));

    }

    @Test
    public void localWithServerPropEnabled() throws Exception {
        Map<String, Object> serverMap = new HashMap<>();
        Map<String, Object> serverInnerData = new HashMap<>();
        serverMap.put(AgentConfigFactory.AGENT_CONFIG, serverInnerData);
        serverInnerData.put(AgentConfigImpl.HIGH_SECURITY, Boolean.FALSE);

        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.HIGH_SECURITY, true);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.RECORD_SQL, "raw");
        ttMap.put(TransactionTracerConfigImpl.COLLECT_SLOW_QUERIES_FROM, PERMITTED_MODULE);
        localMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);

        AgentConfig config = AgentConfigFactory.createAgentConfig(localMap, serverMap, null);

        // no high security change from server
        assertTrue(config.isHighSecurity());
        assertEquals(SqlObfuscator.OBFUSCATED_SETTING, config.getTransactionTracerConfig().getRecordSql());
        // permitted-module should be present
        assertTrue(config.getTransactionTracerConfig().getCollectSlowQueriesFromModules().contains(PERMITTED_MODULE));

    }

    @Test
    public void localWithServerPropDisabled() throws Exception {
        Map<String, Object> serverMap = new HashMap<>();
        Map<String, Object> serverInnerData = new HashMap<>();
        serverMap.put(AgentConfigFactory.AGENT_CONFIG, serverInnerData);
        serverInnerData.put(AgentConfigImpl.HIGH_SECURITY, Boolean.TRUE);

        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.HIGH_SECURITY, false);
        Map<String, Object> ttMap = new HashMap<>();
        ttMap.put(TransactionTracerConfigImpl.RECORD_SQL, "raw");
        ttMap.put(TransactionTracerConfigImpl.COLLECT_SLOW_QUERIES_FROM, PERMITTED_MODULE);
        localMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);

        AgentConfig config = AgentConfigFactory.createAgentConfig(localMap, serverMap, null);

        // no high security change from server - takes local properties
        assertFalse(config.isHighSecurity());
        assertEquals(SqlObfuscator.RAW_SETTING, config.getTransactionTracerConfig().getRecordSql());
        // list to keep should be empty since high_security is disabled
        assertTrue(config.getTransactionTracerConfig().getCollectSlowQueriesFromModules().isEmpty());

    }

    @Test
    public void localAndServerWithNulls() throws Exception {
        Map<String, Object> serverMap = new HashMap<>();
        Map<String, Object> serverInnerData = new HashMap<>();
        serverMap.put(AgentConfigFactory.AGENT_CONFIG, serverInnerData);
        serverInnerData.put(AgentConfigImpl.HIGH_SECURITY, Boolean.FALSE);
        serverInnerData.put(AgentConfigFactory.RECORD_SQL, null);
        serverInnerData.put(AgentConfigFactory.COLLECT_SLOW_QUERIES_FROM, PERMITTED_MODULE);

        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.HIGH_SECURITY, true);
        Map<String, Object> ttMap = new HashMap<>();
        localMap.put(AgentConfigImpl.TRANSACTION_TRACER, ttMap);
        ttMap.put(TransactionTracerConfigImpl.RECORD_SQL, "obfuscated");
        ttMap.put(TransactionTracerConfigImpl.COLLECT_SLOW_QUERIES_FROM, PERMITTED_MODULE_2);

        AgentConfig config = AgentConfigFactory.createAgentConfig(localMap, serverMap, null);

        // make sure null do not throw exceptions
        assertTrue(config.isHighSecurity());
        assertEquals(SqlObfuscator.OBFUSCATED_SETTING, config.getTransactionTracerConfig().getRecordSql());
        // permitted-module should NOT be present (we don't accept list from server) but permitted-module-2 should be present
        assertFalse(config.getTransactionTracerConfig().getCollectSlowQueriesFromModules().contains(PERMITTED_MODULE));
        assertTrue(config.getTransactionTracerConfig().getCollectSlowQueriesFromModules().contains(PERMITTED_MODULE_2));
    }

    @Test
    public void localWithRootServer() throws Exception {
        Map<String, Object> serverMap = new HashMap<>();

        serverMap.put(AgentConfigImpl.HIGH_SECURITY, Boolean.TRUE);
        serverMap.put(TransactionTracerConfigImpl.RECORD_SQL, "raw");
        serverMap.put(TransactionTracerConfigImpl.COLLECT_SLOW_QUERIES_FROM, PERMITTED_MODULE);

        Map<String, Object> localMap = new HashMap<>();
        localMap.put(AgentConfigImpl.HIGH_SECURITY, true);

        AgentConfig config = AgentConfigFactory.createAgentConfig(localMap, serverMap, null);

        // picks up server settings since high security mode is off
        assertTrue(config.isHighSecurity());
        assertEquals(SqlObfuscator.OBFUSCATED_SETTING, config.getTransactionTracerConfig().getRecordSql());
        // list to keep should be empty since high_security is disabled
        assertTrue(config.getTransactionTracerConfig().getCollectSlowQueriesFromModules().isEmpty());
    }
}
