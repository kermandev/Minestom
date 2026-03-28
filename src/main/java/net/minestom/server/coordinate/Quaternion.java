package net.minestom.server.coordinate;

import org.jetbrains.annotations.Contract;

/**
 * A 4-dimensional rotation represented as a unit quaternion.
 * <p>
 * Layout: x, y, z are the imaginary vector components, w is the scalar (real) component.
 * <p>
 * Quaternions provide a robust way to represent 3D rotations without gimbal lock.
 * They are particularly useful for smooth interpolation between rotations (SLERP).
 * <p>
 * All implementations are immutable and subject to become value types.
 *
 * @param x the x component (i)
 * @param y the y component (j)
 * @param z the z component (k)
 * @param w the scalar (real) component
 */
public record Quaternion(double x, double y, double z, double w) {
    /**
     * The smallest difference between two double values to consider them equal if applicable.
     */
    public static final double EPSILON = 1e-6;

    /**
     * The zero quaternion representing no rotation.
     */
    public static final Quaternion ZERO = new Quaternion(0, 0, 0, 0);

    /**
     * The identity quaternion representing no rotation.
     */
    public static final Quaternion IDENTITY = new Quaternion(0, 0, 0, 1);

    /**
     * Quaternion representing a 180-degree rotation around the X axis.
     */
    public static final Quaternion ROTATE_X_180 = new Quaternion(1, 0, 0, 0);

    /**
     * Quaternion representing a 180-degree rotation around the Y axis.
     */
    public static final Quaternion ROTATE_Y_180 = new Quaternion(0, 1, 0, 0);

    /**
     * Quaternion representing a 180-degree rotation around the Z axis.
     */
    public static final Quaternion ROTATE_Z_180 = new Quaternion(0, 0, 1, 0);

    // Private constants for axis vectors (To be removed when value types)
    private static final Vec AXIS_X = new Vec(1, 0, 0);
    private static final Vec AXIS_Y = new Vec(0, 1, 0);

    /**
     * Creates a quaternion from an axis and angle.
     *
     * @param axis  the axis of rotation (will be normalized)
     * @param angle the angle of rotation in radians
     * @return a new quaternion representing the rotation
     */
    @Contract(pure = true, value = "_, _ -> new")
    public static Quaternion fromAxisAngle(Vec axis, double angle) {
        final double halfAngle = angle / 2.0;
        final double sin = Math.sin(halfAngle);
        final Vec normalized = axis.normalize();
        return new Quaternion(
                normalized.x() * sin,
                normalized.y() * sin,
                normalized.z() * sin,
                Math.cos(halfAngle)
        );
    }

    /**
     * Creates a quaternion from an axis and angle.
     *
     * @param axisX the x component of the rotation axis
     * @param axisY the y component of the rotation axis
     * @param axisZ the z component of the rotation axis
     * @param angle the angle of rotation in radians
     * @return a new quaternion representing the rotation
     */
    @Contract(pure = true, value = "_, _, _, _ -> new")
    public static Quaternion fromAxisAngle(double axisX, double axisY, double axisZ, double angle) {
        return fromAxisAngle(new Vec(axisX, axisY, axisZ), angle);
    }

    /**
     * Creates a quaternion from Euler angles (in radians).
     * <p>
     * Rotation order is YXZ (yaw, pitch, roll).
     *
     * @param yaw   rotation around Y axis in radians
     * @param pitch rotation around X axis in radians
     * @param roll  rotation around Z axis in radians
     * @return a new quaternion representing the rotation
     */
    @Contract(pure = true, value = "_, _, _ -> new")
    public static Quaternion fromEulerAngles(double yaw, double pitch, double roll) {
        final double cy = Math.cos(yaw * 0.5);
        final double sy = Math.sin(yaw * 0.5);
        final double cp = Math.cos(pitch * 0.5);
        final double sp = Math.sin(pitch * 0.5);
        final double cr = Math.cos(roll * 0.5);
        final double sr = Math.sin(roll * 0.5);

        return new Quaternion(
                cy * sp * cr + sy * cp * sr,
                sy * cp * cr - cy * sp * sr,
                cy * cp * sr - sy * sp * cr,
                cy * cp * cr + sy * sp * sr
        );
    }

    /**
     * Creates a quaternion from a position's view direction (yaw and pitch).
     * <p>
     * Converts degrees to radians and uses YXZ rotation order with roll = 0.
     *
     * @param pos the position containing yaw and pitch in degrees
     * @return a new quaternion representing the view rotation
     */
    @Contract(pure = true, value = "_ -> new")
    public static Quaternion fromView(Pos pos) {
        return fromView(pos.yaw(), pos.pitch());
    }

    /**
     * Creates a quaternion from yaw and pitch angles (in degrees).
     * <p>
     * Uses YXZ rotation order with roll = 0.
     *
     * @param yawDegrees   the yaw in degrees
     * @param pitchDegrees the pitch in degrees
     * @return a new quaternion representing the rotation
     */
    @Contract(pure = true, value = "_, _ -> new")
    public static Quaternion fromView(float yawDegrees, float pitchDegrees) {
        return fromEulerAngles(
                Math.toRadians(yawDegrees),
                Math.toRadians(pitchDegrees),
                0.0
        );
    }

    /**
     * Creates a quaternion representing a rotation around the X axis.
     *
     * @param angle the angle in radians
     * @return a new quaternion
     */
    @Contract(pure = true, value = "_ -> new")
    public static Quaternion fromXRotation(double angle) {
        final double halfAngle = angle / 2.0;
        return new Quaternion(Math.sin(halfAngle), 0, 0, Math.cos(halfAngle));
    }

    /**
     * Creates a quaternion representing a rotation around the Y axis.
     *
     * @param angle the angle in radians
     * @return a new quaternion
     */
    @Contract(pure = true, value = "_ -> new")
    public static Quaternion fromYRotation(double angle) {
        final double halfAngle = angle / 2.0;
        return new Quaternion(0, Math.sin(halfAngle), 0, Math.cos(halfAngle));
    }

    /**
     * Creates a quaternion representing a rotation around the Z axis.
     *
     * @param angle the angle in radians
     * @return a new quaternion
     */
    @Contract(pure = true, value = "_ -> new")
    public static Quaternion fromZRotation(double angle) {
        final double halfAngle = angle / 2.0;
        return new Quaternion(0, 0, Math.sin(halfAngle), Math.cos(halfAngle));
    }

    /**
     * Creates a quaternion that rotates from one direction to another.
     *
     * @param from the starting direction (will be normalized)
     * @param to   the target direction (will be normalized)
     * @return a new quaternion representing the rotation
     */
    @Contract(pure = true, value = "_, _ -> new")
    public static Quaternion fromRotation(Vec from, Vec to) {
        final Vec f = from.normalize();
        final Vec t = to.normalize();
        final double dot = f.dot(t);

        // Check if vectors are parallel
        if (dot >= 1.0 - EPSILON) {
            return IDENTITY;
        }

        // Check if vectors are opposite
        if (dot <= -1.0 + EPSILON) {
            // Find an orthogonal axis
            Vec axis = AXIS_X.cross(f);
            if (axis.lengthSquared() < EPSILON) {
                axis = AXIS_Y.cross(f);
            }
            return fromAxisAngle(axis.normalize(), Math.PI);
        }

        final Vec axis = f.cross(t);
        final double s = Math.sqrt((1.0 + dot) * 2.0);
        final double invS = 1.0 / s;

        return new Quaternion(
                axis.x() * invS,
                axis.y() * invS,
                axis.z() * invS,
                s * 0.5
        ).normalize();
    }

    /**
     * Multiplies this quaternion by another (concatenates rotations).
     * <p>
     * The resulting quaternion represents applying the other rotation first, then this rotation.
     * In other words, {@code q1.mul(q2)} produces the same result as {@code q1.rotate(q2.rotate(v))}.
     *
     * @param other the quaternion to multiply by
     * @return a new quaternion representing the combined rotation
     */
    @Contract(pure = true, value = "_ -> new")
    public Quaternion mul(Quaternion other) {
        return new Quaternion(
                w * other.x + x * other.w + y * other.z - z * other.y,
                w * other.y - x * other.z + y * other.w + z * other.x,
                w * other.z + x * other.y - y * other.x + z * other.w,
                w * other.w - x * other.x - y * other.y - z * other.z
        );
    }

    /**
     * Multiplies all components of this quaternion by a scalar value.
     * <p>
     * Note: This is not the same as quaternion multiplication and will typically
     * result in a non-unit quaternion. Use {@link #normalize()} if needed.
     *
     * @param scalar the scalar value to multiply by
     * @return a new quaternion with scaled components
     */
    @Contract(pure = true, value = "_ -> new")
    public Quaternion mul(double scalar) {
        return new Quaternion(x * scalar, y * scalar, z * scalar, w * scalar);
    }

    /**
     * Divides all components of this quaternion by a scalar value.
     * <p>
     * Note: This will typically result in a non-unit quaternion.
     * Use {@link #normalize()} if needed.
     *
     * @param scalar the scalar value to divide by
     * @return a new quaternion with divided components
     */
    @Contract(pure = true, value = "_ -> new")
    public Quaternion div(double scalar) {
        return new Quaternion(x / scalar, y / scalar, z / scalar, w / scalar);
    }

    /**
     * Adds another quaternion to this one component-wise.
     * <p>
     * Note: This will typically result in a non-unit quaternion.
     * Use {@link #normalize()} if needed.
     *
     * @param other the quaternion to add
     * @return a new quaternion with summed components
     */
    @Contract(pure = true, value = "_ -> new")
    public Quaternion add(Quaternion other) {
        return new Quaternion(x + other.x, y + other.y, z + other.z, w + other.w);
    }

    /**
     * Subtracts another quaternion from this one component-wise.
     * <p>
     * Note: This will typically result in a non-unit quaternion.
     * Use {@link #normalize()} if needed.
     *
     * @param other the quaternion to subtract
     * @return a new quaternion with subtracted components
     */
    @Contract(pure = true, value = "_ -> new")
    public Quaternion sub(Quaternion other) {
        return new Quaternion(x - other.x, y - other.y, z - other.z, w - other.w);
    }

    /**
     * Rotates a vector by this quaternion.
     *
     * @param vec the vector to rotate
     * @return the rotated vector
     */
    @Contract(pure = true, value = "_ -> new")
    public Vec rotate(Vec vec) {
        final double ix = w * vec.x() + y * vec.z() - z * vec.y();
        final double iy = w * vec.y() + z * vec.x() - x * vec.z();
        final double iz = w * vec.z() + x * vec.y() - y * vec.x();
        final double iw = -x * vec.x() - y * vec.y() - z * vec.z();

        return new Vec(
                ix * w + iw * -x + iy * -z - iz * -y,
                iy * w + iw * -y + iz * -x - ix * -z,
                iz * w + iw * -z + ix * -y - iy * -x
        );
    }

    /**
     * Rotates a point by this quaternion.
     *
     * @param point the point to rotate
     * @return the rotated point as a Vec
     */
    @Contract(pure = true, value = "_ -> new")
    public Vec rotate(Point point) {
        return rotate(new Vec(point.x(), point.y(), point.z()));
    }

    /**
     * Gets the conjugate of this quaternion.
     * <p>
     * For unit quaternions, the conjugate represents the inverse rotation.
     *
     * @return the conjugate quaternion
     */
    @Contract(pure = true, value = "-> new")
    public Quaternion conjugate() {
        return new Quaternion(-x, -y, -z, w);
    }

    /**
     * Negates all components of this quaternion.
     * <p>
     * Note: -q represents the same rotation as q, but this may be useful for certain calculations.
     *
     * @return the negated quaternion
     */
    @Contract(pure = true, value = "-> new")
    public Quaternion neg() {
        return new Quaternion(-x, -y, -z, -w);
    }

    /**
     * Gets the inverse of this quaternion.
     * <p>
     * For unit quaternions, this is the same as the conjugate.
     *
     * @return the inverse quaternion
     */
    @Contract(pure = true, value = "-> new")
    public Quaternion inverse() {
        final double lengthSq = lengthSquared();
        if (Math.abs(lengthSq - 1.0) < EPSILON) {
            return conjugate();
        }
        return new Quaternion(-x / lengthSq, -y / lengthSq, -z / lengthSq, w / lengthSq);
    }

    /**
     * Gets the magnitude of this quaternion squared.
     *
     * @return the squared magnitude
     */
    @Contract(pure = true)
    public double lengthSquared() {
        return x * x + y * y + z * z + w * w;
    }

    /**
     * Gets the magnitude of this quaternion.
     *
     * @return the magnitude
     */
    @Contract(pure = true)
    public double length() {
        return Math.sqrt(lengthSquared());
    }

    /**
     * Normalizes this quaternion to unit length.
     *
     * @return a new normalized quaternion
     */
    @Contract(pure = true, value = "-> new")
    public Quaternion normalize() {
        final double len = length();
        if (len < EPSILON) {
            return IDENTITY;
        }
        return new Quaternion(x / len, y / len, z / len, w / len);
    }

    /**
     * Checks if this quaternion is normalized (unit length).
     *
     * @return true if the quaternion has unit length
     */
    @Contract(pure = true)
    public boolean isNormalized() {
        return Math.abs(lengthSquared() - 1.0) < EPSILON;
    }

    /**
     * Calculates the dot product of this quaternion with another.
     *
     * @param other the other quaternion
     * @return the dot product
     */
    @Contract(pure = true)
    public double dot(Quaternion other) {
        return x * other.x + y * other.y + z * other.z + w * other.w;
    }

    /**
     * Performs spherical linear interpolation (SLERP) between this quaternion and another.
     * <p>
     * SLERP provides smooth interpolation between two rotations.
     *
     * @param target the target quaternion
     * @param alpha  the interpolation factor [0.0, 1.0]
     * @return the interpolated quaternion
     */
    @Contract(pure = true, value = "_, _ -> new")
    public Quaternion slerp(Quaternion target, double alpha) {
        if (alpha <= 0.0) return this;
        if (alpha >= 1.0) return target;

        double dot = dot(target);

        // If the dot product is negative, slerp won't take the shorter path
        // Fix by reversing one quaternion
        Quaternion correctedTarget = target;
        if (dot < 0.0) {
            correctedTarget = new Quaternion(-target.x, -target.y, -target.z, -target.w);
            dot = -dot;
        }

        // If quaternions are very close, use linear interpolation
        if (dot > 1.0 - EPSILON) {
            return new Quaternion(
                    x + alpha * (correctedTarget.x - x),
                    y + alpha * (correctedTarget.y - y),
                    z + alpha * (correctedTarget.z - z),
                    w + alpha * (correctedTarget.w - w)
            ).normalize();
        }

        final double theta = Math.acos(Math.clamp(dot, -1.0, 1.0));
        final double sinTheta = Math.sin(theta);
        final double a = Math.sin((1.0 - alpha) * theta) / sinTheta;
        final double b = Math.sin(alpha * theta) / sinTheta;

        return new Quaternion(
                a * x + b * correctedTarget.x,
                a * y + b * correctedTarget.y,
                a * z + b * correctedTarget.z,
                a * w + b * correctedTarget.w
        );
    }

    /**
     * Performs normalized linear interpolation (NLERP) between this quaternion and another.
     * <p>
     * NLERP is faster than SLERP but doesn't provide constant angular velocity.
     *
     * @param target the target quaternion
     * @param alpha  the interpolation factor [0.0, 1.0]
     * @return the interpolated quaternion
     */
    @Contract(pure = true, value = "_, _ -> new")
    public Quaternion nlerp(Quaternion target, double alpha) {
        if (alpha <= 0.0) return this;
        if (alpha >= 1.0) return target;

        double dot = dot(target);

        // Choose the shortest path
        Quaternion correctedTarget = target;
        if (dot < 0.0) {
            correctedTarget = new Quaternion(-target.x, -target.y, -target.z, -target.w);
        }

        return new Quaternion(
                x + alpha * (correctedTarget.x - x),
                y + alpha * (correctedTarget.y - y),
                z + alpha * (correctedTarget.z - z),
                w + alpha * (correctedTarget.w - w)
        ).normalize();
    }

    /**
     * Gets the rotation axis of this quaternion.
     * <p>
     * Returns the X axis for the identity quaternion.
     *
     * @return the rotation axis
     */
    @Contract(pure = true, value = "-> new")
    public Vec axis() {
        final double s = Math.sqrt(1.0 - w * w);
        if (s < EPSILON) {
            return AXIS_X;
        }
        return new Vec(x / s, y / s, z / s);
    }

    /**
     * Gets the rotation angle of this quaternion in radians.
     *
     * @return the rotation angle
     */
    @Contract(pure = true)
    public double angle() {
        return 2.0 * Math.acos(Math.clamp(w, -1.0, 1.0));
    }

    /**
     * Converts this quaternion to Euler angles (in radians).
     * <p>
     * Returns the angles in YXZ order (yaw, pitch, roll).
     *
     * @return a Vec containing (yaw, pitch, roll) in radians
     */
    @Contract(pure = true, value = "-> new")
    public Vec toEulerAngles() {
        // Roll (z-axis rotation)
        final double sinr_cosp = 2.0 * (w * z + x * y);
        final double cosr_cosp = 1.0 - 2.0 * (y * y + z * z);
        final double roll = Math.atan2(sinr_cosp, cosr_cosp);

        // Pitch (x-axis rotation)
        final double sinp = 2.0 * (w * x - y * z);
        final double pitch;
        if (Math.abs(sinp) >= 1.0) {
            pitch = Math.copySign(Math.PI / 2.0, sinp); // Use 90 degrees if out of range
        } else {
            pitch = Math.asin(sinp);
        }

        // Yaw (y-axis rotation)
        final double siny_cosp = 2.0 * (w * y + z * x);
        final double cosy_cosp = 1.0 - 2.0 * (x * x + y * y);
        final double yaw = Math.atan2(siny_cosp, cosy_cosp);

        return new Vec(yaw, pitch, roll);
    }

    /**
     * Checks if this quaternion is exactly equal to another (bit comparison).
     *
     * @param other the other quaternion
     * @return true if the quaternions are exactly equal
     */
    @Contract(pure = true)
    public boolean same(Quaternion other) {
        return x == other.x && y == other.y && z == other.z && w == other.w;
    }

    /**
     * Checks if this quaternion is approximately equal to another within {@link #EPSILON}.
     *
     * @param other the other quaternion
     * @return true if the quaternions are approximately equal
     */
    @Contract(pure = true)
    public boolean similar(Quaternion other) {
        return similar(other, EPSILON);
    }

    /**
     * Checks if this quaternion is approximately equal to another within the given epsilon.
     *
     * @param other   the other quaternion
     * @param epsilon the maximum difference allowed (exclusive)
     * @return true if the quaternions are approximately equal
     * @throws IllegalArgumentException if epsilon is less than or equal to 0
     */
    @Contract(pure = true)
    public boolean similar(Quaternion other, double epsilon) {
        if (epsilon <= 0) {
            throw new IllegalArgumentException("Epsilon must be greater than 0 but found " + epsilon);
        }
        return Math.abs(x - other.x) < epsilon &&
                Math.abs(y - other.y) < epsilon &&
                Math.abs(z - other.z) < epsilon &&
                Math.abs(w - other.w) < epsilon;
    }

    /**
     * Checks if this quaternion represents exactly the same rotation as another (bit comparison).
     * <p>
     * Note that q and -q represent the same rotation.
     *
     * @param other the other quaternion
     * @return true if the quaternions represent exactly the same rotation
     */
    @Contract(pure = true)
    public boolean sameRotation(Quaternion other) {
        return same(other) || same(other.neg());
    }

    /**
     * Checks if this quaternion represents approximately the same rotation as another within {@link #EPSILON}.
     * <p>
     * Note that q and -q represent the same rotation.
     *
     * @param other the other quaternion
     * @return true if the quaternions represent approximately the same rotation
     */
    @Contract(pure = true)
    public boolean similarRotation(Quaternion other) {
        return similarRotation(other, EPSILON);
    }

    /**
     * Checks if this quaternion represents approximately the same rotation as another within the given epsilon.
     * <p>
     * Note that q and -q represent the same rotation.
     *
     * @param other   the other quaternion
     * @param epsilon the maximum difference allowed (exclusive)
     * @return true if the quaternions represent approximately the same rotation
     * @throws IllegalArgumentException if epsilon is less than or equal to 0
     */
    @Contract(pure = true)
    public boolean similarRotation(Quaternion other, double epsilon) {
        if (epsilon <= 0) {
            throw new IllegalArgumentException("Epsilon must be greater than 0 but found " + epsilon);
        }
        return similar(other, epsilon) || similar(other.neg(), epsilon);
    }
}
