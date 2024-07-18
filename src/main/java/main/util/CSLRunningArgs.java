package main.util;

import com.csl.ids.IDSParams;
import lombok.Getter;
import lombok.Setter;
import main.CSLIDSMainClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Getter
@Setter
public class CSLRunningArgs {
		private static final Logger logger = LoggerFactory.getLogger(CSLIDSMainClient.class);

		String configFile = "application.json";
		String error = "";

		boolean debug = false;
		boolean verbose = false;

		boolean startIdsRunner = true;
		boolean startCSLHttpServer = true;
		boolean startCSLUDPServer = true;

		boolean startCSLDatabaseServer = true;

		boolean doNotUseCurrentIDSParamsFileName = false;

		String userDir0 = System.getProperty("user.dir");

		String userDir = userDir0;

		boolean userDirDefault = true;

		String dataDir = "";
		private boolean testparam = false;


		public String logDir = "";

		public String dataSetForLearning = "";
		public String dataSetForRecording = "";
		public String dataSetForDetectionOffline = "";

		public String dirForLearning = "";
		public String dirForRecording = "";
		public String dirForDetectionOffline = "";

		public String databasedir = "";

		public int idsMode = -1;

		public CSLRunningArgs() {

			this.userDir = System.getProperty("user.dir");

			this.dataDir = System.getProperty("user.dir") + File.separator + "idsdata";

		}

		public String validTestModeparam(String s) {

			s = s.toUpperCase();

			String z = "";
			for (int i = 0; i < IDSParams.idsModeAsString.length; i++) {
				if (IDSParams.idsModeAsString[i].compareTo(s) == 0) return s;
				if (!z.isEmpty()) z = z + ",";
				z = z + IDSParams.idsModeAsString[i];
			}

			System.err.println("Invalid test mode, should be " + z);
			return IDSParams.idsModeAsString[2];  // default detect online

		}

		public CSLRunningArgs setTestParam(boolean testparam) {
			this.testparam = testparam;
			return this;
		}

		public boolean isHasIdsRunner() {
			return startIdsRunner;
		}


		public CSLRunningArgs setHasIdsRunner(boolean hasIdsRunner) {
			this.startIdsRunner = hasIdsRunner;
			return this;
		}

		public boolean hasDatabaseDir() {
			return databasedir != "";
		}

		public boolean hasIdsMode() {
			return idsMode >= 0;
		}

		public boolean hasDataDir() {
			return dataDir != "";
		}

		public boolean hasLogDir() {
			return logDir != "";
		}

		public boolean hasDataSetForLearning() {
			return dataSetForLearning != "";
		}

		public boolean hasDirForLearning() {
			return dirForLearning != "";
		}

		public boolean hasDataSetForRecording() {
			return dataSetForRecording != "";
		}

		public boolean hasDirForRecording() {
			return dirForRecording != "";
		}

		public boolean hasDataSetForDetectionOffLine() {
			return dataSetForDetectionOffline != "";
		}

		public CSLRunningArgs setConfigFile(String s) {
			this.configFile = s;
			return this;
		}

		public boolean hasDirForDetectionOffLine() {
			return dirForDetectionOffline != "";
		}

		public String getPathOfConfigFile() {
			File f = new File(configFile);
			String s = f.getParentFile().toString();
			if (s == null) return "";
			return s;
		}

		public boolean hasError() {
			return !error.isEmpty();
		}


	public static String readResourceAsString(String resourcePath) {
		try (InputStream inputStream = CSLRunningArgs.class.getClassLoader().getResourceAsStream(resourcePath);
			 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

			if (inputStream == null) {
				return null;
			}

			StringBuilder content = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line).append("\n");
			}
			return content.toString();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String readFileAsString(String filePath) {
		try {
			return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
		public CSLRunningArgs parseArgs(String[] args) {


			boolean firstConfig = false;
			String USER_DIR = "-u:";
			String DATA_DIR = "-d:";
			for (int i = 0; i < args.length; i++) {
				if (args[i].toLowerCase().endsWith(".json")) {
					if (firstConfig) {
						configFile = args[i];
						firstConfig = false;
					} else
						System.err.println("Warning : multiple paramsfile " + args[i]);
				} else if (args[i].toLowerCase().startsWith(USER_DIR)) {
					userDir = args[i].toLowerCase().substring(USER_DIR.length());
					userDirDefault = false;
				} else if (args[i].toLowerCase().startsWith(DATA_DIR)) {
					dataDir = args[i].toLowerCase().substring(DATA_DIR.length());
				} else if (args[i].compareTo("-verbose") == 0) {
					setVerbose(true);
				} else if (args[i].compareTo("-debug") == 0) {
					setDebug(true);
				} else if (args[i].toLowerCase().startsWith("--testmode")) {
					boolean test = true;
					String sx = args[i].toLowerCase().substring("--testmode".length());
					if (sx.startsWith(":")) sx = sx.substring(1);
					System.out.println(sx);
					sx = validTestModeparam(sx);
					if (sx.compareToIgnoreCase("false") == 0) test = false;
					if (sx.compareToIgnoreCase("0") == 0) test = false;
					setTestParam(test);

					String z = "";
					sx = "<" + sx + ">";
					for (int j = 0; j < sx.length(); j++) z = z + "=";
					System.err.println("!=====================" + z + "!\n" +
							"!WARNING : TEST MODE " + sx + " !\n" +
							"!=====================" + z + "!");
				} else {
					System.out.println("Invalid parameter:" + args[i]);
				}
			}


			if (dataDir.startsWith(".")) {
				dataDir = dataDir.substring(1);
				if (dataDir.isEmpty())
					dataDir = userDir;
				else
					dataDir = userDir + File.separator + dataDir;
			}


			// LIRE LA CONFIG DE CONTEXT

			String configContent = readResourceAsString(configFile);

			if (configContent == null) {
				// If not found, try reading from the file system paths
				String[] fallbackPaths = {
						getUserDir() + File.separator + "src/main/resources/" + configFile,
						getUserDir() + File.separator + "configuration_template/" + configFile
				};

				for (String path : fallbackPaths) {
					configContent = readFileAsString(path);
					if (configContent != null) {
						configFile = path;
						break;
					}
				}
			}

			if (configContent != null) {
				logger.trace("Config file content:\n" + configContent);
			} else {
				System.err.println("Cannot find config file: " + configFile);
			}

			return this;
		}

}
