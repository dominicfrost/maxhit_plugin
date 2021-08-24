/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Psikoi <https://github.com/psikoi>
 * Copyright (c) 2019, Bram91 <https://github.com/bram91>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.dominicfrost.maxhit_plugin;


import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemMapping;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ComboBoxListRenderer;
import net.runelite.client.util.Text;
import net.runelite.http.api.item.ItemStats;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static net.runelite.api.Experience.getLevelForXp;
import static net.runelite.api.Skill.STRENGTH;

@PluginDescriptor(
        name = "MaxHitPanel",
        description = "Calculates the max hit based on your currently equipped gear"
)
@Slf4j
public class MaxHitPanel extends PluginPanel {
    @Inject
    private MaxHitConfig config;

    private final JLabel maxHitField;

    // gear set bonuses
    private boolean voidSetEquipped = false;
    private boolean inquisitorsSetEquipped = false;
    private boolean salveEquipped = false;
    private boolean salveEEquipped = false;
    private boolean slayerHelmEquipped = false;
    private boolean dragonHunterLandEquipped = false;

    // monster types
    private boolean vsSlayer = false;
    private boolean vsDragon = false;
    private boolean vsUndead = false;

    // bonus types
    private PotionBonus potionBonus = PotionBonus.None;

    enum PotionBonus {
        None,
        Strength,
        Super_Strength,
        Zamorack_Brew,
        Overload_Minus,
        Overload,
        Overload_Plus
    }

    private PrayerMultiplier prayerMultiplier = PrayerMultiplier.None;

    enum PrayerMultiplier {
        None,
        Burst_Of_Strength,
        Superhuman_Strength,
        Ultimate_Strength,
        Chivalry,
        Piety,
    }

    private AttackStyle attackStyle = AttackStyle.Aggressive;

    enum AttackStyle {
        Aggressive,
        Controlled,
        Accurate,
        Defensive
    }

    private ArrayList<ItemStats> equippedItems = new ArrayList<>();

    private final Client client;

    private final ItemManager itemManager;

    private final ClientThread clientThread;

    public MaxHitPanel(Client client, ItemManager itemManager, ClientThread clientThread) {
        this.client = client;
        this.itemManager = itemManager;
        this.clientThread = clientThread;

        final JPanel container = new JPanel();
        container.setLayout(new GridBagLayout());
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(0, 0, 10, 0);
        c.gridy = 0;
        c.gridx = 0;

        container.add(buildPotionBonusComponent(), c);

        c.gridy++;
        container.add(buildPrayerMultiplierComponent(), c);

        c.gridy++;
        container.add(buildOtherMultipliersComponent(), c);

        maxHitField = new JLabel("not calculated");

        c.gridy++;
        container.add(maxHitField, c);

        c.gridy = 0;
        add(container, c);
    }

    private JPanel buildPotionBonusComponent() {
        final JPanel container = new JPanel();
        container.setLayout(new GridBagLayout());

        final JLabel uiLabel = new JLabel("Potion");
        final JComboBox<Enum<PotionBonus>> box = new JComboBox<>(PotionBonus.values());

        box.setRenderer(new ComboBoxListRenderer<>());
        box.setPreferredSize(new Dimension(box.getPreferredSize().width, 25));
        box.setForeground(Color.WHITE);
        box.setFocusable(false);

        box.setSelectedItem(potionBonus);
        box.setToolTipText(Text.titleCase(potionBonus));

        box.addItemListener(e ->
        {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                potionBonus = (PotionBonus) box.getSelectedItem();
                assert potionBonus != null;
                box.setToolTipText(Text.titleCase(potionBonus));
                updateMaxHit();
            }
        });

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        container.add(uiLabel, c);

        c.gridy++;
        container.add(box, c);

        return container;
    }

    private JPanel buildPrayerMultiplierComponent() {
        final JPanel container = new JPanel();
        container.setLayout(new GridBagLayout());

        final JLabel uiLabel = new JLabel("Prayer");
        final JComboBox<Enum<PrayerMultiplier>> box = new JComboBox<>(PrayerMultiplier.values());

        box.setRenderer(new ComboBoxListRenderer<>());
        box.setPreferredSize(new Dimension(box.getPreferredSize().width, 25));
        box.setForeground(Color.WHITE);
        box.setFocusable(false);

        box.setSelectedItem(prayerMultiplier);
        box.setToolTipText(Text.titleCase(prayerMultiplier));

        box.addItemListener(e ->
        {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                prayerMultiplier = (PrayerMultiplier) box.getSelectedItem();
                assert prayerMultiplier != null;
                box.setToolTipText(Text.titleCase(prayerMultiplier));
                updateMaxHit();
            }
        });

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        container.add(uiLabel, c);

        c.gridy++;
        container.add(box, c);

        return container;
    }

    private JPanel buildOtherMultipliersComponent() {
        final JPanel container = new JPanel();
        container.setLayout(new GridBagLayout());

        final JLabel uiLabel = new JLabel("Other Multipliers");
        final JCheckBox checkbox = new JCheckBox();

        checkbox.setLayout(new GridLayout(3, 1));
        JCheckBoxMenuItem slayer = new JCheckBoxMenuItem("On Slayer Task");
        slayer.addItemListener(e -> {
            vsSlayer = e.getStateChange() == ItemEvent.SELECTED;
            updateMaxHit();
        });
        checkbox.add(slayer);

        JCheckBoxMenuItem dragon = new JCheckBoxMenuItem("Vs Dragon");
        dragon.addItemListener(e -> {
            vsDragon = e.getStateChange() == ItemEvent.SELECTED;
            updateMaxHit();
        });
        checkbox.add(dragon);

        JCheckBoxMenuItem undead = new JCheckBoxMenuItem("Vs Undead");
        undead.addItemListener(e -> {
            vsUndead = e.getStateChange() == ItemEvent.SELECTED;
            updateMaxHit();
        });
        checkbox.add(undead);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = 0;

        container.add(uiLabel, c);
        c.gridy++;

        container.add(checkbox, c);

        return container;
    }

    @Subscribe
    public void onItemContainerChanged(final ItemContainerChanged event)
    {
        ItemContainer itemContainer = event.getItemContainer();
        if (event.getItemContainer() == client.getItemContainer(InventoryID.EQUIPMENT))
        {
            Item[] items = itemContainer.getItems();

            boolean voidHelmEquipped = Arrays.stream(items)
                    .map(Item::getId)
                    .anyMatch(id -> Arrays.stream(new int[]{
                            ItemID.VOID_MELEE_HELM,
                            ItemID.VOID_MELEE_HELM_BROKEN,
                            ItemID.VOID_MELEE_HELM_L,
                    }).anyMatch(i -> i == id));

            boolean voidTopEquipped = Arrays.stream(items)
                    .map(Item::getId)
                    .anyMatch(id -> Arrays.stream(new int[]{
                           ItemID.ELITE_VOID_TOP,
                           ItemID.ELITE_VOID_TOP_BROKEN,
                           ItemID.ELITE_VOID_TOP_L,
                           ItemID.VOID_KNIGHT_TOP,
                           ItemID.VOID_KNIGHT_TOP_BROKEN,
                           ItemID.VOID_KNIGHT_TOP_L,
                    }).anyMatch(i -> i == id));

            boolean voidBottomEquipped =  Arrays.stream(items)
                    .map(Item::getId)
                    .anyMatch(id -> Arrays.stream(new int[]{
                            ItemID.ELITE_VOID_ROBE,
                            ItemID.ELITE_VOID_ROBE_BROKEN,
                            ItemID.ELITE_VOID_ROBE_L,
                            ItemID.VOID_KNIGHT_ROBE,
                            ItemID.VOID_KNIGHT_ROBE_BROKEN,
                            ItemID.VOID_KNIGHT_ROBE_L,
                    }).anyMatch(i -> i == id));

            boolean voidGlovesEquipped = Arrays.stream(items)
                    .map(Item::getId)
                    .anyMatch(id -> Arrays.stream(new int[]{
                            ItemID.VOID_KNIGHT_GLOVES,
                            ItemID.VOID_KNIGHT_GLOVES_BROKEN,
                            ItemID.VOID_KNIGHT_GLOVES_L,
                    }).anyMatch(i -> i == id));

            voidSetEquipped = voidHelmEquipped && voidTopEquipped && voidBottomEquipped && voidGlovesEquipped;

            // TODO
            inquisitorsSetEquipped = false;

            salveEquipped = Arrays.stream(items)
                    .map(Item::getId)
                    .anyMatch(id -> Arrays.stream(new int[]{
                            ItemID.SALVE_AMULET,
                            ItemID.SALVE_AMULETI,
                    }).anyMatch(i -> i == id));

            salveEEquipped = Arrays.stream(items)
                    .map(Item::getId)
                    .anyMatch(id -> Arrays.stream(new int[]{
                            ItemID.SALVE_AMULET_E,
                            ItemID.SALVE_AMULETEI,
                            ItemID.SALVE_AMULETEI_25278,
                    }).anyMatch(i -> i == id));

            slayerHelmEquipped = Arrays.stream(items)
                    .map(Item::getId)
                    .anyMatch(id -> {
                        Collection<ItemMapping> coll = ItemMapping.map(id);
                        return coll != null && coll.contains(ItemMapping.ITEM_BLACK_MASK);
                    });

            dragonHunterLandEquipped = Arrays.stream(items)
                    .map(Item::getId)
                    .anyMatch(id -> Arrays.stream(new int[]{
                            ItemID.DRAGON_HUNTER_LANCE,
                    }).anyMatch(i -> i == id));;

            clientThread.invoke(() -> {
                equippedItems.clear();
                for (Item item : items) {
                    if (item.getId() == -1) continue;
                    equippedItems.add(itemManager.getItemStats(item.getId(), false));
                }
                updateMaxHit();
            });
        }
    }


    private void updateMaxHit() {
        maxHitField.setText("Max Hit: " + calculateMaxHit(getStrengthLevel(), getStrengthBonus()));
    }

    // sourced from https://oldschool.runescape.wiki/w/Maximum_melee_hit
    private int calculateMaxHit(int strengthLevel, int strengthBonus) {
        final int effectiveStrength = (int) (((strengthLevel + getPotionBonus(strengthLevel)) * getPrayerMultiplier() * getOtherMultipliers()) + getStyleBonus());

        return (int) (1.3 + (effectiveStrength / 10) + (strengthBonus / 80) + ((effectiveStrength * strengthBonus) / 640));
    }

    private int getStrengthBonus() {
        if (equippedItems == null || equippedItems.isEmpty()) {
            return 0;
        }

        return equippedItems.stream()
                .map(i -> i.getEquipment().getStr())
                .reduce(0, Integer::sum);
    }

    private int getStrengthLevel() {
        int strengthXp = client.getSkillExperience(STRENGTH);
        return getLevelForXp(strengthXp);
    }

    private int getPotionBonus(int strengthLevel) {
        switch (potionBonus) {
            case Strength:
                return 3 + (int) (strengthLevel * .1);
            case Zamorack_Brew:
                return 2 + (int) (strengthLevel * .12);
            case Super_Strength:
                return 5 + (int) (strengthLevel * .15);
            case Overload_Minus:
                return 4 + (int) (strengthLevel * .1);
            case Overload:
                return 5 + (int) (strengthLevel * .13);
            case Overload_Plus:
                return 6 + (int) (strengthLevel * .16);
            default:
                return 0;
        }
    }

    private int getStyleBonus() {
        switch (attackStyle) {
            case Aggressive:
                return 3;
            case Accurate:
                return 1;
            default:
                return 0;
        }
    }

    private double getPrayerMultiplier() {
        switch (prayerMultiplier) {
            case Burst_Of_Strength:
                return 1.05;
            case Superhuman_Strength:
                return 1.1;
            case Ultimate_Strength:
                return 1.15;
            case Chivalry:
                return 1.18;
            case Piety:
                return 1.25;
            default:
                return 1;
        }
    }

    private double getOtherMultipliers() {
        double multiplier = 1.0;

        if (voidSetEquipped) {
            multiplier *= 1.1;
        }

        if (inquisitorsSetEquipped) {
            multiplier *= 1.025;
        }

        // these items do not stack, so only apply one multiplier
        if (slayerHelmEquipped || salveEEquipped || salveEquipped) {
            double maxMultiplier = 1.0;

            if (salveEEquipped && vsUndead) {
                maxMultiplier = Double.max(maxMultiplier, 1.2);
            }

            if (salveEquipped && vsUndead) {
                maxMultiplier = Double.max(maxMultiplier, 1.15);
            }

            if (slayerHelmEquipped && vsSlayer) {
                maxMultiplier = Double.max(maxMultiplier, 7.0 / 6);
            }

            multiplier *= maxMultiplier;
        }

        if (dragonHunterLandEquipped && vsDragon) {
            multiplier *= 1.2;
        }

        return multiplier;
    }
}
