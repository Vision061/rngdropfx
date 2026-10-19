package com.rngdropfx;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class RngDropFx implements ClientModInitializer {
	public static final String MOD_ID = "rngdropfx";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static RngConfig config;

	private static KeyMapping menuKey;
	/** Menü erst im nächsten Tick öffnen (sonst schließt der Chat es direkt wieder). */
	private static boolean openMenuNextTick = false;

	@Override
	public void onInitializeClient() {
		config = RngConfig.load();

		// Hypixel schickt Drop-Nachrichten als System-/Game-Nachrichten
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (overlay || !config.enabled) return; // Actionbar ignorieren
			DropDetector.Drop drop = DropDetector.detect(message.getString(), config);
			if (drop != null) trigger(drop);
		});

		// Taste für das Test-Menü (Standard: K, änderbar unter Optionen > Steuerung)
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.rngdropfx.menu", InputConstants.Type.KEYSYM, InputConstants.KEY_K, category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			SoundSequencer.tick(client);
			while (menuKey.consumeClick()) {
				if (client.screen == null) client.setScreen(new TestScreen());
			}
			if (openMenuNextTick) {
				openMenuNextTick = false;
				client.setScreen(new TestScreen());
			}
		});

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "drop_animation"), DropAnimation::renderHud);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> dispatcher.register(
				ClientCommands.literal("rngfx")
						.executes(RngDropFx::help)
						.then(ClientCommands.literal("menu").executes(c -> {
							openMenuNextTick = true;
							return 1;
						}))
						// /rngfx test            -> große Animation
						// /rngfx test <Item>     -> simuliert einen echten Drop von genau diesem Item
						.then(ClientCommands.literal("test")
								.executes(c -> {
									trigger(new DropDetector.Drop(DropDetector.Level.EPIC, "PRAY TO RNGESUS DROP!", "Chimera I"));
									return 1;
								})
								.then(ClientCommands.argument("item", StringArgumentType.greedyString())
										.suggests((ctx, builder) -> {
											String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
											for (String item : config.epicItems) {
												if (item.toLowerCase(Locale.ROOT).startsWith(typed)) builder.suggest(item);
											}
											return builder.buildFuture();
										})
										.executes(c -> simulate(c, "PRAY TO RNGESUS DROP! (" + StringArgumentType.getString(c, "item") + ")"))))
						.then(ClientCommands.literal("testsmall").executes(c -> {
							trigger(new DropDetector.Drop(DropDetector.Level.GOOD, "CRAZY RARE DROP!", "Digested Mosquito"));
							return 1;
						}))
						// /rngfx simulate <komplette Chatnachricht>  -> testet die Erkennung 1:1
						.then(ClientCommands.literal("simulate")
								.then(ClientCommands.argument("message", StringArgumentType.greedyString())
										.executes(c -> simulate(c, StringArgumentType.getString(c, "message")))))
						.then(ClientCommands.literal("toggle").executes(c -> {
							config.enabled = !config.enabled;
							config.save();
							c.getSource().sendFeedback(Component.literal("§6[RNG FX] §f" + (config.enabled ? "§aAn" : "§cAus")));
							return 1;
						}))
						.then(ClientCommands.literal("reload").executes(c -> {
							config = RngConfig.load();
							c.getSource().sendFeedback(Component.literal("§6[RNG FX] §aConfig neu geladen."));
							return 1;
						}))
		));

		LOGGER.info("RNG Drop FX geladen – viel Glück mit RNGesus!");
	}

	private static int simulate(CommandContext<FabricClientCommandSource> c, String message) {
		DropDetector.Drop drop = DropDetector.detect(message, config);
		if (drop == null) {
			c.getSource().sendFeedback(Component.literal("§6[RNG FX] §cDiese Nachricht würde keine Animation auslösen."));
			return 0;
		}
		trigger(drop);
		return 1;
	}

	private static int help(CommandContext<FabricClientCommandSource> c) {
		c.getSource().sendFeedback(Component.literal(
				"§6§l[RNG FX] Befehle\n" +
				"§e/rngfx menu §7– Test-Menü öffnen (oder Taste §fK§7)\n" +
				"§e/rngfx test [Item] §7– große Animation, optional für ein bestimmtes Item\n" +
				"§e/rngfx testsmall §7– kleine Animation\n" +
				"§e/rngfx simulate <Nachricht> §7– eine Chatnachricht testen\n" +
				"§e/rngfx toggle §7| §e/rngfx reload\n" +
				"§7Items & Einstellungen: §fconfig/rngdropfx.json"));
		return 1;
	}

	public static void trigger(DropDetector.Drop drop) {
		DropAnimation.start(drop, config.durationSeconds);
		SoundSequencer.play(drop.level(), config.volume);
	}
}
