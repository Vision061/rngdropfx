package com.rngdropfx;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Test-Menü: Hier kannst du jede Animation auslösen, ohne das Item wirklich zu droppen.
 * Öffnen mit der Taste (Standard: K) oder /rngfx menu.
 */
public class TestScreen extends Screen {
	private static final int BTN_W = 130;
	private static final int BTN_H = 20;
	private static final int GAP = 4;

	private static int page = 0;
	private static int tierIndex = 0;

	private String status = "";
	private int statusColor = 0xFFAAAAAA;

	public TestScreen() {
		super(Component.literal("RNG Drop FX – Test"));
	}

	@Override
	protected void init() {
		RngConfig cfg = RngDropFx.config;
		int cx = this.width / 2;
		int y = 32;

		// --- Schnelltests -------------------------------------------------
		this.addRenderableWidget(Button.builder(Component.literal("§d§lGroße Animation"), b ->
				play(new DropDetector.Drop(DropDetector.Level.EPIC, "PRAY TO RNGESUS DROP!", "Chimera I"))
		).bounds(cx - BTN_W - GAP / 2, y, BTN_W, BTN_H).build());

		this.addRenderableWidget(Button.builder(Component.literal("§6§lKleine Animation"), b ->
				play(new DropDetector.Drop(DropDetector.Level.GOOD, "CRAZY RARE DROP!", "Digested Mosquito"))
		).bounds(cx + GAP / 2, y, BTN_W, BTN_H).build());
		y += BTN_H + GAP;

		// --- Drop-Stufe wählen (wird für die Item-Buttons benutzt) ---------
		if (tierIndex >= cfg.dropPrefixes.size()) tierIndex = 0;
		this.addRenderableWidget(Button.builder(tierLabel(), b -> {
			tierIndex = (tierIndex + 1) % Math.max(1, cfg.dropPrefixes.size());
			b.setMessage(tierLabel());
		}).bounds(cx - BTN_W - GAP / 2, y, BTN_W * 2 + GAP, BTN_H).build());
		y += BTN_H + GAP + 14; // Platz für Überschrift "Items"

		// --- Item-Buttons (mit Seiten) -----------------------------------
		List<String> items = cfg.epicItems;
		int cols = Math.max(1, Math.min(3, (this.width - 20) / (BTN_W + GAP)));
		int bottomReserved = 3 * (BTN_H + GAP) + 10;
		int rows = Math.max(1, (this.height - y - bottomReserved) / (BTN_H + GAP));
		int perPage = cols * rows;
		int pages = Math.max(1, (items.size() + perPage - 1) / perPage);
		if (page >= pages) page = pages - 1;

		int gridW = cols * BTN_W + (cols - 1) * GAP;
		int startX = cx - gridW / 2;
		int from = page * perPage;
		int to = Math.min(items.size(), from + perPage);
		for (int i = from; i < to; i++) {
			int idx = i - from;
			int bx = startX + (idx % cols) * (BTN_W + GAP);
			int by = y + (idx / cols) * (BTN_H + GAP);
			String item = items.get(i);
			this.addRenderableWidget(Button.builder(Component.literal(shorten(item)), b -> simulate(item))
					.bounds(bx, by, BTN_W, BTN_H).build());
		}
		int gridBottom = y + rows * (BTN_H + GAP);

		// --- Seiten-Navigation -------------------------------------------
		if (pages > 1) {
			Button prev = Button.builder(Component.literal("< Zurück"), b -> { page--; rebuildWidgets(); })
					.bounds(cx - 100 - GAP / 2, gridBottom, 100, BTN_H).build();
			Button next = Button.builder(Component.literal("Weiter >"), b -> { page++; rebuildWidgets(); })
					.bounds(cx + GAP / 2, gridBottom, 100, BTN_H).build();
			prev.active = page > 0;
			next.active = page < pages - 1;
			this.addRenderableWidget(prev);
			this.addRenderableWidget(next);
		}

		// --- Unten: An/Aus + Fertig ---------------------------------------
		int by = this.height - BTN_H - 8;
		this.addRenderableWidget(Button.builder(enabledLabel(), b -> {
			cfg.enabled = !cfg.enabled;
			cfg.save();
			b.setMessage(enabledLabel());
		}).bounds(cx - BTN_W - GAP / 2, by, BTN_W, BTN_H).build());

		this.addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> this.onClose())
				.bounds(cx + GAP / 2, by, BTN_W, BTN_H).build());

		this.pageInfo = pages > 1 ? "Seite " + (page + 1) + "/" + pages : "";
		this.itemsHeaderY = y - 12;
	}

	private String pageInfo = "";
	private int itemsHeaderY = 0;

	/** Baut eine echte Hypixel-Chatnachricht und schickt sie durch die Erkennung – testet also auch deine Config. */
	private void simulate(String item) {
		RngConfig cfg = RngDropFx.config;
		String tier = cfg.dropPrefixes.isEmpty() ? "PRAY TO RNGESUS DROP!" : cfg.dropPrefixes.get(tierIndex);
		String fakeMessage = tier + " (" + item + ") (+350% ✯ Magic Find)";
		DropDetector.Drop drop = DropDetector.detect(fakeMessage, cfg);
		if (drop == null) {
			status = "Diese Kombi würde KEINE Animation auslösen.";
			statusColor = 0xFFFF5555;
			return;
		}
		play(drop);
	}

	private void play(DropDetector.Drop drop) {
		// Menü schließen, damit man die Animation in voller Pracht sieht
		this.minecraft.setScreen(null);
		RngDropFx.trigger(drop);
	}

	private static Component tierLabel() {
		List<String> tiers = RngDropFx.config.dropPrefixes;
		String t = tiers.isEmpty() ? "-" : tiers.get(tierIndex % tiers.size());
		return Component.literal("Drop-Stufe: §e" + t + " §7(klicken zum Wechseln)");
	}

	private static Component enabledLabel() {
		return Component.literal("Mod: " + (RngDropFx.config.enabled ? "§aAn" : "§cAus"));
	}

	private String shorten(String s) {
		int max = BTN_W - 10;
		if (this.font.width(s) <= max) return s;
		while (s.length() > 1 && this.font.width(s + "…") > max) s = s.substring(0, s.length() - 1);
		return s + "…";
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int cx = this.width / 2;
		centered(graphics, "§6§lRNG Drop FX §7– Animationen testen", cx, 10, 0xFFFFFFFF);
		centered(graphics, "§7Items (klicken = Drop simulieren)  §8" + pageInfo, cx, itemsHeaderY, 0xFFFFFFFF);
		if (!status.isEmpty()) {
			centered(graphics, status, cx, this.height - BTN_H - 22, statusColor);
		}
	}

	private void centered(GuiGraphicsExtractor g, String text, int cx, int y, int color) {
		g.text(this.font, text, cx - this.font.width(text) / 2, y, color, true);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
