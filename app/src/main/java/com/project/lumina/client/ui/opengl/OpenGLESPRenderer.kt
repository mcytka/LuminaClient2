package com.project.lumina.client.ui.opengl

import android.opengl.GLES20
import android.opengl.Matrix
import org.cloudburstmc.math.vector.Vector3f
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.properties.Delegates
import com.project.lumina.client.game.entity.Entity // Убедитесь, что Entity импортирован
import com.project.lumina.client.game.entity.Player // Убедитесь, что Player импортирован
import com.project.lumina.client.game.entity.LocalPlayer // Убедитесь, что LocalPlayer импортирован
import com.project.lumina.client.game.entity.Item // Убедитесь, что Item импортирован
import android.graphics.Color as AndroidColor // Импорт для AndroidColor

object OpenGLESPRenderer {

    // Изменено: теперь принимает List<Entity>
    fun renderESPBoxes(
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        entities: List<Entity> // Теперь это List<Entity>
    ) {
        for (entity in entities) { // Итерируемся напрямую по Entity
            // Определяем позицию для рендеринга
            val renderPosition = if (entity.isDisappeared) entity.lastKnownPosition else entity.vec3Position

            // Определяем цвет для рендеринга (ARGB)
            val color = if (entity.isDisappeared) {
                // Маджента с alpha 150 (AndroidColor.argb(150, 255, 0, 255))
                floatArrayOf(AndroidColor.red(AndroidColor.MAGENTA) / 255f, AndroidColor.green(AndroidColor.MAGENTA) / 255f, AndroidColor.blue(AndroidColor.MAGENTA) / 255f, 150f / 255f)
            } else {
                when {
                    entity is Player -> floatArrayOf(AndroidColor.red(AndroidColor.RED) / 255f, AndroidColor.green(AndroidColor.RED) / 255f, AndroidColor.blue(AndroidColor.RED) / 255f, 1.0f)
                    entity is Item -> floatArrayOf(AndroidColor.red(AndroidColor.YELLOW) / 255f, AndroidColor.green(AndroidColor.YELLOW) / 255f, AndroidColor.blue(AndroidColor.YELLOW) / 255f, 1.0f)
                    else -> floatArrayOf(AndroidColor.red(AndroidColor.CYAN) / 255f, AndroidColor.green(AndroidColor.CYAN) / 255f, AndroidColor.blue(AndroidColor.CYAN) / 255f, 1.0f)
                }
            }
            
            drawBoxAroundEntity(renderPosition, viewMatrix, projectionMatrix, color)
        }
    }

    private fun drawBoxAroundEntity(
        entityPosition: Vector3f,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        color: FloatArray
    ) {
        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, entityPosition.x, entityPosition.y, entityPosition.z)

        val mvpMatrix = FloatArray(16)
        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        val vertices = floatArrayOf(
            -0.3f, 0f, -0.3f, // 0: Bottom back left
            0.3f, 0f, -0.3f,  // 1: Bottom back right
            0.3f, 0f,  0.3f,  // 2: Bottom front right
            -0.3f, 0f,  0.3f, // 3: Bottom front left

            -0.3f, 1.8f, -0.3f, // 4: Top back left
            0.3f, 1.8f, -0.3f,  // 5: Top back right
            0.3f, 1.8f,  0.3f,  // 6: Top front right
            -0.3f, 1.8f,  0.3f  // 7: Top front left
        )

        val indices = shortArrayOf(
            0, 1, 1, 2, 2, 3, 3, 0,
            4, 5, 5, 6, 6, 7, 7, 4,
            0, 4, 1, 5, 2, 6, 3, 7
        )

        GLES20.glUseProgram(SimpleShader.program)

        GLES20.glUniformMatrix4fv(SimpleShader.uMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform4fv(SimpleShader.uColorHandle, 1, color, 0)

        GLES20.glEnableVertexAttribArray(SimpleShader.aPositionHandle)
        GLES20.glVertexAttribPointer(SimpleShader.aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, SimpleShader.createFloatBuffer(vertices))
        GLES20.glDrawElements(GLES20.GL_LINES, indices.size, GLES20.GL_UNSIGNED_SHORT, SimpleShader.createShortBuffer(indices))
        GLES20.glDisableVertexAttribArray(SimpleShader.aPositionHandle)
    }
}