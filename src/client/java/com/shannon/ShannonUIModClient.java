package com.shannon;

import com.shannon.ui.ShannonClient;
import net.fabricmc.api.ClientModInitializer;

/** Client entrypoint. Everything client-side lives under {@code com.shannon.ui}. */
public class ShannonUIModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ShannonClient.init();
    }
}
