package de.fhac.mazenet.client;

import java.util.ArrayList;
import java.util.List;

import de.fhac.mazenet.server.game.Position;
import de.fhac.mazenet.server.generated.MoveMessageData;

// Die Path-Klasse stellt den Weg eines Spielers zum Schatz dar
public class Path {

	private ArrayList<MoveMessageData> moves = new ArrayList<MoveMessageData>(); // Die Variable "moves" ist eine Liste von Bewegungen (MoveMessageData), aus denen sich der Pfad zusammensetzt
	private Position spielerPosition;
	private Position schatzPosition;

	public Path(Position spielerPosition, Position schatzPosition) {
		super();
		this.spielerPosition = spielerPosition;
		this.schatzPosition = schatzPosition;
	}

	
	public void emfiler(MoveMessageData move) {
		this.moves.add(move);
	}

	
	public int getDistance() {
		return Math.abs(spielerPosition.getCol() - schatzPosition.getCol())
				+ Math.abs(spielerPosition.getRow() + schatzPosition.getRow());
	}

	
	public MoveMessageData defiler() {
		if (moves.size() != 0) {
			MoveMessageData move = moves.get(0);
			moves.remove(0);
			return move;
		}
		return null;
	}

	
	public boolean isEmpty() {
		return moves.size() == 0;
	}

	
	public static Path getShortestPath(List<Path> paths) {
		if (paths.size() == 1) {
			return paths.get(0);
		} else {
			Path first = paths.get(0);
			for (int i = 1; i < paths.size(); i++) {
				Path temp = paths.get(i);
				if (first.getDistance() > temp.getDistance())
					first = paths.get(i);
				else if (first.getDistance() == temp.getDistance()) {
					if (first.getMovesSize() > temp.getMovesSize())
						first = temp;
				}
			}
			return first;
		}
	}

	// Die folgenden Methoden sind Zugriffs- und Mutatorfunktionen für die Variablen der Klasse Path
	public int getMovesSize() {
		return moves.size();
	}

	public ArrayList<MoveMessageData> getMoves() {
		return moves;
	}

	public void setMoves(ArrayList<MoveMessageData> moves) {
		this.moves = moves;
	}

	public Position getPlayerPosition() {
		return spielerPosition;
	}

	public void setPlayerPosition(Position spielerPosition) {
		this.spielerPosition = spielerPosition;
	}

	public Position getTreasurePosition() {
		return schatzPosition;
	}

	public void setTreasurePosition(Position schatzPosition) {
		this.schatzPosition = schatzPosition;
	}

}
