package xyz.gnarbot.gnar.commands.music.dj

import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Greedy
import xyz.gnarbot.gnar.commands.music.embedTitle
import xyz.gnarbot.gnar.commands.music.embedUri
import xyz.gnarbot.gnar.entities.framework.CheckVoiceState
import xyz.gnarbot.gnar.entities.framework.DJ
import xyz.gnarbot.gnar.entities.framework.MusicCog
import xyz.gnarbot.gnar.utils.PlaylistUtils
import xyz.gnarbot.gnar.utils.extensions.manager
import java.util.regex.Pattern

class Remove : MusicCog(true, false, false) {
    private val pattern = Pattern.compile("(\\d+)?\\s*?\\.\\.\\s*(\\d+)?")

    @DJ
    @CheckVoiceState
    @Command(aliases = ["removesong"], description = "Remove a song from the queue.")
    fun remove(ctx: Context, @Greedy which: String?) {
        val manager = ctx.manager
        val queue = manager.scheduler.queue

        if (queue.isEmpty()) {
            return ctx.send("The queue is empty.")
        }

        val track : String = when (which) {
            null -> return ctx.send("You need to specify what to remove.")
            "first" -> queue.remove() //Remove head
            "last" -> manager.scheduler.removeQueueIndex(queue, queue.size - 1)
            "all" -> {
                queue.clear()
                ctx.send("Cleared the music queue.")
                return
            }
            else -> {
                val matcher = pattern.matcher(which)
                if (matcher.find()) {
                    if (matcher.group(1) == null && matcher.group(2) == null) {
                        return ctx.send("You must specify start range and/or end range.")
                    }

                    val start = matcher.group(1).let {
                        if (it == null) 1
                        else it.toIntOrNull()?.coerceAtLeast(1)
                                ?: return ctx.send("Invalid start of range")
                    }

                    val end = matcher.group(2).let {
                        if (it == null) queue.size
                        else it.toIntOrNull()?.coerceAtMost(queue.size)
                                ?: return ctx.send("Invalid end of range")
                    }

                    for (i in end downTo start) {
                        manager.scheduler.removeQueueIndex(queue, i - 1)
                    }

                    return ctx.send("Removed tracks `$start-$end` from the queue.")
                }

                val num = which.toIntOrNull()
                        ?.takeIf { it >= 1 && it <= queue.size }
                        ?: return ctx.send("That is not a valid track number. Try `1`, `1..${queue.size}`, `first`, or `last`.")

                manager.scheduler.removeQueueIndex(queue, num - 1)
            }
        }

        val decodedTrack = PlaylistUtils.toAudioTrack(track)
        ctx.send("Removed __[${decodedTrack.info.embedTitle}](${decodedTrack.info.embedUri})__ from the queue.")
    }
}