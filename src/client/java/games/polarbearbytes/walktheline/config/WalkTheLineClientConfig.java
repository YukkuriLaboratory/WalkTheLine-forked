package games.polarbearbytes.walktheline.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

@Config(name = "walk-the-line-client")
public class WalkTheLineClientConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    public boolean rotatingColor = true;
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min=1, max=100)
    public int rotatingColorAlpha = 100;
    @ConfigEntry.Gui.Tooltip
    public String singleColor = "#FF0000FF";
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min=1, max=100)
    public int lineWidth = 25;

    @ConfigEntry.Gui.Excluded
    public static double tolerance = 0.5;
    @ConfigEntry.Gui.Excluded
    public static boolean modEnabled = false;

    public static WalkTheLineClientConfig getConfig(){
        return AutoConfig.getConfigHolder(WalkTheLineClientConfig.class).getConfig();
    }

    public static void reset(){
        modEnabled = false;
        tolerance = 0.5;
    }

    public static void register(){
        AutoConfig.register(WalkTheLineClientConfig.class, GsonConfigSerializer::new);
    }
}
