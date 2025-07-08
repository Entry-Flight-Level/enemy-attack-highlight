package com.enemyHighlight;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayRenderer;

import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Slf4j
@PluginDescriptor(
	name = "EnemyHighlight-EFL"
)
public class main extends Plugin
{
	private static final int DEFAULT_ATTACK_SPEED_TICKS = 4;
	private NPC currentAttackingNpc = null;
	private int attackTickCounter = 0;
	private int flashTickCounter = 0;
	private final Overlay tileOverlay = new Overlay()
	{
		@Override
		public Dimension render(Graphics2D graphics)
		{
			if (currentAttackingNpc != null && flashTickCounter == 1)
			{
				// Get NPC's current tile location
				LocalPoint lp = currentAttackingNpc.getLocalLocation();
				if (lp == null)
					return null;

				Polygon poly = Perspective.getCanvasTilePoly(client, lp);
				if (poly != null)
				{
					graphics.setColor(new Color(255, 255, 255, 128)); // semi-transparent white
					graphics.fillPolygon(poly);
				}
			}
			return null;
		}
	};
	@Inject
	private Client client;

	@Inject
	private config config;

	@Inject
	private OverlayManager overlayManager;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(tileOverlay);
		log.info("Example started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(tileOverlay);
		log.info("Example stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
		}
	}
	//Test Code to just notify if an enemy npc is interacting with player
	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{

		//If actor isn't a npc (IE player)
		if (!(event.getActor() instanceof NPC)) return;

		NPC npc = (NPC) event.getActor();
		//If NPC isn't interacting with player
		if (npc.getInteracting() != client.getLocalPlayer()) return;

		//grab animation
		int animationId = npc.getAnimation();
		if (animationId == -1) return;

		if (npc != currentAttackingNpc)
		{
			currentAttackingNpc = npc;
			attackTickCounter = 0;
			flashTickCounter = 1;
		}
		log.debug("NPC {} played animation {}", npc.getName(), animationId);

		client.addChatMessage(
			ChatMessageType.GAMEMESSAGE,
			"",
			"NPC <col=ff0000>" + npc.getName() + "</col> attacked you (<col=00ffff>" + npc.getAnimation() + "</col>).",
			null
		);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (currentAttackingNpc != null)
		{
			attackTickCounter++;

			// Log the tick counter for the current attack
			client.addChatMessage(
				ChatMessageType.GAMEMESSAGE,
				"",
				"(" + attackTickCounter + ")",
				null
			);
		}
		// Flash NPC white for 1 tick
		if (flashTickCounter > 0)
		{
			flashTickCounter++; // Increment to 2 to signal end of flash

			if (flashTickCounter == 2)
			{
				// Reset after 1 tick
				flashTickCounter = 0;
			}
		}

		if(attackTickCounter >= 4)
		{
			currentAttackingNpc = null;
		}
	}
	@Provides
	config provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(config.class);
	}
}
