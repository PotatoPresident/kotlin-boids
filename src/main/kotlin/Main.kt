import io.nacular.doodle.application.Modules.Companion.ImageModule
import io.nacular.doodle.application.Modules.Companion.PointerModule
import io.nacular.doodle.application.application
import io.nacular.doodle.theme.native.NativeTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import org.kodein.di.instance

@DelicateCoroutinesApi
fun main() {
    application(modules = listOf(PointerModule, NativeTheme.nativeSliderBehavior(), ImageModule)) {
        BoidsApp(instance(), instance(), instance(), instance(), instance())
    }
}

