package de.fhac.mazenet.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;

import de.fhac.mazenet.server.generated.AwaitMoveMessageData;
import de.fhac.mazenet.server.generated.Errortype;
import de.fhac.mazenet.server.generated.MazeCom;
import de.fhac.mazenet.server.generated.MazeComMessagetype;
import de.fhac.mazenet.server.generated.MoveMessageData;
import de.fhac.mazenet.server.generated.WinMessageData;
import de.fhac.mazenet.server.generated.WinMessageData.Winner;
import de.fhac.mazenet.server.networking.MazeComMessageFactory;
import de.fhac.mazenet.server.networking.XmlInputStream;
import de.fhac.mazenet.server.networking.XmlOutputStream;

public class Launcher {

	private Socket socket;
	private XmlInputStream inputStream;
	private XmlOutputStream outputStream;
	private String name;
	private int id = -2;
	private static Launcher instance;
	private MyAlgorithm mazeSolver;
	private boolean isRunning = true;

	private Launcher(String name, String address, int port)
			throws IOException, UnknownHostException, UnmarshalException {
		socket = new Socket(address, port);
		inputStream = new XmlInputStream(socket.getInputStream());
		outputStream = new XmlOutputStream(socket.getOutputStream());
		this.name = name;
	}

	public static Launcher getInstance(String name, String address, int port)
			throws IOException, UnknownHostException, UnmarshalException {
		if (instance == null)
			instance = new Launcher(name, address, port);
		return instance;
	}

	// Methode zum Einloggen beim Server.
	public void login() throws IOException, JAXBException {
		MazeCom login = MazeComMessageFactory.createLoginMessage(name);
		outputStream.write(login);
		MazeCom loginResponse = inputStream.readMazeCom();
		switch (loginResponse.getMessagetype()) {
		case LOGINREPLY:
			id = loginResponse.getLoginReplyMessage().getNewID();
			mazeSolver = new MyAlgorithm(id);
			break;
		case ACCEPT:
			System.out.println("Fehler beim Einloggen: Ungültige Nachricht");
			System.exit(1);
		case DISCONNECT:
			System.out.println("Fehler beim Einloggen: Zu viele Anmeldeversuche");
			System.exit(1);
		default:
			System.out.println("Fehler beim Einloggen: Unbekannter Nachrichtentyp " + loginResponse.getMessagetype());
			System.exit(1);
		}
	}

	public void play() {
		long started = System.currentTimeMillis();
		while (isRunning) {
			try {
				MazeCom receivedMazeCom = inputStream.readMazeCom();
				switch (receivedMazeCom.getMessagetype()) {
				case AWAITMOVE:
					awaitMove(receivedMazeCom);
					break;
				case ACCEPT:
					accept(receivedMazeCom.getAcceptMessage().getErrortypeCode());
					break;
				case DISCONNECT:
					System.out.println("Sie wurden getrennt");
					disconnect(receivedMazeCom.getDisconnectMessage().getErrortypeCode());
					break;
				case MOVEINFO:
					System.out.println("MoveInfo erhalten");
					break;
				case WIN:
					WinMessageData win = receivedMazeCom.getWinMessage();
					Winner winner = win.getWinner();
					if (winner.getId() == this.id)
						System.out.println("Sie sind der Gewinner " + winner.getId() + ":" + winner.getValue());
					else
						System.out.println(winner.getId() + ":" + winner.getValue() + " hat das Spiel gewonnen");
					isRunning = false;
					break;
				default:
					System.out.println("Unbekannter Nachrichtentyp: " + receivedMazeCom.getMessagetype());
					break;
				}
			} catch (IOException e) {
				System.out.println("Verbindungsfehler");
				System.out.println("Kann AWAITMOVE nicht erhalten");
				isRunning = false;
			} catch (UnmarshalException e) {
				System.out.println("Unklare Informationen, Umwandlungsfehler");
				isRunning = false;
			}
		}
		System.out.println("Die Laufzeit beträgt " + (System.currentTimeMillis() - started) + " ms");
	}

	// Verarbeitet die Annahmeantwort basierend auf dem angegebenen Fehlertyp.
	public void accept(Errortype errortype) {
		switch (errortype) {
		case NOERROR:
			System.out.println("Bewegung");
			break;
		case AWAIT_MOVE:
			System.out.println("Falsche Nachricht, Bewegung nicht erlaubt.");
			break;
		case ILLEGAL_MOVE:
			System.out.println("Regelverstoß, unerlaubte Bewegung.");
			break;
		default:
			System.out.println("Unbekannter Fehler: " + errortype);
			break;
		}
	}

	// Verarbeitet die Trennung basierend auf dem angegebenen Fehlertyp.
	public void disconnect(Errortype errortype) {
		switch (errortype) {
		case TOO_MANY_TRIES:
			System.out.println("Zu viele Versuche. Vom Server abgelehnt.");
			System.exit(1);
		default:
			System.out.println("Spielende.");
			System.exit(0);
		}
	}

	// Verarbeitet das AWAITMOVE-Event und erzeugt den nächsten Zug.
	public void awaitMove(MazeCom receivedMazeCom) throws IOException {
		AwaitMoveMessageData awaitMove = receivedMazeCom.getAwaitMoveMessage();
		MoveMessageData move = mazeSolver.solve(awaitMove);
		MazeCom mazeComToSend = new MazeCom();
		mazeComToSend.setId(id);
		mazeComToSend.setMessagetype(MazeComMessagetype.MOVE);
		mazeComToSend.setMoveMessage(move);
		outputStream.write(mazeComToSend);
	}

	// * Konfigurationsdatei
	private static final File FILE = new File("maze.conf");
	private static final String HOSTNAME_ATTRIBUTE = "hostname";
	private static final String PORT_ATTRIBUTE = "port";
	private static final String NAME_ATTRIBUTE = "name";
	private static Properties properties = new Properties();

	// * Erstellt die Konfigurationsdatei, falls sie nicht vorhanden ist.
	public static void createConfigFileIfNotExist() {
		if (!FILE.exists()) {
			try {
				FILE.createNewFile();
			} catch (IOException e) {
				System.out.println("Konnte keine Konfigurationsdatei erstellen. Möglicherweise fehlen Betriebssystemberechtigungen.");
				System.exit(1);
			}
			properties.put(HOSTNAME_ATTRIBUTE, "127.0.0.1");
			properties.put(PORT_ATTRIBUTE, "5123");
			properties.put(NAME_ATTRIBUTE, "asmae");
			try {
				properties.save(new FileOutputStream(FILE), "");
			} catch (FileNotFoundException e) {
			}
			System.out.println(
					"Standardkonfigurationsdatei erstellt. Konfigurieren Sie Ihre automatische Konfiguration, indem Sie in maze.conf starten.");
		}
	}

	// * Lädt die Konfigurationswerte aus der Datei.
	public static void loadConfigFile() throws FileNotFoundException {
		try {
			properties.load(new FileInputStream(FILE));
		} catch (FileNotFoundException e) {
			System.out.println("Konfigurationsdatei nicht gefunden.");
			System.exit(1);
		} catch (IOException e) {
			System.out.println("Fehler beim Lesen der Datei.");
			System.exit(1);
		}
	}

	public static void main(String[] args) throws FileNotFoundException {
		createConfigFileIfNotExist();
		loadConfigFile();
		
		int newPort = 0;
		String newAddress = "", newName = "";
		
		newAddress = properties.getProperty(HOSTNAME_ATTRIBUTE);
		newPort = Integer.parseInt(properties.getProperty(PORT_ATTRIBUTE));
		newName = properties.getProperty(NAME_ATTRIBUTE);
		
		Launcher client;
		try {
			client = Launcher.getInstance(newName, newAddress, newPort);
			client.login();
			client.play();
		} catch (UnmarshalException | IOException e) {
			e.printStackTrace();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
}
