package me.lssupportteam.ipdynamic.models;

public class BanEntry {

    private String pattern; // IP o patrón (127.0.0.* o 127.0.*.*)
    private String reason;
    private String bannedBy;
    private long bannedAt;
    private BanType banType;
    private boolean isActive;
    private long expiresAt; // 0 = permanente
    private int affectedCount; // Cantidad de IPs afectadas

    public enum BanType {
        SINGLE("single"),     // IP exacta
        OP1("op1"),          // Un wildcard (256 IPs)
        OP2("op2"),          // Dos wildcards (65,536 IPs)
        OP3("op3"),          // Tres wildcards (16,777,216 IPs)
        CUSTOM("custom");    // Patrón personalizado

        private final String type;

        BanType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public static BanType fromPattern(String pattern) {
            if (pattern == null) return SINGLE;

            long wildcards = pattern.chars().filter(ch -> ch == '*').count();
            switch ((int) wildcards) {
                case 0:
                    return SINGLE;
                case 1:
                    return OP1;
                case 2:
                    return OP2;
                case 3:
                    return OP3;
                default:
                    return CUSTOM;
            }
        }
    }

    public BanEntry(String pattern, String reason, String bannedBy) {
        this.pattern = pattern;
        this.reason = reason;
        this.bannedBy = bannedBy;
        this.bannedAt = System.currentTimeMillis();
        this.banType = BanType.fromPattern(pattern);
        this.isActive = true;
        this.expiresAt = 0; // Permanente por defecto
        this.affectedCount = calculateAffectedCount();
    }

    public BanEntry() {

    }

    private int calculateAffectedCount() {
        switch (banType) {
            case SINGLE:
                return 1;
            case OP1:
                return 256;
            case OP2:
                return 65536;
            case OP3:
                return 16777216;
            default:
                return 0;
        }
    }

    public boolean isExpired() {
        return expiresAt > 0 && System.currentTimeMillis() > expiresAt;
    }

    public boolean isPermanent() {
        return expiresAt == 0;
    }

    public boolean isLargeBan() {
        return banType == BanType.OP2 || banType == BanType.OP3;
    }

    public String getDurationString() {
        if (isPermanent()) {
            return "Permanente";
        }

        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0) {
            return "Expirado";
        }

        long days = remaining / (1000 * 60 * 60 * 24);
        long hours = (remaining % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60);

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }


    public String getPattern() { return pattern; }
    public void setPattern(String pattern) {
        this.pattern = pattern;
        this.banType = BanType.fromPattern(pattern);
        this.affectedCount = calculateAffectedCount();
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getBannedBy() { return bannedBy; }
    public void setBannedBy(String bannedBy) { this.bannedBy = bannedBy; }

    public long getBannedAt() { return bannedAt; }
    public void setBannedAt(long bannedAt) { this.bannedAt = bannedAt; }

    public BanType getBanType() { return banType; }
    public void setBanType(BanType banType) { this.banType = banType; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public int getAffectedCount() { return affectedCount; }
    public void setAffectedCount(int affectedCount) { this.affectedCount = affectedCount; }
}