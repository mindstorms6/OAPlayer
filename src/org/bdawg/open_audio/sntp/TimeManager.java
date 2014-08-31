package org.bdawg.open_audio.sntp;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.log4j.Logger;

public class TimeManager {

	private static TimeManager tmInstance;
	private static final long DEFUALT_RUN_DELAY = 10;
	private static final long DEFAULT_PERIOD = 300;

	private final static Logger logger = Logger.getLogger(TimeManager.class);
	// private final SntpClient ntpClient = new SntpClient();
	private final NTPUDPClient ntpClient = new NTPUDPClient();
	private InetAddress hostAddr;
	private ScheduledExecutorService scheduler;
	private final int NTP_ROUNDS = 5;
	private long runningDelta = 0;
	private long runDeltaRounds = 0;
	private Object syncHandle;
	private long manualOffset = 0;

	private final Runnable updateNTP = new Runnable() {
		@Override
		public void run() {
			try {
				long thisRunDeltaAvg = 0;
				int thisRunSuccesses = 0;
				logger.debug("ERRMERGAHD GETTING TEH NTPSSSS");
				ntpClient.open();
				for (int i = 0; i < NTP_ROUNDS; i++) {
					// We want to timeout if a response takes longer than 10
					// seconds
					try {
						TimeInfo info = ntpClient.getTime(hostAddr);
						info.computeDetails(); // compute offset/delay if not
						// already done
						Long offsetValue = info.getOffset();
						Long delayValue = info.getDelay();

						if (offsetValue != null) {
							float tempNR = (float) ((float) (thisRunDeltaAvg * thisRunSuccesses) + offsetValue
									.longValue())
									/ (float) (thisRunSuccesses + 1);
							thisRunDeltaAvg = (long) tempNR;
							thisRunSuccesses++;
						}

					} catch (SocketException sex) {

					}

					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						logger.debug("Error updating NTP time", e);
					}
				}
				ntpClient.close();
				logger.debug("This round's avg was " + thisRunDeltaAvg);
				if (thisRunSuccesses > 0) {
					float tempRD = (float) ((float) (runningDelta * runDeltaRounds) + thisRunDeltaAvg)
							/ (float) (runDeltaRounds + 1);
					setOffset((long) tempRD);
					runDeltaRounds++;
					long millsOff = (long) (runningDelta);

					logger.debug(String.format(
							"New avg off delta is %d millis.", runningDelta));
					logger.debug(String
							.format("Millis offset is %d. System time is %d. Adjusted time is %d",
									millsOff, System.currentTimeMillis(),
									System.currentTimeMillis() + millsOff));
				}
			} catch (Exception ex) {
				logger.error(
						"Caught an overall exception in TimeManger run method.",
						ex);
			}
		}
	};
	private ScheduledFuture<?> ntpHandle;

	private TimeManager(long delaySeconds, long periodSeconds)
			throws UnknownHostException {
		logger.debug("Setting NTP timers");
		syncHandle = new Object();
		ntpClient.setDefaultTimeout(10000);
		scheduler = Executors.newScheduledThreadPool(5);
		ntpHandle = scheduler.scheduleAtFixedRate(updateNTP, delaySeconds,
				periodSeconds, TimeUnit.SECONDS);
		hostAddr = InetAddress.getByName("pool.ntp.org");

	}

	public static TimeManager getTMInstance() {
		if (tmInstance == null) {
			try {
				tmInstance = new TimeManager(DEFUALT_RUN_DELAY, DEFAULT_PERIOD);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				logger.error("Could find the NTP host!!!!!", e);
			}
		}
		return tmInstance;
	}

	public void shutdown() {
		ntpHandle.cancel(true);
		scheduler.shutdown();

	}

	private long getSystemTimeOffsetMillis() {
		long tr;
		synchronized (this) {
			tr = runningDelta;
		}
		return tr;
	}

	public long getCurrentTimeMillis() {
		long millsOff = (long) (getSystemTimeOffsetMillis());
		return System.currentTimeMillis() + millsOff+ TimeManager.getTMInstance().getManualOffset();
	}

	private synchronized void setOffset(long newOffset) {
		synchronized (syncHandle) {
			runningDelta = newOffset;
		}
	}

	public void updateNow() {
		this.updateNTP.run();
	}

	public long getManualOffset() {
		return this.manualOffset;
	}

	public void setManualOffset(long manualOffset) {
		this.manualOffset = manualOffset;
	}
}
