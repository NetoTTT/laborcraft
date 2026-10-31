package com.foque.laborcraft.worker;

/** One job's work AI. The worker shell calls {@link #tick()} on the server only. */
public interface WorkerBrain {
	void tick();
}
