import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.charset.StandardCharsets;





import de.regioosm.housenumbers.*;
/*
 * ATTENTION: upload of large files (at 01/2015 max 10 MB) can fails.
 * 			  on Server, tomcat7 must be configured to allow larger files.
 * 			  This can be set in $TOMCAT_HOME/conf/server.xml at section
 *    <Connector port="8080" protocol="HTTP/1.1"
 *               connectionTimeout="20000"
 *               URIEncoding="UTF-8"
 *               maxPostSize="30000000"				<==== add this line. Here the limit is set to 30 MB
 *               redirectPort="8443" />
 * 
 */

/**
 *
 * Connection to housenumber server part via Server API
 * <p>
 * You can get housenumber lists from the server or upload evaluation result to the server
 * 
 * @author Dietmar Seifert
 * 	
*/




public class HousenumberServerAPI {

	static final String USER_AGENT = "regio-osm.de Housenumber Evaluation Client, contact: strassenliste@diesei.de";

	Applicationconfiguration configuration = new Applicationconfiguration("");
	private static final Logger logger = Evaluation.logger;


	private String serverUrl = "";
//TODO configuration
	public HousenumberServerAPI() {
		serverUrl = configuration.housenumberseverAPIprotocol + "://"
			+ configuration.housenumberseverAPIhost;
		if(! configuration.housenumberseverAPIport.equals(""))
			serverUrl += ":" + configuration.housenumberseverAPIport;
		if(! configuration.housenumberseverAPIport.equals(""))
			serverUrl += configuration.housenumberseverAPIrooturl;
		else
			serverUrl += "/";
	}
	
	public List<Job> findJobs(String country, String municipality, String jobname, String officialkeys_id, String adminhierarchy) {
		List<Job> foundjobs = new ArrayList<Job>();

		java.util.Date sendToServerStarttime = new java.util.Date();

		try {
			String url_string = serverUrl + "/findjobs";

			String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.

			URLConnection connection = new URL(url_string).openConnection();
			connection.setDoOutput(true); // This sets request method to POST.
			connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			PrintWriter writer = null;
			try {
			    writer = new PrintWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
			    StringBuffer temptoutput = new StringBuffer();
			    	// please note, that all \r\n are necessary. \n is not enough 
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"country\"" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(country + "\r\n");
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"municipality\"" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(municipality + "\r\n");
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"jobname\"" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(jobname + "\r\n");
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"officialkeys\"" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(officialkeys_id + "\r\n");
				temptoutput.append("--" + boundary + "\r\n");
				temptoutput.append("Content-Disposition: form-data; name=\"adminhierarchy\"" + "\r\n");
				temptoutput.append("\r\n");
				temptoutput.append(adminhierarchy + "\r\n");
			    temptoutput.append("--" + boundary + "--" + "\r\n");
			    int laststartpos = (temptoutput.toString().length() > 8000) ? 8000: temptoutput.toString().length();
			    int firstendpos = (temptoutput.toString().length() > 1000) ? temptoutput.toString().length() - 1000 : 0;
			    System.out.println("Upload-Request Start ===\n" + temptoutput.toString().substring(0, laststartpos) + "\n===");
			    System.out.println("Upload-Request Ende ===\n" + temptoutput.toString().substring(firstendpos, temptoutput.toString().length()) + "\n===");
			    writer.println(temptoutput.toString());
			} finally {
			    if (writer != null) writer.close();
			}
	
			// Connection is lazily executed whenever you request any status.
			int responseCode = ((HttpURLConnection) connection).getResponseCode();
			System.out.println(responseCode); // Should be 200
				// ===================================================================================================================
	
	
			Integer headeri = 1;
			System.out.println("Header-Fields Ausgabe ...");
			while(((HttpURLConnection) connection).getHeaderFieldKey(headeri) != null) {
				System.out.println("  Header # "+headeri+":  ["+((HttpURLConnection) connection).getHeaderFieldKey(headeri)+"] ==="+((HttpURLConnection) connection).getHeaderField(headeri)+"===");
				headeri++;
			}
	
			java.util.Date sendToServerEndtime = new java.util.Date();
			System.out.println("Duration for wating to Server response after upload result content in ms: " + (sendToServerEndtime.getTime() - sendToServerStarttime.getTime()));
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(),java.nio.charset.Charset.forName("UTF-8")));
			System.out.println("getcontentencoding ===" + connection.getContentEncoding() + "===");
				// check, if Content metainfo is evailable (null, if no oder incomplete content)
			if(connection.getContentEncoding() == null) {
				System.out.println("WARNING: no Contentencoding available, so content missing or incomplete, will be ignored");
			} else {
				String fileline = "";
				while((fileline = reader.readLine()) != null) {
					System.out.println(fileline);

					if(fileline.equals(""))
						continue;
					System.out.println("first char ===" + fileline.substring(0,1) + "===");
					if(fileline.substring(0,1).equals("#")) {
						System.out.println("ignore comment line ===" + fileline + "===");
						continue;
					}
					String linecolumns[] = fileline.split("\t");
					
					Job actjob = null; 
					if(linecolumns.length == 8) {
						actjob = new Job(linecolumns[0], linecolumns[1], linecolumns[2], linecolumns[3], 
							Integer.parseInt(linecolumns[4]), linecolumns[5], linecolumns[6], Long.parseLong(linecolumns[7]));
					} else if(linecolumns.length == 9) {
						actjob = new Job(linecolumns[0], linecolumns[1], linecolumns[2], linecolumns[3], 
							Integer.parseInt(linecolumns[4]), linecolumns[5],  Long.parseLong(linecolumns[8]), linecolumns[6], Long.parseLong(linecolumns[7]));
					}
					foundjobs.add(actjob);
				}
			}
			writer.close();
			reader.close();

		}							
		catch (MalformedURLException mue) {
			System.out.println("ERROR: MalformedURLException, Details ...");
			mue.printStackTrace();
			return foundjobs;
		} 
		catch (IOException ioe) {
			System.out.println("ERROR: IOException, Details ...");
			ioe.printStackTrace();
			return foundjobs;
		}
		
		
		return foundjobs;
	}

	
	public List<Job> getQueueJobs(Evaluation evaluation, String requestfilter, Integer maxjobcount) {
		List<Job> foundjobs = new ArrayList<Job>();

		java.util.Date sendToServerStarttime = new java.util.Date();

		try {
			String url_string = serverUrl + "/getqueuejobs";

			String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.

			URLConnection connection = new URL(url_string).openConnection();
			connection.setDoOutput(true); // This sets request method to POST.
			connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			PrintWriter writer = null;
			try {
			    writer = new PrintWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
			    StringBuffer temptoutput = new StringBuffer();
			    	// please note, that all \r\n are necessary. \n is not enough 
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"requestreason\"" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(requestfilter + "\r\n");
			    temptoutput.append("--" + boundary + "\r\n");
			    if(!evaluation.getCountry().equals("")) {
				    temptoutput.append("Content-Disposition: form-data; name=\"country\"" + "\r\n");
				    temptoutput.append("\r\n");
				    temptoutput.append(evaluation.getCountry() + "\r\n");
				    temptoutput.append("--" + boundary + "\r\n");
			    }
			    temptoutput.append("Content-Disposition: form-data; name=\"maxjobcount\"" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(maxjobcount+ "\r\n");
			    temptoutput.append("--" + boundary + "--" + "\r\n");
			    int laststartpos = (temptoutput.toString().length() > 8000) ? 8000: temptoutput.toString().length();
			    int firstendpos = (temptoutput.toString().length() > 1000) ? temptoutput.toString().length() - 1000 : 0;
			    System.out.println("Upload-Request Start ===\n" + temptoutput.toString().substring(0, laststartpos) + "\n===");
			    System.out.println("Upload-Request Ende ===\n" + temptoutput.toString().substring(firstendpos, temptoutput.toString().length()) + "\n===");
			    writer.println(temptoutput.toString());
			} finally {
			    if (writer != null) writer.close();
			}
	
			// Connection is lazily executed whenever you request any status.
			int responseCode = ((HttpURLConnection) connection).getResponseCode();
			System.out.println(responseCode); // Should be 200
				// ===================================================================================================================
	
	
			Integer headeri = 1;
			System.out.println("Header-Fields Ausgabe ...");
			while(((HttpURLConnection) connection).getHeaderFieldKey(headeri) != null) {
				System.out.println("  Header # "+headeri+":  ["+((HttpURLConnection) connection).getHeaderFieldKey(headeri)+"] ==="+((HttpURLConnection) connection).getHeaderField(headeri)+"===");
				headeri++;
			}
	
			java.util.Date sendToServerEndtime = new java.util.Date();
			System.out.println("Duration for wating to Server response after upload result content in ms: " + (sendToServerEndtime.getTime() - sendToServerStarttime.getTime()));
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(),java.nio.charset.Charset.forName("UTF-8")));
			System.out.println("getcontentencoding ===" + connection.getContentEncoding() + "===");
				// check, if Content metainfo is evailable (null, if no oder incomplete content)
			if(connection.getContentEncoding() == null) {
				System.out.println("WARNING: no Contentencoding available, so content missing or incomplete, will be ignored");
			} else {
				String fileline = "";
				while((fileline = reader.readLine()) != null) {
					System.out.println(fileline);
					if(fileline.equals(""))
						continue;
					if(fileline.substring(0,1).equals("#")) {
						System.out.println("ignore comment line ===" + fileline + "===");
						continue;
					}
					String linecolumns[] = fileline.split("\t");
					
					Job actjob = null;
					if(linecolumns.length == 9) {
						actjob = new Job(linecolumns[0], linecolumns[1], linecolumns[2], linecolumns[3], 
							Integer.parseInt(linecolumns[4]), linecolumns[5], linecolumns[6], Long.parseLong(linecolumns[7]), linecolumns[8]);
					} else if(linecolumns.length == 10) {
						actjob = new Job(linecolumns[0], linecolumns[1], linecolumns[2], linecolumns[3], 
							Integer.parseInt(linecolumns[4]), linecolumns[5], Long.parseLong(linecolumns[9]), 
							linecolumns[6], Long.parseLong(linecolumns[7]), linecolumns[8]);
					}
					foundjobs.add(actjob);
				}
			}
			writer.close();
			reader.close();

		}							
		catch (MalformedURLException mue) {
			System.out.println("ERROR: MalformedURLException, Details ...");
			mue.printStackTrace();
			return foundjobs;
		} 
		catch (IOException ioe) {
			System.out.println("ERROR: IOException, Details ...");
			ioe.printStackTrace();
			return foundjobs;
		}
		
		
		return foundjobs;
	}
	

		/**
		 * get a list of jobs, which evaluations in a country are still missing until the country is completely evaluated
		 * 
		 * @param country
		 * @return  List of Evaluation Jobs
		 */
	public List<Job> getMissingCountryJobs(String country) {
		List<Job> foundjobs = new ArrayList<Job>();
	
		java.util.Date sendToServerStarttime = new java.util.Date();
	
		try {
			String url_string = serverUrl + "/getMissingCountryJobs";
	
			String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
	
			URLConnection connection = new URL(url_string).openConnection();
			connection.setDoOutput(true); // This sets request method to POST.
			connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			PrintWriter writer = null;
			try {
			    writer = new PrintWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
			    StringBuffer temptoutput = new StringBuffer();
			    	// please note, that all \r\n are necessary. \n is not enough 
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"country\"" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(country + "\r\n");
			    temptoutput.append("--" + boundary + "--" + "\r\n");
			    int laststartpos = (temptoutput.toString().length() > 8000) ? 8000: temptoutput.toString().length();
			    int firstendpos = (temptoutput.toString().length() > 1000) ? temptoutput.toString().length() - 1000 : 0;
			    System.out.println("Upload-Request Start ===\n" + temptoutput.toString().substring(0, laststartpos) + "\n===");
			    System.out.println("Upload-Request Ende ===\n" + temptoutput.toString().substring(firstendpos, temptoutput.toString().length()) + "\n===");
			    writer.println(temptoutput.toString());
			} finally {
			    if (writer != null) writer.close();
			}
	
			// Connection is lazily executed whenever you request any status.
			int responseCode = ((HttpURLConnection) connection).getResponseCode();
			System.out.println(responseCode); // Should be 200
				// ===================================================================================================================
	
	
			Integer headeri = 1;
			System.out.println("Header-Fields Ausgabe ...");
			while(((HttpURLConnection) connection).getHeaderFieldKey(headeri) != null) {
				System.out.println("  Header # "+headeri+":  ["+((HttpURLConnection) connection).getHeaderFieldKey(headeri)+"] ==="+((HttpURLConnection) connection).getHeaderField(headeri)+"===");
				headeri++;
			}
	
			java.util.Date sendToServerEndtime = new java.util.Date();
			System.out.println("Duration for wating to Server response after upload result content in ms: " + (sendToServerEndtime.getTime() - sendToServerStarttime.getTime()));
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(),java.nio.charset.Charset.forName("UTF-8")));
					// just for simulating server response with local file
					//String dateipfadname = "/home/openstreetmap/temp/uploaddata/temp/test.txt";
					//BufferedReader reader = new BufferedReader(new FileReader(dateipfadname));
			System.out.println("getcontentencoding ===" + connection.getContentEncoding() + "===");
			String fileline = "";
			while((fileline = reader.readLine()) != null) {
				System.out.println(fileline);
				if(fileline.equals(""))
					continue;
				if(fileline.substring(0,1).equals("#")) {
					System.out.println("ignore comment line ===" + fileline + "===");
					continue;
				}
				String linecolumns[] = fileline.split("\t");

				Job actjob = null;
				if (linecolumns.length == 8) {
					actjob = new Job(linecolumns[0], linecolumns[1], linecolumns[2], linecolumns[3], 
						Integer.parseInt(linecolumns[4]), linecolumns[5], linecolumns[6], Long.parseLong(linecolumns[7]));
				} else if(linecolumns.length == 9) {
					actjob = new Job(linecolumns[0], linecolumns[1], linecolumns[2], linecolumns[3], 
						Integer.parseInt(linecolumns[4]), linecolumns[5], Long.parseLong(linecolumns[8]), 
						linecolumns[6], Long.parseLong(linecolumns[7]));
				}
				foundjobs.add(actjob);
			}
			writer.close();
			reader.close();
	
		}							
		catch (MalformedURLException mue) {
			System.out.println("ERROR: MalformedURLException, Details ...");
			mue.printStackTrace();
			return foundjobs;
		} 
		catch (IOException ioe) {
			System.out.println("ERROR: IOException, Details ...");
			ioe.printStackTrace();
			return foundjobs;
		}
		return foundjobs;
	}
	
	
	
	
	public boolean writeEvaluationToServer(Evaluation evaluation, HousenumberCollection result) {

		String result_content = result.toString(evaluation, "\t", true);

		String filename = "";
		try {
				// first, save upload data as local file, just for checking or for history
			DateFormat time_formatter = new SimpleDateFormat("yyyyMMdd-HHmmss-SZ");
			String uploadtime = time_formatter.format(new Date());
	
			filename += configuration.application_datadir + File.separator +  "uploaddata" + File.separator + uploadtime + ".result";
			System.out.println("uploaddatei ===" + filename + "===");

			File outputfile = new File(filename);
			outputfile.createNewFile();
			outputfile.setReadable(true);
			outputfile.setWritable(true);
			outputfile.setExecutable(false);

			PrintWriter osmOutput = null;
			osmOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename),StandardCharsets.UTF_8)));
			osmOutput.println(result_content);
			osmOutput.close();

			// sub start
/*	this zip generation and output to file works, but no idea, how to include in upload multiform below, so deactivated
			GZIPOutputStream gos = new GZIPOutputStream(os);
			System.out.println("text.getBytes(): " + result_content.getBytes("UTF-8").length);
			gos.write(result_content.getBytes("UTF-8"));
			gos.close();
			byte[] compressed = os.toByteArray();
			System.out.println("compressed .length: " + compressed.length);
			String zippedResultContent = os.toString();
			System.out.println("zippedResultContent .length: " + zippedResultContent.length());
			os.close();
			// sub end

			FileOutputStream out=new FileOutputStream(filename + ".zip");
			for (int i=0; i < compressed.length; i++) {
				out.write(compressed[i]);
			}
			out.flush();
			out.close();
*/
			
			String url_string = serverUrl + "/Upload";
//TODO configuration
			System.out.println(" will upload result to server with url ===" + url_string + "===");


				// now upload as multipart/form-data request - should work also for larger content
				//http://stackoverflow.com/questions/2469451/upload-files-with-java
				// ===================================================================================================================
			java.util.Date sendToServerStarttime = new java.util.Date();
			
			String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.

			URLConnection connection = new URL(url_string).openConnection();
			connection.setDoOutput(true); // This sets request method to POST.
			connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			PrintWriter writer = null;
			try {
			    writer = new PrintWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
			    StringBuffer temptoutput = new StringBuffer();
			    	// please note, that all \r\n are necessary. \n is not enough 
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"Country\"" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(evaluation.getCountry() + "\r\n");
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"Municipality\"" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(evaluation.getMunicipality() + "\r\n");
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"Officialkeysid\"" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(evaluation.getOfficialkeysId() + "\r\n");
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"Adminlevel\"" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(evaluation.getAdminLevel() + "\r\n");
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"Jobname\"" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(evaluation.getJobname() + "\r\n");
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"JobID\"" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(evaluation.getJobid() + "\r\n");
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"Serverobjectid\"" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(evaluation.getServerobjectid() + "\r\n");
			    temptoutput.append("--" + boundary + "\r\n");
			    temptoutput.append("Content-Disposition: form-data; name=\"result\"; filename=\"result.txt.zip\"" + "\r\n");
			    temptoutput.append("Content-Type: text/plain; charset=" + StandardCharsets.UTF_8.toString() + "\r\n");
			    //temptoutput.append("Content-Type: application/zip" + "\r\n");
			    temptoutput.append("\r\n");
			    temptoutput.append(result_content);
			    //temptoutput.append(zippedResultContent);
	            temptoutput.append("\r\n");
			    temptoutput.append("--" + boundary + "--" + "\r\n");
			    int maxoutput = (temptoutput.toString().length() > 8000) ? 8000: temptoutput.toString().length();
			    //System.out.println("Upload-Request Start ===\n" + temptoutput.toString().substring(0, maxoutput) + "\n===");
			    System.out.println("Upload-Result file length ===\n" + temptoutput.toString().length() + "\n===");
			    int minoutput = (temptoutput.toString().length() > 1000) ? 1000: temptoutput.toString().length();
			    //System.out.println("Upload-Request Ende ===\n" + temptoutput.toString().substring(minoutput, temptoutput.toString().length()) + "\n===");
			    writer.println(temptoutput.toString());
			} finally {
			    if (writer != null) writer.close();
			}

			// Connection is lazily executed whenever you request any status.
			int responseCode = ((HttpURLConnection) connection).getResponseCode();
			System.out.println(responseCode); // Should be 200
				// ===================================================================================================================


			Integer headeri = 1;
			System.out.println("Header-Fields Ausgabe ...");
			while(((HttpURLConnection) connection).getHeaderFieldKey(headeri) != null) {
				System.out.println("  Header # "+headeri+":  ["+((HttpURLConnection) connection).getHeaderFieldKey(headeri)+"] ==="+((HttpURLConnection) connection).getHeaderField(headeri)+"===");
				headeri++;
			}

			java.util.Date sendToServerEndtime = new java.util.Date();
			System.out.println("Duration for wating to Server response after upload result content in ms: " + (sendToServerEndtime.getTime() - sendToServerStarttime.getTime()));
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuffer xml_content = new StringBuffer();
			String fileline = "";
			while((fileline = reader.readLine()) != null) {
				xml_content.append(fileline + "\n");
				System.out.println(fileline);
			}
			writer.close();
			reader.close();
			//only httpurl not url...     connection.disconnect();
		
		}
		catch (MalformedURLException mue) {
			System.out.println("ERROR: MalformedURLException, Details ...");
			mue.printStackTrace();
			return false;
		} 
		catch (IOException ioe) {
			System.out.println("ERROR: IOException, Details ...");
			ioe.printStackTrace();
			return false;
		}
		return true;
	}


	public HousenumberCollection ReadListFromServer(Evaluation evaluation, HousenumberCollection housenumbers) {
		URL                url; 
		BufferedReader     dis;
		
		try {
			if(		(evaluation.getCountry().equals(""))
				||	(evaluation.getMunicipality().equals(""))
				)
			{
				return housenumbers;
			}
	
			String urlString = serverUrl + "/getHousenumberlist";

			url = new URL(urlString);
			
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
			connection.setRequestProperty("User-Agent", USER_AGENT);
			connection.setRequestProperty("Content-Language", "en-US");

			//urlConn.setRequestProperty("Accept-Encoding", "gzip, compress");

	 
			String urlParameters = "country=" +  URLEncoder.encode(evaluation.getCountry(),"UTF-8");
			urlParameters += "&" + "municipality=" + URLEncoder.encode(evaluation.getMunicipality(), "UTF-8");
			urlParameters += "&" + "subid=" + URLEncoder.encode(evaluation.getSubid(),"UTF-8");
			urlParameters += "&" + "adminlevel=" + evaluation.getAdminLevel();
			urlParameters += "&" + "jobname=" + URLEncoder.encode(evaluation.getJobname(),"UTF-8");
			urlParameters += "&" + "job_id=" + evaluation.getJobid();
			urlParameters += "&" + "officialkeysid=" + URLEncoder.encode(evaluation.getOfficialkeysId(),"UTF-8");
			urlParameters += "&" + "serverobjectid=" + URLEncoder.encode(evaluation.getServerobjectid(),"UTF-8");

			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			System.out.println("upload_string==="+urlParameters+"===");
			logger.log(Level.FINE, "Request to get housenumberlist ===" + urlParameters + "===");
			writer.write(urlParameters);
			writer.flush();
	
				
			String inputline = "";
			InputStream serverResponse = connection.getInputStream(); 
	
			Integer headeri = 1;
			System.out.println("Header-Fields Ausgabe ...");
			String responseContentEncoding = "";
			while(connection.getHeaderFieldKey(headeri) != null) {
				System.out.println("  Header # " + headeri 
					+ ":  [" + connection.getHeaderFieldKey(headeri)
					+ "] ===" + connection.getHeaderField(headeri) + "===");
				if(connection.getHeaderFieldKey(headeri).equals("Content-Encoding"))
					responseContentEncoding = connection.getHeaderField(headeri);
				headeri++;
			}
	
			//if(responseContentEncoding.equals("gzip")) {
			//	dis = new BufferedReader(new InputStreamReader(new GZIPInputStream(serverResponse),"UTF-8"));
			//} else {
			dis = new BufferedReader(new InputStreamReader(serverResponse,"UTF-8"));
			//}
			housenumbers.housenumberlist = new StringBuffer();
			while ((inputline = dis.readLine()) != null)
			{
				housenumbers.housenumberlist.append(inputline + "\n");
				//System.out.println(inputline);
				if(inputline.equals(""))
					continue;
				String linecolumns[] = inputline.split("\t");
				if(inputline.indexOf("#") == 0) {
//TODO interpret and/or store metadata into housenumbercollection instance
//TODO interpret first comment line as column header with fieldnames for each columnu for flexibility file format
					continue;
				}
				if(linecolumns.length < 2)
					continue;
					// should be possible not to set isHousenumberaddition_exactly
				Housenumber acthousenumber = new Housenumber(housenumbers);
				//linecolumns[0] "Subadminarea"
				
				acthousenumber.setStrasse(linecolumns[1]);
				acthousenumber.setHausnummer(linecolumns[2]);
				if((linecolumns.length >= 4) && (! linecolumns[3].equals("")))
					acthousenumber.setLonlat(linecolumns[3]);
				if((linecolumns.length >= 5) && (! linecolumns[4].equals("")))
					acthousenumber.setLonlat_source(linecolumns[4]);
				if((linecolumns.length >= 6) && (! linecolumns[5].equals("")))
					acthousenumber.setHousenumberComment(linecolumns[5]);
				if((linecolumns.length >= 7) && (! linecolumns[6].equals("")))
					acthousenumber.setPostcode(linecolumns[6]);
				acthousenumber.setTreffertyp(Housenumber.Treffertyp.LIST_ONLY);
				housenumbers.add_newentry(acthousenumber);
			}
			dis.close();
//TODO dirty save as here, just for test purposes
			String filename = configuration.application_datadir + File.separator + "housenumberlists"
				+ File.separator + URLEncoder.encode(evaluation.getMunicipality(),"UTF-8") + ".txt";
			PrintWriter uploadOutput = null;
			uploadOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename),StandardCharsets.UTF_8)));
			uploadOutput.println(housenumbers.housenumberlist.toString());
			uploadOutput.close();
		}
		catch (MalformedURLException mue) {
			System.out.println("ERROR: MalformedURLException, Details ...");
			mue.printStackTrace();
			return housenumbers;
		} 
		catch (IOException ioe) {
			System.out.println("ERROR: IOException, Details ...");
			ioe.printStackTrace();
			return housenumbers;
		}
		
		return housenumbers;
	}
}
