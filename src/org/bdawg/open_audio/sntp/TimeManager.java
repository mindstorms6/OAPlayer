package org.bdawg.open_audio.sntp;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;


public class TimeManager {

	private static TimeManager tmInstance;
	private static final long DEFUALT_RUN_DELAY = 10;
	private static final long DEFAULT_PERIOD = 300;

	private final static Logger logger = Logger
			.getLogger(TimeManager.class);
	private final SntpClient ntpClient = new SntpClient();
	private ScheduledExecutorService scheduler;
	private final int NTP_ROUNDS = 5;
	private long runningDelta = 0;
	private long runDeltaRounds = 0;
	private Object syncHandle;
	
	
	private final Runnable updateNTP = new Runnable() {
		@Override
		public void run() {
			try {
			long thisRunDeltaAvg = 0;
			int thisRunSuccesses = 0;
			logger.debug("ERRMERGAHD GETTING TEH NTPSSSS");
			for (int i = 0; i < NTP_ROUNDS; i++) {
				boolean didUpdate = ntpClient.requestTime("0.pool.ntp.org",
						30000);
				if (!didUpdate) {
					logger.info("Failed to update NTP time!!!");
				} else {
					long delta = ntpClient.getLastOffset();
					logger.debug("Round delta was " + delta);
					float tempNR = (float) ((float) (thisRunDeltaAvg * thisRunSuccesses) + delta)
							/ (float) (thisRunSuccesses + 1);
					thisRunDeltaAvg = (long) tempNR;
					thisRunSuccesses++;
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					logger.debug("Error updating NTP time", e);
				}
			}
			logger.debug("This round's avg was " + thisRunDeltaAvg);
			if (thisRunSuccesses > 0) {
				float tempRD = (float) ((float) (runningDelta * runDeltaRounds) + thisRunDeltaAvg)
						/ (float) (runDeltaRounds + 1);
				setOffset((long) tempRD);
				runDeltaRounds++;
				long millsOff = (long) (runningDelta * Math.pow(10, -6));
				
				logger.debug(String.format("New avg off delta is %d nanos.",
						runningDelta));
				logger.debug(
						String.format("Millis offset is %d. System time is %d. Adjusted time is %d",
						millsOff, System.currentTimeMillis(),
						System.currentTimeMillis() + millsOff));
			}
			} catch (Exception ex){
				logger.error("Caught an overall exception in TimeManger run method.", ex);
			}
		}
	};
	private ScheduledFuture<?> ntpHandle;

	private TimeManager(long delaySeconds, long periodSeconds) {
		logger.debug("Setting NTP timers");
		syncHandle = new Object();
		scheduler = Executors.newScheduledThreadPool(5);
		ntpHandle = scheduler.scheduleAtFixedRate(updateNTP, delaySeconds,
				periodSeconds, TimeUnit.SECONDS);

	}

	public static TimeManager getTMInstance() {
		if (tmInstance == null) {
			tmInstance = new TimeManager(DEFUALT_RUN_DELAY, DEFAULT_PERIOD);
		}
		return tmInstance;
	}

	public void shutdown() {
		ntpHandle.cancel(true);
		scheduler.shutdown();

	}

	public synchronized long getSystemTimeOffsetNanos() {
		long tr;
		synchronized (syncHandle) {
			tr = runningDelta;
		}
		return tr;
	}

	public synchronized long getCurrentTimeMillis(){
		long millsOff = (long) (getSystemTimeOffsetNanos() * Math.pow(10, -6));
		return System.currentTimeMillis() + millsOff;
	}
	
	private synchronized void setOffset(long newOffset) {
		synchronized (syncHandle) {
			runningDelta = newOffset;
		}
	}
}
