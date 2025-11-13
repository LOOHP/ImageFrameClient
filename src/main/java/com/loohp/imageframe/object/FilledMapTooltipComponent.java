package com.loohp.imageframe.object;

import com.loohp.imageframe.configuration.Configuration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;

import static net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE;

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
        if (!Configuration.previewMapsInTooltip) {
            return;
        }
        context.drawTexture(RenderLayer::getGuiTextured, background, x, y, 0, 0, 64, 64, 64, 64);
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world  = client.world;
        if (world == null) {
            return;
        }
        MapState data = world.getMapState(id);
        if (data == null) {
            return;
        }
        MatrixStack matrix = context.getMatrices();
        matrix.push();
        matrix.translate(x + 3.2F, y + 3.2F, 1F);
        matrix.scale(0.45F, 0.45F, 1F);
        MapRenderer mapRenderer = client.getMapRenderer();
        mapRenderer.update(id, data, mapRenderState);
        context.draw(vertexConsumers -> mapRenderer.draw(mapRenderState, matrix, vertexConsumers, true, MAX_LIGHT_COORDINATE));
        matrix.pop();
    }

}
