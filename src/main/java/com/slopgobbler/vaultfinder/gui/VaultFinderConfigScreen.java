package com.slopgobbler.vaultfinder.gui;

import com.slopgobbler.vaultfinder.config.VaultFinderConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * In-game configuration screen for VaultFinder Pro, integrated with ModMenu.
 */
@Environment(EnvType.CLIENT)
public class VaultFinderConfigScreen extends Screen {

    private final Screen parent;
    private final VaultFinderConfig config;

    private static final int[] RANGE_PRESETS = new int[]{16, 32, 64, 96, 128, 192, 256};

    public VaultFinderConfigScreen(Screen parent) {
        super(Text.literal("VaultFinder Pro Settings"));
        this.parent = parent;
        this.config = VaultFinderConfig.get();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 55;
        int buttonWidth = 220;
        int buttonHeight = 20;
        int spacing = 24;

        // 1. 3D Radar Toggle
        addDrawableChild(ButtonWidget.builder(
                getRadarText(),
                button -> {
                    config.setRadarEnabled(!config.isRadarEnabled());
                    button.setMessage(getRadarText());
                }
        ).dimensions(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight).build());

        // 2. Shulker Peek Toggle
        addDrawableChild(ButtonWidget.builder(
                getShulkerPeekText(),
                button -> {
                    config.setShulkerPeekEnabled(!config.isShulkerPeekEnabled());
                    button.setMessage(getShulkerPeekText());
                }
        ).dimensions(centerX - buttonWidth / 2, startY + spacing, buttonWidth, buttonHeight).build());

        // 3. X-Ray Through Blocks Toggle
        addDrawableChild(ButtonWidget.builder(
                getXrayText(),
                button -> {
                    config.setXrayEnabled(!config.isXrayEnabled());
                    button.setMessage(getXrayText());
                }
        ).dimensions(centerX - buttonWidth / 2, startY + spacing * 2, buttonWidth, buttonHeight).build());

        // 4. Highlighting Range Cycle
        addDrawableChild(ButtonWidget.builder(
                getRangeText(),
                button -> {
                    cycleRange();
                    button.setMessage(getRangeText());
                }
        ).dimensions(centerX - buttonWidth / 2, startY + spacing * 3, buttonWidth, buttonHeight).build());

        // 5. Match Colors Cycle (Spring Green / Bright Yellow)
        addDrawableChild(ButtonWidget.builder(
                getColorsText(),
                button -> {
                    cycleColors();
                    button.setMessage(getColorsText());
                }
        ).dimensions(centerX - buttonWidth / 2, startY + spacing * 4, buttonWidth, buttonHeight).build());

        // 6. Reset Defaults
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Reset to Defaults"),
                button -> {
                    config.setHighlightRange(VaultFinderConfig.DEFAULT_RANGE);
                    config.setRadarEnabled(true);
                    config.setShulkerPeekEnabled(true);
                    config.setXrayEnabled(true);
                    config.setMatchingColor(VaultFinderConfig.DEFAULT_MATCHING_COLOR);
                    config.setPartialMatchingColor(VaultFinderConfig.DEFAULT_PARTIAL_COLOR);
                    config.save();
                    this.clearAndInit();
                }
        ).dimensions(centerX - buttonWidth / 2, startY + spacing * 5 + 10, buttonWidth, buttonHeight).build());

        // 7. Done Button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"),
                button -> close()
        ).dimensions(centerX - 100, this.height - 32, 200, 20).build());
    }

    private Text getRadarText() {
        return Text.literal("3D Container Radar: " + (config.isRadarEnabled() ? "§aEnabled" : "§cDisabled"));
    }

    private Text getShulkerPeekText() {
        return Text.literal("Shulker Box Peek: " + (config.isShulkerPeekEnabled() ? "§aEnabled" : "§cDisabled"));
    }

    private Text getXrayText() {
        return Text.literal("X-Ray Through Walls: " + (config.isXrayEnabled() ? "§aEnabled" : "§cDisabled"));
    }

    private Text getRangeText() {
        return Text.literal("Radar Range: §e" + config.getHighlightRange() + " blocks");
    }

    private Text getColorsText() {
        return Text.literal("Match Colors: §aFull §7/ §ePartial");
    }

    private void cycleRange() {
        int current = config.getHighlightRange();
        int next = RANGE_PRESETS[0];
        for (int i = 0; i < RANGE_PRESETS.length; i++) {
            if (RANGE_PRESETS[i] == current) {
                next = RANGE_PRESETS[(i + 1) % RANGE_PRESETS.length];
                break;
            } else if (RANGE_PRESETS[i] > current) {
                next = RANGE_PRESETS[i];
                break;
            }
        }
        config.setHighlightRange(next);
    }

    private void cycleColors() {
        if (config.getMatchingColor() == VaultFinderConfig.DEFAULT_MATCHING_COLOR) {
            config.setMatchingColor(0xFF00E5FF);       // Cyan
            config.setPartialMatchingColor(0xFFFF8800); // Orange
        } else {
            config.setMatchingColor(VaultFinderConfig.DEFAULT_MATCHING_COLOR);
            config.setPartialMatchingColor(VaultFinderConfig.DEFAULT_PARTIAL_COLOR);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                18,
                0xFFFFFF
        );

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("§7Client-side container cache & radar configuration"),
                this.width / 2,
                34,
                0xAAAAAA
        );
    }

    @Override
    public void close() {
        config.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
