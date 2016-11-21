package com.github.platan.bamboo.sputnik;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskConfiguratorHelper;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Scanned
public class SputnikTaskConfigurator extends AbstractTaskConfigurator {

    private static final List<String> FIELDS_TO_COPY = ImmutableList.of(SputnikTaskConfigurationConstants
            .BUILDER_SPUTNIK_ENVIRONMENT_VARIABLES,
            SputnikTaskConfigurationConstants.BUILDER_SPUTNIK_CONFIG_BODY, SputnikTaskConfigurationConstants.BUILDER_SPUTNIK_PATH,
            SputnikTaskConfigurationConstants.BUILDER_SPUTNIK_FAIL);

    @Autowired
    public SputnikTaskConfigurator(@ComponentImport final TaskConfiguratorHelper taskConfiguratorHelper) {
        this.taskConfiguratorHelper = taskConfiguratorHelper;
    }

    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition
            previousTaskDefinition) {
        final HashMap<String, String> config = Maps.newHashMap();
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(config, params, FIELDS_TO_COPY);
        return config;
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        taskConfiguratorHelper.populateContextWithConfiguration(context, taskDefinition, FIELDS_TO_COPY);
    }
}
