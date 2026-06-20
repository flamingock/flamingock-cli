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
    private static final String[] CLAUDE_PATH = {".claude", "skills"};
    private static final String[] GITHUB_PATH = {".github", "skills"};
    private static final String[] CURSOR_PATH = {".cursor", "skills"};
    private static final String[] OPENCODE_PATH = {".opencode", "skills"};
    private static final String[] GEMINI_PATH = {".gemini", "skills"};
    private static final String[] WINDSURF_PATH = {".windsurf", "skills"};
    private static final String[] PI_PATH = {".pi", "skills"};
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
     * Resolves the skills installation targets for the requested mode (default agent).
     *
     * @param workingDirectory current command working directory
     * @param global whether global mode was requested
     * @return resolved installation targets
     */
    public List<SkillsInstallationTarget> resolveTargets(Path workingDirectory, boolean global) {
        return resolveTargets(workingDirectory, global, null);
    }

    /**
     * Resolves the skills installation targets for the requested mode and agent.
     *
     * @param workingDirectory current command working directory
     * @param global whether global mode was requested
     * @param agent target AI assistant identifier (claude, codex, github, cursor, opencode, gemini, windsurf, pi)
     * @return resolved installation targets
     */
    public List<SkillsInstallationTarget> resolveTargets(Path workingDirectory, boolean global, String agent) {
        if (global) {
            throw new IllegalStateException(GLOBAL_MODE_NOT_IMPLEMENTED);
        }

        if (agent == null) {
            Path destination = directoryResolver.resolveDirectory(workingDirectory, LOCAL_SKILLS_PATH);
            return List.of(SkillsInstallationTarget.agents(destination));
        }

        return switch (agent) {
            case "claude" -> {
                Path destination = directoryResolver.resolveDirectory(workingDirectory, CLAUDE_PATH);
                yield List.of(SkillsInstallationTarget.claude(destination));
            }
            case "codex" -> {
                Path destination = directoryResolver.resolveDirectory(workingDirectory, LOCAL_SKILLS_PATH);
                yield List.of(SkillsInstallationTarget.codex(destination));
            }
            case "github" -> {
                Path destination = directoryResolver.resolveDirectory(workingDirectory, GITHUB_PATH);
                yield List.of(SkillsInstallationTarget.github(destination));
            }
            case "cursor" -> {
                Path destination = directoryResolver.resolveDirectory(workingDirectory, CURSOR_PATH);
                yield List.of(SkillsInstallationTarget.cursor(destination));
            }
            case "opencode" -> {
                Path destination = directoryResolver.resolveDirectory(workingDirectory, OPENCODE_PATH);
                yield List.of(SkillsInstallationTarget.opencode(destination));
            }
            case "gemini" -> {
                Path destination = directoryResolver.resolveDirectory(workingDirectory, GEMINI_PATH);
                yield List.of(SkillsInstallationTarget.gemini(destination));
            }
            case "windsurf" -> {
                Path destination = directoryResolver.resolveDirectory(workingDirectory, WINDSURF_PATH);
                yield List.of(SkillsInstallationTarget.windsurf(destination));
            }
            case "pi" -> {
                Path destination = directoryResolver.resolveDirectory(workingDirectory, PI_PATH);
                yield List.of(SkillsInstallationTarget.pi(destination));
            }
            default -> throw new IllegalStateException(
                    "Unsupported agent: '" + agent + "'. Supported values: claude, codex, github, cursor, opencode, gemini, windsurf, pi.");
        };
    }
}
