package net.md_5.bungee.packet;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = false)
public class PacketCDClientStatus extends Packet
{

    /**
     * Sent from the client to the server upon respawn,
     *
     * @param payload 0 if initial spawn, 1 if respawn after death.
     */
    public PacketCDClientStatus(byte payload)
    {
        super(0xCD);
        writeByte(payload);
    }
}
