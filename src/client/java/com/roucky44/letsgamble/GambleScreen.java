package com.roucky44.letsgamble;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GambleScreen extends Screen {
    private static final int PANEL_WIDTH = 440;
    private static final int INVENTORY_SIZE = 18;
    private final List<Item> matchingItems = new ArrayList<>();
    private EditBox searchBox;
    private Button gambleButton;
    private int selectedSlot = -1;
    private Item selectedTarget;
    private int scrollOffset;
    private boolean waitingForResult;
    private boolean won;
    private float resolvedChance;
    private ItemStack resolvedTarget = ItemStack.EMPTY;
    private long resultTime;

    public GambleScreen() {
        super(Component.literal("Let's Gamble"));
    }

    @Override
    protected void init() {
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - 230) / 2;
        searchBox = new EditBox(font, panelX + 264, panelY + 29, 160, 18, Component.literal("Rechercher"));
        searchBox.setResponder(query -> refreshItems());
        addRenderableWidget(searchBox);
        gambleButton = addRenderableWidget(Button.builder(Component.literal("Lancer le gamble"), button -> requestGamble())
                .bounds(panelX + 17, panelY + 198, 220, 20).build());
        refreshItems();
    }

    private void refreshItems() {
        String query = searchBox == null ? "" : searchBox.getValue().toLowerCase();
        matchingItems.clear();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (query.isBlank() || id.toString().contains(query) || item.getDescription().getString().toLowerCase().contains(query)) {
                matchingItems.add(item);
            }
        }
        matchingItems.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        scrollOffset = 0;
    }

    private void requestGamble() {
        if (waitingForResult || selectedSlot < 0 || selectedTarget == null || minecraft == null || minecraft.player == null) return;
        if (minecraft.player.getInventory().getItem(selectedSlot).isEmpty()) return;

        FriendlyByteBuf request = PacketByteBufs.create();
        request.writeVarInt(selectedSlot);
        request.writeUtf(BuiltInRegistries.ITEM.getKey(selectedTarget).toString());
        ClientPlayNetworking.send(GambleNetworking.GAMBLE_REQUEST, request);
        waitingForResult = true;
        resultTime = 0L;
    }

    public void showResult(boolean won, float chance, ItemStack target) {
        // The server has already changed the inventory; this delay is visual only.
        this.waitingForResult = true;
        this.won = won;
        this.resolvedChance = chance;
        this.resolvedTarget = target;
        this.resultTime = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (waitingForResult && resultTime > 0 && System.currentTimeMillis() - resultTime >= 1500L) {
            waitingForResult = false;
        }
        renderBackground(graphics);
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - 230) / 2;
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 230, 0xE814171D);
        graphics.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + 25, 0xFF313946);
        graphics.drawString(font, title, panelX + 10, panelY + 9, 0xFFFFFFFF, false);

        graphics.drawString(font, Component.literal("Mise (1 objet)"), panelX + 17, panelY + 36, 0xFFBFC7D5, false);
        renderInventory(graphics, panelX + 17, panelY + 55);
        renderWheel(graphics, panelX + 143, panelY + 107);

        graphics.fill(panelX + 252, panelY + 26, panelX + 430, panelY + 190, 0xFF20252E);
        graphics.drawString(font, Component.literal("Objet souhaite"), panelX + 264, panelY + 9, 0xFFBFC7D5, false);
        renderTargets(graphics, panelX + 264, panelY + 55, mouseX, mouseY);
        renderStatus(graphics, panelX + 17, panelY + 177);

        gambleButton.active = selectedSlot >= 0 && selectedTarget != null && !waitingForResult;
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderInventory(GuiGraphics graphics, int left, int top) {
        if (minecraft == null || minecraft.player == null) return;
        for (int slot = 0; slot < 36; slot++) {
            int x = left + (slot % 9) * 20;
            int y = top + (slot / 9) * 20;
            graphics.fill(x, y, x + INVENTORY_SIZE, y + INVENTORY_SIZE, slot == selectedSlot ? 0xFFDBA72C : 0xFF4A5362);
            graphics.fill(x + 1, y + 1, x + INVENTORY_SIZE - 1, y + INVENTORY_SIZE - 1, 0xFF181C23);
            ItemStack stack = minecraft.player.getInventory().getItem(slot);
            graphics.renderItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(font, stack, x + 1, y + 1);
        }
    }

    private void renderWheel(GuiGraphics graphics, int centerX, int centerY) {
        int radius = 45;
        int chancePercent = selectedTarget == null || selectedSlot < 0 || minecraft == null || minecraft.player == null
                ? 0 : Math.round(GambleValues.chance(minecraft.player.getInventory().getItem(selectedSlot).getItem(), selectedTarget) * 100);
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x * x + y * y > radius * radius) continue;
                double angle = Math.atan2(y, x) + Math.PI / 2;
                if (angle < 0) angle += Math.PI * 2;
                int color = angle <= (Math.PI * 2 * chancePercent / 100.0) ? 0xFF40A86B : 0xFF743C4B;
                graphics.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
            }
        }
        graphics.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, 0xFFFFFFFF);
        double phase = waitingForResult ? (System.currentTimeMillis() % 1000L) / 1000.0 * Math.PI * 2 : 0;
        int needleX = centerX + (int) (Math.sin(phase) * 39);
        int needleY = centerY - (int) (Math.cos(phase) * 39);
        graphics.fill(centerX, centerY, needleX + 1, needleY + 1, 0xFFFFFFFF);
        graphics.drawCenteredString(font, chancePercent + "%", centerX, centerY - 4, 0xFF101216);
    }

    private void renderTargets(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        int visible = 6;
        int maxOffset = Math.max(0, matchingItems.size() - visible);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxOffset);
        for (int row = 0; row < visible && row + scrollOffset < matchingItems.size(); row++) {
            Item item = matchingItems.get(row + scrollOffset);
            int y = top + row * 22;
            boolean selected = item == selectedTarget;
            boolean hovered = mouseX >= left && mouseX < left + 160 && mouseY >= y && mouseY < y + 20;
            graphics.fill(left, y, left + 160, y + 20, selected ? 0xFF546D45 : hovered ? 0xFF39404C : 0xFF272D37);
            graphics.renderItem(item.getDefaultInstance(), left + 2, y + 2);
            String name = item.getDescription().getString();
            graphics.drawString(font, font.plainSubstrByWidth(name, 132), left + 22, y + 6, 0xFFFFFFFF, false);
        }
    }

    private void renderStatus(GuiGraphics graphics, int x, int y) {
        if (waitingForResult) {
            graphics.drawString(font, Component.literal("La roulette tourne..."), x, y, 0xFFE2BD52, false);
        } else if (!resolvedTarget.isEmpty()) {
            String result = won ? "Gagne : " : "Perdu : ";
            int color = won ? 0xFF65D883 : 0xFFE06B6B;
            graphics.drawString(font, Component.literal(result + resolvedTarget.getHoverName().getString() + " (" + Math.round(resolvedChance * 100) + "%)"), x, y, color, false);
        } else {
            graphics.drawString(font, Component.literal("Choisis ta mise et ta recompense."), x, y, 0xFFBFC7D5, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - 230) / 2;
        if (button == 0 && mouseX >= panelX + 17 && mouseX < panelX + 197 && mouseY >= panelY + 55 && mouseY < panelY + 135) {
            int column = ((int) mouseX - (panelX + 17)) / 20;
            int row = ((int) mouseY - (panelY + 55)) / 20;
            int slot = row * 9 + column;
            if (slot < 36 && minecraft != null && !minecraft.player.getInventory().getItem(slot).isEmpty()) selectedSlot = slot;
            return true;
        }
        if (button == 0 && mouseX >= panelX + 264 && mouseX < panelX + 424 && mouseY >= panelY + 55 && mouseY < panelY + 187) {
            int row = ((int) mouseY - (panelY + 55)) / 22;
            int index = scrollOffset + row;
            if (index < matchingItems.size()) selectedTarget = matchingItems.get(index);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - 230) / 2;
        if (mouseX >= panelX + 252 && mouseX < panelX + 430 && mouseY >= panelY + 26 && mouseY < panelY + 190) {
            scrollOffset -= (int) Math.signum(amount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
}
