package bgprotobg.net.rankmultipliers.storage;

public class MultiplierData {
    private final String playerName;
    private final String currency;
    private final double amount;
    private final long durationLeft;

    public MultiplierData(String playerName, String currency, double amount, long durationLeft) {
        this.playerName = playerName;
        this.currency = currency;
        this.amount = amount;
        this.durationLeft = durationLeft;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getCurrency() {
        return currency;
    }

    public double getAmount() {
        return amount;
    }

    public long getDurationLeft() {
        return durationLeft;
    }
}
