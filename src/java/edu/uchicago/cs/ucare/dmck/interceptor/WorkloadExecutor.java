package edu.uchicago.cs.ucare.dmck.interceptor;

import java.util.Arrays;
import java.net.InetSocketAddress;

import org.apache.log4j.Logger;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.core.policies.WhiteListPolicy;

public class WorkloadExecutor {

	private static final Logger LOG = Logger.getLogger(WorkloadExecutor.class);

	public static void main(String[] args) {
		if (args.length != 1) {
			LOG.error("Parameters are incorrect: <workload_type>");
			System.exit(1);
		}

		String workload = args[0];

		if (workload.equals("cass-6023")) {
		  cass6023();
		} else if (workload.equals("cass-6013")) {
		  cass6013();
		}
	}

	private static void cass6013() {
		Cluster cluster1 = Cluster.builder().addContactPoint("127.0.0.1")
                .withLoadBalancingPolicy(new WhiteListPolicy(new RoundRobinPolicy(), Arrays.asList(new InetSocketAddress("127.0.0.1", 9042)))).build();
		cluster1.getConfiguration().getPoolingOptions().setPoolTimeoutMillis(120000);

		Cluster cluster2 = Cluster.builder().addContactPoint("127.0.0.2")
                .withLoadBalancingPolicy(new WhiteListPolicy(new RoundRobinPolicy(), Arrays.asList(new InetSocketAddress("127.0.0.2", 9042)))).build();
		cluster2.getConfiguration().getPoolingOptions().setPoolTimeoutMillis(120000);

		// initiate each workload

		Workload w1 = new Workload(1, cluster1, "UPDATE tests SET owner = 'user_2' WHERE name = 'testing' IF  owner = 'user_1'");
		Workload w2 = new Workload(2, cluster2, "UPDATE tests SET value_1 = 'Y' WHERE name = 'testing' IF  owner = 'user_1'");

		// run each workload; keep re-executing if the write does not succeed
		try {
			w1.run();
			Thread.sleep(1000);
			w2.run();
			waitForWorkload(w1);
			waitForWorkload(w2);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		cluster1.close();
		cluster2.close();
	}

	private static void cass6023() {
		Cluster cluster1 = Cluster.builder().addContactPoint("127.0.0.1")
                .withLoadBalancingPolicy(new WhiteListPolicy(new RoundRobinPolicy(), Arrays.asList(new InetSocketAddress("127.0.0.1", 9042)))).build();
		cluster1.getConfiguration().getPoolingOptions().setPoolTimeoutMillis(120000);

		Cluster cluster2 = Cluster.builder().addContactPoint("127.0.0.2")
                .withLoadBalancingPolicy(new WhiteListPolicy(new RoundRobinPolicy(), Arrays.asList(new InetSocketAddress("127.0.0.2", 9042)))).build();
		cluster2.getConfiguration().getPoolingOptions().setPoolTimeoutMillis(120000);

		Cluster cluster3 = Cluster.builder().addContactPoint("127.0.0.3")
                .withLoadBalancingPolicy(new WhiteListPolicy(new RoundRobinPolicy(), Arrays.asList(new InetSocketAddress("127.0.0.3", 9042)))).build();
		cluster3.getConfiguration().getPoolingOptions().setPoolTimeoutMillis(120000);


		// initiate each workload

		Workload w1 = new Workload(1, cluster1, "UPDATE tests SET value_1 = 'A' WHERE name = 'testing' IF owner = 'user_1'");
		Workload w2 = new Workload(2, cluster2, "UPDATE tests SET value_1 = 'B', value_2 = 'B' WHERE name = 'testing' IF  value_1 = 'A'");
		Workload w3 = new Workload(3, cluster3, "UPDATE tests SET value_3 = 'C' WHERE name = 'testing' IF owner = 'user_1'");

		// run each workload; keep re-executing if the write does not succeed
		try {
			w1.run();
			Thread.sleep(2000);
			w2.run();
			Thread.sleep(2000);
			w3.run();
			waitForWorkload(w1);
			waitForWorkload(w2);
			waitForWorkload(w3);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		cluster1.close();
		cluster2.close();
		cluster3.close();
	}

	private static void waitForWorkload(Workload w) {
		while (!w.isFinished()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
