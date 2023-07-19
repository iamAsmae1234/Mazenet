package de.fhac.mazenet.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import de.fhac.mazenet.server.game.Board;
import de.fhac.mazenet.server.game.Position;
import de.fhac.mazenet.server.generated.AwaitMoveMessageData;
import de.fhac.mazenet.server.generated.BoardData;
import de.fhac.mazenet.server.generated.CardData;
import de.fhac.mazenet.server.generated.MoveMessageData;
import de.fhac.mazenet.server.generated.PositionData;
import de.fhac.mazenet.server.generated.Treasure;

public class MyAlgorithm {

	private int playerId;
	private Path path = null; // Die Variable „path“ wird verwendet, um den aktuellen Weg des Spielers zum Schatz zu speichern.

	public MyAlgorithm(int id) {
		System.out.println("Smart algorithm used");
		this.playerId = id;
	}

		
	public Path move(Board boardPrincipal, Treasure treasure) {

		List<Position> potentialShiftMoves = Position.getPossiblePositionsForShiftcard();
		potentialShiftMoves.remove(boardPrincipal.getForbidden());
		List<MoveMessageData> possibleMoves = new ArrayList<MoveMessageData>();

		List<Path> paths = new ArrayList<Path>();
		for (Position position : potentialShiftMoves) {
			List<CardData> orientedShiftCards = Utils.getAllOrientationPossibilities(boardPrincipal.getShiftCard());
			for (CardData orientedShiftCard : orientedShiftCards) {
				MoveMessageData potentialMove = new MoveMessageData();
				potentialMove.setShiftPosition(position);
				potentialMove.setShiftCard(orientedShiftCard);
				Board boardNext = boardPrincipal.fakeShift(potentialMove);
				Position playerPosition = boardNext.findPlayer(this.playerId);
				potentialMove.setNewPinPos(playerPosition);
				possibleMoves.add(potentialMove);

				PositionData treasurePositionData = boardNext.findTreasure(treasure);
				if (treasurePositionData == null)
					continue;
				Position treasurePosition = new Position(treasurePositionData);
				if (boardNext.pathPossible(potentialMove.getNewPinPos(), treasurePosition)) {
					potentialMove.setNewPinPos(treasurePosition);
					Path path = new Path(playerPosition, treasurePosition);
					path.emfiler(potentialMove);
					paths.add(path);
				}
			}
		}

		if (paths.size() != 0) {
			return Path.getShortestPath(paths);
		}

		List<MoveMessageData> thirdTree = new ArrayList<>();
		HashMap<MoveMessageData, MoveMessageData> oldmoves = new HashMap<MoveMessageData, MoveMessageData>();
		for (MoveMessageData myMove : possibleMoves) {
			Board board = boardPrincipal.fakeShift(myMove);
			Position playerPosition;
			for (Position position : potentialShiftMoves) {
				List<CardData> orientedShiftCards = Utils.getAllOrientationPossibilities(board.getShiftCard());
				for (CardData orientedShiftCard : orientedShiftCards) {
					MoveMessageData potentialMove = new MoveMessageData();
					potentialMove.setShiftPosition(position);
					potentialMove.setShiftCard(orientedShiftCard);
					Board boardNext = board.fakeShift(potentialMove);
					playerPosition = boardNext.findPlayer(this.playerId);
					potentialMove.setNewPinPos(playerPosition);
					List<Position> reachablePositions = boardNext
							.getAllReachablePositions(boardNext.findPlayer(this.playerId));
					PositionData treasurePositionData = boardNext.findTreasure(treasure);
					thirdTree.add(potentialMove);
					oldmoves.put(potentialMove, myMove);
					if (treasurePositionData == null)
						continue;
					Position treasurePosition = new Position(treasurePositionData);
					if (reachablePositions.contains(treasurePosition)) {
						Path path = new Path(playerPosition, treasurePosition);
						path.emfiler(myMove);
						path.emfiler(potentialMove);
						paths.add(path);
					}
				}
			}
		}
		if (paths.size() != 0) {
			return Path.getShortestPath(paths);
		} else {
			Path path = new Path(boardPrincipal.findPlayer(this.playerId),
					new Position(boardPrincipal.findTreasure(treasure)));
			List<MoveMessageData> moves = Utils.getAllMoveMessageData(boardPrincipal, playerId);
			MoveMessageData move = moves.get(new Random().nextInt(moves.size()));

			MoveMessageData temp = Utils.getClosePointToTarget(boardPrincipal, treasure, moves, this.playerId);
			if (temp != null)
				path.emfiler(temp);
			else
				path.emfiler(move);
			return path;
		}
	}

		
	public boolean getIfPathstillexist(Board board, Treasure treasure, Path path) {
		Board boardNext = board;
		for (MoveMessageData move : path.getMoves()) {
			board.fakeShift(move);
		}

		Position positionPlayer = boardNext.findPlayer(this.playerId);
		List<Position> reachablePositions = boardNext.getAllReachablePositions(positionPlayer);
		PositionData treasurePositionData = boardNext.findTreasure(treasure);
		if (treasurePositionData == null)
			return false;
		Position treasurePosition = new Position(treasurePositionData);
		if (reachablePositions.contains(treasurePosition)) {
			return true;
		}
		return false;
	}

		
	public MoveMessageData solve(AwaitMoveMessageData awaitMove) {
		BoardData boardData = awaitMove.getBoard();
		Board board = new Board(boardData);
		Treasure treasure = awaitMove.getTreasureToFindNext();
		MoveMessageData nextMove = null;
		if (path != null) {
			boolean check = getIfPathstillexist(board, treasure, path);
			nextMove = path.defiler();
			if (path.isEmpty())
				path = null;
			if (check) {
				return nextMove;
			} else {
				return startAlgorithm(board, treasure);
			}

		} else {
			return startAlgorithm(board, treasure);
		}
	}

		// Diese Methode wird aufgerufen, um einen neuen Labyrinth-Lösungsalgorithmus zu starten.
	public MoveMessageData startAlgorithm(Board board, Treasure treasure) {
		MoveMessageData nextMove = null;
		this.path = move(board, treasure);
		nextMove = path.defiler();
		if (path.isEmpty())
			path = null;
		return nextMove;
	}
}
