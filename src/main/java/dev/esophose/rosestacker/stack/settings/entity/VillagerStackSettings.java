package dev.esophose.rosestacker.stack.settings.entity;

import dev.esophose.rosestacker.config.CommentedFileConfiguration;
import dev.esophose.rosestacker.stack.StackedEntity;
import dev.esophose.rosestacker.stack.settings.EntityStackSettings;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;

public class VillagerStackSettings extends EntityStackSettings {

    private boolean dontStackIfDifferentProfession;
    private boolean dontStackIfDifferentType;
    private boolean dontStackIfDifferentLevel;

    public VillagerStackSettings(CommentedFileConfiguration entitySettingsFileConfiguration) {
        super(entitySettingsFileConfiguration);

        this.dontStackIfDifferentProfession = this.entitySettingsConfiguration.getBoolean("dont-stack-if-different-profession");
        this.dontStackIfDifferentType = this.entitySettingsConfiguration.getBoolean("dont-stack-if-different-type");
        this.dontStackIfDifferentLevel = this.entitySettingsConfiguration.getBoolean("dont-stack-if-different-level");
    }

    @Override
    protected boolean canStackWithInternal(StackedEntity stack1, StackedEntity stack2) {
        Villager villager1 = (Villager) stack1.getEntity();
        Villager villager2 = (Villager) stack2.getEntity();

        if (this.dontStackIfDifferentProfession && villager1.getProfession() != villager2.getProfession())
            return false;

        if (this.dontStackIfDifferentType && villager1.getType() != villager2.getType())
            return false;

        return !this.dontStackIfDifferentLevel || (villager1.getVillagerLevel() == villager2.getVillagerLevel());
    }

    @Override
    protected void setDefaultsInternal() {
        this.setIfNotExists("dont-stack-if-different-profession", false);
        this.setIfNotExists("dont-stack-if-different-type", false);
        this.setIfNotExists("dont-stack-if-different-level", false);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.VILLAGER;
    }

}
