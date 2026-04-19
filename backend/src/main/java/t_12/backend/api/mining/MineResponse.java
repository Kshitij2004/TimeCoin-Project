package t_12.backend.api.mining;

public class MineResponse {

    private final int clickCount;
    private final int cooldownRemaining;
    private final String message;

    public MineResponse(int clickCount, int cooldownRemaining, String message) {
        this.clickCount = clickCount;
        this.cooldownRemaining = cooldownRemaining;
        this.message = message;
    }

    public int getClickCount() {
        return clickCount;
    }

    public int getCooldownRemaining() {
        return cooldownRemaining;
    }

    public String getMessage() {
        return message;
    }
}
