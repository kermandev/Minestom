package net.minestom.server.coordinate;

import org.junit.jupiter.api.Test;

import static net.minestom.server.coordinate.Quaternion.EPSILON;
import static net.minestom.server.coordinate.Quaternion.IDENTITY;
import static org.junit.jupiter.api.Assertions.*;

public class QuaternionTest {

    @Test
    public void testConstructor() {
        Quaternion q = new Quaternion(0.1, 0.2, 0.3, 0.4);
        assertEquals(0.1, q.x());
        assertEquals(0.2, q.y());
        assertEquals(0.3, q.z());
        assertEquals(0.4, q.w());
    }

    @Test
    public void testConstants() {
        // Test IDENTITY
        assertEquals(0.0, Quaternion.IDENTITY.x());
        assertEquals(0.0, Quaternion.IDENTITY.y());
        assertEquals(0.0, Quaternion.IDENTITY.z());
        assertEquals(1.0, Quaternion.IDENTITY.w());

        // Test 180-degree rotations
        assertEquals(1.0, Quaternion.ROTATE_X_180.x());
        assertEquals(0.0, Quaternion.ROTATE_X_180.y());
        assertEquals(0.0, Quaternion.ROTATE_X_180.z());
        assertEquals(0.0, Quaternion.ROTATE_X_180.w());

        assertEquals(0.0, Quaternion.ROTATE_Y_180.x());
        assertEquals(1.0, Quaternion.ROTATE_Y_180.y());
        assertEquals(0.0, Quaternion.ROTATE_Y_180.z());
        assertEquals(0.0, Quaternion.ROTATE_Y_180.w());

        assertEquals(0.0, Quaternion.ROTATE_Z_180.x());
        assertEquals(0.0, Quaternion.ROTATE_Z_180.y());
        assertEquals(1.0, Quaternion.ROTATE_Z_180.z());
        assertEquals(0.0, Quaternion.ROTATE_Z_180.w());
    }

    @Test
    public void testFromAxisAngle() {
        // Test rotation around X axis
        Vec axisX = new Vec(1, 0, 0);
        Quaternion q = Quaternion.fromAxisAngle(axisX, Math.PI / 2);

        Vec rotated = q.rotate(new Vec(0, 1, 0));
        assertEquals(0, rotated.x(), EPSILON);
        assertEquals(0, rotated.y(), EPSILON);
        assertEquals(1, rotated.z(), EPSILON);

        // Test rotation around Y axis
        Vec axisY = new Vec(0, 1, 0);
        q = Quaternion.fromAxisAngle(axisY, Math.PI / 2);

        rotated = q.rotate(new Vec(1, 0, 0));
        assertEquals(0, rotated.x(), EPSILON);
        assertEquals(0, rotated.y(), EPSILON);
        assertEquals(-1, rotated.z(), EPSILON);

        // Test with unnormalized axis (should still work)
        Vec unnormalizedAxis = new Vec(2, 0, 0);
        q = Quaternion.fromAxisAngle(unnormalizedAxis, Math.PI / 2);
        rotated = q.rotate(new Vec(0, 1, 0));
        assertEquals(0, rotated.x(), EPSILON);
        assertEquals(0, rotated.y(), EPSILON);
        assertEquals(1, rotated.z(), EPSILON);
    }

    @Test
    public void testFromAxisAngleComponents() {
        Quaternion q = Quaternion.fromAxisAngle(1, 0, 0, Math.PI / 2);
        Vec rotated = q.rotate(new Vec(0, 1, 0));
        assertEquals(0, rotated.x(), EPSILON);
        assertEquals(0, rotated.y(), EPSILON);
        assertEquals(1, rotated.z(), EPSILON);
    }

    @Test
    public void testFromEulerAngles() {
        // Test identity
        Quaternion q = Quaternion.fromEulerAngles(0, 0, 0);
        assertTrue(q.similar(IDENTITY));

        // Test yaw only (rotation around Y)
        q = Quaternion.fromEulerAngles(Math.PI / 2, 0, 0);
        Vec rotated = q.rotate(new Vec(1, 0, 0));
        assertEquals(0, rotated.x(), EPSILON);
        assertEquals(0, rotated.y(), EPSILON);
        assertEquals(-1, rotated.z(), EPSILON);

        // Test pitch only (rotation around X)
        q = Quaternion.fromEulerAngles(0, Math.PI / 2, 0);
        rotated = q.rotate(new Vec(0, 1, 0));
        assertEquals(0, rotated.x(), EPSILON);
        assertEquals(0, rotated.y(), EPSILON);
        assertEquals(1, rotated.z(), EPSILON);

        // Test roll only (rotation around Z)
        q = Quaternion.fromEulerAngles(0, 0, Math.PI / 2);
        rotated = q.rotate(new Vec(1, 0, 0));
        assertEquals(0, rotated.x(), EPSILON);
        assertEquals(1, rotated.y(), EPSILON);
        assertEquals(0, rotated.z(), EPSILON);
    }

    @Test
    public void testFromView() {
        // Test with Pos
        Pos pos = new Pos(0, 0, 0, 90f, 0f);
        Quaternion q = Quaternion.fromView(pos);
        assertNotNull(q);

        // Test with yaw/pitch directly
        q = Quaternion.fromView(90f, 45f);
        assertNotNull(q);

        // Test identity view
        q = Quaternion.fromView(0f, 0f);
        assertTrue(q.similar(IDENTITY));
    }

    @Test
    public void testFromXRotation() {
        Quaternion q = Quaternion.fromXRotation(Math.PI / 2);
        Vec rotated = q.rotate(new Vec(0, 1, 0));
        assertEquals(0, rotated.x(), EPSILON);
        assertEquals(0, rotated.y(), EPSILON);
        assertEquals(1, rotated.z(), EPSILON);
    }

    @Test
    public void testFromYRotation() {
        Quaternion q = Quaternion.fromYRotation(Math.PI / 2);
        Vec rotated = q.rotate(new Vec(1, 0, 0));
        assertEquals(0, rotated.x(), EPSILON);
        assertEquals(0, rotated.y(), EPSILON);
        assertEquals(-1, rotated.z(), EPSILON);
    }

    @Test
    public void testFromZRotation() {
        Quaternion q = Quaternion.fromZRotation(Math.PI / 2);
        Vec rotated = q.rotate(new Vec(1, 0, 0));
        assertEquals(0, rotated.x(), EPSILON);
        assertEquals(1, rotated.y(), EPSILON);
        assertEquals(0, rotated.z(), EPSILON);
    }

    @Test
    public void testFromRotation() {
        // Test parallel vectors
        Vec from = new Vec(1, 0, 0);
        Vec to = new Vec(1, 0, 0);
        Quaternion q = Quaternion.fromRotation(from, to);
        assertTrue(q.similar(IDENTITY));

        // Test opposite vectors
        from = new Vec(1, 0, 0);
        to = new Vec(-1, 0, 0);
        q = Quaternion.fromRotation(from, to);
        Vec rotated = q.rotate(from);
        assertEquals(-1, rotated.x(), EPSILON);
        assertEquals(0, rotated.y(), EPSILON);
        assertEquals(0, rotated.z(), EPSILON);

        // Test perpendicular vectors
        from = new Vec(1, 0, 0);
        to = new Vec(0, 1, 0);
        q = Quaternion.fromRotation(from, to);
        rotated = q.rotate(from);
        assertEquals(0, rotated.x(), EPSILON);
        assertEquals(1, rotated.y(), EPSILON);
        assertEquals(0, rotated.z(), EPSILON);

        // Test with unnormalized vectors
        from = new Vec(2, 0, 0);
        to = new Vec(0, 3, 0);
        q = Quaternion.fromRotation(from, to);
        rotated = q.rotate(new Vec(1, 0, 0));
        assertEquals(0, rotated.x(), EPSILON);
        assertEquals(1, rotated.y(), EPSILON);
        assertEquals(0, rotated.z(), EPSILON);
    }

    @Test
    public void testMul() {
        // Test quaternion multiplication (rotation composition)
        Quaternion qX = Quaternion.fromXRotation(Math.PI / 2);
        Quaternion qY = Quaternion.fromYRotation(Math.PI / 2);
        Quaternion combined = qX.mul(qY);

        Vec v = new Vec(1, 0, 0);
        Vec rotated = combined.rotate(v);

        Vec expected = qX.rotate(qY.rotate(v));
        assertEquals(expected.x(), rotated.x(), EPSILON);
        assertEquals(expected.y(), rotated.y(), EPSILON);
        assertEquals(expected.z(), rotated.z(), EPSILON);
    }

    @Test
    public void testMulScalar() {
        Quaternion q = new Quaternion(1, 2, 3, 4);
        Quaternion scaled = q.mul(2.0);
        assertEquals(2, scaled.x());
        assertEquals(4, scaled.y());
        assertEquals(6, scaled.z());
        assertEquals(8, scaled.w());
    }

    @Test
    public void testDiv() {
        Quaternion q = new Quaternion(2, 4, 6, 8);
        Quaternion divided = q.div(2.0);
        assertEquals(1, divided.x());
        assertEquals(2, divided.y());
        assertEquals(3, divided.z());
        assertEquals(4, divided.w());
    }

    @Test
    public void testAdd() {
        Quaternion q1 = new Quaternion(1, 2, 3, 4);
        Quaternion q2 = new Quaternion(5, 6, 7, 8);
        Quaternion sum = q1.add(q2);
        assertEquals(6, sum.x());
        assertEquals(8, sum.y());
        assertEquals(10, sum.z());
        assertEquals(12, sum.w());
    }

    @Test
    public void testSub() {
        Quaternion q1 = new Quaternion(5, 6, 7, 8);
        Quaternion q2 = new Quaternion(1, 2, 3, 4);
        Quaternion diff = q1.sub(q2);
        assertEquals(4, diff.x());
        assertEquals(4, diff.y());
        assertEquals(4, diff.z());
        assertEquals(4, diff.w());
    }

    @Test
    public void testRotateVec() {
        Quaternion q = Quaternion.fromYRotation(Math.PI / 2);
        Vec v = new Vec(1, 0, 0);
        Vec rotated = q.rotate(v);
        assertEquals(0, rotated.x(), EPSILON);
        assertEquals(0, rotated.y(), EPSILON);
        assertEquals(-1, rotated.z(), EPSILON);

        // Test identity rotation
        rotated = Quaternion.IDENTITY.rotate(v);
        assertEquals(1, rotated.x(), EPSILON);
        assertEquals(0, rotated.y(), EPSILON);
        assertEquals(0, rotated.z(), EPSILON);
    }

    @Test
    public void testRotatePoint() {
        Quaternion q = Quaternion.fromYRotation(Math.PI / 2);
        Pos pos = new Pos(1, 0, 0);
        Vec rotated = q.rotate(pos);
        assertEquals(0, rotated.x(), EPSILON);
        assertEquals(0, rotated.y(), EPSILON);
        assertEquals(-1, rotated.z(), EPSILON);
    }

    @Test
    public void testConjugate() {
        Quaternion q = new Quaternion(1, 2, 3, 4);
        Quaternion conj = q.conjugate();
        assertEquals(-1, conj.x());
        assertEquals(-2, conj.y());
        assertEquals(-3, conj.z());
        assertEquals(4, conj.w());

        // Test that conjugate of unit quaternion is its inverse
        Quaternion unit = Quaternion.fromXRotation(Math.PI / 4);
        Quaternion product = unit.mul(unit.conjugate());
        assertTrue(product.similar(IDENTITY));
    }

    @Test
    public void testNeg() {
        Quaternion q = new Quaternion(1, 2, 3, 4);
        Quaternion neg = q.neg();
        assertEquals(-1, neg.x());
        assertEquals(-2, neg.y());
        assertEquals(-3, neg.z());
        assertEquals(-4, neg.w());

        // Test that q and -q represent the same rotation
        Quaternion rot = Quaternion.fromXRotation(Math.PI / 4);
        Vec v = new Vec(0, 1, 0);
        Vec r1 = rot.rotate(v);
        Vec r2 = rot.neg().rotate(v);
        assertEquals(r1.x(), r2.x(), EPSILON);
        assertEquals(r1.y(), r2.y(), EPSILON);
        assertEquals(r1.z(), r2.z(), EPSILON);
    }

    @Test
    public void testInverse() {
        // Test unit quaternion inverse
        Quaternion q = Quaternion.fromYRotation(Math.PI / 4);
        Quaternion inv = q.inverse();
        Quaternion product = q.mul(inv);
        assertTrue(product.similar(IDENTITY));

        // Test non-unit quaternion inverse
        Quaternion nonUnit = new Quaternion(1, 2, 3, 4);
        inv = nonUnit.inverse();
        product = nonUnit.mul(inv);
        assertTrue(product.similar(Quaternion.IDENTITY, 0.001));
    }

    @Test
    public void testLength() {
        Quaternion q = new Quaternion(1, 2, 2, 0);
        assertEquals(9, q.lengthSquared());
        assertEquals(3, q.length());

        // Test unit quaternion
        Quaternion unit = Quaternion.fromXRotation(Math.PI / 4);
        assertEquals(1, unit.length(), EPSILON);
    }

    @Test
    public void testNormalize() {
        Quaternion q = new Quaternion(1, 2, 2, 0);
        Quaternion normalized = q.normalize();
        assertEquals(1, normalized.length(), EPSILON);
        assertEquals(1.0 / 3.0, normalized.x(), EPSILON);
        assertEquals(2.0 / 3.0, normalized.y(), EPSILON);
        assertEquals(2.0 / 3.0, normalized.z(), EPSILON);
        assertEquals(0, normalized.w(), EPSILON);

        // Test normalizing zero quaternion returns identity
        Quaternion zero = new Quaternion(0, 0, 0, 0);
        normalized = zero.normalize();
        assertTrue(normalized.same(Quaternion.IDENTITY));
    }

    @Test
    public void testIsNormalized() {
        // Test unit quaternion
        Quaternion unit = Quaternion.fromXRotation(Math.PI / 4);
        assertTrue(unit.isNormalized());

        // Test identity
        assertTrue(Quaternion.IDENTITY.isNormalized());

        // Test non-unit quaternion
        Quaternion nonUnit = new Quaternion(1, 2, 3, 4);
        assertFalse(nonUnit.isNormalized());
    }

    @Test
    public void testDot() {
        Quaternion q1 = new Quaternion(1, 2, 3, 4);
        Quaternion q2 = new Quaternion(5, 6, 7, 8);
        double dot = q1.dot(q2);
        assertEquals(70, dot, EPSILON); // 1*5 + 2*6 + 3*7 + 4*8 = 5 + 12 + 21 + 32 = 70

        // Test orthogonal quaternions
        q1 = new Quaternion(1, 0, 0, 0);
        q2 = new Quaternion(0, 1, 0, 0);
        dot = q1.dot(q2);
        assertEquals(0, dot, EPSILON);
    }

    @Test
    public void testSlerp() {
        Quaternion start = Quaternion.fromYRotation(0);
        Quaternion end = Quaternion.fromYRotation(Math.PI / 2);

        // Test alpha = 0 (should return start)
        Quaternion result = start.slerp(end, 0.0);
        assertTrue(result.similar(start));

        // Test alpha = 1 (should return end)
        result = start.slerp(end, 1.0);
        assertTrue(result.similarRotation(end));

        // Test alpha = 0.5 (should be halfway)
        result = start.slerp(end, 0.5);
        Vec v = new Vec(1, 0, 0);
        Vec rotated = result.rotate(v);
        // At 45 degrees, should be at approximately (sqrt(2)/2, 0, -sqrt(2)/2)
        assertEquals(Math.sqrt(2) / 2, rotated.x(), 0.01);
        assertEquals(0, rotated.y(), EPSILON);
        assertEquals(-Math.sqrt(2) / 2, rotated.z(), 0.01);

        // Test with negative dot product (should take shorter path)
        start = Quaternion.fromYRotation(0);
        end = Quaternion.fromYRotation(Math.PI / 2).neg(); // Negated
        result = start.slerp(end, 0.5);
        assertNotNull(result);
        assertTrue(result.isNormalized());
    }

    @Test
    public void testNlerp() {
        Quaternion start = Quaternion.fromYRotation(0);
        Quaternion end = Quaternion.fromYRotation(Math.PI / 2);

        // Test alpha = 0 (should return start)
        Quaternion result = start.nlerp(end, 0.0);
        assertTrue(result.similar(start));

        // Test alpha = 1 (should return end)
        result = start.nlerp(end, 1.0);
        assertTrue(result.similarRotation(end));

        // Test alpha = 0.5
        result = start.nlerp(end, 0.5);
        assertTrue(result.isNormalized());

        // Test with negative dot product
        start = Quaternion.fromYRotation(0);
        end = Quaternion.fromYRotation(Math.PI / 2).neg();
        result = start.nlerp(end, 0.5);
        assertNotNull(result);
        assertTrue(result.isNormalized());
    }

    @Test
    public void testAxis() {
        // Test X rotation
        Quaternion q = Quaternion.fromXRotation(Math.PI / 4);
        Vec axis = q.axis();
        assertEquals(1, axis.x(), EPSILON);
        assertEquals(0, axis.y(), EPSILON);
        assertEquals(0, axis.z(), EPSILON);

        // Test Y rotation
        q = Quaternion.fromYRotation(Math.PI / 4);
        axis = q.axis();
        assertEquals(0, axis.x(), EPSILON);
        assertEquals(1, axis.y(), EPSILON);
        assertEquals(0, axis.z(), EPSILON);

        // Test identity (should return X axis by default)
        axis = Quaternion.IDENTITY.axis();
        assertEquals(1, axis.x(), EPSILON);
        assertEquals(0, axis.y(), EPSILON);
        assertEquals(0, axis.z(), EPSILON);
    }

    @Test
    public void testAngle() {
        // Test 90-degree rotation
        Quaternion q = Quaternion.fromXRotation(Math.PI / 2);
        assertEquals(Math.PI / 2, q.angle(), EPSILON);

        // Test 180-degree rotation
        q = Quaternion.fromXRotation(Math.PI);
        assertEquals(Math.PI, q.angle(), EPSILON);

        // Test identity (0 degrees)
        assertEquals(0, Quaternion.IDENTITY.angle(), EPSILON);
    }

    @Test
    public void testToEulerAngles() {
        // Test yaw only
        Quaternion q = Quaternion.fromEulerAngles(Math.PI / 4, 0, 0);
        Vec euler = q.toEulerAngles();
        assertEquals(Math.PI / 4, euler.x(), EPSILON); // yaw
        assertEquals(0, euler.y(), EPSILON); // pitch
        assertEquals(0, euler.z(), EPSILON); // roll

        // Test pitch only
        q = Quaternion.fromEulerAngles(0, Math.PI / 4, 0);
        euler = q.toEulerAngles();
        assertEquals(0, euler.x(), EPSILON); // yaw
        assertEquals(Math.PI / 4, euler.y(), EPSILON); // pitch
        assertEquals(0, euler.z(), EPSILON); // roll

        // Test roll only
        q = Quaternion.fromEulerAngles(0, 0, Math.PI / 4);
        euler = q.toEulerAngles();
        assertEquals(0, euler.x(), EPSILON); // yaw
        assertEquals(0, euler.y(), EPSILON); // pitch
        assertEquals(Math.PI / 4, euler.z(), EPSILON); // roll

        // Test identity
        euler = Quaternion.IDENTITY.toEulerAngles();
        assertEquals(0, euler.x(), EPSILON);
        assertEquals(0, euler.y(), EPSILON);
        assertEquals(0, euler.z(), EPSILON);
    }

    @Test
    public void testSame() {
        Quaternion q1 = new Quaternion(1, 2, 3, 4);
        Quaternion q2 = new Quaternion(1, 2, 3, 4);
        Quaternion q3 = new Quaternion(1, 2, 3, 4.0000001);

        // Test exact equality
        assertTrue(q1.same(q2));
        assertFalse(q1.same(q3)); // Should be false even with tiny difference
    }

    @Test
    public void testSimilar() {
        Quaternion q1 = new Quaternion(1, 2, 3, 4);
        Quaternion q2 = new Quaternion(1.0000001, 2.0000001, 3.0000001, 4.0000001);
        Quaternion q3 = new Quaternion(1.1, 2.1, 3.1, 4.1);

        // Test with default EPSILON
        assertTrue(q1.similar(q2));
        assertFalse(q1.similar(q3));

        // Test with custom epsilon
        assertTrue(q1.similar(q3, 0.2));
        assertFalse(q1.similar(q3, 0.05));

        // Test epsilon validation
        assertThrows(IllegalArgumentException.class, () -> q1.similar(q2, 0));
        assertThrows(IllegalArgumentException.class, () -> q1.similar(q2, -0.1));
    }

    @Test
    public void testSameRotation() {
        Quaternion q1 = Quaternion.fromXRotation(Math.PI / 4);
        Quaternion q2 = Quaternion.fromXRotation(Math.PI / 4);
        Quaternion q3 = q1.neg(); // Same rotation, negated

        // Test exact same rotation
        assertTrue(q1.sameRotation(q2));

        // Test that q and -q represent the same rotation
        assertTrue(q1.sameRotation(q3));

        // Test different rotation
        Quaternion q4 = Quaternion.fromYRotation(Math.PI / 4);
        assertFalse(q1.sameRotation(q4));
    }

    @Test
    public void testSimilarRotation() {
        Quaternion q1 = Quaternion.fromXRotation(Math.PI / 4);
        Quaternion q2 = new Quaternion(q1.x() + 0.0000001, q1.y(), q1.z(), q1.w());
        Quaternion q3 = q1.neg();

        // Test with default EPSILON
        assertTrue(q1.similarRotation(q2));
        assertTrue(q1.similarRotation(q3)); // q and -q

        // Test with custom epsilon
        Quaternion q4 = new Quaternion(q1.x() + 0.01, q1.y(), q1.z(), q1.w());
        assertTrue(q1.similarRotation(q4, 0.02));
        assertFalse(q1.similarRotation(q4, 0.005));

        // Test epsilon validation
        assertThrows(IllegalArgumentException.class, () -> q1.similarRotation(q2, 0));
        assertThrows(IllegalArgumentException.class, () -> q1.similarRotation(q2, -0.1));
    }

    @Test
    public void testIsIdentity() {
        // Test exact identity
        assertTrue(Quaternion.IDENTITY.similar(IDENTITY));

        // Test near identity
        Quaternion nearIdentity = new Quaternion(0.0000001, 0, 0, 1);
        assertTrue(nearIdentity.similar(IDENTITY));

        // Test non-identity
        Quaternion notIdentity = new Quaternion(0.1, 0, 0, 1);
        assertFalse(notIdentity.similar(IDENTITY));
    }

    @Test
    public void testIsSameIdentity() {
        // Test exact identity
        assertTrue(Quaternion.IDENTITY.same(IDENTITY));

        // Test near identity (should be false with exact comparison)
        Quaternion nearIdentity = new Quaternion(0.0000001, 0, 0, 1);
        assertFalse(nearIdentity.same(IDENTITY));

        // Test non-identity
        Quaternion notIdentity = new Quaternion(0.1, 0, 0, 1);
        assertFalse(notIdentity.same(IDENTITY));
    }

    @Test
    public void testRotationConsistency() {
        // Test that multiple rotations compose correctly
        Quaternion qX90 = Quaternion.fromXRotation(Math.PI / 2);
        Quaternion qY90 = Quaternion.fromYRotation(Math.PI / 2);

        Vec v = new Vec(1, 0, 0);

        // Apply rotations separately (qY first, then qX)
        Vec step1 = qY90.rotate(v);
        Vec step2 = qX90.rotate(step1);

        // Apply combined rotation (qX90.mul(qY90) means qY90 first, then qX90)
        Quaternion combined = qX90.mul(qY90);
        Vec result = combined.rotate(v);

        assertEquals(step2.x(), result.x(), EPSILON);
        assertEquals(step2.y(), result.y(), EPSILON);
        assertEquals(step2.z(), result.z(), EPSILON);
    }

    @Test
    public void testRotation360Degrees() {
        // Test that 360-degree rotation returns to original
        Vec v = new Vec(1, 2, 3);
        Quaternion q = Quaternion.fromXRotation(2 * Math.PI);
        Vec rotated = q.rotate(v);

        assertEquals(v.x(), rotated.x(), EPSILON);
        assertEquals(v.y(), rotated.y(), EPSILON);
        assertEquals(v.z(), rotated.z(), EPSILON);
    }

    @Test
    public void testFromViewWithPosIntegration() {
        // Test integration with Pos class
        Pos lookingEast = new Pos(0, 0, 0, 90f, 0f);
        Quaternion q = Quaternion.fromView(lookingEast);

        // Verify the quaternion rotates correctly
        Vec forward = new Vec(0, 0, 1);
        Vec rotated = q.rotate(forward);

        // After rotating by 90 degrees yaw, forward should point in different direction
        assertNotNull(rotated);
        assertTrue(q.isNormalized());
    }

    @Test
    public void testMultipleRotationAxes() {
        // Test combined rotation around multiple axes
        Quaternion q = Quaternion.fromEulerAngles(Math.PI / 4, Math.PI / 6, Math.PI / 3);
        assertTrue(q.isNormalized());

        Vec v = new Vec(1, 0, 0);
        Vec rotated = q.rotate(v);

        // Verify the result is a valid rotation (length preserved)
        assertEquals(v.length(), rotated.length(), EPSILON);
    }
}

