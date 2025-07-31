package com.project.lumina.client.ui.opengl

import android.opengl.GLES20
import kotlin.properties.Delegates

object SimpleShader {
    var program by Delegates.notNull<Int>()
    var aPositionHandle = 0
    var uMVPMatrixHandle = 0
    var uColorHandle = 0 // Хэндл для uniform-переменной цвета

    fun init() {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, """
            uniform mat4 uMVPMatrix;
            attribute vec4 aPosition;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
            }
        """.trimIndent())

        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, """
            precision mediump float;
            uniform vec4 uColor; // Uniform для цвета
            void main() {
                gl_FragColor = uColor;
            }
        """.trimIndent())

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)

            aPositionHandle = GLES20.glGetAttribLocation(it, "aPosition")
            uMVPMatrixHandle = GLES20.glGetUniformLocation(it, "uMVPMatrix")
            uColorHandle = GLES20.glGetUniformLocation(it, "uColor") // Инициализируем хэндл цвета
        }
    }

    private fun compileShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Error compiling shader: $log")
        }
        return shader
    }

    fun createFloatBuffer(data: FloatArray): java.nio.FloatBuffer = java.nio.ByteBuffer
        .allocateDirect(data.size * 4)
        .order(java.nio.ByteOrder.nativeOrder())
        .asFloatBuffer().apply {
            put(data)
            position(0)
        }

    fun createShortBuffer(data: ShortArray): java.nio.ShortBuffer = java.nio.ByteBuffer
        .allocateDirect(data.size * 2)
        .order(java.nio.ByteOrder.nativeOrder())
        .asShortBuffer().apply {
            put(data)
            position(0)
        }
}