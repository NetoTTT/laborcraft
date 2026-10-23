package com.foque.laborcraft.client.ui.chat;

import com.foque.laborcraft.command.LaborCraftCommands;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class LcValuePrompt {
	private enum Kind {
		INTEGER,
		TEXT,
		CHOICE
	}

	private record Quick(String label, String value) {
	}

	private static LcValuePrompt active;

	private final Kind kind;
	private final Component label;
	private final String command;
	private final int min;
	private final int max;
	private final int maxLength;
	private final List<String> choices;
	private final List<Quick> quick = new ArrayList<>();
	private String current;
	private String cancelCommand = LaborCraftCommands.UI;
	private long ignoreCancelUntil;

	private LcValuePrompt(Kind kind, Component label, String command, int min, int max, int maxLength,
			List<String> choices) {
		this.kind = kind;
		this.label = label;
		this.command = normalize(command);
		this.min = min;
		this.max = max;
		this.maxLength = maxLength;
		this.choices = List.copyOf(choices);
	}

	public static LcValuePrompt integer(Component label, String command, int min, int max) {
		if (min > max) {
			throw new IllegalArgumentException("min must be <= max");
		}
		return new LcValuePrompt(Kind.INTEGER, label, command, min, max, 0, List.of());
	}

	public static LcValuePrompt text(Component label, String command, int maxLength) {
		if (maxLength < 1) {
			throw new IllegalArgumentException("maxLength must be positive");
		}
		return new LcValuePrompt(Kind.TEXT, label, command, 0, 0, maxLength, List.of());
	}

	public static LcValuePrompt choice(Component label, String command, List<String> choices) {
		if (choices.isEmpty()) {
			throw new IllegalArgumentException("choices must not be empty");
		}
		return new LcValuePrompt(Kind.CHOICE, label, command, 0, 0, 0, choices);
	}

	public LcValuePrompt current(String current) {
		this.current = current;
		return this;
	}

	public LcValuePrompt cancel(String cancelCommand) {
		this.cancelCommand = cancelCommand;
		return this;
	}

	public LcValuePrompt quick(String label, String value) {
		if (kind == Kind.INTEGER) {
			Integer.parseInt(value);
		}
		quick.add(new Quick(label, value));
		return this;
	}

	public void ask() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}

		active = this;
		ignoreCancelUntil = 0L;
		minecraft.gui.hud.getChat().addClientSystemMessage(buildMessage());
		minecraft.gui.openChatScreen(ChatComponent.ChatMethod.MESSAGE);
	}

	public static boolean allowChat(String message) {
		if (active == null) {
			return true;
		}

		active.handleChat(message);
		return false;
	}

	public static boolean allowCommand(String command) {
		active = null;
		return true;
	}

	public static void chatCanceled(String message) {
		LcValuePrompt prompt = active;
		if (prompt != null && System.currentTimeMillis() >= prompt.ignoreCancelUntil) {
			active = null;
		}
	}

	public static void cancelActive() {
		active = null;
	}

	private void handleChat(String message) {
		String value = message.trim();
		if (value.equalsIgnoreCase("cancel") || value.equalsIgnoreCase("cancelar")) {
			active = null;
			sendCommand(LaborCraftCommands.UI);
			return;
		}

		Component problem = validate(value);
		if (problem != null) {
			showError(problem);
			reopenChat();
			return;
		}

		active = null;
		sendCommand(command + value);
	}

	private Component validate(String value) {
		return switch (kind) {
			case INTEGER -> {
				try {
					int parsed = Integer.parseInt(value);
					if (parsed < min || parsed > max) {
						yield Component.translatable("laborcraft.prompt.invalid.range", min, max);
					}
					yield null;
				} catch (NumberFormatException exception) {
					yield Component.translatable("laborcraft.prompt.invalid.integer");
				}
			}
			case TEXT -> {
				if (value.isEmpty()) {
					yield Component.translatable("laborcraft.prompt.invalid.empty");
				}
				if (value.length() > maxLength) {
					yield Component.translatable("laborcraft.prompt.invalid.length", maxLength);
				}
				yield null;
			}
			case CHOICE -> choices.contains(value)
					? null
					: Component.translatable("laborcraft.prompt.invalid.choice", String.join(", ", choices));
		};
	}

	private void reopenChat() {
		ignoreCancelUntil = System.currentTimeMillis() + 500L;
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> minecraft.gui.openChatScreen(ChatComponent.ChatMethod.MESSAGE));
	}

	private static void showError(Component problem) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.gui.hud.getChat().addClientSystemMessage(
				Component.empty()
						.append(Component.literal("[LaborCraft] ").withStyle(ChatFormatting.GOLD))
						.append(problem.copy().withStyle(ChatFormatting.RED))
		);
	}

	private static void sendCommand(String fullCommand) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			minecraft.player.connection.sendCommand(fullCommand);
		}
	}

	private Component buildMessage() {
		MutableComponent message = Component.empty()
				.append(Component.literal("[LaborCraft] ").withStyle(ChatFormatting.GOLD))
				.append(label.copy().withStyle(ChatFormatting.WHITE));
		message.append("\n").append(hint().copy().withStyle(ChatFormatting.GRAY));

		if (current != null && !current.isBlank()) {
			message.append("\n")
					.append(Component.translatable("laborcraft.prompt.current", current).withStyle(ChatFormatting.DARK_GRAY));
		}

		message.append("\n")
				.append(Component.translatable("laborcraft.prompt.type").withStyle(ChatFormatting.GRAY));
		message.append("\n")
				.append(Component.translatable("laborcraft.prompt.command", "/" + command + "<value>")
						.withStyle(ChatFormatting.DARK_AQUA));
		message.append("\n");

		message.append(clickable(
				Component.translatable("laborcraft.prompt.cancel"),
				new ClickEvent.RunCommand("/" + cancelCommand),
				ChatFormatting.RED,
				Component.translatable("laborcraft.prompt.cancel.tip", "/" + cancelCommand)
		));

		if (kind == Kind.CHOICE) {
			for (String choice : choices) {
				message.append(" ").append(suggestion(Component.literal(choice), choice));
			}
		}

		for (Quick entry : quick) {
			message.append(" ").append(suggestion(Component.literal(entry.label()), entry.value()));
		}

		return message;
	}

	private Component hint() {
		return switch (kind) {
			case INTEGER -> Component.translatable("laborcraft.prompt.integer", min, max);
			case TEXT -> Component.translatable("laborcraft.prompt.text", maxLength);
			case CHOICE -> Component.translatable("laborcraft.prompt.choice");
		};
	}

	private Component suggestion(Component label, String value) {
		String fullCommand = "/" + command + value;
		return clickable(
				Component.literal("[").append(label).append("]"),
				new ClickEvent.SuggestCommand(value),
				ChatFormatting.AQUA,
				Component.translatable("laborcraft.prompt.insert.tip", fullCommand)
		);
	}

	private static Component clickable(Component label, ClickEvent click, ChatFormatting color, Component tip) {
		Style style = Style.EMPTY
				.withColor(color)
				.withClickEvent(click)
				.withHoverEvent(new HoverEvent.ShowText(tip));
		return label.copy().withStyle(style);
	}

	private static String normalize(String command) {
		if (command == null || command.isBlank()) {
			throw new IllegalArgumentException("command must not be blank");
		}
		return command.endsWith(" ") ? command : command + " ";
	}
}
