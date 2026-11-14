package com.luminblades.ability;

import com.luminblades.LuminBlades;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Agenda ações "por tick" (efeitos com duração) sem criar entidades próprias. */
@Mod.EventBusSubscriber(modid = LuminBlades.MODID)
public final class ServerScheduler {
    /** Retorne false para encerrar antes do fim. {@code tick} vai de 0 a duration-1. */
    @FunctionalInterface
    public interface Action {
        boolean tick(int tick);
    }

    private static final class Task {
        final ServerLevel level;
        final int duration;
        final Action action;
        int tick;

        Task(ServerLevel level, int duration, Action action) {
            this.level = level;
            this.duration = duration;
            this.action = action;
        }
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();

    private ServerScheduler() {
    }

    public static void run(ServerLevel level, int duration, Action action) {
        PENDING.add(new Task(level, Math.max(1, duration), action));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        TASKS.addAll(PENDING);
        PENDING.clear();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            boolean keep;
            try {
                keep = t.tick < t.duration && t.action.tick(t.tick++) && t.tick < t.duration;
            } catch (Exception ex) {
                LuminBlades.LOGGER.error("Erro em efeito agendado", ex);
                keep = false;
            }
            if (!keep) it.remove();
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        TASKS.clear();
        PENDING.clear();
    }
}
