package com.project.lumina.client.game.module.impl.misc

import android.util.Log
import com.project.lumina.client.R
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.game.entity.*
import com.project.lumina.client.overlay.manager.OverlayManager // Используем OverlayManager
import com.project.lumina.client.ui.opengl.ESPOverlayGLSurface // Импортируем вашу OpenGL поверхность
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import java.util.UUID
import com.project.lumina.client.application.AppContext // Для AppContext.instance

class ESPElement : Element(
    name = "ESP", // Название модуля
    category = CheatCategory.Misc,
    displayNameResId = R.string.module_esp_display_name
) {
    private var playersOnly by boolValue("Players", true)
    private var rangeValue by floatValue("Range", 25f, 2f..500f)
    private var multiTarget = true // В текущей реализации не используется, но оставлено
    private var maxTargets = 100
    // Удаляем use3dBoxes и showPlayerInfo, так как OpenGL всегда рисует 3D-боксы, а инфо игрока - это 2D-оверлей.
    private var showDisappearedPlayers by boolValue(
        "Show Disappeared Players(Suitable for 'Block Hunt' on TheHive server)",
        true
    )

    // Инициализируем glSurface здесь, чтобы он был доступен
    private var glSurface: ESPOverlayGLSurface? = null

    override fun onEnabled() {
        super.onEnabled()
        try {
            if (isSessionCreated && AppContext.instance != null) {
                // Создаем GLSurfaceView при включении модуля
                glSurface = ESPOverlayGLSurface(AppContext.instance)
                // Показываем наш кастомный оверлей OpenGL
                OverlayManager.showCustomOverlay(glSurface!!)
                Log.d("ESPModule", "ESP Overlay enabled")
            } else {
                Log.e("ESPModule", "Session not created or AppContext not available, cannot enable ESP overlay.")
            }
        } catch (e: Exception) {
            Log.e("ESPModule", "Enable error: ${e.message}")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        // Скрываем и очищаем GLSurfaceView при отключении модуля
        glSurface?.let {
            OverlayManager.dismissCustomOverlay(it)
            Log.d("ESPModule", "ESP Overlay disabled")
        }
        glSurface = null // Очищаем ссылку
        if (isSessionCreated) {
            session.clearLastKnownPositions()
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || !isSessionCreated) return

        // Фильтруем только пакеты ввода игрока, как это было в вашей старой версии для PlayerAuthInputPacket
        if (interceptablePacket.packet !is PlayerAuthInputPacket) return

        val currentLocalPlayer = session.localPlayer
        if (currentLocalPlayer != null) {
            val position = currentLocalPlayer.vec3Position
            val rotationYaw = currentLocalPlayer.rotationYaw
            val rotationPitch = currentLocalPlayer.rotationPitch

            glSurface?.let {
                // Обновляем позицию и вращение игрока в GLSurfaceView
                it.updatePlayerPositionAndRotation(position, rotationPitch, rotationYaw)
                // Обновляем сущности, используя вашу логику getEntitiesToRender()
                it.updateEntities(getEntitiesToRender().map { entity ->
                    // Мы не можем напрямую передать List<Entity>, OpenGL ESP ожидает List<ESPRenderEntity>
                    // или List<Entity> если вы соответствующим образом изменили ESPOverlayGLSurface.kt
                    // Предполагается, что getEntitiesToRender() возвращает List<Entity>
                    // и ESPOverlayGLSurface.updateEntities теперь принимает List<ESPRenderEntity>
                    // Если у вас ESPRenderEntity - это обертка Entity, как в CustomESPView,
                    // то вам нужно создать такую обертку здесь.
                    // Если ESPOverlayGLSurface.updateEntities ждет List<Entity>, то map не нужен.

                    // ИСПОЛЬЗУЕМ ESPRenderEntity, как мы договорились, чтобы сохранить Ghost-логику:
                    ESPRenderEntity(entity, renderEntity.username) // Если у ESPRenderEntity есть username
                    // Если у ESPRenderEntity только Entity:
                    // ESPRenderEntity(entity)
                })
            }
        }
    }

    // Этот метод скопирован полностью из вашей текущей 2D-версии ESPElement
    private fun getEntitiesToRender(): List<Entity> {
        if (!isSessionCreated) return emptyList()
        val allEntities = session.getAllEntitiesForEsp()

        return allEntities
            .filter { entity ->
                if (entity is LocalPlayer && entity.uniqueEntityId == session.localPlayer.uniqueEntityId) {
                    false // Игнорируем самого локального игрока
                } else {
                    if (!showDisappearedPlayers && entity.isDisappeared) {
                        false // Если не показываем исчезнувших и сущность исчезла, фильтруем
                    } else {
                        val distance = if (entity.isDisappeared) {
                            entity.lastKnownPosition.distance(session.localPlayer.vec3Position)
                        } else {
                            entity.distance(session.localPlayer)
                        }
                        distance < rangeValue && entity.isTarget() // Проверяем дистанцию и цель
                    }
                }
            }
            .sortedBy { entity -> // Сортируем по дистанции
                if (entity.isDisappeared) {
                    entity.lastKnownPosition.distance(session.localPlayer.vec3Position)
                } else {
                    entity.distance(session.localPlayer)
                }
            }
            .take(maxTargets) // Ограничиваем количество целей
    }

    // Эти вспомогательные методы также скопированы из вашей текущей 2D-версии
    private fun Entity.isTarget(): Boolean {
        return when (this) {
            is LocalPlayer -> false
            is Player -> playersOnly && !isBot()
            else -> false // Только игроки, если playersOnly
        }
    }

    private fun Player.isBot(): Boolean {
        if (this is LocalPlayer) return false
        val playerList = session.level.playerMap[this.uuid] ?: return true
        return playerList.name.isBlank()
    }
}