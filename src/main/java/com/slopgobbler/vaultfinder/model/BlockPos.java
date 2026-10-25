package com.slopgobbler.vaultfinder.model;

import java.util.Objects;

/**
 * Lightweight, self-contained 3D coordinate model for VaultFinder Pro container indexing.
 * Fully decoupled from Minecraft runtime for fast standalone execution and testing.
 */
public class BlockPos implements Comparable<BlockPos> {

    public static final BlockPos ORIGIN = new BlockPos(0, 0, 0);

    private final int x;
    private final int y;
    private final int z;

    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public BlockPos toImmutable() {
        return this;
    }

    public String toShortString() {
        return x + ", " + y + ", " + z;
    }

    public double distanceSq(BlockPos other) {
        if (other == null) return 0.0;
        return distanceSq(other.x, other.y, other.z);
    }

    public double distanceSq(double targetX, double targetY, double targetZ) {
        double dx = this.x - targetX;
        double dy = this.y - targetY;
        double dz = this.z - targetZ;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distanceTo(BlockPos other) {
        return Math.sqrt(distanceSq(other));
    }

    public double distanceTo(double targetX, double targetY, double targetZ) {
        return Math.sqrt(distanceSq(targetX, targetY, targetZ));
    }

    public int manhattanDistance(BlockPos other) {
        if (other == null) return 0;
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y) + Math.abs(this.z - other.z);
    }

    public BlockPos add(int dx, int dy, int dz) {
        return new BlockPos(this.x + dx, this.y + dy, this.z + dz);
    }

    public BlockPos add(BlockPos other) {
        if (other == null) return this;
        return add(other.x, other.y, other.z);
    }

    public BlockPos subtract(BlockPos other) {
        if (other == null) return this;
        return new BlockPos(this.x - other.x, this.y - other.y, this.z - other.z);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockPos blockPos)) return false;
        return x == blockPos.x && y == blockPos.y && z == blockPos.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public int compareTo(BlockPos o) {
        if (this.y != o.y) return Integer.compare(this.y, o.y);
        if (this.z != o.z) return Integer.compare(this.z, o.z);
        return Integer.compare(this.x, o.x);
    }

    @Override
    public String toString() {
        return "[" + x + ", " + y + ", " + z + "]";
    }
}
