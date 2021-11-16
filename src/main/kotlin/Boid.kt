import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.lighter
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.geometry.ConvexPolygon
import io.nacular.doodle.geometry.Point
import io.nacular.measured.units.Angle
import io.nacular.measured.units.times
import kotlin.math.*
import kotlin.random.Random

private val colors = arrayListOf(Color.Green.lighter(0.3f), Color.Blue.lighter(0.3f), Color.Cyan.lighter(0.2f))

data class Boid(
    var pos: Point = Point.Origin,
    var velocity: Point = Point(Random.nextDouble(-1.0, 1.0), Random.nextDouble(-1.0, 1.0)),
    var acceleration: Point = Point(0, 0),
    val viewRadius: Int = 200,
    val maxSpeed: Double = 3.0,
    val maxSteerForce: Double = 0.05,
    val color: Color = colors.random(),
) {

    fun render(canvas: Canvas) {
        canvas.rotate(pos + Point(10, 10), velocity.heading * Angle.radians) {
            canvas.translate(pos) {
                canvas.scale(10.0, 10.0) {
                    val poly = ConvexPolygon(Point(0, 2), Point(0, 0), Point(3, 1))
                    poly(poly, color.paint)

                    /*
                    fish?.let {
                        image(it, pos, 1f, 0.0)
                    }
                     */
                }
            }
        }
    }

    fun addForce(force: Point) {
        acceleration += force
    }

    fun move(canvas: Canvas) {
        velocity += acceleration
        velocity = velocity.limit(maxSpeed)

        pos += velocity

        acceleration = Point(0, 0)

        // Wrap around edges
        val x = pos.x fmod canvas.size.width
        val y = pos.y fmod canvas.size.height

        pos = Point(x, y)
    }

    private fun align(flock: Set<Boid>): Point {
        var avg = Point(0, 0)
        var total = 0
        for (boid in flock) {
            val distance = this.pos.distanceFrom(boid.pos)
            if (boid !== this && distance < viewRadius) {
                avg += boid.velocity
                total++
            }
        }
        return if (total > 0) {
            avg /= total
            avg = avg.normalized()
            avg *= this.maxSpeed

            var steerForce = avg - this.velocity
            steerForce = steerForce.limit(maxSteerForce)
            steerForce
        } else Point(0, 0)
    }

    private fun cohesion(flock: Set<Boid>): Point {
        var avgPos = Point(0, 0)
        var total = 0
        for (boid in flock) {
            val distance = this.pos.distanceFrom(boid.pos)
            if (boid !== this && distance < viewRadius) {
                avgPos += boid.pos
                total++
            }
        }

        return if (total > 0) {
            avgPos /= total

            target(avgPos)
        } else Point(0, 0)
    }

    private fun separation(flock: Set<Boid>): Point {
        val targetSep = 100.0
        var steerForce = Point.Origin
        var total = 0
        for (boid in flock) {
            val distance = this.pos.distanceFrom(boid.pos)
            if (boid !== this && distance < targetSep) {
                var diff = this.pos - boid.pos
                diff = diff.normalized()
                diff /= (distance)
                steerForce += diff

                total++
            }
        }
        if (total > 0) {
            steerForce /= total
        }

        if (steerForce.mag > 0.0) {
            steerForce = steerForce.normalized()
            steerForce *= this.maxSpeed
            steerForce -= this.velocity
            steerForce = steerForce.limit(this.maxSteerForce)
        }

        return steerForce
    }

    fun target(point: Point): Point {
        var targetVec = point - this.pos
        targetVec = targetVec.normalized()
        targetVec *= maxSpeed

        val steerForce = targetVec - this.velocity
        return steerForce.limit(maxSteerForce)
    }

    fun randomize(): Point {
        val steerForce = this.velocity.rotate((-randomness..randomness).random()) - this.velocity
        return steerForce.limit(maxSteerForce)
    }

    fun applyRules(flock: Set<Boid>) {
        addForce(separation(flock) * separation)
        addForce(align(flock) * alignment)
        addForce(cohesion(flock) * cohesion)
    }
}

private fun ClosedFloatingPointRange<Double>.random(): Double = Random.nextDouble(this.start, this.endInclusive)

infix fun Double.fmod(other: Double): Double {
    return ((this % other) + other) % other
}

fun Point.normalized(): Point {
    val l = 1.0 / sqrt(x * x + y * y)
    return Point(x * l, y * l)
}

val Point.mag get() = sqrt(x.pow(2) + y.pow(2))
val Point.magSq get() = x.pow(2) + y.pow(2)

fun Point.limit(max: Double): Point {
    if (magSq > max.pow(2)) {
        val norm = normalized()
        return norm * max
    }

    return this
}

fun Point.rotate(angle: Double) = Point(x * cos(angle) - y * sin(angle), x * sin(angle) + y * cos(angle))

val Point.heading get() = atan2(y, x)
