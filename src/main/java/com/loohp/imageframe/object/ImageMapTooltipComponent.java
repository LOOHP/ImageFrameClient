package com.loohp.imageframe.object;

import com.loohp.imageframe.ImageFrameClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

public class ImageMapTooltipComponent implements MapTooltipComponent {

    private final float BG_INSET = 1F;
    private final float PAD_PX = 2F;

    private final Identifier background = Identifier.of("textures/map/map_background.png");
    private final MapRenderState mapRenderState;
    private final int index;

    public ImageMapTooltipComponent(int index) {
        this.mapRenderState = new MapRenderState();
        this.index = index;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return ImageFrameClient.MOD.getOrRequestImageMapData(index) == null ? 0 : 66;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        ImageMapData data = ImageFrameClient.MOD.getOrRequestImageMapData(index);
        if (data == null) {
            return 0;
        }
        return getWidth(data);
    }

    public int getWidth(ImageMapData data) {
        int cols = data.width();
        int rows = data.height();
        float availableH = 64f - BG_INSET * 2f;
        // Reserve padding first, then fit tiles with an INTEGER stride
        float usableH = Math.max(0f, availableH - PAD_PX * 2f);
        int tileSizePxInt = Math.max(1, (int) Math.floor(usableH / rows));
        // Total width uses integer stride + constant padding + background inset
        float totalWidth = cols * tileSizePxInt + PAD_PX * 2f + BG_INSET * 2f;
        return (int) Math.ceil(totalWidth);
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null) {
            return;
        }
        ImageMapData imageMapData = ImageFrameClient.MOD.getOrRequestImageMapData(index);
        if (imageMapData == null) {
            return;
        }
        int gridCols = imageMapData.width();
        int gridRows = imageMapData.height();
        float availableH = 64f - BG_INSET * 2f;
        float usableH = Math.max(0f, availableH - PAD_PX * 2f);
        int tileSizePxInt = Math.max(1, (int) Math.floor(usableH / gridRows));
        // Background width that matches our layout
        int mapWidth = getWidth(imageMapData);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, background, x, y, 0, 0, mapWidth, 64, mapWidth, 64);
        // Grid dimensions for centering (NO overlap involved)
        float gridW = gridCols * tileSizePxInt + PAD_PX * 2f;
        float gridH = gridRows * tileSizePxInt + PAD_PX * 2f;
        // Center once; round origin only to avoid subtle left shifts
        float originX = x + Math.round((mapWidth - gridW) * 0.5f);
        float originY = y + Math.round((64f     - gridH) * 0.5f);
        // Tiny overlap strictly for seam hiding; does not affect layout/padding
        float overlap = 0.25f; // adjust 0.2–0.5 if needed
        float tileScale = (tileSizePxInt + overlap) / 128f;
        MapRenderer mapRenderer = client.getMapRenderer();
        Matrix3x2fStack matrix = context.getMatrices();
        for (int i = 0; i < imageMapData.mapIds().size(); i++) {
            MapIdComponent id = new MapIdComponent(imageMapData.mapIds().getInt(i));
            int col = i % gridCols;
            int row = i / gridCols;
            MapState data = world.getMapState(id);
            if (data == null) {
                continue;
            }
            mapRenderer.update(id, data, mapRenderState);
            // Integer stride + constant device-px padding
            float drawX = originX + PAD_PX + col * tileSizePxInt;
            float drawY = originY + PAD_PX + row * tileSizePxInt;
            matrix.pushMatrix();
            matrix.translate(drawX, drawY);
            // Apply overlap symmetrically so it doesn't “eat” the padding
            matrix.translate(-overlap * 0.5f, -overlap * 0.5f);
            matrix.scale(tileScale, tileScale);
            context.drawMap(mapRenderState);
            matrix.popMatrix();
        }
    }

}
