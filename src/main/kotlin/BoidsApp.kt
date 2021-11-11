import io.nacular.doodle.application.Application
import io.nacular.doodle.controls.Photo
import io.nacular.doodle.controls.range.Slider
import io.nacular.doodle.core.Display
import io.nacular.doodle.core.plusAssign
import io.nacular.doodle.core.view
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.Stroke
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.geometry.ConvexPolygon
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.image.Image
import io.nacular.doodle.image.ImageLoader
import io.nacular.doodle.scheduler.Scheduler
import io.nacular.doodle.theme.ThemeManager
import io.nacular.doodle.theme.adhoc.DynamicTheme
import io.nacular.measured.units.Angle
import io.nacular.measured.units.Measure
import io.nacular.measured.units.Time
import io.nacular.measured.units.times
import kotlinx.coroutines.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

var separation = 1.0
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
            fish = imageLoader.load("")
        }

        themes.selected = theme

        display += view {
            size = display.size

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

            for (i in 1..40) {
                flock += Boid(
                    Point((0..size.width.toInt()).random(), (0..size.height.toInt()).random()),
                    (0..360).random() * Angle.degrees
                )
            }

            render = {
                rect(bounds.atOrigin, Stroke(Color.Red))
                line(Point(0, 0), Point(0, 100), Stroke(Color.Green))
                line(Point(0, 0), Point(100, 0), Stroke(Color.Green))

                for (boid in flock) {
                    boid.applyRules(flock, this)
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
    var rotation: Measure<Angle> = 0 * Angle.degrees,
    val speed: Double = 1.0,
    val viewRadius: Int = 200,
    val maxTurnForce: Double = 1.0,
    val color: Color = Color.Blue,
) {
    fun render(canvas: Canvas) {
        canvas.rotate(pos + Point(10, 10), rotation) {
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

    val velocity: Point
        get() = Point(
            speed * cos((rotation `as` Angle.radians).amount),
            speed * sin((rotation `as` Angle.radians).amount)
        )

    fun move(canvas: Canvas) {
        pos += velocity

        val x = pos.x fmod canvas.size.width
        val y = pos.y fmod canvas.size.height

        pos = Point(x, y)
    }

    private fun align(flock: Set<Boid>): Measure<Angle> {
        var turnForce = 0.0
        var total = 0
        for (boid in flock) {
            val distance = this.pos.distanceFrom(boid.pos)
            if (boid !== this && distance < viewRadius) {
                turnForce += boid.rotation.amount
                total++
            }
        }
        if (total > 0) {
            turnForce /= total
            turnForce -= this.rotation.amount
            turnForce = turnForce.coerceIn(-maxTurnForce, maxTurnForce)
        }

        return turnForce * Angle.degrees
    }

    private fun cohesion(flock: Set<Boid>, canvas: Canvas): Measure<Angle> {
        var turnForce = 0.0
        var avgPos = Point.Origin
        var total = 0
        for (boid in flock) {
            val distance = this.pos.distanceFrom(boid.pos)
            if (boid !== this && distance < viewRadius) {
                avgPos += boid.pos
                total++
            }
        }
        if (total > 0) {
            avgPos /= total
            val targetVec = avgPos - this.pos
            val targetRot = (atan2(targetVec.y, targetVec.x)) * Angle.radians

            turnForce = (targetRot `as` Angle.degrees).amount
            turnForce -= this.rotation.amount
            turnForce = turnForce.coerceIn(-maxTurnForce * cohesion, maxTurnForce * cohesion)

            canvas.line(this.pos, avgPos, Stroke(Color.Red))
        }

        return turnForce * Angle.degrees
    }

    private fun separation(flock: Set<Boid>): Measure<Angle> {
        var turnForce = 0.0
        var total = 0
        for (boid in flock) {
            val distance = this.pos.distanceFrom(boid.pos)
            if (boid !== this && distance < viewRadius) {
                var diff = this.pos - boid.pos
                diff /= distance
                turnForce += (((atan2(diff.y, diff.x)) * Angle.radians) `as` Angle.degrees).amount

                total++
            }
        }
        if (total > 0) {
            turnForce /= total
            turnForce -= this.rotation.amount
            turnForce = turnForce.coerceIn(-maxTurnForce, maxTurnForce)
        }

        return turnForce * Angle.degrees
    }

    fun applyRules(flock: Set<Boid>, canvas: Canvas) {
        this.rotation += align(flock) * alignment
        //this.rotation += cohesion(flock, canvas)
        //this.rotation += separation(flock) * separation
    }
}

infix fun Double.fmod(other: Double): Double {
    return ((this % other) + other) % other
}