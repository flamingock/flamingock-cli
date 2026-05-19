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

import io.flamingock.internal.common.core.response.data.ErrorInfo;
import io.flamingock.internal.common.core.response.data.PipelineAbortedOutcome;

/**
 * Formats {@link PipelineAbortedOutcome} for CLI output.
 */
public final class PipelineAbortedFormatter {

    private static final String SEPARATOR = "--------------------------------------------------------------------------------";

    private PipelineAbortedFormatter() {
    }

    public static String format(PipelineAbortedOutcome outcome) {
        StringBuilder sb = new StringBuilder("\n");
        sb.append(SEPARATOR).append("\n");
        sb.append("PIPELINE ABORTED").append("\n");
        sb.append(SEPARATOR).append("\n");
        sb.append(String.format("  Reason:     %s%n", outcome.getReason()));
        sb.append(String.format("  Duration:   %sms%n", outcome.getTotalDurationMs()));

        ErrorInfo error = outcome.getError();
        if (error != null) {
            if (error.getErrorType() != null) {
                sb.append(String.format("  Type:       %s%n", error.getErrorType()));
            }
            if (error.getMessage() != null) {
                sb.append(String.format("  Message:    %s%n", error.getMessage()));
            }
        }

        sb.append(SEPARATOR).append("\n");
        return sb.toString();
    }

    public static void print(PipelineAbortedOutcome outcome) {
        System.out.print(format(outcome));
    }
}
