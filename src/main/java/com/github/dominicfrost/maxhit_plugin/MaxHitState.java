package com.github.dominicfrost.maxhit_plugin;

import lombok.Data;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemMapping;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

enum PotionBonus {
    None,
    Strength,
    Super_Strength,
    Zamorack_Brew,
    Overload_Minus,
    Overload,
    Overload_Plus
}

enum PrayerMultiplier {
    None,
    Burst_Of_Strength,
    Superhuman_Strength,
    Ultimate_Strength,
    Chivalry,
    Piety,
}

enum AttackStyle {
    Accurate,
    Aggressive,
    Controlled,
    Defensive,
}

@Data
public class MaxHitState {
    private Item[] equippedItems = new Item[]{};
    private int StrengthLevel = 1;

    private boolean vsDragons = false;
    private boolean vsUndead = false;
    private boolean vsSlayer = false;

    private PotionBonus potionBonus = PotionBonus.None;
    private PrayerMultiplier prayerMultiplier = PrayerMultiplier.None;
    private AttackStyle attackStyle = AttackStyle.Aggressive;

    public int getStrengthBonus(ItemManager itemManager) {
        return Arrays.stream(getEquippedItems())
                .filter(item -> item.getId() != -1)
                .map(item -> itemManager.getItemStats(item.getId(), false))
                .filter(Objects::nonNull)
                .map(i -> i.getEquipment().getStr())
                .reduce(0, Integer::sum);
    }

    public boolean voidSetEquipped() {
        return voidHelmEquipped() && voidTopEquipped() && voidBottomEquipped() && voidGlovesEquipped();
    }

    private boolean voidHelmEquipped() {
        return  Arrays.stream(getEquippedItems())
                .map(Item::getId)
                .anyMatch(id -> Arrays.stream(new int[]{
                        ItemID.VOID_MELEE_HELM,
                        ItemID.VOID_MELEE_HELM_BROKEN,
                        ItemID.VOID_MELEE_HELM_L,
                }).anyMatch(i -> i == id));
    }

    private boolean voidTopEquipped() {
        return Arrays.stream(getEquippedItems())
                .map(Item::getId)
                .anyMatch(id -> Arrays.stream(new int[]{
                        ItemID.ELITE_VOID_TOP,
                        ItemID.ELITE_VOID_TOP_BROKEN,
                        ItemID.ELITE_VOID_TOP_L,
                        ItemID.VOID_KNIGHT_TOP,
                        ItemID.VOID_KNIGHT_TOP_BROKEN,
                        ItemID.VOID_KNIGHT_TOP_L,
                }).anyMatch(i -> i == id));
    }

    private boolean voidBottomEquipped() {
        return Arrays.stream(getEquippedItems())
                .map(Item::getId)
                .anyMatch(id -> Arrays.stream(new int[]{
                        ItemID.ELITE_VOID_ROBE,
                        ItemID.ELITE_VOID_ROBE_BROKEN,
                        ItemID.ELITE_VOID_ROBE_L,
                        ItemID.VOID_KNIGHT_ROBE,
                        ItemID.VOID_KNIGHT_ROBE_BROKEN,
                        ItemID.VOID_KNIGHT_ROBE_L,
                }).anyMatch(i -> i == id));
    }

    private boolean voidGlovesEquipped() {
        return Arrays.stream(getEquippedItems())
                .map(Item::getId)
                .anyMatch(id -> Arrays.stream(new int[]{
                        ItemID.VOID_KNIGHT_GLOVES,
                        ItemID.VOID_KNIGHT_GLOVES_BROKEN,
                        ItemID.VOID_KNIGHT_GLOVES_L,
                }).anyMatch(i -> i == id));
    }

    public boolean salveEquipped() {
        return Arrays.stream(getEquippedItems())
                .map(Item::getId)
                .anyMatch(id -> Arrays.stream(new int[]{
                        ItemID.SALVE_AMULET,
                        ItemID.SALVE_AMULETI,
                }).anyMatch(i -> i == id));
    }

    public boolean salveEEquipped() {
        return Arrays.stream(getEquippedItems())
                .map(Item::getId)
                .anyMatch(id -> Arrays.stream(new int[]{
                        ItemID.SALVE_AMULET_E,
                        ItemID.SALVE_AMULETEI,
                        ItemID.SALVE_AMULETEI_25278,
                }).anyMatch(i -> i == id));
    }

    public boolean slayerHelmEquipped() {
        return Arrays.stream(getEquippedItems())
                .map(Item::getId)
                .anyMatch(id -> {
                    Collection<ItemMapping> coll = ItemMapping.map(id);
                    return coll != null && coll.contains(ItemMapping.ITEM_BLACK_MASK);
                });
    }

    public boolean dragonHunterLanceEquipped() {
        return Arrays.stream(getEquippedItems())
                .map(Item::getId)
                .anyMatch(id -> Arrays.stream(new int[]{
                        ItemID.DRAGON_HUNTER_LANCE,
                }).anyMatch(i -> i == id));
    }

    public boolean inquisitorsSetEquipped() {
        // TODO: implement
        return false;
    }
}