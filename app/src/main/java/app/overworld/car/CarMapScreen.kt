package app.overworld.car

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate

/**
 * v0 car screen: proves the Android Auto plumbing (service, navigation template, drawable surface).
 * The surface currently shows a placeholder "world". Next step: render the real MapLibre map here.
 */
class CarMapScreen(carContext: CarContext) : Screen(carContext) {

    private val callback = object : SurfaceCallback {
        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            draw(surfaceContainer)
        }

        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {}
    }

    init {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(callback)
    }

    private fun draw(c: SurfaceContainer) {
        val surface = c.surface ?: return
        val canvas: Canvas = surface.lockCanvas(null) ?: return
        try {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawColor(Color.parseColor("#0f1420"))
            p.color = Color.parseColor("#0b2a44")
            canvas.drawRect(0f, h * 0.78f, w, h, p)
            p.color = Color.parseColor("#2a3550")
            p.strokeWidth = 14f
            p.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(0f, h * 0.55f, w, h * 0.45f, p)
            canvas.drawLine(w * 0.4f, 0f, w * 0.5f, h * 0.8f, p)
            p.color = Color.parseColor("#ffc93c")
            p.strokeWidth = 10f
            canvas.drawLine(w * 0.1f, h * 0.7f, w * 0.5f, h * 0.5f, p)
            canvas.drawCircle(w * 0.1f, h * 0.7f, 16f, p)
            p.style = Paint.Style.FILL
            p.textSize = 44f
            p.color = Color.parseColor("#9aa7c7")
            canvas.drawText("Overworld - car map coming next", w * 0.06f, h * 0.12f, p)
        } finally {
            surface.unlockCanvasAndPost(canvas)
        }
    }

    override fun onGetTemplate(): Template {
        val strip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Overworld")
                    .setOnClickListener { }
                    .build()
            )
            .build()
        return NavigationTemplate.Builder()
            .setActionStrip(strip)
            .build()
    }
}
