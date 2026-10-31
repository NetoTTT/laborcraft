package com.foque.laborcraft.client.ui.theme;

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
				.set(SCREEN_BG, 0x55040A10)
				.set(PANEL_BG, 0xB80C141C)
				.set(PANEL_BORDER, 0x66CFEFFF)
				.set(PANEL_SHADOW, 0x66000000)
				.set(HEADER_BG, 0x40152233)
				.set(DIVIDER, 0x33CFEFFF)
				.set(TITLE, 0xFFF4FBFF)
				.set(TEXT, 0xFFD6E8F5)
				.set(TEXT_SOFT, 0xFFB7CFE0)
				.set(MUTED, 0xFF8FA9BC)
				.set(MUTED_DARK, 0xFF6E8799)
				.set(ACCENT, 0xFF7DE2FF)
				.set(ACCENT_HOVER, 0xFFB6F1FF)
				.set(PRIMARY_BG, 0x3357C8E8)
				.set(PRIMARY_BG_HOVER, 0x556FE0FF)
				.set(ON_ACCENT, 0xFF04222C)
				.set(ON_DANGER, 0xFF3A0A12)
				.set(GHOST_TEXT, 0xFFB7CFE0)
				.set(FOCUS_RING, 0xFF7DE2FF)
				.set(DANGER, 0xFFFF8A9B)
				.set(DANGER_BG, 0x33FF8A9B)
				.set(SUCCESS, 0xFF8CF0C5)
				.set(INFO, 0xFF8FD0FF)
				.set(BUTTON_BG, 0x6614222E)
				.set(BUTTON_HOVER, 0x99203344)
				.set(BUTTON_BORDER, 0x55CFEFFF)
				.set(BUTTON_DISABLED, 0x33101820)
				.set(BUTTON_DISABLED_BORDER, 0x33CFEFFF)
				.set(BUTTON_DISABLED_TEXT, 0xFF6E8799)
				.set(SELECTED_BG, 0x447DE2FF)
				.set(SURFACE, 0x55101820)
				.set(SURFACE_ALT, 0x331A2A38)
				.set(SURFACE_BORDER, 0x55CFEFFF)
				.set(SURFACE_HEADER, 0x66101C28)
				.set(CHIP_BG, 0x6614222E)
				.set(CHIP_BORDER, 0x55CFEFFF)
				.metrics(new QuillThemeMetrics(22, 220, 320, 120, 10, 26, 10))
				.build();
	}

	private static QuillThemeData flat() {
		return QuillThemeData.builder()
				.set(SCREEN_BG, 0x99070A0D)
				.set(PANEL_BG, 0xFF121418)
				.set(PANEL_BORDER, 0xFF2E333B)
				.set(PANEL_SHADOW, 0x66000000)
				.set(HEADER_BG, 0xFF16191E)
				.set(DIVIDER, 0xFF2E333B)
				.set(TITLE, 0xFFFFFFFF)
				.set(TEXT, 0xFFE2E5E9)
				.set(TEXT_SOFT, 0xFFC3C8CF)
				.set(MUTED, 0xFF8B9099)
				.set(MUTED_DARK, 0xFF6C7178)
				.set(ACCENT, 0xFF4C8BF5)
				.set(ACCENT_HOVER, 0xFF6EA3FF)
				.set(PRIMARY_BG, 0xFF1B2A44)
				.set(PRIMARY_BG_HOVER, 0xFF243A5E)
				.set(ON_ACCENT, 0xFFFFFFFF)
				.set(ON_DANGER, 0xFFFFFFFF)
				.set(GHOST_TEXT, 0xFFB0B6BF)
				.set(FOCUS_RING, 0xFF4C8BF5)
				.set(DANGER, 0xFFFF6B6B)
				.set(DANGER_BG, 0xFF3A1C1E)
				.set(SUCCESS, 0xFF58D68D)
				.set(INFO, 0xFF5DADE2)
				.set(BUTTON_BG, 0xFF181B20)
				.set(BUTTON_HOVER, 0xFF21252C)
				.set(BUTTON_BORDER, 0xFF343A43)
				.set(BUTTON_DISABLED, 0xFF14161A)
				.set(BUTTON_DISABLED_BORDER, 0xFF23262B)
				.set(BUTTON_DISABLED_TEXT, 0xFF6B7078)
				.set(SELECTED_BG, 0xFF23324A)
				.set(SURFACE, 0xFF14171C)
				.set(SURFACE_ALT, 0xFF101317)
				.set(SURFACE_BORDER, 0xFF2A2F37)
				.set(SURFACE_HEADER, 0xFF171A1F)
				.set(CHIP_BG, 0xFF1A1D22)
				.set(CHIP_BORDER, 0xFF343A43)
				.metrics(new QuillThemeMetrics(20, 220, 320, 120, 6, 20, 6))
				.build();
	}

	private static QuillThemeData light() {
		return QuillThemeData.builder()
				.set(SCREEN_BG, 0xFFE9EDF2)
				.set(PANEL_BG, 0xFFF4F6F9)
				.set(PANEL_BORDER, 0xFFC9D2DE)
				.set(PANEL_SHADOW, 0x33000000)
				.set(HEADER_BG, 0xFFE8EDF3)
				.set(DIVIDER, 0xFFD3DBE4)
				.set(TITLE, 0xFF1B2430)
				.set(TEXT, 0xFF2B3542)
				.set(TEXT_SOFT, 0xFF465262)
				.set(MUTED, 0xFF6B7888)
				.set(MUTED_DARK, 0xFF8993A0)
				.set(ACCENT, 0xFFB8730E)
				.set(ACCENT_HOVER, 0xFFD18A1E)
				.set(PRIMARY_BG, 0xFFFFF1D8)
				.set(PRIMARY_BG_HOVER, 0xFFFFE4B5)
				.set(ON_ACCENT, 0xFF3A2400)
				.set(ON_DANGER, 0xFFFFFFFF)
				.set(GHOST_TEXT, 0xFF5A6775)
				.set(FOCUS_RING, 0xFFB8730E)
				.set(DANGER, 0xFFB53A47)
				.set(DANGER_BG, 0xFFFBE3E5)
				.set(SUCCESS, 0xFF1F7A4C)
				.set(INFO, 0xFF1F5FA8)
				.set(BUTTON_BG, 0xFFEDF1F6)
				.set(BUTTON_HOVER, 0xFFE1E7EF)
				.set(BUTTON_BORDER, 0xFFC4CEDB)
				.set(BUTTON_DISABLED, 0xFFF0F2F5)
				.set(BUTTON_DISABLED_BORDER, 0xFFDDE2E8)
				.set(BUTTON_DISABLED_TEXT, 0xFF9AA4B0)
				.set(SELECTED_BG, 0xFFDCE7F7)
				.set(SURFACE, 0xFFF7F9FC)
				.set(SURFACE_ALT, 0xFFEDF1F6)
				.set(SURFACE_BORDER, 0xFFD3DBE4)
				.set(SURFACE_HEADER, 0xFFE8EDF3)
				.set(CHIP_BG, 0xFFE8EDF3)
				.set(CHIP_BORDER, 0xFFCBD5E1)
				.set(MAP_BG, 0xFFE4E9EF)
				.set(MAP_GRID, 0x33203040)
				.set(MAP_UNKNOWN, 0xFFD8DEE6)
				.build();
	}

	private static QuillThemeData forest() {
		return QuillThemeData.builder()
				.set(PANEL_BG, 0xF00C1511)
				.set(PANEL_BORDER, 0xFF2E4A3A)
				.set(HEADER_BG, 0xE60A120E)
				.set(DIVIDER, 0xFF27402F)
				.set(TITLE, 0xFFEAF6EE)
				.set(TEXT, 0xFFC9DED0)
				.set(TEXT_SOFT, 0xFFAECAB8)
				.set(MUTED, 0xFF88A392)
				.set(MUTED_DARK, 0xFF6B8574)
				.set(ACCENT, 0xFF6FD08C)
				.set(ACCENT_HOVER, 0xFF8FE3A6)
				.set(PRIMARY_BG, 0xFF16281D)
				.set(PRIMARY_BG_HOVER, 0xFF1E3627)
				.set(ON_ACCENT, 0xFF062012)
				.set(ON_DANGER, 0xFF2B0A0A)
				.set(GHOST_TEXT, 0xFF88A392)
				.set(FOCUS_RING, 0xFF6FD08C)
				.set(DANGER, 0xFFE07A7A)
				.set(DANGER_BG, 0xFF35201F)
				.set(SUCCESS, 0xFF8FE0A8)
				.set(INFO, 0xFF7FC8E8)
				.set(BUTTON_BG, 0xFF18251D)
				.set(BUTTON_HOVER, 0xFF223527)
				.set(BUTTON_BORDER, 0xFF324A3B)
				.set(BUTTON_DISABLED, 0xFF121C16)
				.set(BUTTON_DISABLED_BORDER, 0xFF1F2E24)
				.set(BUTTON_DISABLED_TEXT, 0xFF5C7563)
				.set(SELECTED_BG, 0xFF26402F)
				.set(SURFACE, 0x99203327)
				.set(SURFACE_ALT, 0x66182A20)
				.set(SURFACE_BORDER, 0xFF2A4233)
				.set(SURFACE_HEADER, 0xFF14241A)
				.set(CHIP_BG, 0xFF1B2E24)
				.set(CHIP_BORDER, 0xFF324A3B)
				.build();
	}

	private static QuillThemeData amethyst() {
		return QuillThemeData.builder()
				.set(PANEL_BG, 0xF0120E1A)
				.set(PANEL_BORDER, 0xFF3B2E52)
				.set(HEADER_BG, 0xE60C0914)
				.set(DIVIDER, 0xFF332846)
				.set(TITLE, 0xFFF5EDFF)
				.set(TEXT, 0xFFD9CDEA)
				.set(TEXT_SOFT, 0xFFC0B2D6)
				.set(MUTED, 0xFF9C8FB2)
				.set(MUTED_DARK, 0xFF7B6E91)
				.set(ACCENT, 0xFFC38BFF)
				.set(ACCENT_HOVER, 0xFFD7ADFF)
				.set(PRIMARY_BG, 0xFF2A1B3D)
				.set(PRIMARY_BG_HOVER, 0xFF3A2755)
				.set(ON_ACCENT, 0xFF1D0B33)
				.set(ON_DANGER, 0xFF330A18)
				.set(GHOST_TEXT, 0xFF9C8FB2)
				.set(FOCUS_RING, 0xFFC38BFF)
				.set(DANGER, 0xFFE07A8A)
				.set(DANGER_BG, 0xFF351C26)
				.set(SUCCESS, 0xFF8FE0C0)
				.set(INFO, 0xFF8FC2FF)
				.set(BUTTON_BG, 0xFF1B1526)
				.set(BUTTON_HOVER, 0xFF261D36)
				.set(BUTTON_BORDER, 0xFF44335F)
				.set(BUTTON_DISABLED, 0xFF140F1C)
				.set(BUTTON_DISABLED_BORDER, 0xFF241B33)
				.set(BUTTON_DISABLED_TEXT, 0xFF6A5C80)
				.set(SELECTED_BG, 0xFF35254C)
				.set(SURFACE, 0x99251B36)
				.set(SURFACE_ALT, 0x661C1429)
				.set(SURFACE_BORDER, 0xFF3B2E52)
				.set(SURFACE_HEADER, 0xFF161020)
				.set(CHIP_BG, 0xFF241A33)
				.set(CHIP_BORDER, 0xFF44335F)
				.metrics(new QuillThemeMetrics(22, 220, 320, 120, 10, 26, 9))
				.build();
	}

	private static final class Data {
		private String theme;
	}
}
