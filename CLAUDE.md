# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Walk The Line is a Fabric Minecraft mod that restricts player movement to a single line (X or Z axis) in each dimension, creating a unique gameplay challenge. The mod locks players to specific coordinates that align with critical game objectives:
- **Overworld**: Locked to the axis passing through the closest stronghold's End portal frame
- **Nether**: Locked to the same axis as Overworld, at the coordinate where the player enters
- **End**: Always locked to Z-axis at coordinate 0 (intersecting with the End Island Portal)

## Technology Stack

- **Minecraft Version**: 1.21.9
- **Fabric Loader**: 0.17.2
- **Java**: 21
- **Loom**: 1.11-SNAPSHOT
- **Key Dependencies**:
  - Fabric API: 0.133.14+1.21.9
  - Cloth Config: 20.0.148 (for config GUI)
  - ModMenu: 16.0.0-rc.1 (for mod integration)
  - Lombok: 1.18.38
  - JOML: 1.10.8 (math library)
  - Fabric Permissions API: 0.5.0

## Build Commands

```bash
# Build the mod (creates JAR in build/libs/)
./gradlew build

# Run client in development
./gradlew runClient

# Run server in development
./gradlew runServer

# Clean build artifacts
./gradlew clean

# Generate sources (for IDE)
./gradlew genSources
```

The output JAR will be named: `walk-the-line-<version>-<minecraft_version>.jar`

## Project Structure

### Source Organization

The project uses Fabric's split source sets:
- `src/main/java/` - Server-side and shared code
- `src/client/java/` - Client-side only code (rendering, client events)
- `src/main/resources/` - Server resources (mixins, fabric.mod.json)
- `src/client/resources/` - Client resources (client mixins)

### Core Architecture

**State Management** (`state/` package):
- `PlayerState`: PersistentState implementation that stores per-player, per-save, per-dimension data using Codecs
- `SavesData`: Maps save names to WorldsData
- `WorldsData`: Maps dimension keys to LockedAxisData + enabled flag
- `LockedAxisData`: Record containing axis (X/Z) and coordinate value
- Data hierarchy: `PlayerState` → `SavesData` (by UUID) → `WorldsData` (by save name) → `LockedAxisData` (by dimension)

**Movement Control** (`movement/` package):
- `AxisLockManager`: Core logic for axis locking, distance checking, pushback/teleport mechanics, and dimension-specific lock determination
- Uses mixin-based movement interception (see `mixin/ServerMovementMixin.java`)
- Implements two-tier boundary enforcement:
  1. Soft boundary (coordinateTolerance): Applies velocity-based pushback
  2. Hard boundary (teleportTolerance): Teleports player back to valid position

**Rendering** (`client/render/` package):
- `RendererHandler`: Manages rendering lifecycle and coordinates line renderers
- `LineRenderer`: Renders the visual line indicator showing movement boundary
- `RainbowLine`: Implements rainbow color cycling effect
- Uses `WorldRendererMixin` to inject rendering during world render

**Configuration**:
- `ConfigManager`: Handles JSON serialization/deserialization using Gson
- `WalkTheLineConfig`: Server-side config (coordinateTolerance, teleportTolerance, etc.)
- `WalkTheLineClientConfig`: Client-side config using Cloth Config (rendering options, colors)
- Config file location: `config/walk-the-line.json`

**Networking** (`network/` package):
- `SyncPacket`: Custom packet for syncing lock state from server to client (needed for rendering)
- Uses Fabric Networking API for server→client communication

### Critical Implementation Details

1. **Stronghold Location**: Uses `StrongholdLocator.getClosestStrongHoldPortalroom()` which finds strongholds and determines portal room orientation. The axis is deliberately flipped to align with the portal frame path.

2. **Dimension Transitions**: Lock state is recalculated on dimension change (see `ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD` in `AxisLockManager`)

3. **Vehicle Support**: Movement checks handle both player and vehicle entities (boats, minecarts, striders)

4. **Safe Teleportation**: `findSafeYAbove()` scans vertically to find safe landing positions (solid block below, air at foot/head level)

5. **Mixins**:
   - `ServerMovementMixin`: Intercepts player movement to enforce axis locks
   - `ClientMovementMixin`: Client-side movement handling
   - `WorldRendererMixin`: Injects custom rendering for the boundary line

## Common Development Patterns

### Adding New Config Options

1. Add field to `WalkTheLineConfig` or `WalkTheLineClientConfig`
2. Update `ConfigManager` if serialization logic changes
3. For client config: Update `ModMenuIntegration.getConfigScreen()` to add GUI controls
4. Reload config in relevant managers (e.g., `AxisLockManager` reads config on initialization)

### Modifying Movement Behavior

Movement enforcement happens in:
- `AxisLockManager.checkDistanceFromLockedAxis()` - Main enforcement logic
- `ServerMovementMixin` - Movement interception point
- Update `applyPushback()` or teleport logic as needed

### Adding Dimension Support

Update `AxisLockManager.determineDimensionLocks()`:
- Add new case for dimension registry key
- Define axis and coordinate calculation logic
- Ensure proper sync to client for rendering

## Testing

No automated tests currently exist. Manual testing workflow:
1. Build mod with `./gradlew build`
2. Copy JAR to Minecraft mods folder or use `./gradlew runClient`
3. Create new world or join existing
4. Run `/WalkTheLine enable` in chat
5. Test movement restrictions, dimension transitions, vehicle interactions

## Known Issues & Edge Cases

- Mod resets state between worlds (see commit 0210b83)
- NullPointerException fixes for Nether travel (see commits 7bdfbb5, 3b50fb2)
- Temporary config settings need manual reset on world change