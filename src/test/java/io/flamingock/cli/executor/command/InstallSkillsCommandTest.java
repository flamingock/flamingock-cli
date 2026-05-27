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

import io.flamingock.cli.executor.FlamingockExecutorCli;
import io.flamingock.cli.executor.skills.SkillsInstallationTarget;
import io.flamingock.cli.executor.skills.SkillsInstallationPipeline;
import io.flamingock.cli.executor.skills.SkillsInstallationResult;
import io.flamingock.cli.executor.skills.SkillsInstallationTargetResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallSkillsCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void rootCommand_registersInstallSkillsSubcommand() {
        CommandLine commandLine = new CommandLine(new FlamingockExecutorCli());

        assertTrue(commandLine.getSubcommands().containsKey("install-skills"));
    }

    @Test
    void call_localInvocationInstallsSkillsIntoResolvedDestination() throws Exception {
        SkillsInstallationTarget resolvedTarget = SkillsInstallationTarget.local(tempDir.resolve(".agents/skills"));
        RecordingTargetResolver targetResolver = new RecordingTargetResolver(List.of(resolvedTarget));
        RecordingPipeline pipeline = new RecordingPipeline(new SkillsInstallationResult(
                List.of(resolvedTarget),
                List.of("flamingock-core", "flamingock-java")
        ));
        InstallSkillsCommand command = new InstallSkillsCommand(targetResolver, pipeline, tempDir);

        int exitCode = new CommandLine(command).execute();

        assertEquals(0, exitCode);
        assertTrue(targetResolver.called);
        assertFalse(targetResolver.global);
        assertEquals(tempDir, targetResolver.workingDirectory);
        assertEquals(List.of(resolvedTarget), pipeline.targets);
    }

    @Test
    void call_globalFlagPrintsNotImplementedAndDoesNotTouchFilesystem() throws Exception {
        FailingTargetResolver targetResolver = new FailingTargetResolver(
                new IllegalStateException("Global skills installation is not implemented yet. Run 'flamingock install-skills' to install into ./.agents/skills.")
        );
        RecordingPipeline pipeline = new RecordingPipeline(new SkillsInstallationResult(List.of(), List.of()));
        InstallSkillsCommand command = new InstallSkillsCommand(targetResolver, pipeline, tempDir);

        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errContent, true, StandardCharsets.UTF_8));
        try {
            int exitCode = new CommandLine(command).execute("--global");

            assertEquals(1, exitCode);
        } finally {
            System.setErr(originalErr);
        }

        assertTrue(targetResolver.called);
        assertTrue(targetResolver.global);
        assertFalse(pipeline.called);
        assertFalse(Files.exists(tempDir.resolve(".agents")));
        assertTrue(errContent.toString(StandardCharsets.UTF_8).contains("not implemented"));
    }

    @Test
    void call_localInvocationUsesSharedPipelineAndPrintsInstalledSkillCount() throws Exception {
        SkillsInstallationTarget resolvedTarget = SkillsInstallationTarget.local(tempDir.resolve(".agents/skills"));
        RecordingTargetResolver targetResolver = new RecordingTargetResolver(List.of(resolvedTarget));
        RecordingPipeline pipeline = new RecordingPipeline(new SkillsInstallationResult(
                List.of(resolvedTarget),
                List.of("flamingock-core")
        ));
        InstallSkillsCommand command = new InstallSkillsCommand(targetResolver, pipeline, tempDir);

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
        try {
            int exitCode = new CommandLine(command).execute();

            assertEquals(0, exitCode);
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(pipeline.called);
        assertTrue(outContent.toString(StandardCharsets.UTF_8).contains("Installed 1 Flamingock skill(s)"));
    }

    @Test
    void call_localInvocationPrintsActionableFailureWithoutStackTraceAndReturnsExitCodeOne() throws Exception {
        SkillsInstallationTarget resolvedTarget = SkillsInstallationTarget.local(tempDir.resolve(".agents/skills"));
        RecordingTargetResolver targetResolver = new RecordingTargetResolver(List.of(resolvedTarget));
        FailingPipeline pipeline = new FailingPipeline(
                new IllegalStateException("Download timed out while fetching official Flamingock skills. Check your network connection and retry.")
        );
        InstallSkillsCommand command = new InstallSkillsCommand(targetResolver, pipeline, tempDir);

        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errContent, true, StandardCharsets.UTF_8));
        try {
            int exitCode = new CommandLine(command).execute();

            assertEquals(1, exitCode);
        } finally {
            System.setErr(originalErr);
        }

        String stderr = errContent.toString(StandardCharsets.UTF_8);
        assertTrue(targetResolver.called);
        assertTrue(pipeline.called);
        assertTrue(stderr.contains("timed out"));
        assertTrue(stderr.contains("retry"));
        assertFalse(stderr.contains("IllegalStateException"));
        assertFalse(stderr.contains("\tat "));
    }

    private static final class RecordingTargetResolver extends SkillsInstallationTargetResolver {

        private final List<SkillsInstallationTarget> targets;
        private boolean called;
        private Path workingDirectory;
        private boolean global;

        private RecordingTargetResolver(List<SkillsInstallationTarget> targets) {
            this.targets = targets;
        }

        @Override
        public List<SkillsInstallationTarget> resolveTargets(Path workingDirectory, boolean global) {
            this.called = true;
            this.workingDirectory = workingDirectory;
            this.global = global;
            return targets;
        }
    }

    private static final class FailingTargetResolver extends SkillsInstallationTargetResolver {

        private final IllegalStateException failure;
        private boolean called;
        private boolean global;

        private FailingTargetResolver(IllegalStateException failure) {
            this.failure = failure;
        }

        @Override
        public List<SkillsInstallationTarget> resolveTargets(Path workingDirectory, boolean global) {
            this.called = true;
            this.global = global;
            throw failure;
        }
    }

    private static final class RecordingPipeline extends SkillsInstallationPipeline {

        private final SkillsInstallationResult result;
        private boolean called;
        private List<SkillsInstallationTarget> targets;

        private RecordingPipeline(SkillsInstallationResult result) {
            this.result = result;
        }

        @Override
        public SkillsInstallationResult install(List<SkillsInstallationTarget> targets) {
            this.called = true;
            this.targets = targets;
            return result;
        }
    }

    private static final class FailingPipeline extends SkillsInstallationPipeline {

        private final IllegalStateException failure;
        private boolean called;

        private FailingPipeline(IllegalStateException failure) {
            this.failure = failure;
        }

        @Override
        public SkillsInstallationResult install(List<SkillsInstallationTarget> targets) {
            this.called = true;
            throw failure;
        }
    }
}
