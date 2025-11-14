package com.loohp.imageframe.object;

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

public class FilledMapTooltipComponent implements MapTooltipComponent {

    private final Identifier background = Identifier.of("textures/map/map_background.png");
    private final MapIdComponent id;
    private final MapRenderState mapRenderState;

    public FilledMapTooltipComponent(int mapId) {
        this.id = new MapIdComponent(mapId);
        this.mapRenderState = new MapRenderState();
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return 66;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return 66;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, background, x, y, 0, 0, 64, 64, 64, 64);
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world  = client.world;
        if (world == null) {
            return;
        }
        MapState data = world.getMapState(id);
        if (data == null) {
            return;
        }
        Matrix3x2fStack matrix = context.getMatrices();
        matrix.pushMatrix();
        matrix.translate(x + 3.2F, y + 3.2F);
        matrix.scale(0.45F, 0.45F);
        MapRenderer mapRenderer = client.getMapRenderer();
        mapRenderer.update(id, data, mapRenderState);
        context.drawMap(mapRenderState);
        matrix.popMatrix();
    }

}
