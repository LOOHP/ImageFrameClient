package com.loohp.imageframe.mixin;

import com.loohp.imageframe.object.FilledMapTooltipComponent;
import com.loohp.imageframe.object.FilledMapTooltipData;
import com.loohp.imageframe.object.ImageMapTooltipComponent;
import com.loohp.imageframe.object.ImageMapTooltipData;
import com.loohp.imageframe.object.PaintingTooltipComponent;
import com.loohp.imageframe.object.PaintingTooltipData;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.tooltip.TooltipData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TooltipComponent.class)
public interface TooltipComponentMixin {

    @Inject(at = @At("HEAD"), cancellable = true, method = "of(Lnet/minecraft/item/tooltip/TooltipData;)Lnet/minecraft/client/gui/tooltip/TooltipComponent;")
    private static void of(TooltipData tooltipData, CallbackInfoReturnable<TooltipComponent> cir) {
        switch (tooltipData) {
            case PaintingTooltipData paintingTooltipData -> {
                cir.setReturnValue(new PaintingTooltipComponent(paintingTooltipData.paintingVariant()));
                cir.cancel();
            }
            case FilledMapTooltipData filledMapTooltipData -> {
                cir.setReturnValue(new FilledMapTooltipComponent(filledMapTooltipData.mapId()));
                cir.cancel();
            }
            case ImageMapTooltipData imageMapTooltipData -> {
                cir.setReturnValue(new ImageMapTooltipComponent(imageMapTooltipData.index()));
                cir.cancel();
            }
            default -> {
                /* do nothing */
            }
        }
    }

}
