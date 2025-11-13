package com.loohp.imageframe.mixin;

import com.loohp.imageframe.object.FilledMapTooltipData;
import com.loohp.imageframe.object.ImageMapTooltipData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
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

    @Inject(at = @At("HEAD"), cancellable = true, method = "getTooltipData")
    public void getTooltipData(ItemStack stack, CallbackInfoReturnable<Optional<TooltipData>> cir) {
        Item item = stack.getItem();
        if (Items.PAPER.equals(item)) {
            NbtComponent customDataComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (customDataComponent != null) {
                NbtCompound tag = customDataComponent.copyNbt();
                if (tag.contains(KEY, NbtElement.INT_TYPE)) {
                    int index = tag.getInt(KEY);
                    cir.setReturnValue(Optional.of(new ImageMapTooltipData(index)));
                    cir.cancel();
                }
            }
        } else if (Items.FILLED_MAP.equals(item)) {
            MapIdComponent mapIdComponent = stack.get(DataComponentTypes.MAP_ID);
            if (mapIdComponent != null) {
                cir.setReturnValue(Optional.of(new FilledMapTooltipData(mapIdComponent.id())));
                cir.cancel();
            }
        }
    }

}
