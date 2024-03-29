package kalita;

//Some of them are useless in the final demo
import org.webbitserver.WebServer;
import org.webbitserver.WebServers;
import org.webbitserver.handler.StaticFileHandler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.File;
import java.io.*;

import java.lang.*;

import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;

import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.URLDecoder;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.cert.X509Certificate;
import java.security.KeyStore;


import java.util.Set;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Properties;

import javax.sound.sampled.AudioInputStream;

import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.*;

import marytts.LocalMaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.MaryAudioUtils;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.github.pemistahl.lingua.api.*;
import com.github.pemistahl.lingua.api.Language.*;

public class kalitaServer {

	public static void main(String[] args) throws Exception {
		kalitaServer server = new kalitaServer();

		String server_address = "127.0.0.1";
		int port = 8080;

		Map<String, String> config = server.getConfig();
		for (String key : config.keySet()){
			if(key.equals("server_address")) {
				server_address = config.get(key);
			}
			if(key.equals("port")) {
				port = Integer.parseInt(config.get(key));
			}
		}
		InetSocketAddress address = new InetSocketAddress(server_address, port);
		HttpServer httpServer = HttpServer.create(address, 0);
		System.out.println("Http server started at " + address);
		httpServer.createContext("/speak", new GetHandler());
		httpServer.setExecutor(null);
		httpServer.start();
	}

	static class GetHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange he) throws IOException {
			// parse request
			Map<String, Object> parameters = new HashMap<String, Object>();
			URI requestedUri = he.getRequestURI();
			String query = requestedUri.getRawQuery();

			Map<String,List<String>> requestHeaders = he.getRequestHeaders();

			he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			he.getResponseHeaders().add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-*, Accept, Authorization");
			he.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
			he.getResponseHeaders().add("Access-Control-Allow-Methods", "POST,GET");
			he.getResponseHeaders().add("Max-Age-Seconds", "3000");
			he.getResponseHeaders().add("Accept-Ranges", "bytes");

			try {
				String range = "";
				for (String key : requestHeaders.keySet()){
					if(key.equals("Range")) {
						range = requestHeaders.get(key).get(0);
					}
				}

				String text = "";
				String language = "";
				Map<String, String> parameterMap = splitQuery(query);
				for (String key : parameterMap.keySet()){
					if(key.equals("text")) {
						text = parameterMap.get(key);
					}
					if(key.equals("language")) {
						language = parameterMap.get(key);
					}
				}

				if(text.length() > 0) {
					final LanguageDetector detector = LanguageDetectorBuilder.fromLanguages(Language.ENGLISH, Language.FRENCH, Language.GERMAN).build();
					final Language detectedLanguage = detector.detectLanguageOf(text);
					System.out.println("Detected Language: " + detectedLanguage.toString());
					if(language.equals("")) {
						language = detectedLanguage.toString();
					}
					byte[] response = tts(text, language);
					int length = response.length;
					if(!range.equals("")) {
						String[] ranges = range.substring("bytes=".length()).split("-");
						int from = Integer.valueOf(ranges[0]);
						int to = response.length-1;
						if(ranges.length > 1) {
							to = Integer.valueOf(ranges[1]);
						}
						response = Arrays.copyOfRange(response, from, to);
						if(from > 44) {
							response = addWavHeader(response);
						}
						he.getResponseHeaders().add("Content-Range", "bytes " + from + "-" + to + "/" + length);
						he.getResponseHeaders().add("Content-Length", String.valueOf(response.length));

					}

					DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss");  
					LocalDateTime time = LocalDateTime.now();  
					he.getResponseHeaders().add("Content-Disposition", "attachment; filename=" + dtf.format(time) + "-speak.wav");

					he.sendResponseHeaders(200, response.length);
					OutputStream os = he.getResponseBody();
					os.write(response);
					os.close();
				} else {
					String response = "Please provide a text string that should be converted to audio.";
					he.sendResponseHeaders(400, response.length());

					OutputStream os = he.getResponseBody();
					os.write(response.getBytes());
					os.close();
				}
				

			} catch (MaryConfigurationException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	private static byte[] addWavHeader(byte[] bytes) throws IOException {

		ByteBuffer bufferWithHeader = ByteBuffer.allocate(bytes.length + 44);
		bufferWithHeader.order(ByteOrder.LITTLE_ENDIAN);
		bufferWithHeader.put("RIFF".getBytes());
		bufferWithHeader.putInt(bytes.length + 36);
		bufferWithHeader.put("WAVE".getBytes());
		bufferWithHeader.put("fmt ".getBytes());
		bufferWithHeader.putInt(16);
		bufferWithHeader.putShort((short) 1);
		bufferWithHeader.putShort((short) 1);
		bufferWithHeader.putInt(16000);
		bufferWithHeader.putInt(32000);
		bufferWithHeader.putShort((short) 2);
		bufferWithHeader.putShort((short) 16);
		bufferWithHeader.put("data".getBytes());
		bufferWithHeader.putInt(bytes.length);
		bufferWithHeader.put(bytes);
		return bufferWithHeader.array();
	}

	public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
		Map<String, String> query_pairs = new LinkedHashMap<String, String>();
		String[] pairs = query.split("&");

		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
		}
		return query_pairs;
	}

	static byte[] tts(String inputText, String language) throws MaryConfigurationException {
		String voice = "cmu-slt-hsmm";
		String voiceLanguage = language.toLowerCase();

		if(voiceLanguage.equals("german")) {
			voice = "dfki-pavoque-neutral-hsmm";
		} else if (voiceLanguage.equals("french")) {
			voice = "upmc-pierre-hsmm";
		} else {
			voice = "cmu-slt-hsmm";
		}
		
		System.out.println(voice);
		// get output option
		String outputFileName = "speak.wav";

		// get input
		LocalMaryInterface mary = null;
		try {
			mary = new LocalMaryInterface();
			mary.setVoice(voice);
		} catch (MaryConfigurationException e) {
			System.err.println("Could not initialize MaryTTS interface: " + e.getMessage());
		}

		// synthesize
		AudioInputStream audio = null;
		try {
			audio = mary.generateAudio(inputText);
		} catch (SynthesisException e) {
			System.err.println("Synthesis failed: " + e.getMessage());
			System.exit(1);
		}

		// write to output
		double[] samples = MaryAudioUtils.getSamplesAsDoubleArray(audio);
		try {
			MaryAudioUtils.writeWavFile(samples, outputFileName, audio.getFormat());
			File f = new File("./speak.wav");
			byte[] byteArray = FileUtils.readFileToByteArray(f);
			System.out.println("Audio generated for message(" + voice + "): " + inputText);
			f.delete();
			return byteArray;
		} catch (IOException e) {
			System.err.println("Could not generate audio." + "\n" + e.getMessage());
			System.exit(1);
		}
		return null;
	}
 
	private Map<String, String> getConfig() throws IOException {
		Map<String, String> properties = new HashMap<String, String>();

		try {
			Properties prop = new Properties();
			String configFileName = System.getProperty("user.dir") + "\\config.properties";

			InputStream input = new FileInputStream(configFileName);

			if (input != null) {
				prop.load(input);
			} else {
				throw new FileNotFoundException("Config file '" + configFileName + "' not found in the classpath");
			}

			// get the property value and print it out
			properties.put("server_address", prop.getProperty("server_address"));
			properties.put("port", prop.getProperty("port"));
			input.close();
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
		return properties;
	}
}
