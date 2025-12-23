package games.polarbearbytes.walktheline.axis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction.Axis;

/**
 * Data class for containing the locked axis and coordinate for a particular world (dimension)
 *
 * @param axis The restricted to axis
 * @param coordinate The coordinate on the locked axis to restrict to
 */
public record LockedAxisData(boolean enabled, Axis axis, double coordinate, Formatting color) {
    public static final Codec<LockedAxisData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("enabled").forGetter(LockedAxisData::enabled),
            Axis.CODEC.fieldOf("axis").forGetter(LockedAxisData::axis),
            Codec.DOUBLE.fieldOf("coordinate").forGetter(LockedAxisData::coordinate),
            Formatting.CODEC.fieldOf("color").forGetter(LockedAxisData::color)
    ).apply(instance, LockedAxisData::new));
}
