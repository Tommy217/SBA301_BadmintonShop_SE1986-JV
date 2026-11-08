package fu.se.badmintonshop.modules.auth.enums;

public enum UserStatus {
    ACTIVE("active"),
    LOCKED("locked");

    private final String value;

    UserStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserStatus fromString(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("UserStatus cannot be null or blank");
        }
        for (UserStatus b : UserStatus.values()) {
            if (b.value.equalsIgnoreCase(text.trim()) || b.name().equalsIgnoreCase(text.trim())) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unknown user status: " + text);
    }
}
