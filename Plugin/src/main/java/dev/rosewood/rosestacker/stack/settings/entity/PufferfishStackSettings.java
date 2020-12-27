package dev.rosewood.rosestacker.stack.settings.entity;

import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosestacker.stack.EntityStackComparisonResult;
import dev.rosewood.rosestacker.stack.StackedEntity;
import dev.rosewood.rosestacker.stack.settings.EntityStackSettings;
import java.util.Collections;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.PufferFish;

public class PufferfishStackSettings extends EntityStackSettings {

    private final boolean dontStackIfDifferentInflation;

    public PufferfishStackSettings(CommentedFileConfiguration entitySettingsFileConfiguration) {
        super(entitySettingsFileConfiguration);

        this.dontStackIfDifferentInflation = this.settingsConfiguration.getBoolean("dont-stack-if-different-inflation");
    }

    @Override
    protected EntityStackComparisonResult canStackWithInternal(StackedEntity stack1, StackedEntity stack2) {
        PufferFish pufferFish1 = (PufferFish) stack1.getEntity();
        PufferFish pufferFish2 = (PufferFish) stack2.getEntity();

        if (this.dontStackIfDifferentInflation && pufferFish1.getPuffState() != pufferFish2.getPuffState())
            return EntityStackComparisonResult.DIFFERENT_INFLATIONS;

        return EntityStackComparisonResult.CAN_STACK;
    }

    @Override
    protected void setDefaultsInternal() {
        this.setIfNotExists("dont-stack-if-different-inflation", false);
    }

    @Override
    public boolean isSwimmingMob() {
        return true;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.PUFFERFISH;
    }

    @Override
    public Material getSpawnEggMaterial() {
        return Material.PUFFERFISH_SPAWN_EGG;
    }

    @Override
    public List<String> getDefaultSpawnRequirements() {
        return Collections.singletonList("fluid:water");
    }

}
