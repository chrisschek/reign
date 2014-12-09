package io.reign.lease;

public class LeaseEvent {

    private LeaseEventType type;
    private Lease lease;

    public LeaseEvent(LeaseEventType type, Lease lease) {
        this.type = type;
        this.lease = lease;
    }

    public LeaseEventType getType() {
        return type;
    }

    public Lease getLease() {
        return lease;
    }

    public static enum LeaseEventType {
        ACQUIRED, RENEWED, RELEASED, REVOKED;
    }

}
