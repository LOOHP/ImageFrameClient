package com.loohp.imageframe;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.loohp.imageframe.configuration.Configuration;
import com.loohp.imageframe.object.ImageMapData;
import com.loohp.imageframe.object.MultipartHdMapInfo;
import com.loohp.imageframe.payload.ClientboundAcknowledgement;
import com.loohp.imageframe.payload.ClientboundHdImageMultipartResponse;
import com.loohp.imageframe.payload.ClientboundHdImageResponse;
import com.loohp.imageframe.payload.ClientboundImageMapDetailsResponse;
import com.loohp.imageframe.payload.ClientboundImageUpdatedSignal;
import com.loohp.imageframe.payload.ServerboundAcknowledgement;
import com.loohp.imageframe.payload.ServerboundHdImageRequest;
import com.loohp.imageframe.payload.ServerboundImageMapDetailsRequest;
import com.mojang.blaze3d.platform.NativeImage;
import eu.midnightdust.lib.config.MidnightConfig;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImageFrameClient implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("imageframeclient");
    public static ImageFrameClient MOD;

    private final AtomicBoolean currentServerSupported = new AtomicBoolean(false);
    private final Int2ObjectMap<Optional<Identifier>> loadedHdImages = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Optional<ImageMapData>> imageMapData = new Int2ObjectOpenHashMap<>();
    private final Cache<Integer, MultipartHdMapInfo> pendingMultipart = CacheBuilder.newBuilder().expireAfterAccess(Duration.of(10, ChronoUnit.SECONDS)).build();

    @Override
    public void onInitialize() {
        MOD = this;
        LOGGER.info("Hello world from ImageFrame Client!");
        MidnightConfig.init("imageframeclient", Configuration.class);

        PayloadTypeRegistry.clientboundPlay().register(ClientboundAcknowledgement.ID, ClientboundAcknowledgement.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ServerboundAcknowledgement.ID, ServerboundAcknowledgement.CODEC);

        PayloadTypeRegistry.serverboundPlay().register(ServerboundHdImageRequest.ID, ServerboundHdImageRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientboundHdImageResponse.ID, ClientboundHdImageResponse.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientboundHdImageMultipartResponse.ID, ClientboundHdImageMultipartResponse.CODEC);

        PayloadTypeRegistry.clientboundPlay().register(ClientboundImageUpdatedSignal.ID, ClientboundImageUpdatedSignal.CODEC);

        PayloadTypeRegistry.serverboundPlay().register(ServerboundImageMapDetailsRequest.ID, ServerboundImageMapDetailsRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientboundImageMapDetailsResponse.ID, ClientboundImageMapDetailsResponse.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(ClientboundAcknowledgement.ID, (payload, context) -> {
            ServerboundAcknowledgement reply = new ServerboundAcknowledgement(payload.id());
            ClientPlayNetworking.send(reply);
            currentServerSupported.set(true);
            if (Configuration.notifyWhenServerSupports) {
                SystemToast.add(
                        Minecraft.getInstance().gui.toastManager(),
                        SystemToast.SystemToastId.UNSECURE_SERVER_WARNING,
                        Component.translatable("imageframeclient.message.server_supported.title").withStyle(ChatFormatting.GOLD),
                        Component.translatable("imageframeclient.message.server_supported.description")
                );
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(ClientboundHdImageResponse.ID, (payload, context) -> {
            if (Configuration.useNativeResMapImages) {
                try {
                    int mapId = payload.mapId();
                    if (payload.requestAccepted()) {
                        Optional<Integer> opt = payload.multipart();
                        byte[] data = payload.data();
                        if (opt.isPresent()) {
                            MultipartHdMapInfo info = new MultipartHdMapInfo();
                            info.put(0, data);
                            pendingMultipart.put(opt.get(), info);
                        } else {
                            if (data.length > 0) {
                                NativeImage nativeImage = resizeToPreference(NativeImage.read(data));
                                Identifier id = Identifier.fromNamespaceAndPath("imageframe", "hdmap_" + mapId);
                                DynamicTexture tex = new DynamicTexture(id::getPath, nativeImage);
                                Minecraft.getInstance().getTextureManager().register(id, tex);
                                loadedHdImages.put(mapId, Optional.of(id));
                            }
                        }
                    } else {
                        loadedHdImages.remove(mapId);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(ClientboundHdImageMultipartResponse.ID, (payload, context) -> {
            if (Configuration.useNativeResMapImages) {
                try {
                    int mapId = payload.mapId();
                    int multipartId = payload.multipart();
                    MultipartHdMapInfo info = pendingMultipart.getIfPresent(multipartId);
                    if (info != null) {
                        byte[] data = payload.data();
                        int index = payload.index();
                        if (data.length > 0) {
                            info.put(index, data);
                        }
                        if (payload.end()) {
                            info.setLastIndex(index);
                        }
                        if (info.isCompleted()) {
                            pendingMultipart.invalidate(multipartId);
                            NativeImage nativeImage = resizeToPreference(NativeImage.read(info.complete()));
                            Identifier id = Identifier.fromNamespaceAndPath("imageframe", "hdmap_" + mapId);
                            DynamicTexture tex = new DynamicTexture(id::getPath, nativeImage);
                            Minecraft.getInstance().getTextureManager().register(id, tex);
                            loadedHdImages.put(mapId, Optional.of(id));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(ClientboundImageUpdatedSignal.ID, (payload, context) -> {
            for (int index : payload.indexes()) {
                imageMapData.remove(index);
            }
            for (int mapId : payload.mapIds()) {
                Optional<Identifier> id = loadedHdImages.remove(mapId);
                if (id != null && id.isPresent()) {
                    Minecraft.getInstance().getTextureManager().release(id.get());
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(ClientboundImageMapDetailsResponse.ID, (payload, context) -> {
            if (payload.width() > 0 && payload.height() > 0) {
                imageMapData.put(payload.index(), Optional.of(new ImageMapData(payload.width(), payload.height(), payload.mapIds())));
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            imageMapData.clear();
            for (int mapId : new IntOpenHashSet(loadedHdImages.keySet())) {
                Optional<Identifier> id = loadedHdImages.remove(mapId);
                if (id != null && id.isPresent()) {
                    Minecraft.getInstance().getTextureManager().release(id.get());
                }
            }
            currentServerSupported.set(false);
        });
    }

    public NativeImage resizeToPreference(NativeImage src) {
        int size = Configuration.maxImageSize.getMaxSize();
        if (src.getWidth() <= size) {
            return src;
        }
        NativeImage dst = new NativeImage(size, size, false);
        src.resizeSubRectTo(0, 0, src.getWidth(), src.getHeight(), dst);
        return dst;
    }

    @SuppressWarnings("OptionalAssignedToNull")
    public Identifier getOrRequestLoadedHdMap(int mapId) {
        Optional<Identifier> result = loadedHdImages.get(mapId);
        if (result == null) {
            if (currentServerSupported.get()) {
                ServerboundHdImageRequest request = new ServerboundHdImageRequest(mapId);
                ClientPlayNetworking.send(request);
                loadedHdImages.put(mapId, Optional.empty());
            }
            return null;
        }
        return result.orElse(null);
    }

    public void clearLoadedHdMaps() {
        loadedHdImages.clear();
    }

    @SuppressWarnings("OptionalAssignedToNull")
    public ImageMapData getOrRequestImageMapData(int index) {
        Optional<ImageMapData> result = imageMapData.get(index);
        if (result == null) {
            if (currentServerSupported.get()) {
                ServerboundImageMapDetailsRequest request = new ServerboundImageMapDetailsRequest(index);
                ClientPlayNetworking.send(request);
                imageMapData.put(index, Optional.empty());
            }
            return null;
        }
        return result.orElse(null);
    }
}
