package com.github.platan.bamboo.sputnik;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.process.ExternalProcessBuilder;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskState;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.utils.process.ExternalProcess;
import com.github.platan.bamboo.sputnik.config.Configuration;
import com.github.platan.bamboo.sputnik.config.Credentials;
import com.github.platan.bamboo.sputnik.config.DefaultConfigurationProvider;
import com.github.platan.bamboo.sputnik.config.SputnikOption;
import com.google.common.base.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.UUID.randomUUID;

@Scanned
public class SputnikTask implements TaskType {

    private static final String SPUTNIK_PROPERTIES_FILE_NAME = "sputnik-%s.properties";
    private final ProcessService processService;
    private final EnvironmentVariableAccessor environmentVariableAccessor;
    private final CapabilityContext capabilityContext;

    @Autowired
    public SputnikTask(@ComponentImport ProcessService processService,
                       @ComponentImport EnvironmentVariableAccessor environmentVariableAccessor,
                       @ComponentImport CapabilityContext capabilityContext) {
        this.processService = processService;
        this.environmentVariableAccessor = environmentVariableAccessor;
        this.capabilityContext = capabilityContext;
    }

    @Override
    public TaskResult execute(final TaskContext taskContext) throws TaskException {
        BuildLogger buildLogger = taskContext.getBuildLogger();
        Map<String, VariableDefinitionContext> effectiveVariables = taskContext.getBuildContext()
                .getVariableContext().getEffectiveVariables();

        Configuration configuration = new DefaultConfigurationProvider().getConfiguration(effectiveVariables, capabilityContext,
                taskContext.getConfigurationMap());
        StashRepositoryDetails stashRepository = new StashRepositoryDetailsLoader(environmentVariableAccessor)
                .getRepositoryDetails(taskContext);
        logRepository(stashRepository, buildLogger);
        Optional<Long> pullRequestId = getPullRequestId(stashRepository, configuration.getCredentials());
        if (!pullRequestId.isPresent()) {
            boolean failOnMissingPullRequest = taskContext.getConfigurationMap()
                    .getAsBoolean(SputnikTaskConfigurationConstants.BUILDER_SPUTNIK_FAIL);
            String message = String.format("No pull request found for branch '%s'.", stashRepository.getBranchName());
            if (failOnMissingPullRequest) {
                throw new TaskException(message);
            } else {
                buildLogger.addBuildLogEntry(message);
                return TaskResultBuilder.newBuilder(taskContext).setState(TaskState.SUCCESS).build();
            }
        }
        buildLogger.addBuildLogEntry("Pull request id: " + pullRequestId.get());

        Properties properties = prepareConfigProperties(stashRepository, configuration.getCredentials(), configuration.getProperties());
        logProperties(buildLogger, properties);
        File sputnikConfigFile = new File(taskContext.getWorkingDirectory(), String.format(SPUTNIK_PROPERTIES_FILE_NAME, randomUUID()));
        saveSputnikConfig(properties, sputnikConfigFile);
        Map<String, String> environmentVariables = buildEnvironmentVariablesMap(taskContext,
                taskContext.getConfigurationMap().get(SputnikTaskConfigurationConstants.BUILDER_SPUTNIK_ENVIRONMENT_VARIABLES));
        try {
            return runExternalProcess(taskContext, stashRepository.getCheckoutLocation(), configuration.getSputnikPath(),
                    pullRequestId.get(), sputnikConfigFile, environmentVariables);
        } finally {
            if (!sputnikConfigFile.delete()) {
                buildLogger.addErrorLogEntry(String.format("Cannot remove configuration file '%s'", sputnikConfigFile.getAbsolutePath()));
            }
        }
    }

    private void logProperties(BuildLogger buildLogger, Properties properties) {
        buildLogger.addBuildLogEntry("Sputnik configuration: ");
        List<String> propertyNames = new ArrayList<String>(properties.stringPropertyNames());
        Collections.sort(propertyNames);
        for (String name : propertyNames) {
            String value = name.contains("password") ? "***" : properties.getProperty(name);
            buildLogger.addBuildLogEntry(String.format("%s = %s", name, value));
        }
    }

    private void logRepository(StashRepositoryDetails stashRepository, BuildLogger buildLogger) {
        buildLogger.addBuildLogEntry(String.format("Stash URL: %s, project key: %s, repository slug: %s, branch: %s",
                stashRepository.getUrl(), stashRepository.getProjectKey(), stashRepository.getRepositorySlug(),
                stashRepository.getBranchName()));
    }

    private Optional<Long> getPullRequestId(StashRepositoryDetails stashRepository, Credentials credentials) throws TaskException {
        StashRestClient stashRestClient = new UnirestStashRestClient(stashRepository.getUrl().toString(),
                credentials.getStashUsername(), credentials.getStashPassword());
        return stashRestClient.getPullRequestId(stashRepository.getProjectKey(),
                stashRepository.getRepositorySlug(), stashRepository.getBranchName());
    }

    private void saveSputnikConfig(Properties properties, File configFile) throws TaskException {
        try {
            properties.store(new FileOutputStream(configFile), "");
        } catch (IOException e) {
            throw new TaskException(String.format("Cannot save sputnik configuration in '%s'!", configFile.getAbsolutePath()), e);
        }
    }

    private TaskResult runExternalProcess(TaskContext taskContext, String checkoutLocation, String sputnikPath, Long pullRequestId,
                                          File sputnikConfigFile, Map<String, String> environmentVariables) {
        String commandString = String.format("%s/bin/sputnik --conf %s --pullRequestId %d",
                sputnikPath, sputnikConfigFile.getAbsolutePath(), pullRequestId);
        taskContext.getBuildLogger().addBuildLogEntry("Executing command: " + commandString);

        ExternalProcessBuilder externalProcessBuilder = new ExternalProcessBuilder().commandFromString(commandString)
                .workingDirectory(new File(checkoutLocation)).env(environmentVariables);
        ExternalProcess process = processService.createExternalProcess(taskContext, externalProcessBuilder);
        process.execute();
        return TaskResultBuilder.newBuilder(taskContext).checkReturnCode(process, 0).build();
    }

    @NotNull
    private Map<String, String> buildEnvironmentVariablesMap(TaskContext taskContext, String environmentVariables) {
        Map<String, String> environmentVariablesMap = new HashMap<String, String>();
        if (environmentVariables != null) {
            taskContext.getBuildLogger().addBuildLogEntry("Using environment variables: " + environmentVariables);
            environmentVariablesMap.putAll(environmentVariableAccessor.splitEnvironmentAssignments(environmentVariables));
        }
        return environmentVariablesMap;
    }

    private Properties prepareConfigProperties(StashRepositoryDetails stashRepository, Credentials credentials,
                                               Map<String, String> extraProperties) {
        Properties properties = new Properties();
        URI stashUrl = stashRepository.getUrl();
        properties.put(SputnikOption.CONNECTOR_TYPE.getKey(), "stash");
        properties.put(SputnikOption.HOST.getKey(), stashUrl.getHost());
        properties.put(SputnikOption.PORT.getKey(), Integer.toString(stashUrl.getPort()));
        properties.put(SputnikOption.USERNAME.getKey(), credentials.getStashUsername());
        properties.put(SputnikOption.PASSWORD.getKey(), credentials.getStashPassword());
        properties.put(SputnikOption.PATH.getKey(), stashUrl.getPath());
        properties.put(SputnikOption.REPOSITORY.getKey(), stashRepository.getRepositorySlug());
        properties.put(SputnikOption.PROJECT.getKey(), stashRepository.getProjectKey());
        properties.putAll(extraProperties);
        return properties;
    }
}