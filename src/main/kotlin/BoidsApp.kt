import io.nacular.doodle.application.Application
import io.nacular.doodle.controls.range.Slider
import io.nacular.doodle.core.Display
import io.nacular.doodle.core.plusAssign
import io.nacular.doodle.core.view
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.event.PointerListener
import io.nacular.doodle.event.PointerMotionListener
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.image.Image
import io.nacular.doodle.image.ImageLoader
import io.nacular.doodle.scheduler.Scheduler
import io.nacular.doodle.system.SystemPointerEvent
import io.nacular.doodle.theme.ThemeManager
import io.nacular.doodle.theme.adhoc.DynamicTheme
import io.nacular.measured.units.Time
import io.nacular.measured.units.times
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

var separation = 1.5
var alignment = 1.0
var cohesion = 1.0

var randomness = 1.0

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
            display.sizeChanged += { _, _, new ->
                size = new
            }

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

            for (i in 1..(bounds.size.area / 18000).toInt()) {
                flock += Boid(
                    Point((0..size.width.toInt()).random(), (0..size.height.toInt()).random())
                )
            }

            var pointerPos: Point? = null
            pointerChanged += PointerListener.pressed { event ->
                if (event.buttons.contains(SystemPointerEvent.Button.Button1)) pointerPos = event.location
            }
            pointerMotionChanged += PointerMotionListener.dragged { event ->
                if (event.buttons.contains(SystemPointerEvent.Button.Button1)) pointerPos = event.location
            }
            pointerChanged += PointerListener.released {
                pointerPos = null
            }

            render = {
                this.rect(bounds.atOrigin, Color(0x151718u).paint)

                for (boid in flock) {
                    boid.applyRules(flock)
                    pointerPos?.let { boid.addForce(boid.target(it) * 0.8) }
                    boid.move(this)
                    boid.render(this)
                }
            }
        }
    }

    override fun shutdown() {}
}