package org.leeminkan.redis;

public enum CommandType {
    GET((byte) 1),
    SET((byte) 2),
    CAS((byte) 3); // Compare-And-Swap (The "Banking" feature)

    private final byte code;

    CommandType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static CommandType fromByte(byte code) {
        for (CommandType type : values()) {
            if (type.code == code) return type;
        }
        return null;
    }
}