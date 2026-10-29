package com.foque.laborcraft.client.ui;

import com.quillui.theme.QuillThemeData;

/**
 * Transitional alias for screens that still use the old token names. New screens use
 * {@link QuillThemeData} roles directly.
 *
 * @deprecated use {@link QuillThemeData} and its {@code ColorRole} values.
 */
@Deprecated
public final class LcTheme {
	private static final QuillThemeData THEME = QuillThemeData.DEFAULT;

	public static final int ACCENT = THEME.color(QuillThemeData.ColorRole.ACCENT);
	public static final int BUTTON_BG = THEME.color(QuillThemeData.ColorRole.BUTTON_BG);
	public static final int BUTTON_BORDER = THEME.color(QuillThemeData.ColorRole.BUTTON_BORDER);
	public static final int BUTTON_HOVER = THEME.color(QuillThemeData.ColorRole.BUTTON_HOVER);
	public static final int DANGER = THEME.color(QuillThemeData.ColorRole.DANGER);
	public static final int DIVIDER = THEME.color(QuillThemeData.ColorRole.DIVIDER);
	public static final int MUTED = THEME.color(QuillThemeData.ColorRole.MUTED);
	public static final int MUTED_DARK = THEME.color(QuillThemeData.ColorRole.MUTED_DARK);
	public static final int PRIMARY_BG = THEME.color(QuillThemeData.ColorRole.PRIMARY_BG);
	public static final int SELECTED_BG = THEME.color(QuillThemeData.ColorRole.SELECTED_BG);
	public static final int TEXT = THEME.color(QuillThemeData.ColorRole.TEXT);
	public static final int TEXT_SOFT = THEME.color(QuillThemeData.ColorRole.TEXT_SOFT);
	public static final int TITLE = THEME.color(QuillThemeData.ColorRole.TITLE);

	private LcTheme() {
	}
}
