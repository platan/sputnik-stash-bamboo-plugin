package com.github.platan.bamboo.sputnik.config;

import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;

import java.util.Map;

public interface ConfigurationProvider {
    Configuration getConfiguration(Map<String, VariableDefinitionContext> variables, CapabilityContext capabilityContext,
                                   ConfigurationMap configurationMap) throws TaskException;
}
