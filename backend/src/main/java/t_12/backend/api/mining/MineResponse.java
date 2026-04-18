package t_12.backend.api.mining;

public class MineResponse {

    private final int clickCount;
    private final long cooldownRemaining;
    private final String message;

    public MineResponse(int clickCount, long cooldownRemaining, String message) {
        this.clickCount = clickCount;
        this.cooldownRemaining = cooldownRemaining;
        this.message = message;
    }

    public int getClickCount() {
        return clickCount;
    }

    public long getCooldownRemaining() {
        return cooldownRemaining;
    }

    public String getMessage() {
        return message;
    }
}
