package com.shannon.ui.screen.tab;

import com.shannon.ShannonUIMod;
import com.shannon.model.ConstantSkillsState;
import com.shannon.sync.StateChannels;
import com.shannon.ui.gfx.Gui;
import com.shannon.ui.gfx.Palette;
import com.shannon.ui.net.ClientActions;
import com.shannon.ui.screen.widget.ScrollArea;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The bot's always-on skills as vanilla option buttons, grouped by what they are for. */
public class SkillsTab extends ShannonTab {
    private static final Identifier ICON = Identifier.of(ShannonUIMod.MOD_ID, "textures/passive_skill.png");
    private static final int BUTTON_H = 20;
    private static final int HEADER_H = 13;

    /** Groups in display order, each with the backend skill ids it holds. Unknown skills go to "other". */
    private static final Map<String, List<String>> GROUPS = new LinkedHashMap<>();
    /** Skills the backend keeps on for the bot's safety. */
    private static final List<String> ALWAYS_ON = List.of("auto-swim", "auto-avoid-dragon-breath");

    static {
        GROUPS.put("survive", List.of("auto-eat", "auto-sleep", "auto-swim"));
        GROUPS.put("danger", List.of("auto-run-from-hostiles", "auto-avoid-projectile-range", "auto-avoid-dragon-breath"));
        GROUPS.put("together", List.of("auto-follow", "auto-pick-up-item"));
        GROUPS.put("look", List.of("auto-face-speaker", "auto-face-nearest-entity", "auto-face-moved-entity",
                "auto-face-updated-block", "auto-update-looking-at"));
        GROUPS.put("sense", List.of("auto-detect-block-or-entity", "auto-update-state"));
        GROUPS.put("other", List.of());
    }

    private record Header(Text text, int offset) {
    }

    private ScrollArea area;
    private final List<Header> headers = new ArrayList<>();
    private final Map<String, ButtonWidget> buttons = new HashMap<>();
    /** Toggles sent but not yet confirmed by the backend, so the button flips at once. */
    private final Map<String, Boolean> pending = new HashMap<>();
    private ConstantSkillsState builtFrom;
    private static double savedScroll;

    @Override
    public Identifier icon() {
        return ICON;
    }

    @Override
    public Text title() {
        return Text.translatable("shannonuimod.tab.skills");
    }

    @Override
    public Text status() {
        ConstantSkillsState state = shannon.store().get(StateChannels.SKILLS);
        if (state == null || state.skills == null) {
            return null;
        }
        long on = state.skills.stream().filter(skill -> isOn(skill)).count();
        return Text.translatable("shannonuimod.skills.count", on, state.skills.size());
    }

    @Override
    protected void build() {
        headers.clear();
        buttons.clear();
        area = new ScrollArea(y, y + h);
        builtFrom = shannon.store().get(StateChannels.SKILLS);
        if (builtFrom == null || builtFrom.skills == null) {
            return;
        }
        Map<String, List<ConstantSkillsState.Skill>> grouped = new LinkedHashMap<>();
        for (String group : GROUPS.keySet()) {
            grouped.put(group, new ArrayList<>());
        }
        for (ConstantSkillsState.Skill skill : builtFrom.skills) {
            grouped.get(groupOf(skill.skillName)).add(skill);
        }
        int offset = 0;
        for (Map.Entry<String, List<ConstantSkillsState.Skill>> group : grouped.entrySet()) {
            if (group.getValue().isEmpty()) {
                continue;
            }
            headers.add(new Header(Text.translatable("shannonuimod.skills.group." + group.getKey()), offset));
            offset += HEADER_H;
            for (int i = 0; i < group.getValue().size(); i++) {
                ConstantSkillsState.Skill skill = group.getValue().get(i);
                int column = i % 2;
                ButtonWidget button = ButtonWidget.builder(label(skill), b -> toggle(skill))
                        .dimensions(x, 0, w, BUTTON_H)
                        .tooltip(Tooltip.of(description(skill)))
                        .build();
                button.active = !ALWAYS_ON.contains(skill.skillName);
                buttons.put(skill.skillName, add(area.placeCell(button, offset, column)));
                if (column == 1 || i == group.getValue().size() - 1) {
                    offset += BUTTON_H + 2;
                }
            }
            offset += 2;
        }
        area.setContentHeight(offset);
        area.layoutColumns(x, w, 4);
        area.setScroll(savedScroll);
    }

    private static String groupOf(String skillName) {
        for (Map.Entry<String, List<String>> group : GROUPS.entrySet()) {
            if (group.getValue().contains(skillName)) {
                return group.getKey();
            }
        }
        return "other";
    }

    private boolean isOn(ConstantSkillsState.Skill skill) {
        return pending.getOrDefault(skill.skillName, skill.status);
    }

    private Text name(ConstantSkillsState.Skill skill) {
        String key = "shannonuimod.skill." + skill.skillName;
        return Text.translatableWithFallback(key, skill.skillName);
    }

    private Text description(ConstantSkillsState.Skill skill) {
        Text name = name(skill).copy().formatted(Formatting.WHITE);
        String key = "shannonuimod.skill." + skill.skillName + ".desc";
        Text desc = Text.translatableWithFallback(key, skill.description == null ? "" : skill.description);
        return Text.empty().append(name).append(Text.literal("\n")).append(desc.copy().formatted(Formatting.GRAY));
    }

    private Text label(ConstantSkillsState.Skill skill) {
        Text value;
        if (ALWAYS_ON.contains(skill.skillName)) {
            value = Text.translatable("shannonuimod.skills.always");
        } else if (isOn(skill)) {
            value = Text.translatable("options.on").formatted(Formatting.GREEN);
        } else {
            value = Text.translatable("options.off").formatted(Formatting.GRAY);
        }
        return Text.translatable("shannonuimod.skills.option", name(skill), value);
    }

    private void toggle(ConstantSkillsState.Skill skill) {
        boolean next = !isOn(skill);
        if (ClientActions.toggleSkill(skill.skillName, next)) {
            pending.put(skill.skillName, next);
        }
    }

    @Override
    public void tick() {
        ConstantSkillsState state = shannon.store().get(StateChannels.SKILLS);
        if (state != builtFrom) {
            // The backend confirmed: drop guesses it has answered, then rebuild if skills changed.
            if (state != null && state.skills != null) {
                for (ConstantSkillsState.Skill skill : state.skills) {
                    if (pending.containsKey(skill.skillName) && pending.get(skill.skillName) == skill.status) {
                        pending.remove(skill.skillName);
                    }
                }
            }
            savedScroll = area == null ? 0 : area.scroll();
            screen.rebuild();
            return;
        }
        if (state != null && state.skills != null) {
            for (ConstantSkillsState.Skill skill : state.skills) {
                ButtonWidget button = buttons.get(skill.skillName);
                if (button != null) {
                    button.setMessage(label(skill));
                }
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (builtFrom == null || builtFrom.skills == null) {
            Gui.label(context, Text.translatable("shannonuimod.skills.none"), x + 1, y + 2);
            return;
        }
        for (Header header : headers) {
            if (area.visible(header.offset(), 9)) {
                Gui.label(context, header.text(), x + 1, area.y(header.offset()) + 2);
            }
        }
        area.drawScrollbar(context, x + w);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        boolean scrolled = area != null && area.scrollBy(amount);
        if (scrolled) {
            savedScroll = area.scroll();
        }
        return scrolled;
    }
}
