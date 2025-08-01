/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * This is open source — not open credit.
 *
 * If you're here to build, welcome. If you're here to repaint and reupload
 * with your tag slapped on it… you're not fooling anyone.
 *
 * Changing colors and class names doesn't make you a developer.
 * Copy-pasting isn't contribution.
 *
 * You have legal permission to fork. But ask yourself — are you improving,
 * or are you just recycling someone else's work to feed your ego?
 *
 * Open source isn't about low-effort clones or chasing clout.
 * It's about making things better. Sharper. Cleaner. Smarter.
 *
 * So go ahead, fork it — but bring something new to the table,
 * or don't bother pretending.
 *
 * This message is philosophical. It does not override your legal rights under GPLv3.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * GPLv3 Summary:
 * - You have the freedom to run, study, share, and modify this software.
 * - If you distribute modified versions, you must also share the source code.
 * - You must keep this license and copyright intact.
 * - You cannot apply further restrictions — the freedom stays with everyone.
 * - This license is irrevocable, and applies to all future redistributions.
 *
 * Full text: https://www.gnu.org/licenses/gpl-3.0.html
 */

package com.project.lumina.client.game.module.impl.misc

import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.overlay.mods.ClientOverlay
import com.project.lumina.client.util.AssetManager

class WaterMarkElement(iconResId: Int = AssetManager.getAsset("ic_waterpolo")) : Element(
    name = "WaterMark",
    category = CheatCategory.Misc,
    displayNameResId = AssetManager.getString("module_watermark_display_name"),
    iconResId = iconResId
) {

    override fun onEnabled() {
        super.onEnabled()

        try {
            if (isSessionCreated) {
                ClientOverlay.setOverlayEnabled(enabled = true)
                ClientOverlay.showOverlay()
            } else {

                println("Session not created, cannot enable WaterMark overlay.")
            }
        } catch (e: Exception) {

            println("Error enabling WaterMark overlay: ${e.message}")
        }


    }

    override fun onDisabled() {
        super.onDisabled()
        ClientOverlay.setOverlayEnabled(enabled = false)
        ClientOverlay.dismissOverlay()
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            return
        }
        return
    }
}