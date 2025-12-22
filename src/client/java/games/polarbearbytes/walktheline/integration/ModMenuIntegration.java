package games.polarbearbytes.walktheline.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import games.polarbearbytes.walktheline.config.WalkTheLineClientConfig;
import me.shedaniel.autoconfig.AutoConfigClient;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfigClient.getConfigScreen(WalkTheLineClientConfig.class, parent).get();
    }
}
