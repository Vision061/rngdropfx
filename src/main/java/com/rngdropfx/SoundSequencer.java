package com.rngdropfx;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Spielt eine zeitlich abgestimmte Sound-Folge ab (wird jeden Client-Tick geprüft). */
public final class SoundSequencer {
	private record Cue(long atMs, SoundEvent sound, float pitch, float volume) {}

	private static final List<Cue> QUEUE = new ArrayList<>();

	private SoundSequencer() {}

	public static void play(DropDetector.Level level, float masterVolume) {
		QUEUE.clear();
		long now = System.currentTimeMillis();
		float v = Math.max(0f, Math.min(2f, masterVolume));

		if (level == DropDetector.Level.EPIC) {
			// Einschlag
			add(now, 0, SoundEvents.TOTEM_USE, 1.0f, 0.55f * v);
			add(now, 0, SoundEvents.ENDER_DRAGON_GROWL, 1.3f, 0.30f * v);
			add(now, 0, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.9f * v);
			// Glitzer-Arpeggio
			float[] notes = {0.75f, 0.94f, 1.12f, 1.5f, 1.88f, 2.0f};
			for (int i = 0; i < notes.length; i++) {
				add(now, 120 + i * 85L, SoundEvents.AMETHYST_BLOCK_CHIME, notes[i], 1.0f * v);
				add(now, 120 + i * 85L, SoundEvents.EXPERIENCE_ORB_PICKUP, notes[i], 0.5f * v);
			}
			add(now, 650, SoundEvents.PLAYER_LEVELUP, 1.0f, 0.9f * v);
			add(now, 900, SoundEvents.BEACON_ACTIVATE, 1.6f, 0.7f * v);
			// Feuerwerk
			long[] booms = {1000, 1450, 1900, 2500};
			for (long b : booms) {
				add(now, b, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.9f + (float) Math.random() * 0.3f, 0.8f * v);
				add(now, b + 220, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0f + (float) Math.random() * 0.2f, 0.7f * v);
			}
			add(now, 2900, SoundEvents.PLAYER_LEVELUP, 1.5f, 0.6f * v);
		} else {
			add(now, 0, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.25f, 0.8f * v);
			float[] notes = {1.0f, 1.26f, 1.5f, 2.0f};
			for (int i = 0; i < notes.length; i++) {
				add(now, 100 + i * 90L, SoundEvents.EXPERIENCE_ORB_PICKUP, notes[i], 0.7f * v);
			}
			add(now, 480, SoundEvents.PLAYER_LEVELUP, 1.3f, 0.7f * v);
			add(now, 800, SoundEvents.FIREWORK_ROCKET_BLAST, 1.1f, 0.7f * v);
			add(now, 1000, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.2f, 0.6f * v);
		}
	}

	private static void add(long now, long delay, SoundEvent sound, float pitch, float volume) {
		if (volume <= 0f) return;
		QUEUE.add(new Cue(now + delay, sound, pitch, volume));
	}

	public static void tick(Minecraft client) {
		if (QUEUE.isEmpty()) return;
		long now = System.currentTimeMillis();
		Iterator<Cue> it = QUEUE.iterator();
		while (it.hasNext()) {
			Cue c = it.next();
			if (c.atMs() <= now) {
				// Wird direkt beim Spieler abgespielt (nur du hörst es)
				if (client.player != null) client.player.playSound(c.sound(), Math.min(1f, c.volume()), c.pitch());
				it.remove();
			}
		}
	}
}
