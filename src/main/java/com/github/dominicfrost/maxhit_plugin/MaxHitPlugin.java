package com.github.dominicfrost.maxhit_plugin;

import net.runelite.api.Client;
import net.runelite.api.events.ItemContainerChanged;
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
		panel.onItemContainerChanged(event);
	}

}
