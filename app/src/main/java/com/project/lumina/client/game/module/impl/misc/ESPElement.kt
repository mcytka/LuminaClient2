package com.project.lumina.client.game.module.impl.misc

import android.util.Log
import com.project.lumina.client.R
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.game.entity.* // Импорт всех классов сущностей
import com.project.lumina.client.overlay.manager.OverlayManager
import com.project.lumina.client.ui.opengl.ESPOverlayGLSurface // Импортируем нашу OpenGL поверхность
// import com.project.lumina.client.overlay.mods.ESPRenderEntity // Эту строку теперь можно удалить
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import com.project.lumina.client.application.AppContext

class ESPElement : Element(
    name = "ESP",
    category = CheatCategory.Misc,
    displayNameResId = R.string.module_esp_display_name
) {
    private var playersOnly by boolValue("Players", true)
    private var rangeValue by floatValue("Range", 25f, 2f..500f)
    private var multiTarget = true
    private var maxTargets = 100
    private var showDisappearedPlayers by boolValue(
        "Show Disappeared Players(Suitable for 'Block Hunt' on TheHive server)",
        true
    )

    private var glSurface: ESPOverlayGLSurface? = null

    override fun onEnabled() {
        super.onEnabled()
        try {
            if (isSessionCreated && AppContext.instance != null) {
                glSurface = ESPOverlayGLSurface(AppContext.instance)
                OverlayManager.showCustomOverlay(glSurface!!)
                Log.d("ESPModule", "ESP Overlay enabled (OpenGL)")
            } else {
                Log.e("ESPModule", "Session not created or AppContext not available, cannot enable ESP overlay.")
            }
        } catch (e: Exception) {
            Log.e("ESPModule", "Enable error: ${e.message}")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        glSurface?.let {
            OverlayManager.dismissCustomOverlay(it)
            Log.d("ESPModule", "ESP Overlay disabled (OpenGL)")
        }
        glSurface = null
        if (isSessionCreated) {
            session.clearLastKnownPositions()
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || !isSessionCreated || interceptablePacket.packet !is PlayerAuthInputPacket) return

        val currentLocalPlayer = session.localPlayer
        if (currentLocalPlayer != null) {
            val position = currentLocalPlayer.vec3Position
            val rotationYaw = currentLocalPlayer.rotationYaw
            val rotationPitch = currentLocalPlayer.rotationPitch

            glSurface?.let {
                it.updatePlayerPositionAndRotation(position, rotationPitch, rotationYaw)
                // ИСПРАВЛЕНИЕ: Передаем List<Entity> напрямую, без преобразования в ESPRenderEntity
                it.updateEntities(getEntitiesToRender())
            }
        }
    }

    private fun getEntitiesToRender(): List<Entity> {
        if (!isSessionCreated) return emptyList()
        val allEntities = session.getAllEntitiesForEsp()

        return allEntities
            .filter { entity ->
                if (entity is LocalPlayer && entity.uniqueEntityId == session.localPlayer.uniqueEntityId) {
                    false
                } else {
                    if (!showDisappearedPlayers && entity.isDisappeared) {
                        false
                    } else {
                        val distance = if (entity.isDisappeared) {
                            entity.lastKnownPosition.distance(session.localPlayer.vec3Position)
                        } else {
                            entity.distance(session.localPlayer)
                        }
                        distance < rangeValue && entity.isTarget()
                    }
                }
            }
            .sortedBy { entity ->
                if (entity.isDisappeared) {
                    entity.lastKnownPosition.distance(session.localPlayer.vec3Position)
                } else {
                    entity.distance(session.localPlayer)
                }
            }
            .take(maxTargets)
    }

    private fun Entity.isTarget(): Boolean {
        return when (this) {
            is LocalPlayer -> false
            is Player -> playersOnly && !isBot()
            else -> false
        }
    }

    private fun Player.isBot(): Boolean {
        if (this is LocalPlayer) return false
        val playerList = session.level.playerMap[this.uuid] ?: return true
        return playerList.name.isBlank()
    }
}