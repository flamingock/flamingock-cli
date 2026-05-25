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
import io.flamingock.cli.executor.skills.InstallDestinationResolver;
import io.flamingock.cli.executor.skills.InstallMode;
import io.flamingock.cli.executor.skills.InstallModeResolver;
import io.flamingock.cli.executor.skills.SkillsInstallationPipeline;
import io.flamingock.cli.executor.skills.SkillsInstallationResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
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

    private static final String GLOBAL_MODE_NOT_IMPLEMENTED =
            "Global skills installation is not implemented yet. Run 'flamingock install-skills' to install into ./.agents/skills.";

    @Option(names = {"-g", "--global"}, description = "Install skills globally (not implemented yet)")
    private boolean global;

    private final InstallModeResolver modeResolver;
    private final InstallDestinationResolver destinationResolver;
    private final SkillsInstallationPipeline pipeline;
    private final Path workingDirectory;

    /**
     * Creates a command with the default production collaborators.
     */
    public InstallSkillsCommand() {
        this(new InstallModeResolver(), new InstallDestinationResolver(), new SkillsInstallationPipeline(), Path.of(""));
    }

    InstallSkillsCommand(
            InstallModeResolver modeResolver,
            InstallDestinationResolver destinationResolver,
            SkillsInstallationPipeline pipeline,
            Path workingDirectory
    ) {
        this.modeResolver = modeResolver;
        this.destinationResolver = destinationResolver;
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
        InstallMode mode = modeResolver.resolve(global);
        if (mode == InstallMode.GLOBAL) {
            ConsoleFormatter.printError(GLOBAL_MODE_NOT_IMPLEMENTED);
            return 1;
        }

        try {
            Path destination = destinationResolver.resolveLocal(workingDirectory.toAbsolutePath().normalize());
            SkillsInstallationResult result = pipeline.install(destination);
            ConsoleFormatter.printInfo("Installed " + result.installedSkills().size()
                    + " Flamingock skill(s) into " + result.destinationSkillsDir());
            return 0;
        } catch (IllegalStateException e) {
            ConsoleFormatter.printError(e.getMessage());
            return 1;
        }
    }
}
