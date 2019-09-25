package edu.uchicago.cs.ucare.dmck.interceptor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;

public class ConsistentVerifier {

	private static final Logger LOG = Logger.getLogger(ConsistentVerifier.class);

	static String workingDir;
    static String path;
    static File[] dataDir;
    static File[] dataLogDir;
    static int numNode;

	public static void main(String[] args) {
		if(args.length != 1){
			System.out.println("Usage: please specify <working_dir>");
			System.exit(1);
		}
		workingDir = args[0];
        getValues();
	}

	public static void getValues(){
		String values = "";
		
		Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
		Session session = cluster.connect("test");
		try {
			LOG.info("Querying row from table");
			ResultSet rs = session.execute("SELECT * FROM tests");
			LOG.info("Row acquired");
			Row row = rs.one();
			values += "owner=" + row.getString("owner") + "\n";
			values += "value_1=" + row.getString("value_1") + "\n";
	        values += "value_2=" + row.getString("value_2") + "\n";
	        values += "value_3=" + row.getString("value_3") + "\n";
		} catch (Exception e) {
			LOG.error("ERROR in reading row.");
			LOG.error(e);
		}

		cluster.close();

        writeResult(values);
	}

	public static void writeResult(String content){
		try{
			PrintWriter writer = new PrintWriter(workingDir + "/temp-verify", "UTF-8");
			writer.println(content);
			writer.close();
		} catch (Exception e){
			LOG.error("ERROR in writing temp-verify result.");
			LOG.error(e);
		}
	}

}
