package net.md_5.bungee.event;

/**
 * Represents a callable event.
 */
public abstract class Event
{

    /**
     * Stores cancelled status. Will be false unless a subclass publishes
     * setCancelled.
     */
    protected boolean cancelled = false;

    /**
     * Get the static handler list of this event subclass.
     *
     * @return HandlerList to call event with
     */
    public abstract HandlerList getHandlers();

    /**
     * Get event type name.
     *
     * @return event name
     */
    protected String getEventName()
    {
        return getClass().getSimpleName();
    }

    @Override
    public String toString()
    {
        return getEventName() + " (" + this.getClass().getName() + ")";
    }

    /**
     * Set cancelled status. Events which wish to be cancellable should
     * implement {@link Cancellable }and implement
     * {@link #setCancelled(boolean)} as:
     *
     * <pre>
     * public void setCancelled(boolean cancelled) {
     *     super.setCancelled(cancelled);
     * }
     * </pre>
     *
     * @param cancelled True to cancel event
     */
    protected void setCancelled(boolean cancelled)
    {
        this.cancelled = cancelled;
    }

    /**
     * Returning true will prevent calling any even {@link EventPriority} slots.
     *
     * @see EventPriority
     * @return false if the event is propagating; events which do not implement
     * {@link Cancellable} should never return true here
     */
    public boolean isCancelled()
    {
        return cancelled;
    }
}
