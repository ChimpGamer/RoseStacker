package dev.esophose.rosestacker.stack.settings.entity;

import dev.esophose.rosestacker.config.CommentedFileConfiguration;
import org.bukkit.entity.EntityType;

public class MagmaCubeStackSettings extends SlimeStackSettings {

    private boolean dontStackIfDifferentSize;

    public MagmaCubeStackSettings(CommentedFileConfiguration entitySettingsFileConfiguration) {
        super(entitySettingsFileConfiguration);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.MAGMA_CUBE;
    }

}
