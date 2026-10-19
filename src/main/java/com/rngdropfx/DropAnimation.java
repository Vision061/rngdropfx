package com.rngdropfx;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Die HUD-Animation: Blitz, Lichtstrahlen, großer Titel, Item-Name, Konfetti und Feuerwerk. */
public final class DropAnimation {
	private static final Random RNG = new Random();

	private static final class Particle {
		float x, y, vx, vy, life, maxLife, size;
		int color;
		boolean sparkle;
	}

	private static DropAnimation current;

	private final DropDetector.Level level;
	private final String title;
	private final String item;
	private final long startMs;
	private final float duration;
	private final List<Particle> particles = new ArrayList<>();
	private final List<Float> pendingBursts = new ArrayList<>();
	private long lastFrameMs;
	private boolean initialBurstDone;

	private DropAnimation(DropDetector.Drop drop, float duration) {
		this.level = drop.level();
		this.title = drop.tier();
		this.item = drop.item();
		this.duration = Math.max(1.5f, duration);
		this.startMs = System.currentTimeMillis();
		this.lastFrameMs = startMs;
		if (level == DropDetector.Level.EPIC) {
			pendingBursts.addAll(List.of(1.0f, 1.45f, 1.9f, 2.5f));
		} else {
			pendingBursts.add(0.8f);
		}
	}

	public static void start(DropDetector.Drop drop, float duration) {
		current = new DropAnimation(drop, duration);
	}

	public static boolean isRunning() {
		return current != null;
	}

	/** Wird von der Fabric-HUD-API jedes Frame aufgerufen. */
	public static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		DropAnimation anim = current;
		if (anim == null) return;
		if (!anim.render(graphics)) current = null;
	}

	// ------------------------------------------------------------------

	private boolean render(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();

		long now = System.currentTimeMillis();
		float t = (now - startMs) / 1000f;
		float dt = Math.min(0.1f, (now - lastFrameMs) / 1000f);
		lastFrameMs = now;
		if (t >= duration) return false;

		boolean epic = level == DropDetector.Level.EPIC;
		float fade = t > duration - 0.8f ? (duration - t) / 0.8f : 1f;
		fade = clamp01(fade);
		float cx = w / 2f;
		float cy = h * 0.36f;

		int theme = epic ? rainbow(t * 0.35f, 0.75f) : 0xFFAA00;
		int theme2 = epic ? 0xFF55FF : 0xFFFF55;

		// 1) Bildschirm-Blitz
		if (t < 0.4f) {
			float a = (1f - t / 0.4f) * (epic ? 0.85f : 0.55f);
			g.fill(0, 0, w, h, argb(a, epic ? 0xFFFFFF : 0xFFE9A0));
		}

		// 2) Farbiger Schimmer + Ränder
		g.fill(0, 0, w, h, argb(0.12f * fade, theme));
		int band = h / 4;
		g.fillGradient(0, 0, w, band, argb(0.55f * fade, theme), argb(0f, theme));
		g.fillGradient(0, h - band, w, h, argb(0f, theme2), argb(0.55f * fade, theme2));

		// 3) Rotierende Lichtstrahlen hinter dem Titel
		float rayIn = easeOut(clamp01(t / 0.5f));
		drawRays(g, cx, cy, t, rayIn * fade, epic ? 18 : 12, (epic ? 0.55f : 0.4f) * Math.min(w, h), theme);

		// 4) Partikel
		if (!initialBurstDone) {
			burst(cx, cy, epic ? 160 : 80, epic ? 260f : 190f, theme);
			initialBurstDone = true;
		}
		while (!pendingBursts.isEmpty() && t >= pendingBursts.get(0)) {
			pendingBursts.remove(0);
			float bx = w * (0.15f + RNG.nextFloat() * 0.7f);
			float by = h * (0.15f + RNG.nextFloat() * 0.45f);
			burst(bx, by, epic ? 90 : 60, 150f, rainbow(RNG.nextFloat(), 0.9f));
		}
		if (epic && t < duration - 1f && RNG.nextFloat() < 0.7f) {
			Particle p = new Particle();
			p.x = RNG.nextFloat() * w;
			p.y = -4;
			p.vx = (RNG.nextFloat() - 0.5f) * 30f;
			p.vy = 40f + RNG.nextFloat() * 60f;
			p.maxLife = p.life = 2.5f;
			p.size = 1.5f + RNG.nextFloat() * 1.5f;
			p.color = rainbow(RNG.nextFloat(), 0.8f);
			p.sparkle = true;
			particles.add(p);
		}
		updateAndDrawParticles(g, dt, fade);

		// 5) Großer Titel mit Pop-In, Puls und Wackeln
		float pop = easeOutBack(clamp01(t / 0.45f));
		float pulse = 1f + 0.05f * (float) Math.sin(t * 9f);
		float baseScale = epic ? 3.2f : 2.5f;
		float shake = t < 0.7f ? (1f - t / 0.7f) * (epic ? 4f : 2f) : 0f;
		float sx = (RNG.nextFloat() - 0.5f) * 2f * shake;
		float sy = (RNG.nextFloat() - 0.5f) * 2f * shake;

		String titleText = epic ? "✦ " + title + " ✦" : title;
		int titleColor = epic ? rainbow(t * 0.8f, 0.6f) : lerpColor(0xFFAA00, 0xFFFF55, 0.5f + 0.5f * (float) Math.sin(t * 6f));
		drawCentered(g, font, titleText, cx + sx, cy + sy, baseScale * pop * pulse, titleColor, fade);

		// 6) Item-Name fährt von unten rein
		float itemT = clamp01((t - 0.35f) / 0.4f);
		if (itemT > 0f) {
			float slide = (1f - easeOut(itemT)) * 20f;
			float iy = cy + font.lineHeight * baseScale * 0.5f + 16f + slide;
			float itemScale = epic ? 2.0f : 1.6f;
			int bgW = (int) (font.width(item) * itemScale / 2f) + 14;
			int bgH = (int) (font.lineHeight * itemScale / 2f) + 6;
			g.fill((int) (cx - bgW), (int) (iy - bgH), (int) (cx + bgW), (int) (iy + bgH), argb(0.55f * itemT * fade, 0x000000));
			g.fill((int) (cx - bgW), (int) (iy + bgH - 1), (int) (cx + bgW), (int) (iy + bgH), argb(itemT * fade, theme));
			int itemColor = lerpColor(0xFFFFFF, epic ? 0xFFD700 : 0xFFAA00, 0.5f + 0.5f * (float) Math.sin(t * 4f));
			drawCentered(g, font, item, cx, iy, itemScale, itemColor, itemT * fade);
		}

		// 7) Untertitel
		float subT = clamp01((t - 0.9f) / 0.4f);
		if (subT > 0f) {
			String sub = epic ? "RNGesus hat gesprochen!" : "Nice Drop!";
			float sy2 = cy + font.lineHeight * baseScale * 0.5f + 48f;
			drawCentered(g, font, sub, cx, sy2, 1.2f, 0xDDDDDD, subT * fade);
		}
		return true;
	}

	private void drawRays(GuiGraphicsExtractor g, float cx, float cy, float t, float alpha, int count, float length, int color) {
		if (alpha <= 0.01f) return;
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(cx, cy);
		pose.rotate(t * 0.5f);
		float step = (float) (Math.PI * 2 / count);
		int len = (int) length;
		for (int i = 0; i < count; i++) {
			pose.rotate(step);
			// Strahl = drei Segmente, nach außen schmaler und durchsichtiger
			g.fillGradient(-6, 0, 6, len / 3, argb(0.30f * alpha, color), argb(0.18f * alpha, color));
			g.fillGradient(-4, len / 3, 4, 2 * len / 3, argb(0.18f * alpha, color), argb(0.08f * alpha, color));
			g.fillGradient(-2, 2 * len / 3, 2, len, argb(0.08f * alpha, color), argb(0f, color));
		}
		pose.popMatrix();
	}

	private void burst(float x, float y, int count, float speed, int baseColor) {
		for (int i = 0; i < count; i++) {
			Particle p = new Particle();
			double ang = RNG.nextDouble() * Math.PI * 2;
			float s = speed * (0.3f + RNG.nextFloat() * 0.7f);
			p.x = x;
			p.y = y;
			p.vx = (float) Math.cos(ang) * s;
			p.vy = (float) Math.sin(ang) * s - 40f;
			p.maxLife = p.life = 1.2f + RNG.nextFloat() * 1.3f;
			p.size = 1.5f + RNG.nextFloat() * 2f;
			p.color = RNG.nextFloat() < 0.5f ? baseColor : rainbow(RNG.nextFloat(), 0.7f);
			p.sparkle = RNG.nextFloat() < 0.3f;
			particles.add(p);
		}
	}

	private void updateAndDrawParticles(GuiGraphicsExtractor g, float dt, float fade) {
		for (int i = particles.size() - 1; i >= 0; i--) {
			Particle p = particles.get(i);
			p.life -= dt;
			if (p.life <= 0) {
				particles.remove(i);
				continue;
			}
			p.vy += 120f * dt;          // Schwerkraft
			p.vx *= (1f - 0.9f * dt);   // Luftwiderstand
			p.x += p.vx * dt;
			p.y += p.vy * dt;
			float a = clamp01(p.life / p.maxLife) * fade;
			if (p.sparkle && RNG.nextFloat() < 0.3f) a *= 0.3f; // funkeln
			int half = Math.max(1, (int) (p.size / 2f));
			int x = (int) p.x;
			int y = (int) p.y;
			g.fill(x - half, y - half, x + half, y + half, argb(a, p.color));
		}
	}

	private static void drawCentered(GuiGraphicsExtractor g, Font font, String text, float x, float y, float scale, int rgb, float alpha) {
		if (alpha <= 0.02f || scale <= 0.01f) return;
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(scale, scale);
		int tw = font.width(text);
		int col = argb(Math.max(alpha, 0.1f), rgb);
		g.text(font, text, -tw / 2, -font.lineHeight / 2, col, true);
		pose.popMatrix();
	}

	// ---------------------------- Helfer ------------------------------

	private static float clamp01(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}

	private static float easeOut(float t) {
		return 1f - (1f - t) * (1f - t) * (1f - t);
	}

	private static float easeOutBack(float t) {
		float c1 = 1.70158f * 1.4f;
		float c3 = c1 + 1f;
		float u = t - 1f;
		return 1f + c3 * u * u * u + c1 * u * u;
	}

	private static int argb(float alpha, int rgb) {
		int a = (int) (clamp01(alpha) * 255f);
		return (a << 24) | (rgb & 0xFFFFFF);
	}

	private static int lerpColor(int a, int b, float t) {
		t = clamp01(t);
		int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
		int gr = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
		int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
		return (r << 16) | (gr << 8) | bl;
	}

	/** HSV-Regenbogenfarbe, hue 0..1 (wiederholt sich). */
	private static int rainbow(float hue, float saturation) {
		float hh = (hue % 1f + 1f) % 1f * 6f;
		int sector = (int) hh;
		float f = hh - sector;
		float v = 1f, s = saturation;
		float p = v * (1 - s), q = v * (1 - s * f), tt = v * (1 - s * (1 - f));
		float r, g, b;
		switch (sector) {
			case 0 -> { r = v; g = tt; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = tt; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = tt; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
	}
}
