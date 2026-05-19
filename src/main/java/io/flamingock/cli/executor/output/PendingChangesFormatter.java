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
package io.flamingock.cli.executor.output;

import io.flamingock.internal.common.core.response.data.PendingChangesOutcome;

/**
 * Formats {@link PendingChangesOutcome} for CLI output.
 */
public final class PendingChangesFormatter {

    private static final String SEPARATOR = "--------------------------------------------------------------------------------";

    private PendingChangesFormatter() {
    }

    public static String format(PendingChangesOutcome outcome) {
        StringBuilder sb = new StringBuilder("\n");
        sb.append(SEPARATOR).append("\n");
        sb.append("PENDING CHANGES DETECTED").append("\n");
        sb.append(SEPARATOR).append("\n");
        if (outcome.getMessage() != null) {
            sb.append(String.format("  Message:    %s%n", outcome.getMessage()));
        }
        if (outcome.getTimestamp() != null) {
            sb.append(String.format("  Detected:   %s%n", outcome.getTimestamp()));
        }
        sb.append(SEPARATOR).append("\n");
        return sb.toString();
    }

    public static void print(PendingChangesOutcome outcome) {
        System.out.print(format(outcome));
    }
}
