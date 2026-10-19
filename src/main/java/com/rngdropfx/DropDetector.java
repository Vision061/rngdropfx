package com.rngdropfx;

import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Erkennt Hypixel-SkyBlock-Drop-Nachrichten, z.B.
 * "CRAZY RARE DROP! (Chimera I) (+412% ✯ Magic Find)"
 */
public final class DropDetector {
	private static final Pattern COLOR_CODES = Pattern.compile("§.");
	private static final Pattern MAGIC_FIND = Pattern.compile("\\s*\\(\\+[^)]*\\)\\s*$");

	public enum Level { EPIC, GOOD }

	public record Drop(Level level, String tier, String item) {}

	private DropDetector() {}

	public static Drop detect(String raw, RngConfig cfg) {
		if (raw == null || raw.isEmpty()) return null;
		String msg = COLOR_CODES.matcher(raw).replaceAll("").trim();
		String upper = msg.toUpperCase(Locale.ROOT);

		// Längste Präfixe zuerst prüfen ("VERY RARE DROP!" vor "RARE DROP!")
		String tier = cfg.dropPrefixes.stream()
				.sorted(Comparator.comparingInt(String::length).reversed())
				.filter(p -> upper.startsWith(p.toUpperCase(Locale.ROOT)))
				.findFirst().orElse(null);
		if (tier == null) return null;

		String item = msg.substring(tier.length()).trim();
		item = MAGIC_FIND.matcher(item).replaceAll("").trim();
		if (item.startsWith("(") && item.endsWith(")") && item.length() > 2) {
			item = item.substring(1, item.length() - 1).trim();
		}
		if (item.isEmpty()) item = "???";

		String itemLower = item.toLowerCase(Locale.ROOT);
		boolean epicItem = cfg.epicItems.stream()
				.anyMatch(i -> !i.isBlank() && itemLower.contains(i.toLowerCase(Locale.ROOT)));
		boolean epicTier = cfg.epicTiers.stream().anyMatch(t -> t.equalsIgnoreCase(tier));
		boolean goodTier = cfg.goodTiers.stream().anyMatch(t -> t.equalsIgnoreCase(tier));

		if (epicItem || epicTier) return new Drop(Level.EPIC, tier.toUpperCase(Locale.ROOT), item);
		if (goodTier && cfg.animateCrazyRareDrops) return new Drop(Level.GOOD, tier.toUpperCase(Locale.ROOT), item);
		return null;
	}
}
