package dev.rosewood.rosestacker.stack.settings.entity;

import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosestacker.stack.EntityStackComparisonResult;
import dev.rosewood.rosestacker.stack.StackedEntity;
import dev.rosewood.rosestacker.stack.settings.EntityStackSettings;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class SquidStackSettings extends EntityStackSettings {

    public SquidStackSettings(CommentedFileConfiguration entitySettingsFileConfiguration) {
        super(entitySettingsFileConfiguration);
    }

    @Override
    protected EntityStackComparisonResult canStackWithInternal(StackedEntity stack1, StackedEntity stack2) {
        return EntityStackComparisonResult.CAN_STACK;
    }

    @Override
    protected void setDefaultsInternal() {

    }

    @Override
    public boolean isSwimmingMob() {
        return true;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SQUID;
    }

    @Override
    public Material getSpawnEggMaterial() {
        return Material.SQUID_SPAWN_EGG;
    }

    @Override
    public List<String> getDefaultSpawnRequirements() {
        return Arrays.asList(
                "fluid:water",
                "below-sea-level",
                "above-y-axis:45"
        );
    }

}
