package io.github.trianmc.skyblock.members;

import io.github.trianmc.skyblock.util.IOUtils;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

public class Members {
    private final UUID owner;
    private final ArrayList<UUID> peers = new ArrayList<>();
    public Members(UUID owner) {
        this.owner = owner;
    }

    @SneakyThrows(IOException.class)
    public static Members read(InputStream stream) {
        UUID owner = IOUtils.readUUID(stream);
        Members members = new Members(owner);
        int count = IOUtils.readInt(stream);
        for (int i = 0; i < count; i++) {
            members.peers.add(IOUtils.readUUID(stream));
        }

        return members;
    }

    public UUID getOwner() {
        return owner;
    }

    public Members addPeer(UUID uuid) {
        peers.add(uuid);
        return this;
    }

    public Members addPeers(UUID... uuids) {
        peers.addAll(Arrays.asList(uuids));
        return this;
    }

    public Members addPeers(Collection<UUID> uuids) {
        peers.addAll(uuids);
        return this;
    }

    public Rights getRights(UUID uuid) {
        if (owner.equals(uuid)) return Rights.OWNER;
        if (peers.contains(uuid)) return Rights.PEER;
        return Rights.VISITOR;
    }

    public boolean hasRights(UUID uuid, Rights rights) {
        Rights trueRights = Rights.VISITOR;
        if (peers.contains(uuid)) trueRights = Rights.PEER;
        if (owner.equals(uuid)) trueRights = Rights.OWNER;
        return trueRights.ordinal() <= rights.ordinal();
    }

    @SneakyThrows(IOException.class)
    public int write(OutputStream stream) {
        int size = 0;
        size += IOUtils.writeUUID(stream, owner);
        size += IOUtils.writeInt(stream, peers.size());
        for (UUID peer : peers) {
            size += IOUtils.writeUUID(stream, peer);
        }

        return size;
    }
}
