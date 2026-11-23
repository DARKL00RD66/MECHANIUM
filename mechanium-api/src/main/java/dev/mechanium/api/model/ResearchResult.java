package dev.mechanium.api.model;

import java.util.Objects;

/** Result of starting a timed research job. */
public record ResearchResult(Status status, String message) {
    public enum Status {
        STARTED,
        ALREADY_RESEARCHING,
        ALREADY_UNLOCKED,
        UNKNOWN_TECHNOLOGY,
        UNKNOWN_PLAYER_ERA,
        WRONG_ERA,
        MISSING_PREREQUISITES,
        INSUFFICIENT_RESEARCH_POINTS,
        CONCURRENT_UPDATE,
        UNSUPPORTED
    }

    public ResearchResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(message, "message");
    }

    public boolean successful() {
        return status == Status.STARTED;
    }

    public static ResearchResult of(Status status, String message) {
        return new ResearchResult(status, message);
    }
}
