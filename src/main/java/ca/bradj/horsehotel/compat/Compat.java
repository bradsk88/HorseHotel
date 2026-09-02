package ca.bradj.horsehotel.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.joml.Vector2i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Compat {
    public static final RandomSource RANDOM = RandomSource.create();

    public static void playNeutralSound(
            ServerLevel serverLevel,
            BlockPos pos,
            SoundEvent sound
    ) {
        float volume = 0.5f;
        float pitchUpOrDown = 1.0F + (serverLevel.random.nextFloat() - serverLevel.random.nextFloat()) * 0.4F;
        serverLevel.playSound(
                null,
                pos,
                sound,
                SoundSource.NEUTRAL,
                volume,
                pitchUpOrDown
        );
    }

    public static void playSound(
            ServerLevel serverLevel,
            BlockPos pos,
            SoundEvent sound,
            SoundSource source
    ) {
        float volume = 0.5f;
        float pitchUpOrDown = 1.0F + (serverLevel.random.nextFloat() - serverLevel.random.nextFloat()) * 0.4F;
        serverLevel.playSound(
                null,
                pos,
                sound,
                source,
                volume,
                pitchUpOrDown
        );
    }

    public static Component translatable(String key) {
        return Component.translatable(key);
    }

    public static MutableComponent translatable(
            String key,
            Object... args
    ) {
        return Component.translatable(key, args);
    }

    public static Component translatableStyled(
            String s,
            Style style,
            Object... args
    ) {
        MutableComponent v = translatable(s, args);
        v.setStyle(style);
        return v;
    }

    public static Component literal(String x) {
        return Component.literal(x);
    }

    public static <X> ArrayList<X> shuffle(
            Collection<X> c,
            ServerLevel serverLevel
    ) {
        ArrayList<X> list = new ArrayList<>(c);
        int size = list.size();
        for (int i = size; i > 1; --i) {
            Collections.swap(list, i - 1, serverLevel.getRandom().nextInt(i));
        }
        return list;
    }

    public static int nextInt(
            @Nullable ServerLevel server,
            int i
    ) {
        return server.getRandom().nextInt(i);
    }

    public static Direction getRandomHorizontal(ServerLevel serverLevel) {
        return Direction.Plane.HORIZONTAL.getRandomDirection(serverLevel.getRandom());
    }

    public static void setCutoutRenderType(Block block) {
        // Render layer is set via model JSON files
    }

    public static <MSG> SimpleChannel.MessageBuilder<MSG> withConsumer(
            SimpleChannel.MessageBuilder<MSG> decoder,
            BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer
    ) {
        return decoder.consumerNetworkThread(consumer);
    }

    public static void openScreen(
            ServerPlayer sender,
            MenuProvider menuProvider,
            Consumer<FriendlyByteBuf> consumer
    ) {
        NetworkHooks.openScreen(sender, menuProvider, consumer);
    }

    public static DeferredRegister<MenuType<?>> CreateMenuRegister(String modid) {
        return DeferredRegister.create(ForgeRegistries.MENU_TYPES, modid);
    }

    public static <X> Supplier<X> configGet(ForgeConfigSpec.ConfigValue<X> cfg) {
        return cfg::get;
    }

    public static boolean insertInNextOpenSlot(
            IItemHandler iItemHandler,
            ItemStack inserted,
            int targetSize
    ) {
        if (inserted.getOrCreateTag().isEmpty()) {
            for (int i = 0; i < iItemHandler.getSlots(); i++) {
                ItemStack stackInSlot = iItemHandler.getStackInSlot(i);
                if (stackInSlot.getOrCreateTag().isEmpty() && stackInSlot.is(inserted.getItem())) {
                    if (stackInSlot.getCount() < targetSize) {
                        iItemHandler.insertItem(i, inserted, false);
                        return true;
                    }
                }
            }
        }
        for (int i = 0; i < iItemHandler.getSlots(); i++) {
            ItemStack stackInSlot = iItemHandler.getStackInSlot(i);
            if (stackInSlot.isEmpty()) {
                iItemHandler.insertItem(i, inserted, false);
                return true;
            }
        }
        return false;
    }

    public static void drawDarkText(
            GuiGraphics gui,
            FormattedCharSequence translatable,
            int x,
            int y
    ) {
        gui.drawString(font(), translatable, x, y, 0x00000000);
    }

    public static void drawDarkText(
            GuiGraphics gui,
            Component translatable,
            int x,
            int y
    ) {
        gui.drawString(font(), translatable, x, y, 0x00000000);
    }

    public static void drawLightText(
            GuiGraphics gui,
            String translatable,
            int x,
            int y
    ) {
        // No drawStringWithShadow in 1.20; shadow is the trailing boolean.
        gui.drawString(font(), translatable, x, y, 0xFFFFFFFF, true);
    }

    /**
     * Replaces the removed {@code GuiComponent.blit}: blit a sprite region from a texture.
     * 1.19.2 signature order (x, y, width, height, u, v, uScale, vScale, maxU, maxV) is kept at
     * the call site and mapped to the 1.20 {@code GuiGraphics.blit} ordering.
     */
    public static void blit(
            GuiGraphics gui,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            int u,
            int v,
            int uScale,
            int vScale,
            int maxU,
            int maxV
    ) {
        gui.blit(texture, x, y, u, v, uScale, vScale, width, height, maxU, maxV);
    }

    /**
     * Replaces the removed {@code GuiComponent.fill}.
     */
    public static void fill(
            GuiGraphics gui,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        gui.fill(x, y, x + width, y + height, color);
    }

    public static net.minecraft.client.gui.Font font() {
        return Minecraft.getInstance().font;
    }

    /**
     * Default button narration. The 1.19.2 {@code Button::createDefaultNarration} method reference
     * is not a {@code CreateNarration} in 1.20.1 (the type changed to a message-supplier mapper and
     * the default became a protected field), so the version-specific bit lives here and call sites
     * pass this instead of the reference.
     */
    public static Button.CreateNarration defaultButtonNarration() {
        return messageSupplier -> messageSupplier.get();
    }

    /**
     * Build a {@link Button}. The 1.19.2 seven-arg {@code Button} constructor became protected in
     * 1.20.1, so call sites route through this instead of {@code new Button(...)}. The default
     * narration (see {@link #defaultButtonNarration()}) is applied here, keeping the version-specific
     * builder + narration out of the call sites.
     */
    public static Button button(
            int x,
            int y,
            int width,
            int height,
            Component message,
            Button.OnPress onPress
    ) {
        return Button.builder(message, onPress)
                .bounds(x, y, width, height)
                .createNarration(defaultButtonNarration())
                .build();
    }

    /**
     * Default tooltip positioner. In 1.19.2 {@code ClientTooltipPositioner} was an abstract class
     * whose empty {@code new ...() {}} gave a usable default; in 1.20.1 it is a functional interface
     * that must be implemented, so the version-specific bit lives here. Positions the tooltip offset
     * from the cursor, matching the old single-line tooltip behaviour.
     */
    public static ClientTooltipPositioner defaultTooltipPositioner() {
        return (x, y, x2, y2, mouseX, mouseY) -> new Vector2i(mouseX + 12, mouseY + 12);
    }

    /**
     * A damage source for the 1-point "you shouldn't have that" punishment. The 1.19.2
     * {@code DamageSource.ANVIL} constant has no 1.20.1 equivalent (damage sources moved to the
     * per-server {@code DamageSources} registry), so the version-specific bit lives here. Falls back
     * to the generic fall source, which is the nearest common punishment source in 1.20.1.
     */
    public static net.minecraft.world.damagesource.DamageSource anvilDamageSource(
            net.minecraft.world.level.Level level
    ) {
        return level.damageSources().fall();
    }

    public static Component getItemName(Item item) {
        return translatable(getItemId(item).toString());
    }

    public static ResourceLocation getItemId(Item item) {
        return ForgeRegistries.ITEMS.getKey(item);
    }

    public static ResourceLocation getItemId(Block block) {
        return ForgeRegistries.BLOCKS.getKey(block);
    }

    public static boolean getRandomBool(@Nullable ServerLevel serverLevel) {
        return serverLevel.getRandom().nextBoolean();
    }

    public static int getRandomInt(
            ServerLevel serverLevel,
            int size
    ) {
        return serverLevel.getRandom().nextInt(size);
    }

    public static void sendMessage(
            ServerPlayer sender,
            Component message
    ) {
        sender.sendSystemMessage(message);
    }
}

