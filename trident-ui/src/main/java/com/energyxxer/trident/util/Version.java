package com.energyxxer.trident.util;

/**
 * Created by User on 1/21/2017.
 */
public class Version {
    public final String gen;
    public final int major;
    public final int minor;
    public final int patch;

    public Version(int major, int minor, int patch) {
        this(null, major, minor, patch);
    }

    public Version(String gen, int major, int minor, int patch) {
        this.gen = gen;
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public int compare(Version v) {
        if(this.major - v.major != 0) return this.major - v.major;
        if(this.minor - v.minor != 0) return this.minor - v.minor;
        return this.patch - v.patch;
    }

    @Override
    public String toString() {
        return (gen != null) ? String.format("%d.%d.%d-%s",major, minor, patch, gen) : String.format("%d.%d.%d", major, minor, patch);
    }
}
