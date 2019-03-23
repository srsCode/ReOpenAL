/*
 * Project      : ReOpenAL
 * File         : ReOpenAL.java
 * Last Modified: 20190323-10:48:22-0400
 *
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 *
 */

package srscode.reopenal;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import paulscode.sound.Library;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.SoundSystemException;
import paulscode.sound.libraries.LibraryLWJGLOpenAL;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenOptionsSounds;

import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = "reopenal", name = "ReOpenAL", version = "@VERSION@", acceptedMinecraftVersions = "[1.9, 1.13)", clientSideOnly = true)
public final class ReOpenAL
{
    private static final Logger LOGGER = LogManager.getLogger("ReOpenAL");

    @Mod.EventHandler
    public final void init(final FMLPreInitializationEvent event)
    {
        boolean failed = false;
        try {
            soundLibraryField = SoundSystem.class.getDeclaredField("soundLibrary");
            soundLibraryField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            failed = true;
            LOGGER.error("Error getting private field 'soundLibrary' in SoundSystem.");
            e.printStackTrace();
        } catch (SecurityException e) {
            failed = true;
            LOGGER.error("Error changing access of private field 'soundLibrary' in SoundSystem.");
            e.printStackTrace();
        } finally {
            if (failed) {
                LOGGER.error("Initialization failed.");
            } else {
                LOGGER.debug("Registering ReOpenAL event handler.");
                MinecraftForge.EVENT_BUS.register(this);
            }
        }
    }

    @SubscribeEvent
    public final void onGuiOpen(final GuiOpenEvent event)
    {
        if (event.getGui() instanceof GuiScreenOptionsSounds) {
            LOGGER.debug("Checking SoundSystem Library...");
            maybeRestartSoundSystem();
        }
    }

    private Field soundLibraryField = null;

    private void maybeRestartSoundSystem()
    {
        if (soundLibraryField != null) {
            final SoundSystem sndSystem = Minecraft.getMinecraft().getSoundHandler().sndManager.sndSystem;
            if (sndSystem != null) {
                try {
                    final Library lib = (Library) soundLibraryField.get(sndSystem);
                    if (lib instanceof LibraryLWJGLOpenAL) {
                        LOGGER.debug("SoundSystem is using LibraryLWJGLOpenAL.");
                    } else {
                        LOGGER.warn("SoundSystem is not currently using LibraryLWJGLOpenAL. (Probably running in silent mode); Resetting...");
                        restartSoundSystem();
                    }
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    LOGGER.error("Exception during reflective access of SoundSystem. Could not check library.");
                    e.printStackTrace();
                }
            } else {
                LOGGER.error("Sound system doesn't exist. Trying to restart it...");
                restartSoundSystem();
            }
        }
    }

    private void restartSoundSystem()
    {
        try {
            Minecraft.getMinecraft().getSoundHandler().sndManager.unloadSoundSystem();
            // ensure LibraryLWJGLOpenAL is added before restarting
            SoundSystemConfig.addLibrary(LibraryLWJGLOpenAL.class);
            Minecraft.getMinecraft().getSoundHandler().sndManager.reloadSoundSystem();
        } catch (SoundSystemException e) {
            LOGGER.error("Failed to add sound library: LibraryLWJGLOpenAL");
            e.printStackTrace();
        }
    }
}
