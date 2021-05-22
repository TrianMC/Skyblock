package io.github.trianmc.skyblock.economy;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.trianmc.skyblock.Skyblock;
import io.github.trianmc.skyblock.util.IOUtils;
import lombok.SneakyThrows;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.*;

import static io.github.trianmc.skyblock.util.PlayerUtils.getUUID;
import static net.milkbowl.vault.economy.EconomyResponse.ResponseType.NOT_IMPLEMENTED;

public class SkyEconomy extends AbstractEconomy {

    private final Skyblock host;
    private final Map<UUID, Double> balances = new HashMap<>();

    public SkyEconomy(Skyblock host) {
        this.host = host;
    }

    @Override
    public boolean isEnabled() {
        return host.isEnabled();
    }

    @Override
    public String getName() {
        return "Trian Economy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 0;
    }

    @Override
    public String format(double amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(host.getLang().getLocale());
        nf.setMinimumFractionDigits(fractionalDigits());
        nf.setMaximumFractionDigits(fractionalDigits());
        return nf.format(amount);
    }

    @Override
    public String currencyNamePlural() {
        return PlainComponentSerializer.plain().serialize(host.getLang().get("currency.simple_plural"));
    }

    @Override
    public String currencyNameSingular() {
        return PlainComponentSerializer.plain().serialize(host.getLang().get("currency.singular"));
    }

    @Override
    public boolean hasAccount(String playerName) {
        return balances.containsKey(getUUID(playerName));
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public double getBalance(String playerName) {
        return balances.getOrDefault(getUUID(playerName), 0D);
    }

    @Override
    public double getBalance(String playerName, String world) {
        return balances.get(getUUID(playerName));
    }

    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        if (!has(playerName, amount)) return new EconomyResponse(
                0,                                    // Change in balance
                getBalance(playerName),               // New balance
                EconomyResponse.ResponseType.FAILURE, // Exit code
                host.getLang().getString(
                        "economy.missing_money", // Error message
                        playerName
                ));

        AtomicDouble diff = new AtomicDouble();
        balances.computeIfPresent(getUUID(playerName), (uuid, amt) -> {
            double newAmt = amt - amount;
            diff.set(amount - newAmt);
            return newAmt;
        });

        return new EconomyResponse(
                amount,
                getBalance(playerName),
                EconomyResponse.ResponseType.SUCCESS,
                null
        );
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        balances.compute(getUUID(playerName), (uuid, amt) -> {
            if (amt == null) return amount;
            return amt + amount;
        });

        return new EconomyResponse(
                amount,
                getBalance(playerName),
                EconomyResponse.ResponseType.SUCCESS,
                null
        );
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return unsupported(player);
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return unsupported();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return unsupported();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return unsupported();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return unsupported();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return unsupported();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return unsupported();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return unsupported();
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        UUID uuid;
        boolean b = balances.containsKey(uuid = getUUID(playerName));
        balances.putIfAbsent(uuid, 0D);
        return !b;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    private EconomyResponse unsupported(String playerName) {
        return new EconomyResponse(0, getBalance(playerName), NOT_IMPLEMENTED, "Unsupported operation");
    }

    private EconomyResponse unsupported() {
        return new EconomyResponse(0, 0, NOT_IMPLEMENTED, "Unsupported operation");
    }

    @SneakyThrows(IOException.class)
    public int write(OutputStream stream) {
        int size = 0;
        size += IOUtils.writeInt(stream, balances.size());
        for (Map.Entry<UUID, Double> balance : balances.entrySet()) {
            size += IOUtils.writeUUID(stream, balance.getKey());
            size += IOUtils.writeDouble(stream, balance.getValue());
        }
        return size;
    }

    @SneakyThrows(IOException.class)
    public static SkyEconomy read(Skyblock host, InputStream stream) {
        SkyEconomy economy = new SkyEconomy(host);
        int ct = IOUtils.readInt(stream);
        for (int i = 0; i < ct; i++) {
            economy.balances.put(
                    IOUtils.readUUID(stream),
                    IOUtils.readDouble(stream)
            );
        }

        return economy;
    }
}
