package com.loohp.imageframe.configuration;

import com.loohp.imageframe.ImageFrameClient;
import com.loohp.imageframe.object.Resolution;
import eu.midnightdust.lib.config.MidnightConfig;

public class Configuration extends MidnightConfig {

    @Entry
    public static boolean useNativeResMapImages = true;

    @Entry
    public static Resolution maxImageSize = Resolution.NATIVE;

    @Entry
    public static boolean previewMapsInTooltip = true;

    @Entry
    public static boolean notifyWhenServerSupports = true;

    @Override
    public void writeChanges(String modid) {
        ImageFrameClient.MOD.clearLoadedHdMaps();
        super.writeChanges(modid);
    }

}