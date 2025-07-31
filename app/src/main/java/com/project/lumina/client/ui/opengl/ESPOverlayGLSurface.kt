package com.project.lumina.client.ui.opengl

import android.content.Context
import android.opengl.GLSurfaceView
import android.opengl.GLES20
import android.opengl.Matrix
import org.cloudburstmc.math.vector.Vector3f
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin
import com.project.lumina.client.game.entity.Entity // Убедитесь, что Entity импортирован

class ESPOverlayGLSurface(context: Context) : GLSurfaceView(context) {

    private val renderer: ESPRenderer

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
        renderer = ESPRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        setZOrderOnTop(true)
    }

    // Изменено: теперь принимает List<Entity>
    fun updateEntities(entities: List<Entity>) {
        renderer.updateEntities(entities)
    }

    fun updatePlayerPositionAndRotation(playerPos: Vector3f, playerPitch: Float, playerYaw: Float) {
        renderer.updatePlayerPositionAndRotation(playerPos, playerPitch, playerYaw)
    }

    private class ESPRenderer : Renderer {
        private var playerPos = Vector3f.from(0f, 0f, 0f)
        private var playerPitch: Float = 0f
        private var playerYaw: Float = 0f
        // Изменено: теперь хранит List<Entity>
        private var entityList = emptyList<Entity>()

        private val viewMatrix = FloatArray(16)
        private val projectionMatrix = FloatArray(16)

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            SimpleShader.init()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            val ratio = width.toFloat() / height
            Matrix.perspectiveM(projectionMatrix, 0, 60f, ratio, 0.1f, 100f)
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

            val cameraX = playerPos.x
            val cameraY = playerPos.y + 1.62f
            val cameraZ = playerPos.z

            val yawRad = Math.toRadians(-playerYaw.toDouble() - 90.0).toFloat()
            val pitchRad = Math.toRadians(-playerPitch.toDouble()).toFloat()

            val lookAtX = cameraX + cos(pitchRad) * cos(yawRad)
            val lookAtY = cameraY + sin(pitchRad)
            val lookAtZ = cameraZ + cos(pitchRad) * sin(yawRad)

            Matrix.setLookAtM(
                viewMatrix, 0,
                cameraX, cameraY, cameraZ,
                lookAtX, lookAtY, lookAtZ,
                0f, 1f, 0f
            )

            // Изменено: теперь передаем список Entity
            OpenGLESPRenderer.renderESPBoxes(viewMatrix, projectionMatrix, entityList)
        }

        // Изменено: теперь принимает List<Entity>
        fun updateEntities(entities: List<Entity>) {
            this.entityList = entities
        }

        fun updatePlayerPositionAndRotation(pos: Vector3f, pitch: Float, yaw: Float) {
            this.playerPos = pos
            this.playerPitch = pitch
            this.playerYaw = yaw
        }
    }
}