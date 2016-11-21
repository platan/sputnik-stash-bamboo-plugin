package com.github.platan.bamboo.sputnik;

import com.atlassian.bamboo.v2.build.agent.capability.AbstractHomeDirectoryCapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.ExecutablePathUtils;
import org.jetbrains.annotations.NotNull;

public class SputnikCapabilityDefaultsHelper extends AbstractHomeDirectoryCapabilityDefaultsHelper {
    private static final String SPUTNIK_EXE_NAME = "sputnik";
    public static final String CAPABILITY_KEY = CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".sputnik.Sputnik";

    @NotNull
    @Override
    protected String getExecutableName() {
        return ExecutablePathUtils.makeBatchIfOnWindows(SPUTNIK_EXE_NAME);
    }

    @NotNull
    @Override
    protected String getCapabilityKey() {
        return CAPABILITY_KEY;
    }
}
