package dev.rosewood.rosestacker.event;

import dev.rosewood.rosestacker.stack.Stack;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Base event for when the size of a Stack decreases
 *
 * @param <T> The stack type
 */
public abstract class UnstackEvent<T extends Stack<?>> extends Event implements Cancellable {

    private boolean cancelled;
    protected final T stack;

    public UnstackEvent(@NotNull T stack) {
        super(!Bukkit.isPrimaryThread());

        this.stack = stack;
        this.cancelled = false;
    }

    /**
     * @return the Stack that is getting unstacked from
     */
    @NotNull
    public T getStack() {
        return this.stack;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}
