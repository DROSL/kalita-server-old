package kalita;

//Some of them are useless in the final demo
import org.webbitserver.WebServer;
import org.webbitserver.WebServers;
import org.webbitserver.handler.StaticFileHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;

import java.nio.charset.Charset;

import java.net.InetSocketAddress;

import java.util.Set;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;

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
		server.createContext("/tts", new PostHandler());
		server.setExecutor(null);
		server.start();

		//Old WebSocket
		/*WebServer webServer = WebServers.createWebServer(8080);
		webServer.add(new StaticFileHandler("/static-files"));
		webServer.add("/kalitatts", new WebSocketHandler());
		webServer.start();
		System.out.println("Server started on port " + webServer.getPort());*/
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

	static class PostHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange he) throws IOException {
			// parse request
			Map<String, Object> parameters = new HashMap<String, Object>();
			InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
			BufferedReader br = new BufferedReader(isr);
			String query = br.readLine();

			he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			he.getResponseHeaders().add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-*, Accept, Authorization");
			he.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
			he.getResponseHeaders().add("Access-Control-Allow-Methods", "POST,GET");
			he.getResponseHeaders().add("Max-Age-Seconds", "3000");

			// send response
			try {
				String response = tts(query);
				he.sendResponseHeaders(200, response.length());
				OutputStream os = he.getResponseBody();
				os.write(response.getBytes());
				os.close();
			} catch (MaryConfigurationException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	static String tts(String inputText) throws MaryConfigurationException {
		String voice = "cmu-slt-hsmm";
		// get output option
		String outputFileName = "output.wav";

		// get input
		LocalMaryInterface mary = null;
		try {
			mary = new LocalMaryInterface();
		} catch (MaryConfigurationException e) {
			System.err.println("Could not initialize MaryTTS interface: " + e.getMessage());
		}

			// Set voice / language
			mary.setVoice(voice);

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
			File f = new File("./output.wav");
			byte[] byteArray = FileUtils.readFileToByteArray(f);
			System.out.println("Audio generated for message: " + inputText);
			f.delete();
			return Base64.getEncoder().encodeToString(byteArray);
		} catch (IOException e) {
			System.err.println("Could not generate audio." + "\n" + e.getMessage());
			System.exit(1);
		}
		return null;
	}
}
