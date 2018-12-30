package com.energyxxer.nbtmapper;

import com.energyxxer.nbtmapper.tags.PathProtocol;

public class PathContext {
    private boolean setting = false;
    private PathProtocol protocol = PathProtocol.DEFAULT;
    private Object protocolMetadata = null;


    public PathContext setIsSetting(boolean setting) {
        this.setting = setting;
        return this;
    }

    public boolean isSetting() {
        return setting;
    }

    public PathProtocol getProtocol() {
        return protocol;
    }

    public PathContext setProtocol(PathProtocol protocol) {
        this.protocol = protocol;
        return this;
    }

    public PathContext setProtocol(PathProtocol protocol, Object metadata) {
        this.protocol = protocol;
        this.protocolMetadata = metadata;
        return this;
    }

    public Object getProtocolMetadata() {
        return protocolMetadata;
    }

    public PathContext setProtocolMetadata(Object protocolMetadata) {
        this.protocolMetadata = protocolMetadata;
        return this;
    }
}
