package games.polarbearbytes.walktheline.axis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction.Axis;

import java.util.Objects;

/**
 * Data class for containing the locked axis and coordinate for a particular world (dimension)
 *
 */
public final class LockedAxisData {
    public static final Codec<LockedAxisData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("enabled").forGetter(LockedAxisData::enabled),
            Axis.CODEC.fieldOf("axis").forGetter(LockedAxisData::axis),
            Codec.DOUBLE.fieldOf("coordinate").forGetter(LockedAxisData::coordinate),
            Formatting.CODEC.fieldOf("color").forGetter(LockedAxisData::color)
    ).apply(instance, LockedAxisData::new));
    private boolean enabled;
    private Axis axis;
    private double coordinate;
    private Formatting color;

    /**
     * @param axis       The restricted to axis
     * @param coordinate The coordinate on the locked axis to restrict to
     */
    public LockedAxisData(boolean enabled, Axis axis, double coordinate, Formatting color) {
        this.enabled = enabled;
        this.axis = axis;
        this.coordinate = coordinate;
        this.color = color;
    }

    public void setColor(Formatting color) {
        this.color = color;
    }

    public void setCoordinate(double coordinate) {
        this.coordinate = coordinate;
    }

    public void setAxis(Axis axis) {
        this.axis = axis;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public Axis axis() {
        return axis;
    }

    public double coordinate() {
        return coordinate;
    }

    public Formatting color() {
        return color;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LockedAxisData) obj;
        return this.enabled == that.enabled &&
                Objects.equals(this.axis, that.axis) &&
                Double.doubleToLongBits(this.coordinate) == Double.doubleToLongBits(that.coordinate) &&
                Objects.equals(this.color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, axis, coordinate, color);
    }

    @Override
    public String toString() {
        return "LockedAxisData[" +
                "enabled=" + enabled + ", " +
                "axis=" + axis + ", " +
                "coordinate=" + coordinate + ", " +
                "color=" + color + ']';
    }

}
