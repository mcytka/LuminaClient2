package com.project.lumina.client.overlay.mods

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect // Импорт для android.graphics.Rect
import android.graphics.RectF // Импорт для android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.project.lumina.client.game.entity.Entity
import com.project.lumina.client.game.entity.Item
import com.project.lumina.client.game.entity.Player
import com.project.lumina.client.game.entity.LocalPlayer
import org.cloudburstmc.math.matrix.Matrix4f
import org.cloudburstmc.math.vector.Vector2f
import org.cloudburstmc.math.vector.Vector3f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.tan
import kotlin.math.atan // Импорт для atan
import android.util.Log
import android.os.Build

data class ESPData(
    val playerPosition: Vector3f,
    val playerRotation: Vector3f, // rotation.x = pitch, rotation.y = yaw
    val entities: List<ESPRenderEntity>,
    val fov: Float,
    val use3dBoxes: Boolean,
    val showPlayerInfo: Boolean
)

class CustomESPView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var espData: ESPData? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val textInfoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 30f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
    }

    private val outlineInfoPaint = Paint(Paint.ANTI_ALIAS_FLAT_TEXT_FLAG).apply { // Использовал _TEXT_FLAG, тут была опечатка, должно быть ANTI_ALIAS_FLAG
        color = AndroidColor.BLACK
        textSize = 30f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val bgInfoPaint = Paint().apply {
        color = AndroidColor.argb(160, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private val reusableBgRect = RectF()
    private val reusableTextBounds = Rect()

    init {
        setWillNotDraw(false)
        setBackgroundColor(AndroidColor.TRANSPARENT)
    }

    fun updateESPData(data: ESPData) {
        this.espData = data
        invalidate()
    }

    @SuppressLint("DefaultLocale")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val data = espData ?: return

        val playerPosition = data.playerPosition
        val playerYaw = data.playerRotation.y
        val playerPitch = data.playerRotation.x
        val entities = data.entities
        val fov = data.fov

        val screenWidth = width.toFloat()
        val screenHeight = height.toFloat()

        // *** ИСПРАВЛЕНИЕ ИСКАЖЕНИЙ: Корректировка FOV и ближней плоскости ***
        // Предполагаем, что data.fov - это горизонтальный FOV (Field of View).
        // Matrix4f.createPerspective обычно ожидает вертикальный FOV.
        val aspectRatio = screenWidth / screenHeight
        val fovHorizontalRadians = Math.toRadians(fov.toDouble()).toFloat()
        // Расчет вертикального FOV из горизонтального и соотношения сторон
        val fovVerticalRadians = 2f * atan(tan(fovHorizontalRadians / 2f) / aspectRatio)
        val fovVerticalDegrees = Math.toDegrees(fovVerticalRadians.toDouble()).toFloat()

        val viewProjMatrix = Matrix4f.createPerspective(
            fovVerticalDegrees, // Используем скорректированный вертикальный FOV
            aspectRatio,         // Аспектное соотношение
            0.5f,                // НОВОЕ: Увеличена ближняя плоскость для уменьшения искажений
            128f
        )
            .mul(
                Matrix4f.createTranslation(playerPosition)
                    .mul(rotateY(-playerYaw - 180f))
                    .mul(rotateX(-playerPitch))
                    .invert()
            )
        // *******************************************************************

        canvas.save()
        canvas.clipRect(0f, 0f, screenWidth, screenHeight)

        entities.forEach { renderEntity ->
            val entity = renderEntity.entity

            val originalUsernameWithCodes: String? = renderEntity.username
            val usernameForDisplay = originalUsernameWithCodes?.let { parseColorCodes(it) } ?: ""

            val renderPosition = if (entity.isDisappeared) entity.lastKnownPosition else entity.vec3Position

            val (entityWidth, entityHeight) = getEntitySize(entity)

            val entityCenterX = renderPosition.x
            val entityCenterZ = renderPosition.z

            val entityFeetY = renderPosition.y - 1.62f
            val entityHeadY = entityFeetY + entityHeight

            val halfWidth = entityWidth / 2f
            val halfDepth = entityWidth / 2f

            val boxVertices = arrayOf(
                Vector3f.from(entityCenterX - halfWidth, entityFeetY, entityCenterZ - halfDepth), // Bottom front left
                Vector3f.from(entityCenterX - halfWidth, entityHeadY, entityCenterZ - halfDepth), // Top front left
                Vector3f.from(entityCenterX + halfWidth, entityHeadY, entityCenterZ - halfDepth), // Top front right
                Vector3f.from(entityCenterX + halfWidth, entityFeetY, entityCenterZ - halfDepth), // Bottom front right
                Vector3f.from(entityCenterX - halfWidth, entityFeetY, entityCenterZ + halfDepth), // Bottom back left
                Vector3f.from(entityCenterX - halfWidth, entityHeadY, entityCenterZ + halfDepth), // Top back left
                Vector3f.from(entityCenterX + halfWidth, entityHeadY, entityCenterZ + halfDepth), // Top back right
                Vector3f.from(entityCenterX + halfWidth, entityFeetY, entityCenterZ + halfDepth)  // Bottom back right
            )

            var minX_screen = screenWidth
            var minY_screen = screenHeight
            var maxX_screen = 0f
            var maxY_screen = 0f
            var anyVertexBehindCamera = false

            val screenPositions = boxVertices.mapNotNull { vertex ->
                val screenPos = worldToScreen(vertex, viewProjMatrix, screenWidth.toInt(), screenHeight.toInt())
                if (screenPos == null) {
                    anyVertexBehindCamera = true
                }
                screenPos
            }

            if (anyVertexBehindCamera || screenPositions.isEmpty()) {
                return@forEach
            }

            screenPositions.forEach { screenPos ->
                minX_screen = minX_screen.coerceAtMost(screenPos.x)
                minY_screen = minY_screen.coerceAtMost(screenPos.y)
                maxX_screen = maxX_screen.coerceAtLeast(screenPos.x)
                maxY_screen = maxY_screen.coerceAtLeast(screenPos.y)

                // Агрессивная проверка на слишком большие/малые экранные координаты (для устранения "мусора")
                val extremeThreshold = 2.0f // Например, в 2 раза больше размера экрана
                if (screenPos.x < -screenWidth * extremeThreshold || screenPos.x > screenWidth * extremeThreshold ||
                    screenPos.y < -screenHeight * extremeThreshold || screenPos.y > screenHeight * extremeThreshold) {
                    Log.d("CustomESPView", "Discarding entity due to extreme screen coordinates: ${screenPos.x}, ${screenPos.y}")
                    return@forEach // Пропускаем отрисовку этой сущности
                }
            }

            val margin = 10f
            if (maxX_screen <= -margin ||
                minX_screen >= screenWidth + margin ||
                maxY_screen <= -margin ||
                minY_screen >= screenHeight + margin
            ) {
                return@forEach
            }

            val distance = sqrt(
                (renderPosition.x - playerPosition.x).pow(2) +
                        (renderPosition.y - playerPosition.y).pow(2) +
                        (renderPosition.z - playerPosition.z).pow(2)
            ).toFloat()

            val color = if (entity.isDisappeared) AndroidColor.argb(150, 255, 0, 255) else getEntityColor(entity)

            paint.color = color

            if (data.use3dBoxes) {
                draw3DBox(canvas, paint, screenPositions)
            } else {
                draw2DBox(canvas, paint, minX_screen, minY_screen, maxX_screen, maxY_screen, color)
            }

            if (data.showPlayerInfo && (usernameForDisplay.isNotEmpty() || distance > 0)) {
                drawEntityInfo(
                    canvas,
                    originalUsernameWithCodes,
                    usernameForDisplay,
                    distance,
                    minX_screen,
                    minY_screen,
                    maxX_screen,
                    maxY_screen
                )
            }
        }

        canvas.restore()
    }

    private fun parseColorCodes(text: String): String {
        return text.replace("§[0-9a-fk-orA-FK-OR]".toRegex(), "")
    }

    private fun getTextColor(text: String): Int {
        Log.d("CustomESPView", "getTextColor input: \"$text\"")

        val colorCode = text.substringBefore(":").substringAfterLast("§", "")

        Log.d("CustomESPView", "Extracted colorCode: \"$colorCode\"")

        val finalColor = when (colorCode.lowercase()) {
            "0" -> AndroidColor.BLACK
            "1" -> AndroidColor.BLUE
            "2" -> AndroidColor.GREEN
            "3" -> AndroidColor.CYAN
            "4" -> AndroidColor.RED
            "5" -> AndroidColor.MAGENTA
            "6" -> AndroidColor.YELLOW
            "7" -> AndroidColor.LTGRAY
            "8" -> AndroidColor.DKGRAY
            "9" -> AndroidColor.BLUE
            "a" -> AndroidColor.GREEN
            "b" -> AndroidColor.CYAN
            "c" -> AndroidColor.RED
            "d" -> AndroidColor.MAGENTA
            "e" -> AndroidColor.YELLOW
            "f" -> AndroidColor.WHITE
            else -> AndroidColor.WHITE
        }

        Log.d("CustomESPView", "Final color for text: $finalColor (AndroidColor value)")

        return finalColor
    }

    private fun rotateX(angle: Float): Matrix4f {
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val c = cos(rad)
        val s = sin(rad)
        return Matrix4f.from(
            1f, 0f, 0f, 0f,
            0f, c, -s, 0f,
            0f, s, c, 0f,
            0f, 0f, 0f, 1f
        )
    }

    private fun rotateY(angle: Float): Matrix4f {
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val c = cos(rad)
        val s = sin(rad)
        return Matrix4f.from(
            c, 0f, s, 0f,
            0f, 1f, 0f, 0f,
            -s, 0f, c, 0f,
            0f, 0f, 0f, 1f
        )
    }

    private fun worldToScreen(pos: Vector3f, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int): Vector2f? {
        val clipX = viewProjMatrix.get(0, 0) * pos.x + viewProjMatrix.get(0, 1) * pos.y + viewProjMatrix.get(0, 2) * pos.z + viewProjMatrix.get(0, 3)
        val clipY = viewProjMatrix.get(1, 0) * pos.x + viewProjMatrix.get(1, 1) * pos.y + viewProjMatrix.get(1, 2) * pos.z + viewProjMatrix.get(1, 3)
        val clipZ = viewProjMatrix.get(2, 0) * pos.x + viewProjMatrix.get(2, 1) * pos.y + viewProjMatrix.get(2, 2) * pos.z + viewProjMatrix.get(2, 3)
        val clipW = viewProjMatrix.get(3, 0) * pos.x + viewProjMatrix.get(3, 1) * pos.y + viewProjMatrix.get(3, 2) * pos.z + viewProjMatrix.get(3, 3)

        if (clipW < 0.1f) return null

        val inverseW = 1f / clipW
        val ndcX = clipX * inverseW
        val ndcY = clipY * inverseW
        val screenX = screenWidth / 2f + (0.5f * ndcX * screenWidth)
        val screenY = screenHeight / 2f - (0.5f * ndcY * screenHeight)
        return Vector2f.from(screenX, screenY)
    }

    private fun getEntitySize(entity: Entity): Pair<Float, Float> {
        return when (entity) {
            is Player, is LocalPlayer -> Pair(0.6f, 1.8f)
            is Item -> Pair(0.25f, 0.25f)
            else -> Pair(0.5f, 0.5f)
        }
    }

    private fun getEntityColor(entity: Entity): Int {
        return when {
            entity is Player -> AndroidColor.RED
            entity is Item -> AndroidColor.YELLOW
            else -> AndroidColor.CYAN
        }
    }

    private fun draw2DBox(canvas: Canvas, paint: Paint, minX: Float, minY: Float, maxX: Float, maxY: Float, color: Int) {
        val originalColor = paint.color
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.alpha = (0.9f * 255).toInt()
        canvas.drawRect(minX, minY, maxX, maxY, paint)
        paint.color = originalColor
    }

    private fun draw3DBox(canvas: Canvas, paint: Paint, screenPositions: List<Vector2f>) {
        if (screenPositions.size != 8) return

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.alpha = (0.9f * 255).toInt()

        drawLine(canvas, paint, screenPositions[0], screenPositions[3])
        drawLine(canvas, paint, screenPositions[3], screenPositions[7])
        drawLine(canvas, paint, screenPositions[7], screenPositions[4])
        drawLine(canvas, paint, screenPositions[4], screenPositions[0])

        drawLine(canvas, paint, screenPositions[1], screenPositions[2])
        drawLine(canvas, paint, screenPositions[2], screenPositions[6])
        drawLine(canvas, paint, screenPositions[6], screenPositions[5])
        drawLine(canvas, paint, screenPositions[5], screenPositions[1])

        drawLine(canvas, paint, screenPositions[0], screenPositions[1])
        drawLine(canvas, paint, screenPositions[3], screenPositions[2])
        drawLine(canvas, paint, screenPositions[7], screenPositions[6])
        drawLine(canvas, paint, screenPositions[4], screenPositions[5])
    }

    private fun drawLine(canvas: Canvas, paint: Paint, p1: Vector2f, p2: Vector2f) {
        canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
    }

    @SuppressLint("DefaultLocale")
    private fun drawEntityInfo(
        canvas: Canvas,
        originalUsername: String?,
        usernameForDisplay: String,
        distance: Float,
        minX: Float,
        minY: Float,
        maxX: Float,
        maxY: Float
    ) {
        val info = buildString {
            if (usernameForDisplay.isNotEmpty()) {
                append(usernameForDisplay)
            }
            if (distance > 0) {
                if (isNotEmpty()) append(" | ")
                append("%.1fm".format(distance))
            }
        }

        if (info.isEmpty()) return

        textInfoPaint.color = getTextColor(originalUsername ?: "")

        val textX = (minX + maxX) / 2
        val textY = minY - 10

        textInfoPaint.getTextBounds(info, 0, info.length, reusableTextBounds)

        val padding = 8f
        val textHeight = reusableTextBounds.height().toFloat()
        val textHalfWidth = reusableTextBounds.width() / 2f

        reusableBgRect.set(
            textX - textHalfWidth - padding,
            textY - textHeight - padding,
            textX + textHalfWidth + padding,
            textY + padding
        )

        canvas.drawRoundRect(reusableBgRect, 4f, 4f, bgInfoPaint)

        canvas.drawText(info, textX, textY, outlineInfoPaint)
        canvas.drawText(info, textX, textY, textInfoPaint)
    }
}