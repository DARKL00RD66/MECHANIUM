package dev.mechanium.api.model;

/** Result of a technology unlock request. */
public record UnlockResult(Status status, String message) {
    public enum Status {
        UNLOCKED, ALREADY_UNLOCKED, UNKNOWN_TECHNOLOGY, UNKNOWN_PLAYER_ERA,
        WRONG_ERA, MISSING_PREREQUISITES, INSUFFICIENT_RESEARCH_POINTS, CONCURRENT_UPDATE
    }
    public boolean successful() {
        return status == Status.UNLOCKED || status == Status.ALREADY_UNLOCKED;
    }
    public static UnlockResult of(Status status, String message) { return new UnlockResult(status, message); }
}