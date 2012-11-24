package net.md_5.bungee.packet;

import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public class Packet1Login extends Packet
{

    public int entityId;
    public String levelType;
    public byte gameMode;
    public byte dimension;
    public byte difficulty;
    public byte unused;
    public byte maxPlayers;

    public Packet1Login(int entityId, String levelType, byte gameMode, byte dimension, byte difficulty, byte unused, byte maxPlayers)
    {
        super(0x01);
        writeInt(entityId);
        writeString(levelType);
        writeByte(gameMode);
        writeByte(dimension);
        writeByte(difficulty);
        writeByte(unused);
        writeByte(maxPlayers);
    }

    public Packet1Login(ByteBuf buf)
    {
        super(0x01, buf);
        this.entityId = readInt();
        this.levelType = readString();
        this.gameMode = readByte();
        this.dimension = readByte();
        this.difficulty = readByte();
        this.unused = readByte();
        this.maxPlayers = readByte();
    }
}
