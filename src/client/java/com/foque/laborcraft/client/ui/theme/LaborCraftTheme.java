package com.foque.laborcraft.client.ui.theme;

import static com.quillui.theme.QuillColors.argb;
import static com.quillui.theme.QuillColors.rgb;
import static com.quillui.theme.QuillThemeData.ColorRole.ACCENT;
import static com.quillui.theme.QuillThemeData.ColorRole.ACCENT_HOVER;
import static com.quillui.theme.QuillThemeData.ColorRole.BUTTON_BG;
import static com.quillui.theme.QuillThemeData.ColorRole.BUTTON_BORDER;
import static com.quillui.theme.QuillThemeData.ColorRole.BUTTON_DISABLED;
import static com.quillui.theme.QuillThemeData.ColorRole.BUTTON_DISABLED_BORDER;
import static com.quillui.theme.QuillThemeData.ColorRole.BUTTON_DISABLED_TEXT;
import static com.quillui.theme.QuillThemeData.ColorRole.BUTTON_HOVER;
import static com.quillui.theme.QuillThemeData.ColorRole.CHIP_BG;
import static com.quillui.theme.QuillThemeData.ColorRole.CHIP_BORDER;
import static com.quillui.theme.QuillThemeData.ColorRole.DANGER;
import static com.quillui.theme.QuillThemeData.ColorRole.DANGER_BG;
import static com.quillui.theme.QuillThemeData.ColorRole.DIVIDER;
import static com.quillui.theme.QuillThemeData.ColorRole.FOCUS_RING;
import static com.quillui.theme.QuillThemeData.ColorRole.GHOST_TEXT;
import static com.quillui.theme.QuillThemeData.ColorRole.HEADER_BG;
import static com.quillui.theme.QuillThemeData.ColorRole.INFO;
import static com.quillui.theme.QuillThemeData.ColorRole.MAP_BG;
import static com.quillui.theme.QuillThemeData.ColorRole.MAP_GRID;
import static com.quillui.theme.QuillThemeData.ColorRole.MAP_UNKNOWN;
import static com.quillui.theme.QuillThemeData.ColorRole.MUTED;
import static com.quillui.theme.QuillThemeData.ColorRole.MUTED_DARK;
import static com.quillui.theme.QuillThemeData.ColorRole.ON_ACCENT;
import static com.quillui.theme.QuillThemeData.ColorRole.ON_DANGER;
import static com.quillui.theme.QuillThemeData.ColorRole.PANEL_BG;
import static com.quillui.theme.QuillThemeData.ColorRole.PANEL_BORDER;
import static com.quillui.theme.QuillThemeData.ColorRole.PANEL_SHADOW;
import static com.quillui.theme.QuillThemeData.ColorRole.PRIMARY_BG;
import static com.quillui.theme.QuillThemeData.ColorRole.PRIMARY_BG_HOVER;
import static com.quillui.theme.QuillThemeData.ColorRole.SCREEN_BG;
import static com.quillui.theme.QuillThemeData.ColorRole.SELECTED_BG;
import static com.quillui.theme.QuillThemeData.ColorRole.SUCCESS;
import static com.quillui.theme.QuillThemeData.ColorRole.SURFACE;
import static com.quillui.theme.QuillThemeData.ColorRole.SURFACE_ALT;
import static com.quillui.theme.QuillThemeData.ColorRole.SURFACE_BORDER;
import static com.quillui.theme.QuillThemeData.ColorRole.SURFACE_HEADER;
import static com.quillui.theme.QuillThemeData.ColorRole.TEXT;
import static com.quillui.theme.QuillThemeData.ColorRole.TEXT_SOFT;
import static com.quillui.theme.QuillThemeData.ColorRole.TITLE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.quillui.theme.QuillThemeData;
import com.quillui.theme.QuillThemeMetrics;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

/**
 * Player-selectable palettes. The choice is client-side and stored in
 * {@code config/laborcraft-ui.json}; unknown ids fall back to {@link #LABOR}.
 */
public enum LaborCraftTheme {
	LABOR("labor", QuillThemeData.DEFAULT),
	GLASS("glass", glass()),
	FLAT("flat", flat()),
	LIGHT("light", light()),
	FOREST("forest", forest()),
	AMETHYST("amethyst", amethyst());

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static LaborCraftTheme current = LABOR;

	private final String id;
	private final QuillThemeData data;

	LaborCraftTheme(String id, QuillThemeData data) {
		this.id = id;
		this.data = data;
	}

	public String id() {
		return id;
	}

	public QuillThemeData data() {
		return data;
	}

	public Component displayName() {
		return Component.translatable("laborcraft.ui.theme." + id);
	}

	public Component description() {
		return Component.translatable("laborcraft.ui.theme." + id + ".tip");
	}

	public static LaborCraftTheme current() {
		return current;
	}

	public static QuillThemeData currentData() {
		return current.data;
	}

	public static void select(LaborCraftTheme theme) {
		if (theme == null || theme == current) {
			return;
		}
		current = theme;
		save();
	}

	public static void load() {
		Path path = path();
		if (!Files.isRegularFile(path)) {
			return;
		}

		try {
			Data stored = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Data.class);
			if (stored != null && stored.theme != null) {
				Arrays.stream(values())
						.filter(theme -> theme.id.equals(stored.theme))
						.findFirst()
						.ifPresent(theme -> current = theme);
			}
		}
		catch (IOException | RuntimeException ignored) {
		}
	}

	private static void save() {
		Path path = path();
		try {
			Files.createDirectories(path.getParent());
			Data data = new Data();
			data.theme = current.id;
			Files.writeString(path, GSON.toJson(data), StandardCharsets.UTF_8);
		}
		catch (IOException | RuntimeException ignored) {
		}
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("laborcraft-ui.json");
	}

	private static QuillThemeData glass() {
		return QuillThemeData.builder()
				.set(SCREEN_BG, argb(85, 4, 10, 16))
				.set(PANEL_BG, argb(184, 12, 20, 28))
				.set(PANEL_BORDER, argb(102, 207, 239, 255))
				.set(PANEL_SHADOW, argb(102, 0, 0, 0))
				.set(HEADER_BG, argb(64, 21, 34, 51))
				.set(DIVIDER, argb(51, 207, 239, 255))
				.set(TITLE, rgb(244, 251, 255))
				.set(TEXT, rgb(214, 232, 245))
				.set(TEXT_SOFT, rgb(183, 207, 224))
				.set(MUTED, rgb(143, 169, 188))
				.set(MUTED_DARK, rgb(110, 135, 153))
				.set(ACCENT, rgb(125, 226, 255))
				.set(ACCENT_HOVER, rgb(182, 241, 255))
				.set(PRIMARY_BG, argb(170, 30, 97, 114))
				.set(PRIMARY_BG_HOVER, argb(204, 42, 122, 143))
				.set(ON_ACCENT, rgb(244, 251, 255))
				.set(ON_DANGER, rgb(255, 179, 193))
				.set(GHOST_TEXT, rgb(183, 207, 224))
				.set(FOCUS_RING, rgb(125, 226, 255))
				.set(DANGER, rgb(255, 138, 155))
				.set(DANGER_BG, argb(51, 255, 138, 155))
				.set(SUCCESS, rgb(140, 240, 197))
				.set(INFO, rgb(143, 208, 255))
				.set(BUTTON_BG, argb(102, 20, 34, 46))
				.set(BUTTON_HOVER, argb(153, 32, 51, 68))
				.set(BUTTON_BORDER, argb(85, 207, 239, 255))
				.set(BUTTON_DISABLED, argb(51, 16, 24, 32))
				.set(BUTTON_DISABLED_BORDER, argb(51, 207, 239, 255))
				.set(BUTTON_DISABLED_TEXT, rgb(110, 135, 153))
				.set(SELECTED_BG, argb(102, 51, 95, 115))
				.set(SURFACE, argb(85, 16, 24, 32))
				.set(SURFACE_ALT, argb(51, 26, 42, 56))
				.set(SURFACE_BORDER, argb(85, 207, 239, 255))
				.set(SURFACE_HEADER, argb(102, 16, 28, 40))
				.set(CHIP_BG, argb(102, 20, 34, 46))
				.set(CHIP_BORDER, argb(85, 207, 239, 255))
				.metrics(new QuillThemeMetrics(22, 220, 320, 120, 10, 26, 10))
				.build();
	}

	private static QuillThemeData flat() {
		return QuillThemeData.builder()
				.set(SCREEN_BG, argb(153, 7, 10, 13))
				.set(PANEL_BG, rgb(18, 20, 24))
				.set(PANEL_BORDER, rgb(46, 51, 59))
				.set(PANEL_SHADOW, argb(102, 0, 0, 0))
				.set(HEADER_BG, rgb(22, 25, 30))
				.set(DIVIDER, rgb(46, 51, 59))
				.set(TITLE, rgb(255, 255, 255))
				.set(TEXT, rgb(226, 229, 233))
				.set(TEXT_SOFT, rgb(195, 200, 207))
				.set(MUTED, rgb(139, 144, 153))
				.set(MUTED_DARK, rgb(108, 113, 120))
				.set(ACCENT, rgb(76, 139, 245))
				.set(ACCENT_HOVER, rgb(110, 163, 255))
				.set(PRIMARY_BG, rgb(27, 42, 68))
				.set(PRIMARY_BG_HOVER, rgb(36, 58, 94))
				.set(ON_ACCENT, rgb(255, 255, 255))
				.set(ON_DANGER, rgb(255, 179, 179))
				.set(GHOST_TEXT, rgb(176, 182, 191))
				.set(FOCUS_RING, rgb(76, 139, 245))
				.set(DANGER, rgb(255, 107, 107))
				.set(DANGER_BG, rgb(58, 28, 30))
				.set(SUCCESS, rgb(88, 214, 141))
				.set(INFO, rgb(93, 173, 226))
				.set(BUTTON_BG, rgb(24, 27, 32))
				.set(BUTTON_HOVER, rgb(33, 37, 44))
				.set(BUTTON_BORDER, rgb(52, 58, 67))
				.set(BUTTON_DISABLED, rgb(20, 22, 26))
				.set(BUTTON_DISABLED_BORDER, rgb(35, 38, 43))
				.set(BUTTON_DISABLED_TEXT, rgb(107, 112, 120))
				.set(SELECTED_BG, rgb(35, 50, 74))
				.set(SURFACE, rgb(20, 23, 28))
				.set(SURFACE_ALT, rgb(16, 19, 23))
				.set(SURFACE_BORDER, rgb(42, 47, 55))
				.set(SURFACE_HEADER, rgb(23, 26, 31))
				.set(CHIP_BG, rgb(26, 29, 34))
				.set(CHIP_BORDER, rgb(52, 58, 67))
				.metrics(new QuillThemeMetrics(20, 220, 320, 120, 6, 20, 6))
				.build();
	}

	private static QuillThemeData light() {
		return QuillThemeData.builder()
				.set(SCREEN_BG, argb(0, 0, 0, 0))
				.set(PANEL_BG, argb(240, 244, 246, 249))
				.set(PANEL_BORDER, rgb(201, 210, 222))
				.set(PANEL_SHADOW, argb(51, 0, 0, 0))
				.set(HEADER_BG, argb(230, 232, 237, 243))
				.set(DIVIDER, rgb(211, 219, 228))
				.set(TITLE, rgb(27, 36, 48))
				.set(TEXT, rgb(43, 53, 66))
				.set(TEXT_SOFT, rgb(70, 82, 98))
				.set(MUTED, rgb(107, 120, 136))
				.set(MUTED_DARK, rgb(137, 147, 160))
				.set(ACCENT, rgb(184, 115, 14))
				.set(ACCENT_HOVER, rgb(209, 138, 30))
				.set(PRIMARY_BG, rgb(255, 241, 216))
				.set(PRIMARY_BG_HOVER, rgb(255, 228, 181))
				.set(ON_ACCENT, rgb(58, 36, 0))
				.set(ON_DANGER, rgb(143, 31, 43))
				.set(GHOST_TEXT, rgb(90, 103, 117))
				.set(FOCUS_RING, rgb(184, 115, 14))
				.set(DANGER, rgb(181, 58, 71))
				.set(DANGER_BG, rgb(251, 227, 229))
				.set(SUCCESS, rgb(31, 122, 76))
				.set(INFO, rgb(31, 95, 168))
				.set(BUTTON_BG, rgb(237, 241, 246))
				.set(BUTTON_HOVER, rgb(225, 231, 239))
				.set(BUTTON_BORDER, rgb(196, 206, 219))
				.set(BUTTON_DISABLED, rgb(240, 242, 245))
				.set(BUTTON_DISABLED_BORDER, rgb(221, 226, 232))
				.set(BUTTON_DISABLED_TEXT, rgb(154, 164, 176))
				.set(SELECTED_BG, rgb(220, 231, 247))
				.set(SURFACE, rgb(247, 249, 252))
				.set(SURFACE_ALT, rgb(237, 241, 246))
				.set(SURFACE_BORDER, rgb(211, 219, 228))
				.set(SURFACE_HEADER, rgb(232, 237, 243))
				.set(CHIP_BG, rgb(232, 237, 243))
				.set(CHIP_BORDER, rgb(203, 213, 225))
				.set(MAP_BG, rgb(228, 233, 239))
				.set(MAP_GRID, argb(51, 32, 48, 64))
				.set(MAP_UNKNOWN, rgb(216, 222, 230))
				.build();
	}

	private static QuillThemeData forest() {
		return QuillThemeData.builder()
				.set(PANEL_BG, argb(240, 12, 21, 17))
				.set(PANEL_BORDER, rgb(46, 74, 58))
				.set(HEADER_BG, argb(230, 10, 18, 14))
				.set(DIVIDER, rgb(39, 64, 47))
				.set(TITLE, rgb(234, 246, 238))
				.set(TEXT, rgb(201, 222, 208))
				.set(TEXT_SOFT, rgb(174, 202, 184))
				.set(MUTED, rgb(136, 163, 146))
				.set(MUTED_DARK, rgb(107, 133, 116))
				.set(ACCENT, rgb(111, 208, 140))
				.set(ACCENT_HOVER, rgb(143, 227, 166))
				.set(PRIMARY_BG, rgb(22, 40, 29))
				.set(PRIMARY_BG_HOVER, rgb(30, 54, 39))
				.set(ON_ACCENT, rgb(234, 246, 238))
				.set(ON_DANGER, rgb(255, 179, 179))
				.set(GHOST_TEXT, rgb(136, 163, 146))
				.set(FOCUS_RING, rgb(111, 208, 140))
				.set(DANGER, rgb(224, 122, 122))
				.set(DANGER_BG, rgb(53, 32, 31))
				.set(SUCCESS, rgb(143, 224, 168))
				.set(INFO, rgb(127, 200, 232))
				.set(BUTTON_BG, rgb(24, 37, 29))
				.set(BUTTON_HOVER, rgb(34, 53, 39))
				.set(BUTTON_BORDER, rgb(50, 74, 59))
				.set(BUTTON_DISABLED, rgb(18, 28, 22))
				.set(BUTTON_DISABLED_BORDER, rgb(31, 46, 36))
				.set(BUTTON_DISABLED_TEXT, rgb(92, 117, 99))
				.set(SELECTED_BG, rgb(38, 64, 47))
				.set(SURFACE, argb(153, 32, 51, 39))
				.set(SURFACE_ALT, argb(102, 24, 42, 32))
				.set(SURFACE_BORDER, rgb(42, 66, 51))
				.set(SURFACE_HEADER, rgb(20, 36, 26))
				.set(CHIP_BG, rgb(27, 46, 36))
				.set(CHIP_BORDER, rgb(50, 74, 59))
				.build();
	}

	private static QuillThemeData amethyst() {
		return QuillThemeData.builder()
				.set(PANEL_BG, argb(240, 18, 14, 26))
				.set(PANEL_BORDER, rgb(59, 46, 82))
				.set(HEADER_BG, argb(230, 12, 9, 20))
				.set(DIVIDER, rgb(51, 40, 70))
				.set(TITLE, rgb(245, 237, 255))
				.set(TEXT, rgb(217, 205, 234))
				.set(TEXT_SOFT, rgb(192, 178, 214))
				.set(MUTED, rgb(156, 143, 178))
				.set(MUTED_DARK, rgb(123, 110, 145))
				.set(ACCENT, rgb(195, 139, 255))
				.set(ACCENT_HOVER, rgb(215, 173, 255))
				.set(PRIMARY_BG, rgb(42, 27, 61))
				.set(PRIMARY_BG_HOVER, rgb(58, 39, 85))
				.set(ON_ACCENT, rgb(245, 237, 255))
				.set(ON_DANGER, rgb(255, 179, 200))
				.set(GHOST_TEXT, rgb(156, 143, 178))
				.set(FOCUS_RING, rgb(195, 139, 255))
				.set(DANGER, rgb(224, 122, 138))
				.set(DANGER_BG, rgb(53, 28, 38))
				.set(SUCCESS, rgb(143, 224, 192))
				.set(INFO, rgb(143, 194, 255))
				.set(BUTTON_BG, rgb(27, 21, 38))
				.set(BUTTON_HOVER, rgb(38, 29, 54))
				.set(BUTTON_BORDER, rgb(68, 51, 95))
				.set(BUTTON_DISABLED, rgb(20, 15, 28))
				.set(BUTTON_DISABLED_BORDER, rgb(36, 27, 51))
				.set(BUTTON_DISABLED_TEXT, rgb(106, 92, 128))
				.set(SELECTED_BG, rgb(53, 37, 76))
				.set(SURFACE, argb(153, 37, 27, 54))
				.set(SURFACE_ALT, argb(102, 28, 20, 41))
				.set(SURFACE_BORDER, rgb(59, 46, 82))
				.set(SURFACE_HEADER, rgb(22, 16, 32))
				.set(CHIP_BG, rgb(36, 26, 51))
				.set(CHIP_BORDER, rgb(68, 51, 95))
				.metrics(new QuillThemeMetrics(22, 220, 320, 120, 10, 26, 9))
				.build();
	}

	private static final class Data {
		private String theme;
	}
}
