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
package io.flamingock.cli.executor.command;

import io.flamingock.cli.executor.output.ConsoleFormatter;
import io.flamingock.cli.executor.skills.SkillsInstallationTarget;
import io.flamingock.cli.executor.skills.SkillsInstallationPipeline;
import io.flamingock.cli.executor.skills.SkillsInstallationResult;
import io.flamingock.cli.executor.skills.SkillsInstallationTargetResolver;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Installs the official Flamingock AI skills into the current project.
 */
@Command(
        name = "install-skills",
        description = "Install official Flamingock AI skills into the current project",
        mixinStandardHelpOptions = true
)
public class InstallSkillsCommand implements Callable<Integer> {

    @Option(names = {"-g", "--global"}, description = "Install skills globally (not implemented yet)")
    private boolean global;

    @Option(names = {"-a", "--agent"},
            description = "Target AI assistant: claude or all (default: agents, local project)")
    private String agent;

    private final SkillsInstallationTargetResolver targetResolver;
    private final SkillsInstallationPipeline pipeline;
    private final Path workingDirectory;

    /**
     * Creates a command with the default production collaborators.
     */
    public InstallSkillsCommand() {
        this(new SkillsInstallationTargetResolver(), new SkillsInstallationPipeline(), Path.of(""));
    }

    InstallSkillsCommand(
            SkillsInstallationTargetResolver targetResolver,
            SkillsInstallationPipeline pipeline,
            Path workingDirectory
    ) {
        this.targetResolver = targetResolver;
        this.pipeline = pipeline;
        this.workingDirectory = workingDirectory;
    }

    /**
     * Executes the install-skills command.
     *
     * @return process exit code
     */
    @Override
    public Integer call() {
        try {
            List<SkillsInstallationTarget> targets = targetResolver.resolveTargets(workingDirectory.toAbsolutePath().normalize(), global, agent);
            SkillsInstallationResult result = pipeline.install(targets);
            ConsoleFormatter.printInfo(buildSuccessMessage(result));
            return 0;
        } catch (IllegalStateException e) {
            ConsoleFormatter.printError(e.getMessage());
            return 1;
        }
    }

    private String buildSuccessMessage(SkillsInstallationResult result) {
        if (result.targets().size() == 1) {
            return "Installed " + result.installedSkills().size()
                    + " Flamingock skill(s) into " + result.destinationSkillsDir();
        }

        return "Installed " + result.installedSkills().size() + " Flamingock skill(s) into "
                + result.targets().size() + " destinations.";
    }
}
