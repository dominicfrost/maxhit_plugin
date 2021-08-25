package com.github.dominicfrost.maxhit_plugin;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

@PluginDescriptor(
	name = "Max Hit Calculator",
	description = "Display the max hit for your current gear"
)
@Slf4j
public class MaxHitPlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ClientThread clientThread;

	private  NavigationButton navButton;
	private MaxHitPanel panel;

	@Override
	protected void startUp() throws Exception {
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "73.png");

		panel = new MaxHitPanel(client, itemManager, clientThread);
		navButton = NavigationButton.builder()
				.tooltip("Max Hit Calculator")
				.icon(icon)
				.priority(99)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() {
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onItemContainerChanged(final ItemContainerChanged event)
	{
		if (panel == null) return;
		if (event.getItemContainer() != client.getItemContainer(InventoryID.EQUIPMENT)) return;

		panel.state.setEquippedItems(event.getItemContainer().getItems());
		panel.redraw();
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		if (panel == null) return;
		if (statChanged.getSkill() != Skill.STRENGTH) return;

		panel.state.setStrengthLevel(statChanged.getLevel());
		panel.redraw();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getIndex() != VarPlayer.ATTACK_STYLE.getId()) return;

		int currentAttackStyleVarbit = client.getVar(VarPlayer.ATTACK_STYLE);
		switch (currentAttackStyleVarbit) {
			case 0:
				panel.state.setAttackStyle(AttackStyle.Accurate);
				break;
			case 1:
				panel.state.setAttackStyle(AttackStyle.Aggressive);
				break;
			case 2:
				panel.state.setAttackStyle(AttackStyle.Controlled);
				break;
			case 3:
				panel.state.setAttackStyle(AttackStyle.Defensive);
		}
		panel.redraw();
	}
}
