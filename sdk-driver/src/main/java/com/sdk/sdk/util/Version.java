package com.sdk.sdk.util;

import java.util.Objects;
import java.util.Optional;

public class Version {
    public final int major;
    public final int minor;
    public final int patch;
    public final Optional<Integer> increment;

    public Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.increment = Optional.empty();
    }

    public Version(int major, int minor, int patch, int increment) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.increment = Optional.of(increment);
    }

    // "1.1.2" or "1.1.2-1234"
    public static Version fromString(String in) {
        String[] split = in.split("\\.");
        String[] split2 = split[2].split("-");
        if (split2.length > 1) {
            return new Version(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split2[0]), Integer.parseInt(split2[1]));
        }
        else {
            return new Version(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
        }
    }

    public boolean isBelow(Version other) {
        if (major < other.major) return true;
        if (major > other.major) return false;
        if (minor < other.minor) return true;
        if (minor > other.minor) return false;
        if (increment.isPresent() && other.increment.isPresent()) {
            if (patch < other.patch) return true;
            if (patch > other.patch) return false;
            if (increment.get() < other.increment.get()) return true;
            if (increment.get() > other.increment.get()) return false;
            return false;
        }
        else {
            return patch < other.patch;
        }
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch + increment.map(v -> "-" + v).orElse("");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return major == version.major &&
                minor == version.minor &&
                patch == version.patch &&
                // We're not testing true object equality here - we only care about version equality on the increment
                // field if both versions have that present
                (!increment.isPresent() || !version.increment.isPresent() || increment.equals(version.increment));
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, increment);
    }
}
