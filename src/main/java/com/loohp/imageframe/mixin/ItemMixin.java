package com.loohp.imageframe.mixin;

import com.loohp.imageframe.configuration.Configuration;
import com.loohp.imageframe.object.FilledMapTooltipData;
import com.loohp.imageframe.object.ImageMapTooltipData;
import com.loohp.imageframe.object.PaintingTooltipData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Item.class)
public class ItemMixin {

    @Unique
    private static final String KEY = "CombinedImageMap";

    @Unique
    private static final String VARIANT = "variant";

    @Inject(at = @At("HEAD"), cancellable = true, method = "getTooltipData")
    public void getTooltipData(ItemStack stack, CallbackInfoReturnable<Optional<TooltipData>> cir) {
        Item item = stack.getItem();
        if (Configuration.previewPaintingsInTooltip && Items.PAINTING.equals(item)) {
            NbtComponent customDataComponent = stack.get(DataComponentTypes.ENTITY_DATA);
            if (customDataComponent != null) {
                NbtCompound tag = customDataComponent.copyNbt();
                if (tag.contains(VARIANT, NbtElement.STRING_TYPE)) {
                    Identifier variant = Identifier.of(tag.getString(VARIANT));
                    ClientWorld world = MinecraftClient.getInstance().world;
                    if (world != null) {
                        Optional<Registry<PaintingVariant>> optPaintingRegistry = world.getRegistryManager().getOptional(RegistryKeys.PAINTING_VARIANT);
                        if (optPaintingRegistry.isPresent()) {
                            PaintingVariant paintingVariant = optPaintingRegistry.get().get(variant);
                            if (paintingVariant != null) {
                                cir.setReturnValue(Optional.of(new PaintingTooltipData(paintingVariant)));
                                cir.cancel();
                            }
                        }
                    }
                }
            }
        } else if (Configuration.previewMapsInTooltip && Items.PAPER.equals(item)) {
            NbtComponent customDataComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (customDataComponent != null) {
                NbtCompound tag = customDataComponent.copyNbt();
                if (tag.contains(KEY, NbtElement.INT_TYPE)) {
                    int index = tag.getInt(KEY);
                    cir.setReturnValue(Optional.of(new ImageMapTooltipData(index)));
                    cir.cancel();
                }
            }
        } else if (Configuration.previewMapsInTooltip && Items.FILLED_MAP.equals(item)) {
            MapIdComponent mapIdComponent = stack.get(DataComponentTypes.MAP_ID);
            if (mapIdComponent != null) {
                cir.setReturnValue(Optional.of(new FilledMapTooltipData(mapIdComponent.id())));
                cir.cancel();
            }
        }
    }

}
