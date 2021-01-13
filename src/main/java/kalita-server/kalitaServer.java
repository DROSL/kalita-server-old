package kalita;

//Some of them are useless in the final demo
import org.webbitserver.WebServer;
import org.webbitserver.WebServers;
import org.webbitserver.handler.StaticFileHandler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;

import java.nio.charset.Charset;

import java.net.InetSocketAddress;

import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;

import java.util.Set;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import marytts.LocalMaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.MaryAudioUtils;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class kalitaServer {

	public static void main(String[] args) throws Exception {
		int port = 8080;
		HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
		System.out.println("server started at " + port);
		//server.createContext("/", new RootHandler());
		server.createContext("/speak", new GetHandler());
		server.setExecutor(null);
		server.start();
	}

	//Maybe serve the demo from here?
	static class RootHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange he) throws IOException {
			String response = "<h1>Server started successfully.</h1>";
			he.sendResponseHeaders(200, response.length());
			OutputStream os = he.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
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
						he.sendResponseHeaders(200, response.length);
						he.getResponseHeaders().add("Content-Range", "bytes " + from + "-" + to + "/" + length);
						he.getResponseHeaders().add("Content-Length", String.valueOf(response.length));

					} else {
						he.sendResponseHeaders(200, response.length);
					}

					he.getResponseHeaders().add("Content-Disposition", "attachment; filename=" + "speak.wav");

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
}
