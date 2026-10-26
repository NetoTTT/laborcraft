package com.foque.laborcraft.client.ui.widget;

import com.quillui.widget.QuillScrollList;

/**
 * LaborCraft-facing alias for the shared Quill UI scroll list.
 */
public final class LcScrollList<T> extends QuillScrollList<T> {
	public LcScrollList(
			int x,
			int y,
			int width,
			int height,
			int rowHeight,
			RowRenderer<T> rowRenderer,
			ClickHandler<T> clickHandler
	) {
		super(x, y, width, height, rowHeight, rowRenderer, clickHandler);
	}
}
