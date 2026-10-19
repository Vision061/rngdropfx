package com.rngdropfx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Einstellungen der Mod. Wird als config/rngdropfx.json gespeichert
 * und kann im Spiel mit /rngfx reload neu geladen werden.
 */
public class RngConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("rngdropfx.json");

	/** Mod an/aus. */
	public boolean enabled = true;
	/** Lautstärke aller Sounds (0.0 - 2.0). */
	public float volume = 1.0f;
	/** Dauer der Animation in Sekunden. */
	public float durationSeconds = 4.5f;
	/** Auch bei "CRAZY RARE DROP!" (ohne Item aus der Liste) eine kleinere Animation zeigen. */
	public boolean animateCrazyRareDrops = true;

	/** Chat-Anfänge, die als Drop-Nachricht zählen. */
	public List<String> dropPrefixes = new ArrayList<>(List.of(
			"PRAY TO RNGESUS DROP!",
			"RNGESUS INCARNATE DROP!",
			"INSANE DROP!",
			"CRAZY RARE DROP!",
			"VERY RARE DROP!",
			"RARE DROP!",
			"PET DROP!"
	));

	/** Diese Drop-Stufen lösen immer die große Animation aus. */
	public List<String> epicTiers = new ArrayList<>(List.of(
			"PRAY TO RNGESUS DROP!",
			"RNGESUS INCARNATE DROP!",
			"INSANE DROP!"
	));

	/** Diese Drop-Stufen lösen die kleinere Animation aus. */
	public List<String> goodTiers = new ArrayList<>(List.of(
			"CRAZY RARE DROP!"
	));

	/** Items, die IMMER die große Animation auslösen (egal welche Stufe). Groß-/Kleinschreibung egal. */
	public List<String> epicItems = new ArrayList<>(List.of(
			"Chimera",
			"Digested Mosquito",
			"Primordial Eye",
			"Judgement Core",
			"Overflux Capacitor",
			"Warden Heart",
			"Scythe Blade",
			"Shard of the Shredded",
			"Necron's Handle",
			"Implosion",
			"Shadow Warp",
			"Wither Shield",
			"Dark Claymore",
			"Severed Pincer",
			"Tarantula Talisman",
			"Fly Swatter",
			"Beheaded Horror",
			"Snake Rune",
			"Devourer",
			"Red Claw Egg",
			"Pocket Espresso Machine",
			"Handy Blood Chalice",
			"Fudge Book",
			"High Class Archfiend Dice",
			"Kelvin Inverter"
	));

	public static RngConfig load() {
		try {
			if (Files.exists(FILE)) {
				try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
					RngConfig cfg = GSON.fromJson(r, RngConfig.class);
					if (cfg != null) {
						cfg.save(); // fehlende neue Felder ergänzen
						return cfg;
					}
				}
			}
		} catch (Exception e) {
			RngDropFx.LOGGER.error("Konnte {} nicht lesen, nutze Standardwerte", FILE, e);
		}
		RngConfig cfg = new RngConfig();
		cfg.save();
		return cfg;
	}

	public void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(this, w);
			}
		} catch (Exception e) {
			RngDropFx.LOGGER.error("Konnte {} nicht speichern", FILE, e);
		}
	}
}
