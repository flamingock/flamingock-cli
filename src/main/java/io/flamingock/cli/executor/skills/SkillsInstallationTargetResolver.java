/*
 * Copyright 2026 Flamingock (https://www.flamingock.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.flamingock.cli.executor.skills;

import io.flamingock.cli.executor.util.filesystem.DirectoryResolver;

import java.nio.file.Path;
import java.util.List;

/**
 * Resolves the install intent into concrete destination targets.
 */
public class SkillsInstallationTargetResolver {

    private static final String[] LOCAL_SKILLS_PATH = {".agents", "skills"};
    private static final String GLOBAL_MODE_NOT_IMPLEMENTED =
            "Global skills installation is not implemented yet. Run 'flamingock install-skills' to install into ./.agents/skills.";

    private final DirectoryResolver directoryResolver;

    public SkillsInstallationTargetResolver() {
        this(new DirectoryResolver());
    }

    SkillsInstallationTargetResolver(DirectoryResolver directoryResolver) {
        this.directoryResolver = directoryResolver;
    }

    /**
     * Resolves the skills installation targets for the requested mode.
     *
     * @param workingDirectory current command working directory
     * @param global whether global mode was requested
     * @return resolved installation targets
     */
    public List<SkillsInstallationTarget> resolveTargets(Path workingDirectory, boolean global) {
        if (global) {
            throw new IllegalStateException(GLOBAL_MODE_NOT_IMPLEMENTED);
        }

        Path destination = directoryResolver.resolveDirectory(workingDirectory, LOCAL_SKILLS_PATH);
        return List.of(SkillsInstallationTarget.local(destination));
    }
}
