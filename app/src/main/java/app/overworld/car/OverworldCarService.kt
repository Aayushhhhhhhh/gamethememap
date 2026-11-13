package app.overworld.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Android Auto entry point. Debug builds accept any host so the Desktop Head Unit works;
 * before a Play release replace this with an allow-list of Google's Android Auto hosts.
 */
class OverworldCarService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = object : Session() {
        override fun onCreateScreen(intent: Intent) = CarMapScreen(carContext)
    }
}
