package com.github.platan.bamboo.sputnik.config;

import static com.atlassian.bamboo.variable.VariableType.GLOBAL;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.TRUE;

import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.atlassian.bamboo.variable.VariableType;
import com.github.platan.bamboo.sputnik.SputnikCapabilityDefaultsHelper;
import com.github.platan.bamboo.sputnik.SputnikTaskConfigurationConstants;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DefaultConfigurationProvider implements ConfigurationProvider {

    private static final List<SputnikOption> UNSETTABLE = ImmutableList.of(
            SputnikOption.CONNECTOR_TYPE,
            SputnikOption.HOST,
            SputnikOption.PORT,
            SputnikOption.PATH,
            SputnikOption.PROJECT,
            SputnikOption.REPOSITORY);
    private static final List<SputnikOption> GLOBAL_BY_DEFAULT = ImmutableList.of(SputnikOption.VERIFY_SSL, SputnikOption.USE_HTTPS);
    private static final List<SputnikOption> OVERRIDABLE_BY_ANY_VARIABLE_TYPE = removeAll(newArrayList(SputnikOption.values()),
            concat(GLOBAL_BY_DEFAULT, UNSETTABLE));
    private static final Map<String, String> DEFAULT_OPTIONS = ImmutableMap.of(SputnikOption.VERIFY_SSL.getKey(), TRUE.toString(),
            SputnikOption.USE_HTTPS.getKey(), TRUE.toString());

    private static <T> List<T> concat(List<T> a, List<T> b) {
        List<T> union = Lists.newArrayList(a);
        union.addAll(b);
        return ImmutableList.copyOf(union);
    }

    private static <T> List<T> removeAll(List<T> a, List<T> b) {
        List<T> aMinusB = newArrayList(a);
        aMinusB.removeAll(b);
        return ImmutableList.copyOf(aMinusB);
    }

    @Override
    public Configuration getConfiguration(Map<String, VariableDefinitionContext> variables, CapabilityContext capabilityContext,
                                          ConfigurationMap configurationMap) throws TaskException {
        String sputnikPath = getSputnikPath(configurationMap, capabilityContext);
        if (sputnikPath == null) {
            throw new TaskException("Neither Sputnik capability nor Sputnik path is defined!");
        }
        Map<String, String> properties = getProperties(variables, configurationMap);
        return new Configuration(getCredentials(properties), sputnikPath, properties);
    }

    private String getSputnikPath(ConfigurationMap configurationMap, CapabilityContext capabilityContext) {
        if (configurationMap.containsKey(SputnikTaskConfigurationConstants.BUILDER_SPUTNIK_PATH)) {
            return configurationMap.get(SputnikTaskConfigurationConstants.BUILDER_SPUTNIK_PATH);
        }
        return capabilityContext.getCapabilityValue(SputnikCapabilityDefaultsHelper.CAPABILITY_KEY);
    }

    private Map<String, String> getProperties(Map<String, VariableDefinitionContext> effectiveVariables,
                                              ConfigurationMap configurationMap) throws TaskException {
        Map<String, String> properties = new HashMap<String, String>();
        properties.putAll(DEFAULT_OPTIONS);
        for (SputnikOption option : OVERRIDABLE_BY_ANY_VARIABLE_TYPE) {
            String prefixedKey = option.getPrefixedKey();
            if (effectiveVariables.containsKey(prefixedKey)) {
                properties.put(option.getKey(), effectiveVariables.get(prefixedKey).getValue());
            }
        }
        for (SputnikOption option : GLOBAL_BY_DEFAULT) {
            String prefixedKey = option.getPrefixedKey();
            if (containsVariable(effectiveVariables, prefixedKey, GLOBAL) ||
                    (effectiveVariables.containsKey(prefixedKey)
                            && containsVariable(effectiveVariables, prefixedKey + ".override", GLOBAL))) {
                properties.put(option.getKey(), effectiveVariables.get(prefixedKey).getValue());
            }
        }
        if (configurationMap.containsKey(SputnikTaskConfigurationConstants.BUILDER_SPUTNIK_CONFIG_BODY)) {
            String configBody = configurationMap.get(SputnikTaskConfigurationConstants.BUILDER_SPUTNIK_CONFIG_BODY);
            properties.putAll(toMap(configBody));
        }
        return properties;
    }

    private Map<String, String> toMap(String configBody) throws TaskException {
        Map<String, String> propertiesMap = new HashMap<String, String>();
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(configBody));
        } catch (IOException e) {
            throw new TaskException("Cannot read configuration", e);
        }
        for (String propertyName : properties.stringPropertyNames()) {
            propertiesMap.put(propertyName, properties.getProperty(propertyName));
        }
        return propertiesMap;
    }

    private boolean containsVariable(Map<String, VariableDefinitionContext> effectiveVariables, String prefixedKey, VariableType type) {
        return effectiveVariables.containsKey(prefixedKey) && effectiveVariables.get(prefixedKey).getVariableType() == type;
    }

    private Credentials getCredentials(Map<String, String> properties) throws TaskException {
        String stashUsername = getRequiredVariableValue(properties, SputnikOption.USERNAME.getKey());
        String stashPassword = getRequiredVariableValue(properties, SputnikOption.PASSWORD.getKey());
        return new Credentials(stashUsername, stashPassword);
    }

    private String getRequiredVariableValue(Map<String, String> properties, String propertyKey) throws TaskException {
        if (!properties.containsKey(propertyKey)) {
            throw new TaskException("Property " + propertyKey + " is missing!");
        }
        return properties.get(propertyKey);
    }
}
