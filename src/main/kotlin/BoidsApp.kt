import io.nacular.doodle.application.Application
import io.nacular.doodle.controls.range.Slider
import io.nacular.doodle.core.Display
import io.nacular.doodle.core.plusAssign
import io.nacular.doodle.core.view
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.Stroke
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.event.PointerMotionListener
import io.nacular.doodle.geometry.ConvexPolygon
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.image.Image
import io.nacular.doodle.image.ImageLoader
import io.nacular.doodle.scheduler.Scheduler
import io.nacular.doodle.theme.ThemeManager
import io.nacular.doodle.theme.adhoc.DynamicTheme
import io.nacular.measured.units.Angle
import io.nacular.measured.units.Time
import io.nacular.measured.units.times
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

var separation = 1.5
var alignment = 1.0
var cohesion = 1.0

var fish: Image? = null

@DelicateCoroutinesApi
class BoidsApp(
    display: Display, theme: DynamicTheme,
    themes: ThemeManager, scheduler: Scheduler,
    imageLoader: ImageLoader
) : Application {
    val flock: MutableSet<Boid> = mutableSetOf()

    init {
        GlobalScope.launch {
            fish = imageLoader.load("fish.png")
        }

        themes.selected = theme

        display += view {
            size = display.size
            println(size.area)

            scheduler.every(10 * Time.milliseconds) {
                rerender()
            }

            val sepSlider = Slider(0.0..10.0).apply {
                size = Size(30)
                value = separation
                changed += { source, old, new -> separation = new }
            }

            val alignSlider = Slider(0.0..10.0).apply {
                size = Size(20)
                value = alignment
                changed += { source, old, new -> alignment = new }
            }

            val cohSlider = Slider(0.0..10.0).apply {
                size = Size(20)
                value = cohesion
                changed += { source, old, new -> cohesion = new }
            }

            for (i in 1..(bounds.size.area/15000).toInt()) {
                flock += Boid(
                    Point((0..size.width.toInt()).random(), (0..size.height.toInt()).random())
                )
            }

            var pointerPos: Point? = null
            pointerMotionChanged += PointerMotionListener.moved { event ->
                pointerPos = event.location
            }

            render = {
                rect(bounds.atOrigin, Stroke(Color.Red))
                line(Point(0, 0), Point(0, 100), Stroke(Color.Green))
                line(Point(0, 0), Point(100, 0), Stroke(Color.Green))

                for (boid in flock) {
                    boid.applyRules(flock)
                    //pointerPos?.let { boid.addForce(boid.target(it) * 0.8) }
                    boid.move(this)
                    boid.render(this)
                }
            }
        }
    }

    override fun shutdown() {}
}

data class Boid(
    var pos: Point = Point.Origin,
    var velocity: Point = Point(Random.nextDouble(-1.0, 1.0), Random.nextDouble(-1.0, 1.0)),
    var acceleration: Point = Point(0, 0),
    val viewRadius: Int = 200,
    val maxSpeed: Double = 3.0,
    val maxSteerForce: Double = 0.05,
    val color: Color = Color.Blue,
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
            avg = avg.normalize()
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
                diff = diff.normalize()
                diff /= (distance)
                steerForce += diff

                total++
            }
        }
        if (total > 0) {
            steerForce /= total
        }

        if (steerForce.mag() > 0.0) {
            steerForce = steerForce.normalize()
            steerForce *= this.maxSpeed
            steerForce -= this.velocity
            steerForce = steerForce.limit(this.maxSteerForce)
        }

        return steerForce
    }

    fun target(point: Point): Point {
        var targetVec = point - this.pos
        targetVec = targetVec.normalize()
        targetVec *= maxSpeed

        val steerForce = targetVec - this.velocity
        return steerForce.limit(maxSteerForce)
    }

    fun applyRules(flock: Set<Boid>) {
        addForce(separation(flock) * separation)
        addForce(align(flock) * alignment)
        addForce(cohesion(flock) * cohesion)
    }
}

infix fun Double.fmod(other: Double): Double {
    return ((this % other) + other) % other
}

fun Point.normalize(): Point {
    val l = 1.0 / sqrt(x * x + y * y)
    return Point(x * l, y * l)
}

fun Point.mag() = sqrt(x.pow(2) + y.pow(2))
fun Point.magSq() = x.pow(2) + y.pow(2)

fun Point.limit(max: Double): Point {
    if (magSq() > max.pow(2)) {
        val norm = normalize()
        return norm * max
    }

    return this
}

val Point.heading get() = atan2(y, x)