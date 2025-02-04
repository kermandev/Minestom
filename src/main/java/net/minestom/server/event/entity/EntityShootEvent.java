package net.minestom.server.event.entity;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityProjectile;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.event.trait.mutation.EventMutatorCancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Called with {@link EntityProjectile#shoot(Point, double, double)}
 */
public record EntityShootEvent(@NotNull Entity entity, @NotNull Entity projectile, @NotNull Point to, double power, double spread, boolean cancelled) implements EntityInstanceEvent, CancellableEvent<EntityShootEvent> {

    public EntityShootEvent(@NotNull Entity entity, @NotNull Entity projectile, @NotNull Point to, double power, double spread) {
        this(entity, projectile, to, power, spread, false);
    }

    /**
     * Gets the projectile.
     *
     * @return the projectile.
     */
    public @NotNull Entity projectile() {
        return projectile;
    }

    /**
     * Gets the position projectile was shot to.
     *
     * @return the position projectile was shot to.
     */
    public @NotNull Point to() {
        return to;
    }

    /**
     * Gets shot spread.
     *
     * @return shot spread.
     */
    @Override
    public double spread() {
        return spread;
    }

    /**
     * Gets shot power.
     *
     * @return shot power.
     */
    @Override
    public double power() {
        return power;
    }

    @Override
    public @NotNull Mutator mutator() {
        return new Mutator(this);
    }

    public static final class Mutator extends EventMutatorCancellable.Simple<EntityShootEvent> {
        private double power;
        private double spread;

        public Mutator(EntityShootEvent event) {
            super(event);
            this.power = event.power;
            this.spread = event.spread;
        }

        /**
         * Gets shot spread.
         *
         * @return shot spread.
         */
        public double getSpread() {
            return this.spread;
        }

        /**
         * Sets shot spread.
         *
         * @param spread shot spread.
         */
        public void setSpread(double spread) {
            this.spread = spread;
        }

        /**
         * Gets shot power.
         *
         * @return shot power.
         */
        public double getPower() {
            return this.power;
        }

        /**
         * Sets shot power.
         *
         * @param power shot power.
         */
        public void setPower(double power) {
            this.power = power;
        }

        @Override
        public @NotNull EntityShootEvent mutated() {
            return new EntityShootEvent(this.originalEvent.entity, this.originalEvent.projectile, this.originalEvent.to, this.power, this.spread, this.isCancelled());
        }
    }

}

