package com.jacopomii.gappsmod.data;

public class Version implements Comparable<Version> {
    private final String mVersion;

    public Version(String version) {
        if (version == null)
            throw new IllegalArgumentException("Version can not be null");
        if (!version.matches("\\d+(\\.\\d+)*"))
            throw new IllegalArgumentException("Invalid version format");
        mVersion = version;
    }

    public final String getVersion() {
        return mVersion;
    }

    @Override
    public int compareTo(Version that) {
        if(that == null)
            return 1;
        String[] thisParts = this.getVersion().split("\\.");
        String[] thatParts = that.getVersion().split("\\.");
        int length = Math.max(thisParts.length, thatParts.length);
        for(int i = 0; i < length; i++) {
            int thisPart = i < thisParts.length ?
                    Integer.parseInt(thisParts[i]) : 0;
            int thatPart = i < thatParts.length ?
                    Integer.parseInt(thatParts[i]) : 0;
            if(thisPart < thatPart)
                return -1;
            if(thisPart > thatPart)
                return 1;
        }
        return 0;
    }
}
