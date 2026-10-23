package com.foque.laborcraft.client.ui.component;

import java.util.List;

public final class LcPagination<T> {
	private List<T> items;
	private final int pageSize;
	private int page;

	public LcPagination(List<T> items, int pageSize) {
		if (pageSize <= 0) {
			throw new IllegalArgumentException("pageSize must be positive");
		}

		this.items = List.copyOf(items);
		this.pageSize = pageSize;
		setPage(0);
	}

	public void setItems(List<T> items) {
		this.items = List.copyOf(items);
		setPage(this.page);
	}

	public List<T> items() {
		return items;
	}

	public int pageSize() {
		return pageSize;
	}

	public int total() {
		return items.size();
	}

	public int pageCount() {
		return Math.max(1, (items.size() + pageSize - 1) / pageSize);
	}

	public int pageIndex() {
		return page;
	}

	public List<T> pageItems() {
		int from = Math.min(page * pageSize, items.size());
		int to = Math.min(from + pageSize, items.size());
		return items.subList(from, to);
	}

	public boolean hasPrevious() {
		return page > 0;
	}

	public boolean hasNext() {
		return page + 1 < pageCount();
	}

	public boolean previous() {
		if (!hasPrevious()) {
			return false;
		}

		page--;
		return true;
	}

	public boolean next() {
		if (!hasNext()) {
			return false;
		}

		page++;
		return true;
	}

	public void setPage(int page) {
		this.page = Math.max(0, Math.min(page, pageCount() - 1));
	}
}
