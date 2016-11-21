package com.github.platan.bamboo.sputnik;

import static com.atlassian.bamboo.repository.ReflectionRepositoryAccessor.StashRepositoryAccessor.getApplicationLink;
import static com.atlassian.bamboo.repository.ReflectionRepositoryAccessor.StashRepositoryAccessor.getStashProjectKey;
import static com.atlassian.bamboo.repository.ReflectionRepositoryAccessor.StashRepositoryAccessor.getStashRepositorySlug;
import static com.atlassian.bamboo.repository.ReflectionRepositoryAccessor.StashRepositoryAccessor.isStashRepository;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.RepositoryDefinition;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.v2.build.BuildContext;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.List;

public class StashRepositoryDetailsLoader {

    private final EnvironmentVariableAccessor environmentVariableAccessor;

    public StashRepositoryDetailsLoader(EnvironmentVariableAccessor environmentVariableAccessor) {
        this.environmentVariableAccessor = environmentVariableAccessor;
    }

    @NotNull
    public StashRepositoryDetails getRepositoryDetails(TaskContext taskContext) throws TaskException {
        RepositoryDefinition repositoryDefinition = getMainStashRepository(taskContext);
        Repository repository = repositoryDefinition.getRepository();
        ApplicationLink applicationLink = getApplicationLink(repository);
        if (applicationLink == null) {
            throw new TaskException(String.format("Application link for '%s' repository is empty", repository.getName()));
        }
        URI stashUrl = applicationLink.getDisplayUrl();
        String stashProjectKey = getStashProjectKey(repository);
        String stashRepositorySlug = getStashRepositorySlug(repository);
        String branchName = getBranchName(taskContext, repositoryDefinition);
        String checkoutLocation = taskContext.getBuildContext().getCheckoutLocation().get(repositoryDefinition.getId());
        return new StashRepositoryDetails(stashUrl, stashProjectKey, stashRepositorySlug, branchName, checkoutLocation);
    }

    private String getBranchName(TaskContext taskContext, RepositoryDefinition repositoryDefinition) {
        int position = repositoryDefinition.getPosition() + 1;
        String variableKey = String.format("bamboo_planRepository_%d_branch", position);
        return environmentVariableAccessor.getEnvironment(taskContext).get(variableKey);
    }

    private RepositoryDefinition getMainStashRepository(TaskContext taskContext) throws TaskException {
        BuildContext buildContext = taskContext.getBuildContext();
        List<RepositoryDefinition> repositoryDefinitions = buildContext.getRepositoryDefinitions();

        if (repositoryDefinitions.isEmpty()) {
            throw new TaskException("No Stash repository is defined!");
        }
        RepositoryDefinition repositoryDefinition = repositoryDefinitions.get(0);
        if (!isStashRepository(repositoryDefinition.getRepository())) {
            throw new TaskException(String.format("'%s' is not a Stash repository!", repositoryDefinition.getName()));
        }
        if (!buildContext.getCheckoutLocation().containsKey(repositoryDefinition.getId())) {
            throw new TaskException(String.format("Repository '%s' is not checked out!", repositoryDefinition.getName()));
        }
        return repositoryDefinition;
    }

}
