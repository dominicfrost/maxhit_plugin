package com.github.dominicfrost.maxhit_plugin;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ComboBoxListRenderer;
import net.runelite.client.util.Text;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

@Slf4j
public class MaxHitPanel extends PluginPanel {
    private final JLabel maxHitField;

    private final Client client;

    private final ItemManager itemManager;

    private final ClientThread clientThread;

    public final MaxHitState state;

    public MaxHitPanel(Client client, ItemManager itemManager, ClientThread clientThread) {
        this.state = new MaxHitState();
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

        box.setSelectedItem(state.getPotionBonus());
        box.setToolTipText(Text.titleCase(state.getPotionBonus()));

        box.addItemListener(e ->
        {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                state.setPotionBonus((PotionBonus) box.getSelectedItem());
                assert state.getPotionBonus() != null;
                box.setToolTipText(Text.titleCase(state.getPotionBonus()));
                redraw();
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

        box.setSelectedItem(state.getPrayerMultiplier());
        box.setToolTipText(Text.titleCase(state.getPrayerMultiplier()));

        box.addItemListener(e ->
        {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                state.setPrayerMultiplier((PrayerMultiplier) box.getSelectedItem());
                assert state.getPrayerMultiplier() != null;
                box.setToolTipText(Text.titleCase(state.getPrayerMultiplier()));
                redraw();
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
            state.setVsSlayer(e.getStateChange() == ItemEvent.SELECTED);
            redraw();
        });
        checkbox.add(slayer);

        JCheckBoxMenuItem dragon = new JCheckBoxMenuItem("Vs Dragon");
        dragon.addItemListener(e -> {
            state.setVsDragons(e.getStateChange() == ItemEvent.SELECTED);
            redraw();
        });
        checkbox.add(dragon);

        JCheckBoxMenuItem undead = new JCheckBoxMenuItem("Vs Undead");
        undead.addItemListener(e -> {
            state.setVsUndead(e.getStateChange() == ItemEvent.SELECTED);
            redraw();
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


    public void redraw() {
        clientThread.invoke(() -> {
            maxHitField.setText("Max Hit: " + calculateMaxHit(state.getStrengthLevel(), state.getStrengthBonus(itemManager)));
        });
    }

    // sourced from https://oldschool.runescape.wiki/w/Maximum_melee_hit
    private int calculateMaxHit(int strengthLevel, int strengthBonus) {
        final int effectiveStrength = (int) (((strengthLevel + getPotionBonus(strengthLevel)) * getPrayerMultiplier() * getOtherMultipliers()) + getStyleBonus());

        return (int) (1.3 + (effectiveStrength / 10) + (strengthBonus / 80) + ((effectiveStrength * strengthBonus) / 640));
    }

    private int getPotionBonus(int strengthLevel) {
        switch (state.getPotionBonus()) {
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
        switch (state.getAttackStyle()) {
            case Aggressive:
                return 3;
            case Accurate:
                return 1;
            default:
                return 0;
        }
    }

    private double getPrayerMultiplier() {
        switch (state.getPrayerMultiplier()) {
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

        if (state.voidSetEquipped()) {
            multiplier *= 1.1;
        }

        if (state.inquisitorsSetEquipped()) {
            multiplier *= 1.025;
        }

        if (state.isVsDragons() && state.dragonHunterLanceEquipped()) {
            multiplier *= 1.2;
        }

        // salveE, salve, and slayerHelm do not stack, so only apply
        // the highest multiplier of the three
        if (state.isVsUndead() && state.salveEEquipped()) {
            multiplier *= 1.2;
        } else if (state.isVsSlayer() && state.slayerHelmEquipped()) {
            multiplier *= (7.0 / 6);
        } else if (state.isVsUndead() && state.salveEquipped()) {
            multiplier *= 1.15;
        }

        return multiplier;
    }
}
