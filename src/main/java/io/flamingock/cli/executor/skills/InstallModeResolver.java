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

/**
 * Resolves the requested skills installation mode from CLI flags.
 */
public class InstallModeResolver {

    /**
     * Resolves the requested installation mode.
     *
     * @param globalRequested whether the global flag was requested
     * @return the resolved install mode
     */
    public InstallMode resolve(boolean globalRequested) {
        return globalRequested ? InstallMode.GLOBAL : InstallMode.LOCAL;
    }
}
