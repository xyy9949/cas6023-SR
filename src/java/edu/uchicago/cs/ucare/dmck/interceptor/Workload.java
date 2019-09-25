package edu.uchicago.cs.ucare.dmck.interceptor;

import org.apache.log4j.Logger;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.ResultSet;

public class Workload implements Runnable {

	private static final Logger LOG = Logger.getLogger(Workload.class);

	private int id;
	private Cluster cluster;
	private String cql;

	private Session session;
	private boolean isFinished;
	private Thread t;

	private boolean isApplied = false;


	public Workload(int id, Cluster cluster, String cql) {
		this.id = id;
		this.cluster = cluster;
		this.cql = cql;

		this.session = null;
		this.isFinished = false;
	}

	public void reset() {
		if (session != null) {
			try {
				session.close();
			} catch (Exception e) {
				LOG.error("Failed to close Cassandra connection.");
			}
		}
		session = null;
		isFinished = false;
	}

	@Override
	public void run() {
		t = new Thread(new Runnable() {

			@Override
			public void run() {
				session = cluster.connect("test");
				LOG.info("Executing: " + cql);
				ResultSet rs = session.execute(cql);
				isApplied = rs.wasApplied();
				LOG.info("Finished executing: " + cql);
				finish();
			}

		});

		t.start();
	}

	private void finish() {
		if (!isFinished) {
			notifyDMCK();
			isFinished = true;
		}
	}

	private void notifyDMCK() {
		LOG.info("Notify DMCK that it has finished a workload with isApplied=" + isApplied);
		InterceptionLayer.updateWorkloadAccomplishement(id, isApplied);
	}

	public boolean isFinished() {
		return isFinished;
	}

	public boolean isApplied() {
		return isApplied;
	}

}
