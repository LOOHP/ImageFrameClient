package com.loohp.imageframe.object;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.util.Identifier;

public record PaintingTooltipComponent(PaintingVariant paintingVariant) implements MapTooltipComponent {

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return shouldFillWidth() ? Math.round(getHeightToWidthRatio() * 66F) : 66;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return shouldFillWidth() ? 66 : Math.round(getWidthToHeightRatio() * 66F);
    }

    public boolean shouldFillWidth() {
        return paintingVariant.width() < paintingVariant.height();
    }

    public float getWidthToHeightRatio() {
        return (float) paintingVariant.width() / (float) paintingVariant.height();
    }

    public float getHeightToWidthRatio() {
        return (float) paintingVariant.height() / (float) paintingVariant.width();
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        Identifier painting = paintingVariant.assetId().withPrefixedPath("textures/painting/").withSuffixedPath(".png");
        int w = shouldFillWidth() ? 64 : Math.round(getWidthToHeightRatio() * 64F);
        int h = shouldFillWidth() ? Math.round(getHeightToWidthRatio() * 64F) : 64;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, painting, x, y, 0, 0, w, h, w, h);
    }

}
