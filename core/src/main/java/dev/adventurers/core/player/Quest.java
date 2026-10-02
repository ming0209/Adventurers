package dev.adventurers.core.player;

import dev.adventurers.core.world.Resource;
import java.util.*;

public final class Quest {
    public enum Status { OPEN, CLAIMED, COMPLETED, EXPIRED, COMPETITOR, CANCELLED }
    private final long id, city, deadline;
    private final Resource resource;
    private final int amount;
    private Status status = Status.OPEN;
    private UUID claimant;
    public Quest(long id, long city, Resource resource, int amount, long deadline) {
        if (id < 1 || city < 1 || amount < 1 || deadline < 0) throw new IllegalArgumentException();
        this.id = id; this.city = city; this.resource = Objects.requireNonNull(resource); this.amount = amount; this.deadline = deadline;
    }
    public long id() { return id; }
    public long city() { return city; }
    public Resource resource() { return resource; }
    public int amount() { return amount; }
    public long deadline() { return deadline; }
    public Status status() { return status; }
    public UUID claimant() { return claimant; }
    public boolean active() { return status == Status.OPEN || status == Status.CLAIMED; }
    public void claim(UUID player, long tick) {
        if (status != Status.OPEN || tick >= deadline) throw new IllegalStateException("委托已不可接取");
        claimant = Objects.requireNonNull(player); status = Status.CLAIMED;
    }
    public void complete(UUID player, long tick) {
        if (status != Status.CLAIMED || !player.equals(claimant) || tick >= deadline) throw new IllegalStateException("委托归属或期限无效");
        status = Status.COMPLETED;
    }
    public void close(Status status) {
        if (status == Status.OPEN || status == Status.CLAIMED || status == Status.COMPLETED) throw new IllegalArgumentException();
        if (active()) this.status = status;
    }
    public void restore(Status status, UUID claimant) {
        if (status == Status.CLAIMED && claimant == null) throw new IllegalArgumentException();
        this.status = status; this.claimant = claimant;
    }
}
