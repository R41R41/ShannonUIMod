package com.shannon.ui.screen.tab;

import com.shannon.ShannonUIMod;
import com.shannon.model.ReactionSettingsState;
import com.shannon.sync.Actions;
import com.shannon.sync.StateChannels;
import com.shannon.ui.ClientConfig;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.net.ClientActions;
import com.shannon.ui.screen.DebugLogScreen;
import com.shannon.ui.screen.widget.ScrollArea;
import com.shannon.ui.screen.widget.ValueSlider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Display preferences, how often the bot speaks up, and voice, as vanilla option widgets.
 *
 * <p>Sliders that change the bot's settings send once the knob has rested for a moment, so
 * dragging does not flood the backend.
 */
public class SettingsTab extends ShannonTab {
    private static final Identifier ICON = Identifier.of(ShannonUIMod.MOD_ID, "textures/settings.png");
    private static final int ROW_H = 22;
    private static final int HEADER_H = 13;
    private static final long SEND_DELAY_MS = 400;
    /** The backend never fires this reaction, so it has no slider. */
    private static final String UNUSED_REACTION = "player_speak";

    private record Header(Text text, int offset) {
    }

    /** A setting change waiting to be sent. */
    private record Pending(Runnable send, long at) {
    }

    private ScrollArea area;
    private final List<Header> headers = new ArrayList<>();
    private final Map<String, Pending> pending = new HashMap<>();
    private ReactionSettingsState builtFrom;
    private int offset;
    private int column;
    private static double savedScroll;

    @Override
    public Identifier icon() {
        return ICON;
    }

    @Override
    public Text title() {
        return Text.translatable("shannonuimod.tab.settings");
    }

    @Override
    protected void build() {
        headers.clear();
        area = new ScrollArea(y, y + h - 24);
        offset = 0;
        column = 0;
        ClientConfig config = shannon.config();

        header("shannonuimod.settings.display");
        cell(toggle(() -> Text.translatable("shannonuimod.settings.card." + config.cardCorner.name().toLowerCase()),
                () -> config.cardCorner = config.cardCorner.next(), "shannonuimod.settings.card.tip"));
        cell(toggle(() -> onOff("shannonuimod.settings.speech", config.showSpeech),
                () -> config.showSpeech = !config.showSpeech, "shannonuimod.settings.speech.tip"));
        cell(toggle(() -> onOff("shannonuimod.settings.toasts", config.showToasts),
                () -> config.showToasts = !config.showToasts, "shannonuimod.settings.toasts.tip"));
        cell(toggle(() -> onOff("shannonuimod.settings.show_card", config.showCard),
                () -> config.showCard = !config.showCard, "shannonuimod.settings.show_card.tip"));

        builtFrom = shannon.store().get(StateChannels.REACTIONS);
        if (builtFrom != null && builtFrom.reactions != null) {
            header("shannonuimod.settings.reactions");
            for (ReactionSettingsState.Reaction reaction : builtFrom.reactions) {
                if (reaction.eventType == null || UNUSED_REACTION.equals(reaction.eventType)) {
                    continue;
                }
                Text name = Text.translatableWithFallback("shannonuimod.reaction." + reaction.eventType, reaction.eventType);
                int start = reaction.enabled ? reaction.probability : 0;
                cell(new ValueSlider(0, 0, cellWidth(), 20, 0, 100, start,
                        value -> value <= 0
                                ? Text.translatable("shannonuimod.settings.option", name, ScreenTexts.OFF)
                                : Text.translatable("shannonuimod.settings.percent", name, (int) value),
                        value -> later("reaction:" + reaction.eventType, () -> ClientActions.send(Actions.REACTION_UPDATE,
                                new Actions.ReactionUpdate(reaction.eventType, value > 0, (int) value)))));
            }
        }
        if (builtFrom != null && builtFrom.hostileDetection != null) {
            header("shannonuimod.settings.hostile");
            ReactionSettingsState.HostileDetection hostile = builtFrom.hostileDetection;
            cell(new ValueSlider(0, 0, cellWidth(), 20, 1, 16, hostile.criticalDistance,
                    value -> Text.translatable("shannonuimod.settings.blocks",
                            Text.translatable("shannonuimod.settings.hostile.critical"), (int) value),
                    value -> later("hostile:critical", () -> {
                        Actions.ReactionUpdate update = hostileUpdate();
                        update.criticalDistance = value;
                        ClientActions.send(Actions.REACTION_UPDATE, update);
                    })));
            cell(new ValueSlider(0, 0, cellWidth(), 20, 4, 48, hostile.detectionDistance,
                    value -> Text.translatable("shannonuimod.settings.blocks",
                            Text.translatable("shannonuimod.settings.hostile.detection"), (int) value),
                    value -> later("hostile:detection", () -> {
                        Actions.ReactionUpdate update = hostileUpdate();
                        update.detectionDistance = value;
                        ClientActions.send(Actions.REACTION_UPDATE, update);
                    })));
        }

        header("shannonuimod.settings.voice");
        cell(ButtonWidget.builder(Text.translatable("shannonuimod.settings.voice_mode"),
                        button -> ClientActions.send(Actions.VOICE_MODE, new Actions.Empty()))
                .tooltip(Tooltip.of(Text.translatable("shannonuimod.settings.voice_mode.tip")))
                .dimensions(0, 0, cellWidth(), 20).build());
        cell(ButtonWidget.builder(Text.translatable("shannonuimod.settings.keys"),
                        button -> client.setScreen(new KeybindsScreen(screen, client.options)))
                .dimensions(0, 0, cellWidth(), 20).build());
        area.layoutColumns(x, w, 4);
        area.setScroll(savedScroll);

        int footerY = y + h - 20;
        int half = (w - 4) / 2;
        add(ButtonWidget.builder(Text.translatable("shannonuimod.settings.logs"),
                        button -> client.setScreen(new DebugLogScreen(screen)))
                .dimensions(x, footerY, half, 20).build());
        if (builtFrom != null) {
            // Only offered once the bot has sent the settings it would reset.
            add(ButtonWidget.builder(Text.translatable("shannonuimod.settings.reset"),
                            button -> ClientActions.send(Actions.REACTION_RESET, new Actions.Empty()))
                    .tooltip(Tooltip.of(Text.translatable("shannonuimod.settings.reset.tip")))
                    .dimensions(x + w - half, footerY, half, 20).build());
        }
    }

    private Actions.ReactionUpdate hostileUpdate() {
        ReactionSettingsState state = shannon.store().get(StateChannels.REACTIONS);
        Actions.ReactionUpdate update = new Actions.ReactionUpdate("hostile_approach", true, 100);
        if (state != null && state.reactions != null) {
            for (ReactionSettingsState.Reaction reaction : state.reactions) {
                if ("hostile_approach".equals(reaction.eventType)) {
                    update.enabled = reaction.enabled;
                    update.probability = reaction.probability;
                }
            }
        }
        return update;
    }

    /** A cell's width before {@link ScrollArea#layoutColumns} sizes it. */
    private int cellWidth() {
        return (w - 4) / 2;
    }

    private void header(String key) {
        if (column == 1) {
            column = 0;
            offset += ROW_H;
        }
        headers.add(new Header(Text.translatable(key), offset));
        offset += HEADER_H;
    }

    private void cell(ClickableWidget widget) {
        add(area.placeCell(widget, offset, column));
        if (column == 1) {
            column = 0;
            offset += ROW_H;
        } else {
            column = 1;
        }
        area.setContentHeight(offset + ROW_H);
    }

    private ButtonWidget toggle(Supplier<Text> label, Runnable change, String tipKey) {
        ButtonWidget[] self = new ButtonWidget[1];
        self[0] = ButtonWidget.builder(label.get(), button -> {
                    change.run();
                    shannon.config().save();
                    self[0].setMessage(label.get());
                })
                .tooltip(Tooltip.of(Text.translatable(tipKey)))
                .dimensions(0, 0, cellWidth(), 20)
                .build();
        return self[0];
    }

    private static Text onOff(String key, boolean on) {
        return Text.translatable("shannonuimod.settings.option", Text.translatable(key), on ? ScreenTexts.ON : ScreenTexts.OFF);
    }

    private void later(String key, Runnable send) {
        pending.put(key, new Pending(send, System.currentTimeMillis()));
    }

    @Override
    public void tick() {
        long now = System.currentTimeMillis();
        pending.entrySet().removeIf(entry -> {
            if (now - entry.getValue().at() < SEND_DELAY_MS) {
                return false;
            }
            entry.getValue().send().run();
            return true;
        });
        ReactionSettingsState state = shannon.store().get(StateChannels.REACTIONS);
        if (state != builtFrom && pending.isEmpty() && (builtFrom == null || !screenDragging())) {
            savedScroll = area.scroll();
            screen.rebuild();
        }
    }

    private boolean screenDragging() {
        return screen.isDragging();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        for (Header header : headers) {
            if (area.visible(header.offset(), 9)) {
                Gui.label(context, header.text(), x + 1, area.y(header.offset()) + 2);
            }
        }
        area.drawScrollbar(context, x + w);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        boolean scrolled = area.scrollBy(amount);
        if (scrolled) {
            savedScroll = area.scroll();
        }
        return scrolled;
    }
}
