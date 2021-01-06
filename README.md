<p align="center">
  <img src="https://user-images.githubusercontent.com/50206261/102134585-07456f80-3e57-11eb-9a90-d9c81ee48c1d.png" alt="Logo Kalita" width="200">
</p>

# kalita-server

**Kalita** is text-to-speech software with a special focus on data minimization and protection. We do not collect any personal data, do not set tracking cookies and do not outsource our service to third-party cloud solutions. The speech synthesis takes place on-premises on your own server and still offers all the convenience of a conventional readspeaker.

- [**kalita-server**](https://github.com/azmke/kalita-server) is a server written in Java that provides the speech synthesis.
- [**kalita-js**](https://github.com/azmke/kalita-js) is a JavaScript client for integration into a website that provides a graphical user interface.

## Prequesites

- Have the same JRE and JDK version installed *(jdk1.8.0_271 and jre1.8.0_271 used for testing)*
- Have the environment variable **JAVA_HOME** set to the path of the JDK mentioned in the previous step*(e.g. C:\Program Files\Java\jdk1.8.0_271)*

## How to use

1. Build the server.

```sh
cd kalita-server
gradlew shadowJar
```

2. Serve the build folder on a server by running the **kalita-server-1.0.jar** from **build/libs/**:
```sh
java -jar kalita-server-1.0.jar
```

2. Open the file *Kalita Server Demo.html** in your browser.
3. Type any text you want to be read out loud into the textbox.
4. Click the **TTS** button.

## Disclaimer

THIS SOURCE CODE IS PART OF A PROJECT WORK FOR THE MODULES ["IT SECURITY AND DIGITAL SELF-DEFENSE" (MMDAP)](https://omen.cs.uni-magdeburg.de/itiamsl/deutsch/lehre/ws-20-21/mmdap.html) AND ["KEY AND METHODOLOGICAL COMPETENCIES IN IT SECURITY" (SMK-ITS)](https://omen.cs.uni-magdeburg.de/itiamsl/deutsch/lehre/ws-20-21/smkits.html) IN THE WINTER SEMESTER 2020/21 AT THE OTTO VON GUERICKE UNIVERSITY MAGDEBURG UNDER THE FACULTY SUPERVISION OF PROF. DR.-ING. JANA DITTMANN, PROF. DR.-ING. CLAUS VIELHAUER, DR.-ING. STEFAN KILTZ AND ROBERT ALTSCHAFFEL.
