package games.polarbearbytes.walktheline.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

@Config(name = "walk-the-line-client")
public class WalkTheLineClientConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min=1, max=100)
    public int lineWidth = 25;

    public static WalkTheLineClientConfig getConfig(){
        return AutoConfig.getConfigHolder(WalkTheLineClientConfig.class).getConfig();
    }

    public static void register(){
        AutoConfig.register(WalkTheLineClientConfig.class, GsonConfigSerializer::new);
    }
}
